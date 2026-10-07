# Musiqay 1.4 — Egyptian radio implementation

The reference is the public store description/screenshots for `com.appmind.radios.egypt`. Its implementation and internal station database were not accessed. Musiqay implements the same public concept: internet radio, searchable stations, favorites, background playback and a sleep timer. It does not implement the reference app's alarm, podcasts or Chromecast.

## Sources

- Reference: https://play.google.com/store/apps/details?id=com.appmind.radios.egypt
- Public community directory/API: https://docs.radio-browser.info/
- Egypt query: `/json/stations/bycountrycodeexact/EG?hidebroken=true&order=clickcount&reverse=true&limit=1000`
- Quran: https://misrquran.gov.eg/ — public `radioStream` in the site's player JavaScript, HLS with AAC audio and a static video track. Video selection is disabled, but multiplexed video bytes can still be downloaded. The original official WebP header logo is bundled without editing.
- Nogoum/Nile: the broadcasters' public players at `audio.nrpstream.com/public/nogoumfm/embed` and `audio.nrpstream.com/public/nile_fm/embed` expose their live MP3 mounts.
- 9090: https://www.9090.fm/ and its public `9090streaming.mobtada.com/9090FMEGYPT` endpoint.
- Mega, Sha3by and Radio Hits: public Icecast endpoints at `megafm927.radioca.st/stream`, `radio95.radioca.st/stream` and `radiohits882.radioca.st/;`. The servers returned station names and MP3 samples during research. These are not claimed to be permanent or guaranteed.

The bundled snapshot contains 170 directory entries, with many duplicate mounts. The UI deduplicates stream addresses, prioritizes curated broadcaster streams, and uses stable Zeno/Radiojar mounts instead of expiring redirect tokens. The displayed count is the actual merged count and can change after refresh. A directory health flag is not an Android playback test. The catalogue includes online music/reciter channels classified as Egyptian by the community, as well as terrestrial stations with internet streams.

There is no claim that every reference-app station is present. Radio Masr's official page currently embeds a Dailymotion video player; an expiring signed video URL is not stored as an audio station. Nagham's probed endpoint returned audio without identifying its station, so it was not silently labelled as verified Nagham. These need confirmed stable audio sources before inclusion.

## Behavior and persistence

- Existing package, Room database name/version, music favorites, playlists and hidden-folder keys are preserved.
- Radio favorites and last station use additive DataStore keys; catalogue cache uses separate SharedPreferences.
- Radio media IDs are prefixed `radio:`. They never overwrite `playback_state`'s local queue IDs, current occurrence, time, shuffle or repeat.
- A return-to-music action filters missing files and restores a valid selected occurrence/position. Queue additions/undo during radio update the saved music session without interrupting the station.
- Cache lasts one day; a bundled snapshot is used on first open or invalid cache. User refresh bypasses the cache. Failed refresh retains the previous catalogue and offers retry. Favorite/last stations omitted by a fresh directory result are retained.
- Network fetch/JSON processing run on IO; a cached search index avoids normalizing every row on every keystroke. Arabic and Persian digits are normalized for search/identity. Radio does not run local-song progress updates; its controller polling interval is 10 seconds and playback state changes arrive through listeners.
- One Media3 service handles both modes, with audio focus and noisy-output handling. HTTP source timeouts are 10/12 seconds; radio reconnect delays are 3/6/12 seconds, then stop. Explicit pause/stop cancels retries and releases the live connection; resume seeks the live edge.
- No automatic station playback on app start. Notification permission is requested once after the user chooses a station; denial does not block radio.
- Radio needs internet; catalogue/logo downloads also use data. Some community streams use cleartext HTTP. There is no account, analytics SDK or radio-directory click/vote submission.

## Validation status

Final local verification on 2026-10-07 succeeded with `-PpreviewBuild=true :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. 23 tests passed, with zero failures/errors. Lint reported zero errors and 10 nonblocking warnings: two ModifierParameter, one ExportedService and seven UseKtx. The final build took one minute with cached dependencies. This measures build verification, not phone performance.

Six curated streams returned HTTP 200 and MP3 samples decoded by ffprobe at 44.1 kHz: Nogoum, Nile, 9090, Mega, Sha3by and Hits. Quran returned a valid HTTP 200 HLS master playlist; an earlier research probe decoded its AAC audio. Full listening, background playback, audio focus, Bluetooth, timers and UI acceptance on Android remain pending. Directory stations were not all individually listened to or verified on a phone.

`docs/APK-VERIFICATION.txt` records the signed preview APK's package, version, certificate and SHA-256. The original Room database source was compared byte-for-byte with the saved 1.4 baseline and remains unchanged. Release shrinking and a same-key production upgrade have not been verified.

## Signing and the separate preview install

The saved `Musiqay-Premium-V3-Final.apk` uses Android Debug certificate SHA-256 `81596938da2feadf91869cb6e6458a8a24c7ef3437c9e285c3b968c91ed51031`. The available build key differs; the saved 1.3 source archive contains no keystore and its old workflow generated a debug APK without persisting the key. No claim is made that the current APK can upgrade that installation.

`-PpreviewBuild=true` builds a separate debug application, `com.musiqay.app.preview`, labelled `موسيقاي • تجربة`. It has its own empty favorites/settings/queue and leaves the original app installed. The default production applicationId remains `com.musiqay.app`. A direct update must use the original signing key; do not uninstall the old app or clear its data to work around a certificate mismatch.

Preview certificate SHA-256: `dc4f45f0a372b854d4447197178b1dcca655485b2573c943de0b5a68a34f5221`. Its debug key is saved separately as a private signing backup for subsequent preview updates; it is not bundled in the source repository/archive or used as the old production key.
