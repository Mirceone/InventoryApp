#!/usr/bin/env python3
"""OCR fallback for scanned invoice PDFs/images when MarkItDown returns empty."""
from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path


def _ocr_reader(languages: list[str]):
    import easyocr

    return easyocr.Reader(languages, gpu=False, verbose=False)


def ocr_pdf(path: Path, languages: list[str]) -> str:
    import fitz
    import numpy as np
    from PIL import Image

    reader = _ocr_reader(languages)
    doc = fitz.open(path)
    parts: list[str] = []
    for index in range(doc.page_count):
        page = doc.load_page(index)
        pix = page.get_pixmap(matrix=fitz.Matrix(2, 2))
        img = Image.open(io.BytesIO(pix.tobytes("png")))
        results = reader.readtext(np.array(img), detail=0, paragraph=True)
        text = "\n".join(results).strip()
        if not text:
            continue
        if doc.page_count > 1:
            parts.append(f"# Page {index + 1}\n{text}")
        else:
            parts.append(text)
    return "\n\n".join(parts).strip()


def ocr_image(path: Path, languages: list[str]) -> str:
    import numpy as np
    from PIL import Image

    reader = _ocr_reader(languages)
    img = Image.open(path)
    results = reader.readtext(np.array(img), detail=0, paragraph=True)
    return "\n".join(results).strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("file", type=Path)
    parser.add_argument("--languages", default="ro,en")
    args = parser.parse_args()

    if not args.file.is_file():
        print(f"File not found: {args.file}", file=sys.stderr)
        return 2

    languages = [part.strip() for part in args.languages.split(",") if part.strip()]
    suffix = args.file.suffix.lower()
    if suffix == ".pdf":
        text = ocr_pdf(args.file, languages)
    elif suffix in {".jpg", ".jpeg", ".png", ".webp", ".tif", ".tiff"}:
        text = ocr_image(args.file, languages)
    else:
        print(f"Unsupported file type for OCR: {suffix}", file=sys.stderr)
        return 2

    if not text:
        print("OCR produced empty output", file=sys.stderr)
        return 1

    print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
