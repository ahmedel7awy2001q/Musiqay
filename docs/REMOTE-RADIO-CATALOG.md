# Musiqay Remote Radio Catalog

The app reads the curated catalog from:

`https://raw.githubusercontent.com/ahmedel7awy2001q/Musiqay/main/config/radio-catalog-v1.json`

This allows station content to be maintained without shipping a new APK.

## Supported remote changes

- Add a station.
- Replace a broken stream URL.
- Provide a fallback stream URL.
- Change artwork, title, description, category or source label.
- Mark a station as featured and control priority.
- Disable a station immediately through `disabledIds`.
- Add blocked names/terms through `blockedTerms`.
- Change the refresh interval between 1 and 24 hours.

## Safety and resilience

- Only HTTP/HTTPS URLs accepted by Musiqay's URL validator are used.
- Remote payload is size-limited.
- Catalog schema is versioned.
- Older revisions cannot overwrite a newer cached catalog.
- A cached catalog is retained offline.
- Built-in stations remain as a final fallback.
- Radio Browser remains a secondary directory source.
- Android Auto refreshes the same catalog.

## Publishing a content-only radio update

1. Edit `config/radio-catalog-v1.json` on `main`.
2. Increase `revision`.
3. Commit the JSON file.
4. No APK build is required.
5. Musiqay will fetch the new catalog on Radio entry after the refresh TTL, or immediately when the user taps Refresh.

## Current policy

Content matching Mishary Alafasy / مشاري العفاسي is excluded by both built-in defaults and the remote block list.
