# Playback and navigation audio

Quiet TV launches apps through normal Android activity intents. Returning Home does not force-stop a player or automatically pause/stop a Cast session. Playback policy remains with the player and Android.

The now-playing card observes active Android media sessions and invokes only supported transport actions when requested. Its access bridge does not process notification contents. The launcher has a short, original navigation sound; it does not request media audio focus. MK-IV is silent.

A previous investigation observed media activity near APK replacements. Replacing a foreground launcher can expose a previous task, so the documented update workflow first opens the neutral TV settings screen. That observation does not prove the cause of ordinary app-switching audio behavior.

The live YouTube Cast check was intentionally skipped in favor of a controlled, silent media-session test. No blanket claim is made about background audio across all receivers or streaming apps. Pause/stop the player or disconnect the Cast session when you want playback to end.

References: [Android audio focus](https://developer.android.com/media/optimize/audio-focus), [YouTube casting](https://support.google.com/youtube/answer/7640706).
