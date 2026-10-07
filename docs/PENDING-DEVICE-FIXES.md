# Musiqay 1.4 — Radio crash hardening + UI polish (UNBUILT)

Date: 2026-10-07
Status: source-only working batch. **No Gradle build, APK generation, GitHub Actions run, push, PR, or merge was performed for this batch.**

## Device evidence
The supplied 82.6s screen recording shows the app returning to the Android launcher immediately after tapping the Radio destination, repeatedly. The Radio UI does not remain on screen long enough to stabilize. No Logcat stack trace is available yet, so the exact throwable is not claimed as proven.

## Radio crash hardening
- `RadioRepository.refresh` now guards cache load, bundled catalog merge, directory refresh, deduplication, persistence, and fallback as one failure-contained path.
- Cancellation still propagates normally; ordinary radio/data failures fall back to curated Egyptian stations instead of escaping the coroutine.
- Station IDs are deduplicated before presentation.
- `MusicViewModel.refreshRadio` no longer allows settings/catalog exceptions to take down the app process.
- `RadioScreen` renders a safe shell first, sanitizes station entries, guards search normalization, and uses unique prefixed LazyColumn keys.
- Radio loading/error/empty states were redesigned and kept interactive.

## UI polish batch
- Home cards: smaller radius, lighter outlines/shadows, denser spacing and icon containers.
- Library/album/folder/playlist cards: reduced vertical footprint and more consistent iconography.
- Song rows: clearer active state with less visual weight.
- Mini player: slimmer, lighter, more compact controls.
- Bottom navigation: lighter selected treatment and more balanced icon scaling.
- Now Playing: compact tonal top actions, refined artwork frame, tighter controls/action buttons.
- Settings: denser theme cards and setting surfaces, lighter borders/shadows.
- Ambient background: retained premium identity with subtler glow/wave strength.
- Radio screen: fully restyled into a compact live-radio layout with favorites, filters, status, retry and current-station controls.

## Validation intentionally deferred
Per project rule, no build was executed. Before a release APK is produced, the next authorized build should verify:
1. Tap Radio repeatedly from every root tab without process death.
2. Cold-start then enter Radio with network on/off and with airplane mode.
3. Radio catalog fallback when cache is empty/corrupt and when directory request fails.
4. Start/stop several curated and directory stations, then return to local music.
5. Favorite/unfavorite, search, categories, timer, notification/lock-screen controls.
6. Light/dark/system themes and compact card/icon layout on the target phone.
7. Confirm no regression in local music, queue, playlists or hidden folders.

If Radio still crashes after the authorized build, capture Android Logcat around the tap; that will identify the exact remaining throwable rather than relying on inference.
