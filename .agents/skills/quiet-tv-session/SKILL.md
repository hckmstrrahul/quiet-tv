---
name: quiet-tv-session
description: Start or resume development in this Quiet TV checkout with the localhost companion preview, verified Wi-Fi ADB connection and scrcpy live TV mirror. Use for Quiet TV editing-session setup; documentation-only work does not require a live TV.
---

# Quiet TV session

Prepare the existing tools, report what is ready, then continue the user's edits. Do not reinstall the app just to start a session.

## Find the checkout and retain current work

Use the actual repository root and read `git status --short` and the latest commit. On this Mac the folder is now `quiet-tv`, not `oneplus-tv-patch`. If an old chat points to the deleted folder and emits `Hook failed / os error 2`, use the real root for commands and tell the user to reopen that project in a new chat. Do not rename the folder back or change global hooks.

Read [Development](../../../docs/DEVELOPMENT.md) for build/test commands when editing code. Check the source and installed versions rather than treating a version recorded in an earlier conversation as current.

## Start or reuse the local HTML preview

From the repository root:

```sh
python3 scripts/preview-companion.py
```

Keep the process alive using the available persistent terminal/session mechanism. It serves **http://localhost:8766** on loopback. Before starting another copy, check for an existing `preview-companion.py` process and a listener on port 8766. Confirm its working directory belongs to this checkout. Reuse it when healthy; do not kill an unrelated process using the port. Static HTML/CSS/JS files are read on request; changes to the Python preview server require a restart of that specific process.

Open the URL in the user's browser when browser/open capabilities are available. Use approved browser tools to inspect it. If browser access is unavailable or explicitly blocked, provide the URL and continue code/server checks; do not loop through permission prompts or work around a browser restriction with another automation route. User-provided screenshots can support visual review, but do not claim browser interactions were tested.

This is the **sample-data editor**, not a connection to the physical TV. Its Home header is a static fixture, edits stay in memory, and `/?pair=1` uses the public demo code `123456`. The actual paired editor is served by the TV's **Connect device** screen on its own LAN address/port; use its current QR/code only when testing real companion actions, and keep them private.

## Connect to the TV and open the live view

The computer and TV need to be on the same reachable home network, with the TV's existing ADB authorization available. The ignored `.quiet-tv-device.json` holds `target` and `serial`; `QUIET_TV_ADB_TARGET` and `QUIET_TV_SERIAL` can override it. Read these through `scripts/tv.py`, not by copying their values into commands, public notes or chat output.

From the repository root:

```sh
python3 scripts/mirror-tv.py
```

On macOS, double-clicking **Open Quiet TV.command** runs the same helper. Keep its process alive, and reuse an existing mirror for the configured target instead of opening duplicates.

The helper connects over Wi-Fi ADB, checks the expected serial, OnePlus 55U1S model and firmware, then opens **Quiet TV — Live**. It is a **scrcpy mirror of the real TV**, not an Android emulator. Defaults are 720p / 30 fps, no forwarded audio, no clipboard sync and no power-on request. `--view-only` disables remote control. Arrows navigate, Return selects, Control+B goes Back, Control+H goes Home and Control+Q closes the mirror.

If connection fails, inspect the configured target once and report the specific problem. Ask for the TV's new address or on-TV ADB approval only when needed; update only the ignored local config. Never scan the network or select another ADB device automatically. Stop all TV interaction on an identity mismatch.

When the requested editing session needs Quiet TV in the foreground, use the identity-checked helper:

```sh
python3 scripts/tv.py launch
```

If the TV is asleep, wake it only when requested or necessary for the user's authorized live-editing/checking task. Load `tv` from the repository's `scripts` directory, call `tv.verify()` first, then `tv.adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')`. Never use the power-toggle key, which can turn an awake TV off. If another app or HDMI playback is active and interruption is not already authorized, ask before taking over. Do not select an HDMI source during layout checks.

Report the preview URL, mirror readiness, TV app version and any blocker. If the requested edits are already known, continue them; otherwise say the workspace is ready for the next change.

## Build, verify and publish when requested

```sh
source scripts/env.sh
./gradlew assembleDebug lintDebug testDebugUnitTest
python3 scripts/tv.py install app/build/outputs/apk/debug/app-debug.apk
python3 scripts/tv.py launch
```

Installation is a same-key APK update preserving preferences. Use it only when the task calls for deployment; documentation and this startup skill do not need a TV build. For behavioral changes, run the relevant native checks in [Development](../../../docs/DEVELOPMENT.md), remove the temporary test APK afterward, and restore any test preferences. A live mirror sends real input: coordinate with the user if controls conflict, without repeatedly asking after they have agreed to leave them idle.

Before reporting a deployment complete, verify the installed package version and APK hash against the local build. Keep raw diagnostics under ignored `logs/` or `artifacts/`. Review screenshots before copying selected images into `docs/images/`.

When committing/pushing is requested, inspect the diff and staged paths, exclude private data, and run appropriate checks. The project remote is `hckmstrrahul/quiet-tv`. On a multi-account GitHub setup, use `hckmstrrahul` for this push and restore the previously active account afterward. Do not change visibility or publish APKs unless requested.

## Private local files

`.quiet-tv-device.json`, `local.properties`, `.env*`, `logs/`, `artifacts/`, `inventory/` and build outputs are already ignored. Keep actual addresses, serials, pairing/session credentials, SDK locations and unreviewed captures there. Only this reusable workflow and its `AGENTS.md` pointer belong in GitHub.
