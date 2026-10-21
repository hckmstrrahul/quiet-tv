# Development

## Toolchain

| Component | Version |
| --- | --- |
| Kotlin | 2.1.20 |
| Android Gradle Plugin | 8.9.2 |
| Gradle wrapper | 8.11.1, SHA-256 pinned |
| JDK / bytecode | 17 |
| Android SDK | compile/target 35; minimum 26 |
| Runtime dependency | ZXing core 3.5.3 |
| Unit tests | JUnit 4.13.2, org.json 20240303 |

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest assembleDebugAndroidTest
```

The source is architecture-neutral Kotlin/Java bytecode, with no bundled native ABI libraries. Android Studio and the CLI use the same build.

## Layout

```text
app/src/main/java/dev/rahul/quiettv/  Home, widgets, companion, shared UI
  screensaver/                       MK-IV renderer, settings and DreamService
app/src/main/assets/                 Wallpapers, companion UI and licenses
app/src/main/res/                    Fonts, vector assets, sound and strings
app/src/test/                        JVM tests
app/src/androidTest/                 Native on-device checks
scripts/                            Explicit-device setup, recovery and asset tools
docs/                               Guides and curated, privacy-reviewed screenshots
```

## Automated checks

The 28 JVM tests exercise HDMI grouping/order, lifecycle workers, playback math, weather parsing/freshness, wallpaper shuffle, companion-server access boundaries, MK-IV layout packing and color evolution.

Native instrumentation uses `WidgetInstrumentation`. The default suite creates a **silent controlled media session** to verify metadata, transport actions, clock lifecycle and navigation sound behavior. The appearance suite checks native settings, 17 distinct static theme samples, focus routes and unchanged saved preferences.

With the test TV explicitly configured, the app installed, and the remote idle:

```sh
python3 scripts/tv.py verify
adb -s "$QUIET_TV_ADB_TARGET" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$QUIET_TV_ADB_TARGET" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$QUIET_TV_ADB_TARGET" shell am instrument -w dev.rahul.quiettv.test/dev.rahul.quiettv.WidgetInstrumentation
adb -s "$QUIET_TV_ADB_TARGET" shell am instrument -w -e suite appearance dev.rahul.quiettv.test/dev.rahul.quiettv.WidgetInstrumentation
adb -s "$QUIET_TV_ADB_TARGET" uninstall dev.rahul.quiettv.test
```

These commands use the environment-variable configuration, not the JSON-file alternative. The suites take over the app foreground; do not run them during playback or remote use. Generated diagnostics and screenshots can contain local details and should stay under ignored `logs/` or `artifacts/` until reviewed.

`python3 scripts/smoke.py` performs a narrower Home navigation check without selecting HDMI sources or changing saved preferences. It requires Quiet TV Home already in the foreground and a compatible configured TV.

## Reproduce assets

- `python3 scripts/import-ui-assets.py`: pinned Google Sans and Hugeicons sources; checks archive integrity.
- `python3 scripts/generate-navigation-sound.py`: original 65 ms PCM navigation tone; no sampled Apple audio.
- `uv run --with pillow python scripts/import-wallpapers.py /path/to/originals`: uses the explicit catalog and source hashes; builds 1080p images and 320px thumbnails without publishing local paths. Original photographs are not downloaded by this script.

See [asset provenance](UI-ASSETS.md) and [third-party notices](../THIRD_PARTY_NOTICES.md). Build inputs are already bundled; these tools are not required for a normal build.

## Working conventions

Keep remote focus and Back behavior predictable. Scope rendering/network work to visible surfaces. Preserve settings on upgrades. MK-IV edits remain a draft until Save & apply; Home appearance edits save immediately.

For a TV change, build and run the relevant tests, install as an ordinary app first, then inspect real device screenshots. Keep system mutations separate from UI development and retain local rollback records. Do not commit APKs, keys, device configuration, inventories or raw logs.

The current validation boundary and remaining physical tests are recorded in [VALIDATION.md](VALIDATION.md).

## Companion UI preview

```sh
python3 scripts/preview-companion.py
```

Open **http://localhost:8766** for the sample editor, or **http://localhost:8766/?pair=1** for the pairing screen (sample code `123456`). The server binds only to loopback, serves the real bundled HTML/CSS/JS and fonts, and simulates the companion API in memory. It never connects to a TV. Restart it to reset the sample layout. `--port` selects another local port.

The companion shares the TV’s Google Sans, selected Hugeicons vectors, original utility-tile artwork, dark panels, accent colors and gallery selection treatment. Appearance opens first; colors, tile size and app order update the layout preview. The paired TV supplies native header and tile renders through authenticated `/api/preview`. The localhost sandbox supplies a reviewed fixture from `scripts/fixtures/companion-home.json`; its header is static, while tile order, columns and background remain editable. Monograms are only a fallback while artwork loads. Connect device and MK-IV remain the first two fixed Home tiles.


`HomePreview` draws only the Home header and tiles, excluding dialogs, QR codes and other applications. The browser requests a fresh snapshot after edits, Refresh, or returning to the tab. This is not continuous screen streaming. Native artwork is refreshed after layout settles; wallpaper layers animate independently using opacity with a 1.2-second ease-in-out transition. Hidden browser tabs cancel shuffle timers; native Home cancels timers when paused and releases images when stopped.

The `companion` instrumentation suite exports a native fixture into app-private `files/companion-preview.json` and checks the 20-second shuffle interval, an intermediate crossfade frame, an opaque underlying image, pause retention and stop cleanup. Its isolated test preferences do not change Home choices. Review any exported header for personal now-playing information before copying it into the repository.
