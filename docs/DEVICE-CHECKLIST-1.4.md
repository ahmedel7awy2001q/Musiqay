# Musiqay 1.4 — Device acceptance checklist

This checklist is pending Android device execution. An APK build does not replace these checks.

1. Upgrade with the SAME signing certificate; favorites, playlists, settings and hidden folders must remain.
2. Permission denied, permanently denied, grant from system Settings and return: show the right screen and refresh without restarting.
3. Home: Play all starts playback; Resume opens the existing track; pause/resume preserves progress.
4. Browse a library of 5,000+ tracks while playing. Scroll and search should remain responsive. Compare start time, frame timings and memory on the same device before and after.
5. Search Arabic text with/without diacritics and alef variants; blank and no-match queries must show distinct states.
6. Add/delete a media file outside the app. The library should update and show loading/retry states as appropriate.
7. Create same-named folders at different paths. Hide a new path and verify the other stays visible. Old name-based hidden choices still apply until shown from Settings.
8. Open albums sharing a title but having different album IDs. They must stay separate.
9. Open a group, then press system Back: return to group list first. Playback remains available through the mini player.
10. Add a track twice to the queue. Selecting the second occurrence plays that occurrence; the active marker identifies only its index.
11. Drag queue entries up/down, including duplicates. Accessible move-up/move-down actions should also work. Test a long queue.
12. Remove an entry and Undo. Remove the current entry and Undo. Clear the queue, close/reopen the app: no old queue should return.
13. Restart with a saved duplicate occurrence. Delete files before/current/after it, then restart: restore a valid occurrence and bounded position without autoplay.
14. Create/rename/delete playlists. Deletion requires confirmation; audio files remain. Counts/durations refer to visible songs.
15. Rapidly toggle a favorite and create a playlist with its first track: no partial database operation or lost favorite state.
16. Test portrait/landscape, 320dp width, split screen and enlarged system font. Main controls must be reachable by scrolling where needed.
17. Test dark/light/system/dynamic colors, reduce motion and themed launcher icons. Confirm the notification icon remains legible.
18. Share a local audio file to a compatible receiving app. Confirm the file is readable without permanent storage permissions granted to that app.
19. Start playback and open Effects on a phone with/without a system equalizer. Effects target the current session; unsupported devices show a message.
20. Timer presets/custom Arabic digits/cancellation; background playback, screen lock, Bluetooth controls, headphone unplug and audio focus during calls.

21. Deny audio-file permission: Radio remains available; local music surfaces offer permission; no stale track can be played through Home/search.
22. Radio catalogue: seeded list, successful refresh, offline refresh error, search/Arabic normalization, favorites, categories and empty states; restart with an omitted favorite/last station.
23. Play Quran/Nogoum/9090/Nile/Mega/Sha3by/Hits and several directory stations on the actual Egyptian network. Confirm identity and audio, not just HTTP status. Logos must fit without cropping.
24. Switch local music at a nonzero position, with duplicates/shuffle/repeat, to radio and back. Restore the selected occurrence and time; add-to-queue and undo during radio must not interrupt its stream or corrupt local playback.
25. Lose network, switch station during retry, explicitly stop, resume from Bluetooth/notification, receive a call and unplug headphones. Check bounded retries and that stopped radio does not keep downloading.
26. Radio sleep timer, app background, screen lock, notification permission accepted/denied, service/process restart and no autoplay on app reopen.

Do not label the APK stable until this checklist and a signed Release build are verified. Unit tests and lint alone do not measure device speed or battery use.
