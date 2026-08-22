"""Torch device selection and inference speed-ups.

``OPENPRONOUNCE_DEVICE`` forces the device; :func:`optimize_model` quantizes the
model to int8 on CPU (``OPENPRONOUNCE_QUANTIZE=off`` to disable) and enables
TF32/fp16-friendly matmuls on CUDA.
"""

import logging
import os
from contextlib import contextmanager
from functools import lru_cache

import torch

logger = logging.getLogger(__name__)

_OFF_VALUES = ("", "0", "off", "false", "no")


def env_flag(name, default=True):
    """Read a boolean environment variable ("off", "0", "false", "no" mean disabled)."""
    value = os.environ.get(name)
    if value is None:
        return default
    return value.strip().lower() not in _OFF_VALUES


@lru_cache(maxsize=1)
def get_device():
    """Return the torch device models run on.

    ``OPENPRONOUNCE_DEVICE`` (``cpu``, ``cuda``, ``cuda:1``, ``mps``...) wins; otherwise
    CUDA if available, else CPU. Resolved once per process.
    """
    name = os.environ.get("OPENPRONOUNCE_DEVICE")
    if not name:
        name = "cuda" if torch.cuda.is_available() else "cpu"
    device = torch.device(name)
    logger.info("Using device %s", device)
    return device


def optimize_model(model):
    """Apply one-shot inference speed-ups to a loaded model, and return it.

    - CPU: dynamic int8 quantization of Linear layers, typically 1.5-2.5x faster forwards.
      Off by default: it slightly shifts posteriors and embeddings, which degrades the
      phone recognition and moves the acoustic distances away from their calibrated
      values. Enable with ``OPENPRONOUNCE_QUANTIZE=on`` if speed matters more than the
      last few percent of accuracy.
    - CUDA: TF32 matmuls (Ampere+), faster fp32 inference.
    """
    device = get_device()
    if device.type == "cuda":
        torch.set_float32_matmul_precision("high")
        return model
    if device.type != "cpu" or not env_flag("OPENPRONOUNCE_QUANTIZE", False):
        return model
    try:
        from torch.ao.quantization import quantize_dynamic

        model = quantize_dynamic(model, {torch.nn.Linear}, dtype=torch.qint8)
        logger.info("Dynamic int8 quantization enabled")
    except Exception as e:  # noqa: BLE001 - quantization is an optimization, never a requirement
        logger.warning("Quantization unavailable (%s), running full precision", e)
    return model


@contextmanager
def inference_autocast():
    """Run model forwards under fp16 autocast on CUDA (a no-op elsewhere)."""
    if get_device().type == "cuda":
        with torch.autocast("cuda", dtype=torch.float16):
            yield
    else:
        yield
