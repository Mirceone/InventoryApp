#!/usr/bin/env python3
"""Ensure an MLX Gemma model is present locally (download from Hugging Face if missing)."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path


def is_model_complete(model_dir: Path) -> bool:
    if not model_dir.is_dir():
        return False
    if not (model_dir / "config.json").is_file():
        return False
    return any(model_dir.glob("*.safetensors"))


def ensure_model(repo_id: str, local_dir: Path) -> Path:
    local_dir.mkdir(parents=True, exist_ok=True)
    if is_model_complete(local_dir):
        print(f"READY {local_dir.resolve()}", flush=True)
        return local_dir

    try:
        from huggingface_hub import snapshot_download
    except ImportError as exc:
        print(
            "Missing huggingface_hub. Install with: pip install huggingface_hub",
            file=sys.stderr,
        )
        raise SystemExit(2) from exc

    print(f"DOWNLOADING {repo_id} -> {local_dir.resolve()}", flush=True)
    snapshot_download(
        repo_id=repo_id,
        local_dir=str(local_dir),
        local_dir_use_symlinks=False,
    )

    if not is_model_complete(local_dir):
        print(f"Download finished but model looks incomplete: {local_dir}", file=sys.stderr)
        raise SystemExit(1)

    print(f"READY {local_dir.resolve()}", flush=True)
    return local_dir


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True, help="Hugging Face repo id")
    parser.add_argument("--local-dir", type=Path, required=True, help="Local model directory")
    parser.add_argument(
        "--check-only",
        action="store_true",
        help="Exit 0 if model is present, 1 if missing (no download)",
    )
    args = parser.parse_args()

    if args.check_only:
        return 0 if is_model_complete(args.local_dir) else 1

    ensure_model(args.repo, args.local_dir)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
