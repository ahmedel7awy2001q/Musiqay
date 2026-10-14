# Musiqay 1.4.9 — Native Bluetooth Player Gateway

## Why this change
The tested Android car head unit receives phone audio correctly through its vendor Bluetooth player, but it does not expose the incoming A2DP Sink connection to ordinary third-party apps. Showing a connection state inside Musiqay was therefore misleading.

## New behavior
- Removed dependency on A2DP connection discovery.
- Removed BLUETOOTH / BLUETOOTH_CONNECT permissions from Musiqay.
- First use:
  1. Open **بلوتوث الهاتف**.
  2. Choose the head unit's native Bluetooth Music app.
  3. Musiqay stores the selected launcher component locally.
- After selection, pressing **بلوتوث الهاتف** on Home opens the saved native Bluetooth player directly with one tap.
- If the saved app is later removed or fails to launch, Musiqay automatically returns to the chooser.
- The chooser prioritizes likely Bluetooth / BT Music / A2DP apps, with an option to show all launcher apps when vendor naming is unusual.
- The selected player can be changed at any time.
- System Bluetooth Settings remains available as a fallback.

## Safety / privacy
- No Bluetooth scanning.
- No contact access.
- No notification access.
- No microphone access.
- No audio interception or rebroadcasting.

## Version
- versionName: 1.4.9
- versionCode: 13
- branch: develop/v1.4.9-native-bluetooth-launcher
- Build has NOT been started.
