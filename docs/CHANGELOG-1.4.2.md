# Musiqay 1.4.2 — Listening update

Based on the exact source archive used for the installed 1.4.1 preview. This branch restores that source directly into the repository, eliminating the stale archive build carrier. Main remains unchanged.

## Changes

- Independent resume positions per local file; recently unfinished files on Home.
- Named audio bookmarks, bounded rewind 10s / forward 30s, and optional 0.75–2x playback.
- Sleep timer owned by playback service, including stop at end of current recording.
- Explicit known-surah recognition, compact display titles, recitation/reader grouping, and hidden unknown/source-only metadata. Original file metadata is preserved.
- Library title/count spacing; slimmer progress slider with consistent left-to-right elapsed/total times; accessible mini player and larger touch targets; useful empty playlist guidance.
- Manual radio stations, favorite ordering, broadcast metadata when supplied, category fixes for reciters, and favorites on Home.
- Local JSON backup with preview and merge confirmation; repeated imports preserve existing favorites and playlist tracks. Audio files are excluded.
- Configurable initial Home/Library/Radio page and accurate privacy text.

## Data and compatibility

Room remains version 1 with unchanged entities. History/bookmarks use a separate preference store. The update APK must retain `com.musiqay.app.preview`, version code 6, and the prior preview signing certificate. The signing key is not in git or CI.

Backups reference local MediaStore IDs/URIs. Moving to another device or re-indexing files can require selecting files again; content is never copied or deleted by restore. The service timer survives leaving the screen, but not force-stop or operating-system process termination.

## Validation

The requested build runs unit tests, lint, and emulator tests with Arabic text at font scale 1.3. Device tests exercise independent resume, bookmarks, playback speed, end-of-recording sleep while backgrounded, navigation, manual-station entry, and repeated backup merge preservation. Final build evidence is provided with the delivered APK.

FM hardware access, cloud accounts, Android Auto, widgets, and speech crossfade are deferred from this update.
