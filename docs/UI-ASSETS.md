# Asset provenance

## Fonts and icons

Google Sans is bundled for the native UI, Canvas labels and companion browser. It is served locally; no font CDN is contacted. The unmodified variable font is pinned to a Google Fonts commit and SHA-256 in [`ui-assets.json`](ui-assets.json).

Twenty-three Hugeicons free Stroke Rounded icons are imported from `@hugeicons/core-free-icons` 4.3.5. The importer checks the npm SHA-512 integrity, converts only the selected icons to Android vectors, and rounds coordinates to three decimals. No icon font or JavaScript icon runtime is packaged.

Silkscreen remains bundled from the earlier digital-clock design; the current square analog clock uses Google Sans and native geometry. Its pinned origin is in [`pixel-font.json`](pixel-font.json).

The original licenses are in [`app/src/main/assets/licenses/`](../app/src/main/assets/licenses/). Rebuild selected font/icon assets with `python3 scripts/import-ui-assets.py`.

## Home artwork

The Connect device and MK-IV tiles, utility-app illustrations, centered selection badge and clock geometry are original vector/Canvas artwork. The Netflix red-N treatment and runtime app logos remain third-party marks, not original brands or MIT-licensed trademarks.

OxygenPlay uses the official icon loaded from its installed package, retaining the image and aspect ratio. The APK icon is not redistributed. [`oxygenplay-icon.json`](oxygenplay-icon.json) records the inspected version/resource/checksum, without a private device identifier.

The clock, weather symbols, vertical AQI segments and MK-IV panels are drawn locally. Theme-gallery previews use the real native renderer at a fixed time and seed; they are not downloaded images.

## Wallpaper photographs

Nine Unsplash originals are bundled as center-cropped 1920×1080 JPEGs, with 320×180 thumbnails. The original source files are not included. Import applies orientation and writes fresh image pixels without retaining source EXIF metadata.

[`wallpapers.json`](wallpapers.json) records titles, public source URLs, photographer names, source filenames and SHA-256 checksums. It contains no absolute local paths. Attribution and separate licensing are in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

```sh
uv run --with pillow python scripts/import-wallpapers.py /path/to/originals
```

The importer verifies all originals before replacing assets, preserving the explicit catalog order. Normal builds use the bundled assets and do not need the originals or Pillow.

## Navigation audio

`res/raw/home_navigation.wav` is original synthesized audio: mono 44.1 kHz, 16-bit PCM, 65 ms. A cosine attack and exponential decay soften a low sine pluck with a faint upper partial. It contains no Apple recording or external samples.

Recreate it with `python3 scripts/generate-navigation-sound.py`. Home plays it at low gain, with one SoundPool stream and a 90 ms navigation debounce. Resources are released when Home pauses.

## Documentation images

The visual README uses real 1080p TV captures, not composited UI mockups. Full-screen MK-IV samples are transient native previews. Third-party app marks and wallpaper images visible inside screenshots retain their respective rights. See [validation and capture scope](VALIDATION.md).

The companion embeds a small selection of the same MIT-licensed Hugeicons paths and original utility-tile vectors in `app.js`. Simple navigation/upload glyphs are original SVG geometry. No external icon or font service is used.
