# Quiet TV working sessions

At the start of a session in this repository, read
[quiet-tv-session](.agents/skills/quiet-tv-session/SKILL.md) before choosing the setup steps.

- When the user asks to start/resume a Quiet TV editing session, follow that skill to prepare the localhost preview, verified Wi-Fi ADB connection and live TV mirror. The request authorizes that setup; do not ask for the same permission again.
- For a specific code or documentation task, perform only the setup it needs. If live editing would help but the user has not requested it, offer the session workflow once while continuing independent work.
- Use this checkout's actual root. The local project was renamed from `oneplus-tv-patch` to `quiet-tv`; do not rely on an old chat's working directory.
- Keep device configuration, serials, pairing codes, raw captures and connection logs in the existing ignored local paths. Never copy them into the skill or public documentation.
- Preserve current Home and MK-IV preferences. Session startup does not authorize installing a build, changing the default launcher, switching HDMI, changing saved settings, or publishing code; follow the user's current task authorization for those actions.

Build and deployment details: [Development](docs/DEVELOPMENT.md),
[Setup & recovery](docs/SETUP.md), [Validation](docs/VALIDATION.md).
