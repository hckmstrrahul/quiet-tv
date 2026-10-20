# MK-IV screensaver

MK-IV is a native Kotlin/Canvas adaptation of Rahul Chakraborty’s original Omni-data console. It is bundled with Quiet TV and also appears as **MK-IV Screensaver** in Android TV’s app launcher.

![MK-IV settings](images/mkiv-settings.png)

## Use it

1. Open MK-IV Screensaver and expand a settings group.
2. Change the draft; the framed TV preview updates immediately.
3. Use **Preview** for a full-screen view. Back or OK returns.
4. Choose **Save & apply** to persist the design. Leaving without saving preserves the previous configuration.
5. Select MK-IV in the Android screensaver picker, or use the verified-device helper in [Setup](SETUP.md#6-optional-mk-iv-screensaver).

The actual screensaver has no settings overlay. A normal remote key dismisses it. Android controls idle detection; video apps can keep the display awake.

## Controls

| Group | Control | Choices / behavior |
| --- | --- | --- |
| Color & mood | Theme | 17 palettes, shown as static thumbnail previews |
| | Random colors forever | On enables automatic mixed colors; Off holds colors while instruments keep animating |
| | Mode | Ever-changing colors or hold colors |
| | Panel palettes | Random colors or selected theme only |
| Layout & data | Source | Aerospace Telemetry, Server Infrastructure, Market Analytics, Bio-metrics |
| | Density | Spacious 8×4, balanced 10×6, dense 12×7 packing grid; panels can span cells |
| | Shuffle now | New arrangement; save to retain its seed |
| Motion | Speed | Slow, Normal, Fast |
| | Evolution | Colors only, or colors plus new instruments |
| | Change interval | One panel about every 0.5, 2 or 5 seconds |
| | Reshuffle layout | Off, 30 seconds, 1 minute or 2 minutes |
| Display | Brightness | 35%, 50%, 70%, 85%, 100% artwork brightness |
| | Frame limit | 20 or 30 fps |
| | Scanlines | On / off |
| System & reset | System settings | Opens the firmware’s screensaver picker when supported |
| | Restore default design | Resets the draft; Save & apply commits it |

Defaults include **85% brightness**, **Dense**, **Bio-metrics**, **20 fps**, random colors, color-only evolution and no layout reshuffling. Upgrades preserve existing saved choices.

## Palettes and instruments

**Palettes:** Engineering, Blueprint, Terminal, Midnight, Paper, Braun, Vaporwave, Forest, Monochrome, Solaris, Neon Grid, Acid Protocol, BIOS Error, Hazard Zone, Virtual Red, Candy Pop and Ultraviolet.

![Static theme gallery](images/mkiv-theme-gallery.png)

**30 instrument families:** sine wave, histogram, gauge, radar, rolling line, matrix, wireframe cube, binary stream, target rings, hex dump, scatter, horizon, pie, DNA helix, progress bars, spectrum, globe, terminal, vector field, circular buffer, waterfall, network, barcode, compass, liquid, seismic, orbit, fan, crosshairs and scanner.

All readings are simulated art. Source changes the labels/data theme; it does not connect to markets, medical devices, aircraft, system metrics or a microphone. Spectrum panels do not analyze playing audio.

## Continuous color evolution

Mixed mode distributes palettes across panels from the first frame. Automatic mode then fades one panel at a time over roughly 0.9 seconds. Shuffled rounds give each panel a turn; there is no final round or periodic restart.

Color-only evolution preserves geometry, visualization, label, procedural noise and animation phase. **Colors + new instruments** opts into replacement. Whole-layout reshuffling is independent. These guarantees hold within a running session; starting a new dream begins a new animation session.

## Static gallery, animated preview

Each gallery sample uses the same seed, spacious layout, single palette, full brightness, no scanlines and time zero. Samples never start a render loop, and the settings preview pauses while the chooser is open. This makes palette comparison stable and limits work.

The embedded/full-screen previews and Android dream use the same renderer. Their loops stop when paused, hidden or detached. The screensaver has no network, audio, audio-focus or HDMI/CEC behavior.

![MK-IV Blueprint](images/mkiv-blueprint.png)

## Saved state and restoration

The design lives in app-private `screensaver` preferences, separately from Home. Same-key app updates preserve it; uninstall/data clearing removes it. Companion Home backups do not include MK-IV.

Android’s selected dream service and timeout are separate system settings. `scripts/screensaver.py` records their original values before changing them; `restore` returns to that baseline. See [Setup and recovery](SETUP.md#restore-the-original-tv-experience), including the firmware-specific Recommendations workaround.
