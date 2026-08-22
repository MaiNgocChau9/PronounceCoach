"""Pronunciation assessment: Wav2Vec2 embeddings, phonemization, alignment and scoring."""

import logging
import re
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from contextlib import contextmanager
from functools import lru_cache

import Levenshtein
import librosa
import numpy as np
import torch
from phonemizer import phonemize

from . import audio, phones
from .device import env_flag, get_device, inference_autocast, optimize_model
from .languages import DEFAULT_LANGUAGE, get_language

logger = logging.getLogger(__name__)

MODEL_NAME = "facebook/wav2vec2-large-960h"
SAMPLING_RATE = 16000

# Threshold above which a word is considered mispronounced:
# ratio of edited phonemes over the number of expected phonemes.
WORD_ERROR_THRESHOLD = 0.4


@contextmanager
def _stage(name):
    """Log the duration of a pipeline stage when ``OPENPRONOUNCE_PROFILE=1``."""
    if not env_flag("OPENPRONOUNCE_PROFILE", False):
        yield
        return
    start = time.perf_counter()
    try:
        yield
    finally:
        logger.info("[profile] %-12s %6.0f ms", name, (time.perf_counter() - start) * 1000)


# ---------------------------------------------------------------------------
# Models (loaded lazily, once)
# ---------------------------------------------------------------------------

_models_cache = {}
_models_lock = threading.Lock()


def _load_models(model_name=MODEL_NAME):
    """Load a Wav2Vec2 processor and CTC model on first use (cached per checkpoint).

    ``Wav2Vec2ForCTC`` embeds a ``Wav2Vec2Model`` (``.wav2vec2``), so a single
    checkpoint serves both transcription (CTC head) and embedding extraction.
    Double-checked locking keeps the parallel analysis stages from loading the
    same checkpoint twice.
    """
    if model_name not in _models_cache:
        with _models_lock:
            if model_name not in _models_cache:
                from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor

                logger.info("Loading %s", model_name)
                processor = Wav2Vec2Processor.from_pretrained(model_name)
                model_ctc = Wav2Vec2ForCTC.from_pretrained(model_name).to(get_device())
                model_ctc.eval()
                _models_cache[model_name] = processor, optimize_model(model_ctc)
    return _models_cache[model_name]


def _get_processor(lang=DEFAULT_LANGUAGE):
    return _load_models(get_language(lang).asr_model)[0]


def _get_model_ctc(lang=DEFAULT_LANGUAGE):
    return _load_models(get_language(lang).asr_model)[1]


def _get_model():
    """Embedding extractor: always the English checkpoint, whatever the language."""
    return _load_models(MODEL_NAME)[1].wav2vec2


@lru_cache(maxsize=32)
def _reference_embeddings(reference_file, sampling_rate=SAMPLING_RATE):
    """Wav2Vec2 embeddings of a synthesized reference sentence, cached per wav file.

    The synthesized wav itself is already cached on disk by :func:`audio.text2speech`;
    this saves re-encoding it through Wav2Vec2 every time the same sentence is assessed.
    The waveform gets the same silence-trim as the learner's recording, so the acoustic
    comparison stays apples-to-apples. The returned array is shared between calls and
    must not be mutated.
    """
    waveform = audio.load(reference_file, sr=sampling_rate)
    if env_flag("OPENPRONOUNCE_TRIM", True):
        waveform = audio.trim_silence(waveform, sr=sampling_rate)
    return extract_embeddings(waveform, sampling_rate)


# ---------------------------------------------------------------------------
# Embeddings & transcription
# ---------------------------------------------------------------------------

def extract_embeddings(audio_waveform, sampling_rate=SAMPLING_RATE):
    """Extract raw Wav2Vec2 hidden states, shape (frames, features)."""
    inputs = _get_processor()(audio_waveform, sampling_rate=sampling_rate, return_tensors="pt", padding=True)
    input_values = inputs.input_values
    if len(input_values.shape) > 2:
        input_values = input_values.squeeze(0)

    with torch.inference_mode(), inference_autocast():
        features = _get_model()(input_values.to(get_device())).last_hidden_state  # (batch, time, features)

    return features.float().squeeze(0).cpu().numpy()


