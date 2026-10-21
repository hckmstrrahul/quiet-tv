# Privacy and security boundaries

## What leaves the TV

| Feature | Network behavior |
| --- | --- |
| Home grid, clock, HDMI and bundled wallpapers | No external requests |
| MK-IV | Entirely local procedural rendering |
| Weather / AQI | HTTPS to `api.open-meteo.com` and `air-quality-api.open-meteo.com`, using fixed Bengaluru coordinates |
| Media widget | Android media-session metadata; no artwork URL fetching or external uploads |
| Companion editor | Optional HTTP server bound to the TV’s selected LAN address; used by a paired browser |

There is no analytics SDK, advertising SDK, account backend or cloud sync. Weather providers still receive the network request, including the public source IP. Fixed city coordinates do not use GPS or discover the household location.

## Permissions

- **Internet / network state:** weather requests and the optional LAN companion.
- **Notification-listener access, separately granted:** required by Android to inspect other apps’ active media sessions. `MediaAccessService` does not read, store or forward notification contents.
- **Dream binding:** `BIND_DREAM_SERVICE` protects the screensaver service for Android’s use; the app does not gain system privileges.
- No microphone, camera, location, contacts, broad storage or accessibility permission is requested.

ADB is a development/installation channel. The application does not contain an ADB client or gain root through these scripts.

## Companion editor

The authenticated Home-preview endpoint returns a rendered Home header and app tiles. It can include the now-playing title/artwork, weather, clock and HDMI labels currently displayed on Home. It does not capture other apps, pairing dialogs or QR codes. These snapshots are sent only to the paired local browser and are not uploaded anywhere.

Opening **Connect device** starts an explicitly requested foreground session. The pairing window is ten minutes; successful pairing issues a random 256-bit bearer token. Sessions expire after one hour, with at most eight sessions and eight pairing attempts per minute. Request size, header size, timeouts, queue depth and worker count are bounded.

API requests require authorization. Numeric Host and matching Origin checks reduce cross-origin and DNS-rebinding exposure. Assets are bundled; there are no CORS wildcards, cookies, arbitrary file access or shell-command endpoints. Pausing Home stops the server and invalidates its in-memory sessions.

**The LAN connection is HTTP, not TLS.** Use it only on a trusted private network. Pairing is access control, not encryption against someone observing LAN traffic. Do not port-forward or expose this server to the Internet.

The browser stores its token in session storage. Layout exports may contain installed app identifiers and personal customization. Review them before sharing.

## Local storage and publishing

Home order/preferences, wallpaper uploads, weather cache and MK-IV settings stay in app-private storage. Android cloud/device backup is disabled. No signing keys, private device configuration, inventories, pairing screens, network addresses or raw device logs are included in the public repository.

The example device configuration contains placeholders. Real values belong in ignored `.quiet-tv-device.json` or environment variables. Generated logs and recovery baselines remain local and may contain identifying details.

## Security reports

Do not put credentials, pairing codes, private device addresses or personal logs into a public issue. Use GitHub private vulnerability reporting for security-sensitive findings when available. Ordinary reproducible UI bugs can use public issues with redacted screenshots and model/OS details.
