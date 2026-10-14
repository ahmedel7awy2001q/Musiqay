# Musiqay 1.4.8 — Bluetooth Car Hub

## Goal
Make Musiqay useful on Android car head units that already receive phone audio through the head unit's built-in Bluetooth/A2DP receiver.

## Added
- New **بلوتوث الهاتف** source on Home.
- Dedicated Bluetooth Phone screen.
- Runtime Bluetooth Connect permission on Android 12+.
- Bluetooth enabled/disabled state.
- Best-effort A2DP connected-device discovery where the head unit exposes the standard Android profile.
- Automatic discovery of launcher apps whose labels/packages indicate Bluetooth Music / A2DP.
- One-tap launch of the most likely native Bluetooth player.
- List of additional detected Bluetooth audio apps.
- Direct shortcut to system Bluetooth settings.
- Refresh action after pairing or changing the connected phone.
- Musiqay Mini Player is hidden on the Bluetooth screen to avoid confusing local/radio playback with the head unit's Bluetooth source.

## Compatibility model
Musiqay does **not** attempt to become an A2DP Sink itself. Android normally reserves that role for the system/vendor Bluetooth stack.

Some Android car head units expose their incoming A2DP connection through standard Android APIs; others keep the A2DP Sink profile private inside the vendor Bluetooth app. For that reason:
- A phone may be actively streaming even when Musiqay cannot read the connection state.
- The vendor Bluetooth Music app remains the authoritative playback path.
- Musiqay acts as a stable hub/launcher around that native path rather than intercepting or rebroadcasting the audio.

## Privacy
No notification-access service, notification reading, contact reading, microphone access, or Bluetooth scanning permission is introduced by this feature.

## Version
- versionName: 1.4.8
- versionCode: 12
- branch: develop/v1.4.8-bluetooth-car-hub
- Build has NOT been started.
