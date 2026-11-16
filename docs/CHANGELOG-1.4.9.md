# Musiqay 1.4.9

## Bluetooth Car Gateway
- Select the car head-unit native Bluetooth player once.
- Persist the selected launcher component locally.
- Open the saved Bluetooth player directly from Home.
- Fall back to the chooser if the selected app is removed or cannot launch.
- No Bluetooth scan, contact, notification or microphone permissions.

## Radio and external audio maintenance
- Remote radio catalogue refresh interval reduced to 4 hours.
- Radio Browser directory refresh reduced from 24 hours to 6 hours.
- Refreshed curated Egyptian station links and source labels.
- Added verified current ON Sport FM source.
- Added primary/fallback stream pairs for critical stations where a second usable route is available.
- Kept the remote disabled/block-list mechanism for emergency link removal without a new APK.
- Kept Mishary/Alafasy exclusion across remote, directory and retained stations.
- Network User-Agent now follows the actual app version.

## Live streams vs recordings
- RadioStation now distinguishes live streams from recorded archive audio.
- Nasr El Din Tobar archive is treated as recordings, not LIVE radio.
- Radio screen shows separate live/recording counts.
- Search says "station or recording".
- Tobar has a dedicated compact feature card and shorter category chip.
- Playback status and failure text now distinguish live connection failures from recording failures.
- Android Auto exposes archived recordings as playable audio rather than radio stations.

## Resilience
- Quran Cairo uses a stable RadioJar route with the existing HLS source as fallback.
- Nogoum FM has a current primary route plus previous stream fallback.
- Radio 9090 and Sha3by have fallback routes.
- Existing retry/backoff remains active after fallback selection.
- Platform launching is guarded against missing handlers on vendor car head units.
- Apple Music web search is localized to Egypt.

## Library diagnostics
- Settings now shows visible file count.
- Shows hidden-file count when folders are hidden.
- Shows the active minimum-duration filter.
- Shows whether only MediaStore music items are included.
- This makes changes in library count diagnosable without changing or deleting user data.

## UI
- Sleep timer redesigned into a compact grid for small phones and car screens.
- Radio source footer is generated from actual active source labels.
- Generic radio copy now uses "content" when the section may contain recordings.

## Version
- versionName: 1.4.9
- versionCode: 13
- branch: develop/v1.4.9-native-bluetooth-launcher
- Build has NOT been started.
