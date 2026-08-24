# Gallery QR scanning

## Goal

Release version 1.0.2 with two changes:

- Keep the **Content type** label readable in light and dark themes.
- Decode a QR code from an image selected through Android Photo Picker.

## UI

`CreatorUi.kt` uses `MaterialTheme.colorScheme.onSurface` for the **Content type** label. The label is dark in the light theme and light in the dark theme.

`ScannerActivity` shows **Scan from gallery** next to **Enter content manually**. Both actions remain available when camera permission is denied. The gallery action uses the system Photo Picker and does not request photo or storage permission.

## Image decoding

The picker accepts images only. `ScannerActivity` decodes the selected URI into a software bitmap with a bounded maximum dimension. A shared decoder converts bitmap pixels to `RGBLuminanceSource`, limits ZXing to `BarcodeFormat.QR_CODE`, and returns the decoded text.

The activity returns nonblank decoded text through the existing `EXTRA_RESULT` contract. Main-screen parsing and device actions remain unchanged.

## Errors

The scanner stays open when the user cancels the picker, the image cannot be read, or the image contains no QR code. A localized message distinguishes an unreadable image from an image without a QR code.

## Verification

Automated tests cover:

- Decoding a rendered QR bitmap back to its original content.
- Rejecting an ordinary bitmap without a QR code.
- Rejecting blank or invalid image input at the decoder boundary.

The release gate runs unit tests, Android lint, debug APK assembly, and release AAB assembly. Device and emulator testing are excluded by request.

## Release

Version 1.0.2 uses `versionCode = 3`. The change updates the changelog and English and Czech Play release notes. Delivery uses a feature branch, pull request, GitHub Release, and the existing Google Play testing tracks.

## Acceptance criteria

- **Content type** has sufficient contrast in both themes.
- The scanner can select an image without camera permission.
- A valid QR image returns the same result flow as camera scanning.
- Cancel, unreadable image, and no-QR image paths do not close the scanner.
- No photo or storage permission is added.
- CI and release workflows pass for version 1.0.2.
