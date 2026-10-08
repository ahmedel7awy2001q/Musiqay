# Musiqay 1.4.2 — Verified build and delivery status

Date: 2026-10-08.

The installed 1.4.1 preview source archive was restored directly into this branch before implementing the listening update. This branch is the reviewable source for version 1.4.2.

## Validation

- Application source was built from commit 2a71abf61d93085db9a62ba580d9ef5a98fa5086.
- 32 JVM unit tests passed, with 0 failures, errors, or skipped tests.
- Android Lint completed with 0 errors and 30 warnings.
- Two Android 15 emulator tests passed at font scale 1.3. They cover per-file resume, named bookmarks, playback speed, end-of-recording sleep while the Activity is backgrounded, navigation, opening the manual station form, and repeated backup merges preserving existing favorites/playlists.
- The final UI-only validation at commit 282b533d06aa2e58aaed14f4b2579da32177e81d additionally checked explicit Arabic surah recognition on an Arabic recording filename, captured full Android windows including dialogs, and verified screenshot copy byte counts. Production application code is unchanged from the fully tested build.

Full tests/lint/build: https://github.com/ahmedel7awy2001q/Musiqay/actions/runs/37704010909
Final Arabic UI/screenshot verification: https://github.com/ahmedel7awy2001q/Musiqay/actions/runs/37706509046

## Update compatibility

| Property | Previous installed preview | New signed preview |
| --- | --- | --- |
| Package | com.musiqay.app.preview | com.musiqay.app.preview |
| Version name | 1.4.1-preview | 1.4.2-preview |
| Version code | 5 | 6 |
| Room schema version | 1 | 1 |
| Signing certificate SHA-256 | dc4f45f0a372b854d4447197178b1dcca655485b2573c943de0b5a68a34f5221 | dc4f45f0a372b854d4447197178b1dcca655485b2573c943de0b5a68a34f5221 |

Package/version attributes were read directly from both binary Android manifests. Both certificates were compared using apksigner. The signed update verifies with APK Signature Scheme v2 and v3.

The existing Room entities, columns and database version remain unchanged; resume positions and bookmarks use a separate additive preference store.

## Signed file checkpoint

The update was re-signed locally with the existing preview key. The key remains outside git and CI.

- Filename: Musiqay-1.4.2-Listening-Preview.apk
- Size: 24,219,366 bytes
- SHA-256: 2642da7d9ea26d765a2f80b9f257a22db14ed40e8b788f36353c3dde39b0bb82
- Runner artifact for the exact fully tested application: artifact 11519032009, run 37704010909.
- Artifact ZIP SHA-256: b332d0139f9f18374535e3025d0a103f95e33d8f7cadbec64818ff342dd4a8c6.

Final signed-file delivery is pending: the workspace disconnected during attachment preparation. The file upload failed with environment_offline, so no downloadable signed APK was delivered. CI artifacts contain the runner-signed APK before the local update-signing step.

To recover after reconnecting the workspace, use the existing local signed checkpoint if it remains present. Otherwise download the verified runner artifact and its apksigner.jar, materialize the private existing preview signing backup, sign locally, and repeat the certificate/package/hash verification. No new application build is necessary for this recovery.

## Practical scope

JSON backups merge favorites, lists, bookmarks, positions, manual stations and settings; they exclude audio files. MediaStore references are intended for the same phone and can require recreating file favorites/bookmarks when moved to a different file index.

The timer is owned by the playback service and survives leaving the Activity. Force-stop/process termination ends the service. Emulator tests do not validate every phone's battery policy, Bluetooth device, or every radio stream.

Hardware FM, Android Auto, widgets and cloud synchronization remain deferred.
