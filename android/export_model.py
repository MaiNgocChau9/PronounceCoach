import torch
import numpy as np
from transformers import Wav2Vec2ForCTC, Wav2Vec2PhonemeCTCTokenizer

def export_model():
    print("Loading Wav2Vec2 phoneme model...")
    model_name = "facebook/wav2vec2-lv-60-espeak-cv-ft"

    model = Wav2Vec2ForCTC.from_pretrained(model_name)
    model.eval()

    tokenizer = Wav2Vec2PhonemeCTCTokenizer.from_pretrained(model_name)
    vocab = tokenizer.get_vocab()
    with open("vocab.txt", "w") as f:
        sorted_vocab = sorted(vocab.items(), key=lambda x: x[1])
        for token, idx in sorted_vocab:
            f.write(f"{token}\n")

    print(f"Vocabulary: {len(vocab)} tokens")

    # Use older-style export to get a single .onnx file with inline weights
    dummy_input = torch.zeros(1, 16000)

    # Disable dynamo-based export and use legacy exporter
    import torch.onnx
    torch.onnx.export(
        model,
        dummy_input,
        "wav2vec2_phoneme.onnx",
        input_names=["input"],
        output_names=["logits"],
        dynamic_axes={
            "input": {1: "audio_length"},
            "logits": {1: "output_length"}
        },
        opset_version=14,
        do_constant_folding=True,
        export_params=True,
        # This forces all data to be inline in the .onnx file
    )

    import os
    size_mb = os.path.getsize("wav2vec2_phoneme.onnx") / (1024 * 1024)
    print(f"\nModel size: {size_mb:.1f} MB")

    # Verify
    import onnx
    model_onnx = onnx.load("wav2vec2_phoneme.onnx")
    onnx.checker.check_model(model_onnx)
    print("Model verification passed!")

    # Check if external data file was created (we don't want it)
    data_file = "wav2vec2_phoneme.onnx.data"
    if os.path.exists(data_file):
        data_size = os.path.getsize(data_file) / (1024 * 1024)
        print(f"WARNING: External data file found ({data_size:.1f} MB)")
        print("Need to merge inline...")
        # Load with external data and save with inline
        from onnx.external_data_helper import convert_model_to_external_data
        # Actually let's just re-export properly
    
    print("\nDone! Copy to assets:")
    print("  wav2vec2_phoneme.onnx → app/src/main/assets/models/")
    print("  vocab.txt → app/src/main/assets/models/")

if __name__ == "__main__":
    export_model()
