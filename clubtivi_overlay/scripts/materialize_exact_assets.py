#!/usr/bin/env python3
from __future__ import annotations

import base64
from pathlib import Path
import sys


ASSETS = {
    "home": "home_exact.webp",
    "users": "users_exact.webp",
}


def read_chunks(root: Path, name: str) -> bytes:
    chunk_dir = root / "assets_b64"
    chunks = sorted(chunk_dir.glob(f"{name}.part.*"))
    if not chunks:
        raise SystemExit(f"missing {name} artwork: no chunks in {chunk_dir}")
    encoded = "".join(path.read_text(encoding="ascii").strip() for path in chunks)
    try:
        return base64.b64decode(encoded, validate=True)
    except Exception as exc:
        raise SystemExit(f"{name} artwork chunks are not valid base64: {exc}") from exc


def validate_webp(path: Path) -> None:
    data = path.read_bytes()
    if len(data) < 16 or data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise SystemExit(f"{path} is not a WebP file")
    riff_size = int.from_bytes(data[4:8], "little") + 8
    if riff_size != len(data):
        raise SystemExit(
            f"{path} is truncated or has extra bytes: header={riff_size} actual={len(data)}"
        )


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: materialize_exact_assets.py <overlay-root>")

    root = Path(sys.argv[1]).resolve()
    target_dir = root / "files" / "assets" / "emurph"
    target_dir.mkdir(parents=True, exist_ok=True)

    for chunk_name, filename in ASSETS.items():
        target = target_dir / filename
        if not target.exists():
            target.write_bytes(read_chunks(root, chunk_name))
        validate_webp(target)
        print(f"asset: {target.relative_to(root)}")


if __name__ == "__main__":
    main()
