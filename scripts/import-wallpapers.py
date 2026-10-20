#!/usr/bin/env python3
"""Rebuild bundled backgrounds from the credited originals, without storing local paths.
Usage: uv run --with pillow python scripts/import-wallpapers.py /path/to/originals
Original files are never changed. Uses the explicit order in docs/wallpapers.json.
"""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageOps

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('source', type=Path)
args = parser.parse_args()
manifest = root / 'docs/wallpapers.json'
records = json.loads(manifest.read_text())
out = root / 'app/src/main/assets/wallpapers'
# Validate the complete input before replacing any assets.
for record in records:
    original = args.source / record['sourceFile']
    if hashlib.sha256(original.read_bytes()).hexdigest() != record['sourceSha256']:
        parser.error('Original checksum differs for ' + original.name)
(out / 'thumbs').mkdir(parents=True, exist_ok=True)
for record in records:
    original = args.source / record['sourceFile']
    with Image.open(original) as source_image:
        image = ImageOps.exif_transpose(source_image).convert('RGB')
        full = ImageOps.fit(image, (1920, 1080), method=Image.Resampling.LANCZOS)
        destination = out / (record['id'] + '.jpg')
        full.save(destination, quality=88, optimize=True)
        full.resize((320, 180), Image.Resampling.LANCZOS).save(
            out / 'thumbs' / destination.name, quality=82, optimize=True)
    record['assetSha256'] = hashlib.sha256(destination.read_bytes()).hexdigest()
manifest.write_text(json.dumps(records, indent=2) + '\n')
print('Prepared', len(records), 'wallpapers and thumbnails')
