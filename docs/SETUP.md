# Setup and recovery

These instructions describe the verified **OnePlus 55U1S, Android 11, build RTM8.240401.135**. The app targets Android TV API 26+, but the device-management helpers intentionally reject other models/builds. Do not weaken that guard to apply OnePlus-specific package operations to an untested TV.

## 1. Build

Install JDK 17 and Android SDK platform 35. Set `JAVA_HOME` and `ANDROID_HOME`, or configure `sdk.dir` in the ignored `local.properties`. Android Studio can open the repository directly.

On a Mac with Homebrew JDK 17 and the default Android SDK location:

```sh
source scripts/env.sh
./gradlew assembleDebug lintDebug testDebugUnitTest
```

On other systems, set those environment variables to your installation paths and run the same Gradle command. The wrapper pins Gradle 8.11.1 and verifies its distribution SHA-256. `scripts/bootstrap-gradle.py` is an optional macOS TLS-trust workaround; it downloads the same pinned archive without disabling TLS validation.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It is a development build. No release signing key is included. Updates must use the same signing key to preserve installed app data; do not uninstall as a shortcut around a signature mismatch.

## 2. Configure an explicit device

Enable developer options and ADB debugging on your own TV. Authorize your computer on the TV. Use the address shown by that TV; HDMI-connected devices are separate targets.

```sh
cp .quiet-tv-device.example.json .quiet-tv-device.json
```

Fill in the TV’s ADB endpoint and serial in that **ignored** JSON file. Alternatively:

```sh
export QUIET_TV_ADB_TARGET='<your-tv-address>:5555'
export QUIET_TV_SERIAL='<your-tv-serial>'
adb connect "$QUIET_TV_ADB_TARGET"
python3 scripts/tv.py verify
```

The helper uses `adb -s` for every operation, checks the manufacturer, boot model, serial and exact firmware build, and records changes under ignored `logs/`. Keep those recovery files locally. They contain device details and are not intended for publication.

## 3. Try Quiet TV as an app

```sh
python3 scripts/tv.py install app/build/outputs/apk/debug/app-debug.apk
python3 scripts/tv.py launch
```

Check the app grid, Back, TV settings and Appearance before changing Home. Installing alone does not disable the stock launcher or select MK-IV as the screensaver.

For an **update** while Quiet TV is already Home, first open TV settings from Home. Replacing the foreground launcher can expose another app from the previous task stack; the neutral settings screen avoids accidentally resuming media during installation. Then install and launch explicitly.

## 4. Select Quiet TV as Home

```sh
python3 scripts/tv.py home
```

The helper performs these steps:

1. Verify the exact TV and record the existing HOME resolver.
2. Launch Quiet TV and confirm it resumes.
3. Set `dev.rahul.quiettv/.Home` as the preferred Home.
4. If the stock launcher still wins, save its enabled state, disable `com.google.android.tvlauncher` for **user 0**, and retry.
5. Verify the resolver, send Home, and confirm Quiet TV resumes. Confirm OxygenPlay remains enabled.
6. On failure, attempt to restore and launch stock Home; record the outcome.

Why step 4 exists: this firmware gives the privileged Google launcher’s HOME filter priority 2, while an ordinary app is capped at 0. The preferred activity alone did not override that ordering. Disabling the stock launcher’s user-0 instance removes that resolver candidate without deleting its system APK or data.

This is a reversible launcher substitution, not an OS/firmware patch. No root, bootloader unlock, flashing, factory reset or accessibility automation is required.

## 5. Optional playback access

The music card needs Android notification-listener access to observe other apps’ active media sessions. The bridge does not process notification contents.

```sh
python3 scripts/media-access.py enable
python3 scripts/media-access.py status
```

Revoke it with `python3 scripts/media-access.py disable`. Playback controls only appear enabled when the session advertises support. An external HDMI device’s audio does not automatically become an Android media session. Cast support depends on the receiver/player app.

## 6. Optional MK-IV screensaver

Open **MK-IV Screensaver** from the app grid. Choose a design, use **Preview**, then **Save & apply**. To select its Android dream service and a five-minute idle timeout:

```sh
python3 scripts/screensaver.py status
python3 scripts/screensaver.py enable --minutes 5
python3 scripts/screensaver.py preview
```

The first call saves the original service, enable flag and screen-off timeout under `logs/screensaver/baseline.json`. The helper does not change the sleep timeout, HDMI/CEC configuration or disable OnePlus Pictorial. Video apps holding the display awake can defer Android’s idle activation.

### Firmware-specific Recommendations workaround

On this firmware, Google Recommendations reacts to `DREAMING_STARTED` by launching a private stock-Home helper. That fails when Google Home is disabled. Enabling MK-IV therefore records and reversibly disables `com.google.android.tvrecommendations` for user 0 if necessary.

This is distinct from Google TV/Play Movies. `restore-home` restores the recorded Recommendations state; screensaver-only restoration leaves the Home-related workaround alone.

## Restore the original TV experience

**Keep Quiet TV installed until stock Home works again.** With the same local configuration and recovery records:

```sh
python3 scripts/screensaver.py restore   # If MK-IV was enabled through the helper
python3 scripts/media-access.py disable  # If media access was granted
python3 scripts/tv.py restore-home
```

`restore-home` restores the recorded Google launcher enabled/default state and Recommendations state, selects stock Home, and verifies the Home key. Test normal navigation before removing Quiet TV.

If the local recovery records were lost, the normal helper cannot reconstruct the original screensaver timeout or every original package state. For emergency **Home** recovery on the documented OnePlus firmware:

```sh
adb -s "$QUIET_TV_ADB_TARGET" shell pm enable --user 0 com.google.android.tvlauncher
adb -s "$QUIET_TV_ADB_TARGET" shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
adb -s "$QUIET_TV_ADB_TARGET" shell am start -W -n com.google.android.tvlauncher/.MainActivity
```

After confirming stock Home works, re-enable Recommendations if it was disabled by this setup. Use Android’s screensaver picker to choose the original service and timeout. This fallback enables stock Home; it does not claim an exact restoration of lost settings.

## Optional live mirror

With `adb` and `scrcpy` installed:

```sh
python3 scripts/mirror-tv.py
# Or a read-only view:
python3 scripts/mirror-tv.py --view-only
```

On macOS, double-click `Open Quiet TV.command`. The mirror uses the verified target, 720p/30 fps, no audio forwarding, no clipboard sync and no power-on request. Arrows navigate; Return selects; Control+B goes Back; Control+H goes Home; Control+Q closes the mirror.

## Optional package cleanup

App removal is not required for Quiet TV. The helper has an explicit optional-package allowlist, records previous states and writes local restore commands. It does not run a blanket debloat operation. See [package cleanup notes](DEBLOAT.md) before using it.