def transcribe(audio_waveform, lang=DEFAULT_LANGUAGE):
    """Transcribe a 16 kHz waveform into text with the Wav2Vec2 CTC model of ``lang``.

    The English model emits upper-case text; the other checkpoints emit lower-case.
    """
    processor = _get_processor(lang)
    inputs = processor(audio_waveform, sampling_rate=SAMPLING_RATE, return_tensors="pt", padding=True)
    with torch.inference_mode(), inference_autocast():
        logits = _get_model_ctc(lang)(inputs.input_values.to(get_device())).logits
    predicted_ids = torch.argmax(logits, dim=-1).cpu()
    return processor.batch_decode(predicted_ids)[0]


def clean_transcription(text):
    """Lower-case, strip and keep only letters, apostrophes and single spaces."""
    text = text.lower().strip()
    text = re.sub(r"[^a-zA-Z' ]+", "", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


# ---------------------------------------------------------------------------
# Phonemization
# ---------------------------------------------------------------------------

_WORD_RE = re.compile(r"\b[\w']+\b")


def _words(text):
    """Split text into words, ignoring punctuation (mirrors the frontend logic)."""
    return _WORD_RE.findall(text)


@lru_cache(maxsize=4096)
def _phonemize_word(word, lang=DEFAULT_LANGUAGE):
    """Return the IPA phonemes of a single word (espeak first, festival as fallback).

    The word is lower-cased first: espeak spells upper-case words letter by letter
    ("IT" -> /aɪtiː/), which would flag every word of an upper-case sentence as wrong.
    """
    word = word.lower()
    language = get_language(lang).espeak
    for backend in ("espeak", "festival"):
        try:
            return tuple(
                phonemize(word, language=language, backend=backend, strip=True, preserve_punctuation=False).split()
            )
        except Exception as e:  # noqa: BLE001 - try the next backend
            logger.debug("phonemize(%r) failed with %s: %s", word, backend, e)
    return ()


def get_phonemes(text, lang=DEFAULT_LANGUAGE):
    """Return the flat list of phonemes for ``text``."""
    return get_phonemes_with_word_mapping(text, lang)[0]


def get_phonemes_with_word_mapping(text, lang=DEFAULT_LANGUAGE):
    """Return ``(phonemes, phoneme_to_word)`` where ``phoneme_to_word[i]`` is the word phoneme ``i`` belongs to."""
    phonemes = []
    phoneme_to_word = {}
    for word in _words(text):
        for phoneme in _phonemize_word(word, lang):
            phoneme_to_word[len(phonemes)] = word
            phonemes.append(phoneme)
    return phonemes, phoneme_to_word


def get_phoneme_embeddings(phoneme_seq):
    """Turn a phoneme sequence into a numeric (n, 1) array (codepoints) usable by DTW."""
    return np.array([ord(p) for p in phoneme_seq], dtype=float).reshape(-1, 1)


def _dtw(seq_a, seq_b):
    """Exact DTW between two sequences of 1-D points or feature vectors, via librosa.

    Replaces ``fastdtw(..., dist=euclidean)``, which paid one Python call per pair of
    frames: librosa computes the whole cost matrix vectorized and runs the recursion
    compiled (numba), orders of magnitude faster on Wav2Vec2 embeddings. The alignment
    is exact instead of approximate.

    Returns ``(total_cost, path)`` with path a list of ``(index_a, index_b)`` pairs;
    when either sequence is empty the distance falls back to the length of the other.
    """
    a = np.asarray(seq_a, dtype=np.float64)
    b = np.asarray(seq_b, dtype=np.float64)
    if not len(a) or not len(b):
        return float(max(len(a), len(b))), []
    cost_matrix, path = librosa.sequence.dtw(X=a.T, Y=b.T)
    return float(cost_matrix[-1, -1]), [(int(i), int(j)) for i, j in path]


def compare_pronunciation(expected_phonemes, actual_phonemes):
    """Edit distance between two phoneme sequences."""
    return float(Levenshtein.distance(list(expected_phonemes), list(actual_phonemes)))


# ---------------------------------------------------------------------------
# Comparison
# ---------------------------------------------------------------------------

def _align_phoneme_indices(expected_phonemes, transcribed_phonemes, lang=DEFAULT_LANGUAGE):
    """Map every expected phoneme index to the set of transcribed phoneme indices it aligns with.

    Uses the phonetically-priced alignment of :mod:`openpronounce.phones`, so close
    phones (voicing pairs, tense/lax vowels...) line up as one substitution instead of
    a delete-plus-insert pair that would smear errors onto neighbouring words.
    """
    alignment_map = [set() for _ in range(len(expected_phonemes))]

    for tag, i1, i2, j1, j2 in phones.weighted_opcodes(expected_phonemes, transcribed_phonemes, lang):
        if tag == "equal":
            for k, l in zip(range(i1, i2), range(j1, j2)):
                alignment_map[k].add(l)
        elif tag == "replace":
            # Spread the replaced range proportionally, so that "I'm" (3 phonemes)
            # can align with "I M" (4 phonemes) without mapping everything to everything.
            len_i, len_j = i2 - i1, j2 - j1
            for k in range(i1, i2):
                start_j = j1 + int((k - i1) * len_j / len_i)
                end_j = j1 + int((k - i1 + 1) * len_j / len_i)
                if start_j == end_j and len_j > 0:
                    alignment_map[k].add(min(start_j, j2 - 1))
                else:
                    alignment_map[k].update(range(start_j, end_j))
        elif tag == "insert":
            # Extra transcribed words ("hell no" for "hello") are attached to the
            # previous expected word so the feedback shows everything that was heard.
            k = i1 - 1 if i1 > 0 else i1
            if k < len(alignment_map):
                alignment_map[k].update(range(j1, j2))
        # "delete" (missing expected phonemes) produces no mapping.

    return alignment_map


def compare_transcriptions(transcription, text_reference, lang=DEFAULT_LANGUAGE):
    """Compare an automatic transcription with the expected text, word by word.

    Returns a JSON-serializable dict with distances, per-word errors and feedback.
    """
    transcription_clean = transcription.lower().strip()
    reference_clean = text_reference.lower().strip()

    word_distance = Levenshtein.distance(transcription_clean, reference_clean)

    expected_phonemes, _ = get_phonemes_with_word_mapping(text_reference, lang)
    transcribed_phonemes, transcribed_map = get_phonemes_with_word_mapping(transcription_clean, lang)

    # Global phoneme distance (DTW on codepoints, kept for the score and the charts)
    expected_seq = get_phoneme_embeddings(" ".join(expected_phonemes))
    transcribed_seq = get_phoneme_embeddings(" ".join(transcribed_phonemes))
    distance, _ = _dtw(expected_seq, transcribed_seq)

    alignment_map = _align_phoneme_indices(expected_phonemes, transcribed_phonemes, lang)

    errors = []
    words_with_errors = []
    current_phoneme_idx = 0

    for word in _words(text_reference):
        word_phonemes = _phonemize_word(word, lang)
        if not word_phonemes:
            continue

        word_indices = range(current_phoneme_idx, current_phoneme_idx + len(word_phonemes))
        current_phoneme_idx += len(word_phonemes)

        matched = set()
        for idx in word_indices:
            if idx < len(alignment_map):
                matched.update(alignment_map[idx])

        if not matched:
            errors.append({
                "position": word_indices.start,
                "expected": "".join(word_phonemes),
                "actual": "",
                "word": word,
                "actual_word": "",
            })
            words_with_errors.append(word)
            continue

        sorted_matched = sorted(matched)
        actual_words = []
        for tidx in sorted_matched:
            w = transcribed_map.get(tidx)
            if w is not None and (not actual_words or actual_words[-1] != w):
                actual_words.append(w)

        expected_seg = [expected_phonemes[i] for i in word_indices]
        actual_seg = [transcribed_phonemes[i] for i in sorted_matched]

        if Levenshtein.distance(expected_seg, actual_seg) > len(expected_seg) * WORD_ERROR_THRESHOLD:
            errors.append({
                "position": word_indices.start,
                "expected": "".join(expected_seg),
                "actual": "".join(actual_seg),
                "word": word,
                "actual_word": " ".join(actual_words),
            })
            words_with_errors.append(word)

    # De-duplicate while preserving order
    words_with_errors = list(dict.fromkeys(words_with_errors))

    expected_words = [w.lower() for w in _words(text_reference)]
    transcribed_words = [w.lower() for w in _words(transcription_clean)]
    word_error_rate = Levenshtein.distance(expected_words, transcribed_words) / max(1, len(expected_words))
    phoneme_error_rate = Levenshtein.distance(expected_phonemes, transcribed_phonemes) / max(1, len(expected_phonemes))

    feedback = _feedback(words_with_errors)

    expected_vector, transcribed_vector = align_sequences_dtw(expected_seq.tolist(), transcribed_seq.tolist())

    return {
        "word_distance": word_distance,
        "phoneme_distance": distance,
        "word_error_rate": round(word_error_rate, 4),
        "phoneme_error_rate": round(phoneme_error_rate, 4),
        "errors": errors,
        "feedback": feedback,
        "transcribe": transcription,
        "expected_vector": expected_vector.astype(float).tolist(),
        "transcribed_vector": transcribed_vector.astype(float).tolist(),
        "expected_phonemes": expected_phonemes,
        "transcribed_phonemes": transcribed_phonemes,
        "words_with_errors": words_with_errors,
    }


def _feedback(words_with_errors):
    feedback = "🔊 Feedback on your pronunciation:\n"
    if words_with_errors:
        feedback += "❌ You need to better pronounce these words: " + ", ".join(words_with_errors) + "\n"
    else:
        feedback += "✅ Your pronunciation is excellent! 🎉\n"
    return feedback


def align_sequences_dtw(seq1, seq2):
    """Align two 1-D numeric sequences (given as lists of ``[x]``) with DTW.

    Returns two arrays of identical length, so that curves of different
    durations can be plotted on top of each other.
    """
    if not len(seq1) or not len(seq2):
        return np.array([]), np.array([])

    _, path = _dtw(seq1, seq2)
    aligned_seq1 = np.array([seq1[i][0] for i, _ in path])
    aligned_seq2 = np.array([seq2[j][0] for _, j in path])
    return aligned_seq1, aligned_seq2


# Mean per-step DTW distance between Wav2Vec2 frames of the learner and of the TTS
# reference. Measured on the bundled samples: ~6 for a clean native-like reading,
# ~10 for a good reading by a different voice, ~12-15 for a wrong sentence. On
# speechocean762 the median is 11.5 (9.3 to 13.6 for the middle half).
ACOUSTIC_DISTANCE_GOOD = 6.0
ACOUSTIC_DISTANCE_BAD = 15.0
# The English embeddings put two native voices of another language further apart
# (9-13 instead of ~6.5), so each language carries its own "good" distance
# (``Language.acoustic_good``); the good-to-bad span stays the same.
ACOUSTIC_DISTANCE_SPAN = ACOUSTIC_DISTANCE_BAD - ACOUSTIC_DISTANCE_GOOD

# Calibrated on 500 speechocean762 utterances (see benchmarks/README.md): Spearman 0.65
# with the human total score. Heavier acoustic weights correlate slightly better (0.68
# at 0.7/0.2/0.1) but no longer punish a wrong sentence, so the phone and word terms
# keep the majority of the weight.
SCORE_WEIGHTS = {"acoustic": 0.3, "phonemes": 0.4, "words": 0.3}


def compute_pronunciation_score(acoustic_distance, phoneme_error_rate, word_error_rate, lang=DEFAULT_LANGUAGE):
    """Combine length-independent measures into a 0-100 score.

    - ``acoustic_distance``: mean per-step DTW distance between Wav2Vec2 embeddings,
      mapped linearly from ``Language.acoustic_good`` (100) to ``acoustic_good +``
      :data:`ACOUSTIC_DISTANCE_SPAN` (0); for English that is 6 to 15. 30%.
    - ``phoneme_error_rate``: edited phonemes / expected phonemes, 40%.
    - ``word_error_rate``: edited words / expected words, 30%.

    Every component is clipped to [0, 100] before weighting.
    """
    good = get_language(lang).acoustic_good
    acoustic_score = 100 * (1 - (acoustic_distance - good) / ACOUSTIC_DISTANCE_SPAN)
    phoneme_score = 100 * (1 - phoneme_error_rate)
    word_score = 100 * (1 - word_error_rate)

    clip = lambda x: min(100.0, max(0.0, x))  # noqa: E731
    final_score = (
        SCORE_WEIGHTS["acoustic"] * clip(acoustic_score)
        + SCORE_WEIGHTS["phonemes"] * clip(phoneme_score)
        + SCORE_WEIGHTS["words"] * clip(word_score)
    )
    return round(clip(final_score), 2)


def _word_and_phone_differences(audio_waveform, sampling_rate, text_reference, lang, use_phone_model):
    """Word transcription followed by phone recognition, sequentially.

    Both are heavy Wav2Vec2 passes, and torch already spreads each forward over every
    CPU core: running the two side by side would just split (and thrash) the same
    cores. They share one worker thread instead, while lighter stages (prosody,
    reference synthesis) run next to them.
    """
    with _stage("words"):
        transcription = transcribe(audio_waveform, lang)
        differences = compare_transcriptions(transcription, text_reference, lang)
    phone_result = None
    if use_phone_model:
        with _stage("phones"):
            recognition = phones.recognize_phones(audio_waveform, sampling_rate, lang=lang)
            phone_result = phones.compare_phones(recognition, text_reference, lang)
    return transcription, differences, phone_result


def _prosody_contours(audio_waveform, sampling_rate):
    """Pitch and energy contours of ``audio_waveform``."""
    with _stage("prosody"):
        energy = extract_energy(audio_waveform)
        f0 = interpolate_f0(extract_f0(audio_waveform, sampling_rate))
    return energy, f0


def compare_audio_with_text(audio_1, text_reference, sampling_rate=SAMPLING_RATE, use_phone_model=None,
                            lang=DEFAULT_LANGUAGE, use_prosody=True):
    """Assess how well ``audio_1`` (16 kHz mono waveform) pronounces ``text_reference``.

    ``lang`` selects the language (see :data:`openpronounce.languages.LANGUAGES`);
    it drives the reference TTS voice, the phonemizer and the word transcription model.
    Leading and trailing silence is trimmed first (``OPENPRONOUNCE_TRIM=off`` to keep
    everything), which speeds every model up and removes the junk phones silence produces.

    The two heavy Wav2Vec2 passes (words then phones) share a worker thread; prosody
    extraction and the TTS reference synthesis run in parallel threads next to them.
    The reference sentence is embedded once, then cached per sentence. Pass
    ``use_prosody=False`` to skip the pitch/energy contours altogether (the
    ``--no-prosody`` CLI flag). Set ``OPENPRONOUNCE_PROFILE=1`` to log per-stage timings.

    Returns a JSON-serializable dict with ``score`` (0-100), ``distance``,
    ``differences`` (per-word errors, phonemes, feedback), ``transcribe``,
    ``language`` and ``prosody`` (``f0`` and ``energy`` contours).

    When the phone recognizer is enabled (default, see :mod:`openpronounce.phones`),
    ``differences.errors`` and ``differences.phoneme_error_rate`` come from phones
    recognized directly in the audio; otherwise they are derived from the word
    transcription.
    """
    if use_phone_model is None:
        use_phone_model = phones.is_enabled()
    lang = get_language(lang).code
    profile_start = time.perf_counter()

    if env_flag("OPENPRONOUNCE_TRIM", True):
        with _stage("trim"):
            audio_1 = audio.trim_silence(audio_1, sr=sampling_rate)

    # Reference synthesis (a network call on first use of a sentence) starts right away.
    with ThreadPoolExecutor(max_workers=3) as pool:
        tts_future = pool.submit(audio.text2speech, text_reference, lang=lang)
        combo_future = pool.submit(_word_and_phone_differences, audio_1, sampling_rate,
                                   text_reference, lang, use_phone_model)
        prosody_future = pool.submit(_prosody_contours, audio_1, sampling_rate) if use_prosody else None

        with _stage("embeddings"):
            emb_1 = extract_embeddings(audio_1, sampling_rate)

        with _stage("reference"):
            reference_file = tts_future.result()
            emb_2 = _reference_embeddings(reference_file, sampling_rate)

        with _stage("dtw"):
            distance, path = _dtw(emb_1, emb_2)
        acoustic_distance = distance / max(1, len(path))
        distance = int(distance)

        transcription, differences, phone_result = combo_future.result()

    if phone_result is not None:
        differences.update({
            "errors": phone_result["errors"],
            "words_with_errors": phone_result["words_with_errors"],
            "phoneme_error_rate": phone_result["phone_error_rate"],
            "expected_phones": phone_result["expected_phones"],
            "heard_phones": phone_result["heard_phones"],
            "heard_phones_confidence": phone_result["heard_phones_confidence"],
            "feedback": _feedback(phone_result["words_with_errors"]),
        })

    score = compute_pronunciation_score(
        acoustic_distance, differences["phoneme_error_rate"], differences["word_error_rate"], lang
    )

    if env_flag("OPENPRONOUNCE_PROFILE", False):
        logger.info("[profile] %-12s %6.0f ms", "total", (time.perf_counter() - profile_start) * 1000)

    result = {
        "score": score,
        "distance": distance,
        "acoustic_distance": round(acoustic_distance, 3),
        "differences": differences,
        "feedback": differences["feedback"],
        "transcribe": differences["transcribe"],
        "language": lang,
    }
    if prosody_future is not None:
        energy, f0 = prosody_future.result()
        result["prosody"] = {
            "f0": f0.tolist(),
            "energy": energy.tolist(),
        }
    return result


# ---------------------------------------------------------------------------
# Prosody
# ---------------------------------------------------------------------------

def extract_f0(audio_waveform, sr=SAMPLING_RATE):
    """Fundamental frequency (pitch) contour.

    Uses YIN instead of pYIN: an order of magnitude faster. YIN has no voicing
    detection, so unvoiced frames keep a (noisy) value instead of 0; the contour
    is only used for visualization.
    """
    return librosa.yin(audio_waveform, fmin=50, fmax=300, sr=sr)


def extract_energy(audio_waveform):
    """RMS energy contour scaled to 0-250 so it can share an axis with F0."""
    energy = librosa.feature.rms(y=np.asarray(audio_waveform)).T
    low, high = float(energy.min()), float(energy.max())
    if high <= low:  # constant signal: MinMaxScaler would map everything to 0
        return np.zeros(energy.shape, dtype=float).flatten()
    return ((energy - low) * (250.0 / (high - low))).flatten()


def interpolate_f0(f0):
    """Linearly interpolate unvoiced (0) frames so the pitch curve has no gaps."""
    f0 = np.array(f0, dtype=float)
    mask = f0 > 0
    if not mask.any():
        return f0
    return np.interp(np.arange(len(f0)), np.where(mask)[0], f0[mask])
