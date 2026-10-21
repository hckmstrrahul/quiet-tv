# Validation

Scope: Quiet TV **0.11.0**, including Home and MK-IV checks carried forward from **0.10.2**, on OnePlus **55U1S**, Android **11/API 30**, firmware **RTM8.240401.135**. This is one verified hardware/firmware combination, not a general Android TV certification.

## Automated checks

| Check | Result / coverage |
| --- | --- |
| Debug APK | Builds with JDK 17 and the pinned Gradle wrapper |
| JVM suite | 28 passing tests: HDMI grouping/order, worker cancellation, playback math, weather/cache, wallpaper shuffle, companion HTTP access, MK-IV layout and color evolution |
| Android lint | 0 errors; 3 existing warnings: one complex vector path and two programmatic custom-view constructors |
| Controlled media session | Title/artist/artwork/progress and supported transport behavior; no live Cast playback required |
| Clock / sound | Foreground clock updates, pause/teardown behavior and navigation-tone checks |
| Companion preview / shuffle | Native Home header and tiles render; no advance before 20 seconds; intermediate crossfade with an opaque underlying layer; pause retains wallpaper and stop releases images. |
| Appearance instrumentation | 17 distinct static theme renders, no sample animation loops, settings preview paused behind chooser, selection badge, draft update, D-pad routes and unchanged saved preferences |

Commands and test-suite entry points are in [Development](DEVELOPMENT.md).

## On-TV observations

- Quiet TV launches as an app and resolves as Home after the documented stock-launcher workaround. Stock Home restoration was exercised during development.
- Connected HDMI entries are first; CEC names appear for Apple TV and PlayStation 5.
- Appearance, app grid, settings and MK-IV support remote navigation. Outside dismissal and Back routes were checked.
- Home keeps the wallpaper frame behind the system settings overlay and releases hidden rendering work.
- Wallpaper selection, accent, grid size and MK-IV saved preferences survive same-key APK updates.
- The weather card handles cached/unavailable data, shows update/source details and uses the US AQI scale.
- Native MK-IV full-screen previews and Android DreamService were exercised. The Google Recommendations helper issue and reversible workaround are documented in Setup.

## Screenshot provenance

The `home-*.png` and `mkiv-*.png` images in [`docs/images/`](images/) are 1920×1080 captures of the real TV. Most show version 0.10.2; `home-backgrounds.png` was refreshed on 0.11.0 to show the 20-second shuffle option. They include four Home wallpapers, appearance controls, MK-IV settings, static theme galleries and five full-screen designs.

Wallpaper variants were selected for documentation and the original Deep ocean choice was restored. Full-screen MK-IV variants use transient preview configuration; they do not overwrite the saved design. Home and MK-IV preferences were compared before and after capture. Pairing codes, QR screens, device addresses, serials and raw diagnostics are excluded.

Screensaver numbers are generated artwork. Weather values are a snapshot, not a promise of present conditions. Green HDMI dots reflect Android’s connection status.

The seven `companion-*.png` images are browser screenshots supplied by Rahul on October 21, 2026. They show the localhost demo after the TV-model header and copy cleanup in commit `d3ae2c1`. The original PNGs were copied byte-for-byte with descriptive filenames, without retouching. They were visually reviewed for pairing codes, QR codes, private addresses, serials and other personal information before publication.

## Companion browser checks

The redesigned companion passes JavaScript syntax, HTML ID/ARIA-target and selector checks, plus local sample-state checks for wallpaper, color, tile size and app order. The native fixture was visually reviewed for personal information. The Android build and all 28 JVM tests pass; lint retains the same three existing warnings.

The supplied captures were visually reviewed and added to the README:

| Format | Captured views |
| --- | --- |
| Desktop | Background, Colors, Layout, Apps and Your setup; each includes the Home preview |
| Mobile | Background gallery, plus a scrolled view of upload controls and the Home preview |

These are sample-data demo captures, not a live paired-TV session. The captured demo shows blank thumbnails for Weather, YouTube and YouTube Music; the images preserve that state. Mobile captures do not cover every tab or the pairing screen.

Browser-tool interaction checks remain unverified because localhost access was blocked in the session. Reviewing the supplied screenshots establishes visual evidence only; it does not verify uploads, restore flows, touch behavior or persistence against a real TV.

## Not verified

- Actual HDMI source switching or physical CEC power behavior; inputs were enumerated without activating them.
- Live YouTube Cast transport/artwork behavior; the user chose a controlled session test instead.
- Background playback behavior of every third-party player. Returning Home does not force-stop players.
- Other TV models, firmware versions, Android versions or unusual display scaling.
- A store-distributed release build or an independent security audit of the companion server.

Private device inventories, installation logs and detailed historical test captures remain local. They are not required to build the public source.
