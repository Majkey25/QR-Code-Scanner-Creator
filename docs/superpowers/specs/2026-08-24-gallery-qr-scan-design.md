# Gallery QR scanning

## Goal

Release version 1.1.0 with these changes:

- Keep the **Content type** label readable in light and dark themes.
- Decode a QR code from an image selected through Android Photo Picker.
- Add consent-gated advertising with a five-minute fullscreen cooldown.
- Add a Google Play Premium purchase that removes all ads.

## UI

`CreatorUi.kt` uses `MaterialTheme.colorScheme.onSurface` for the **Content type** label. The label is dark in the light theme and light in the dark theme.

`ScannerActivity` shows **Scan from gallery** next to **Enter content manually**. Both actions remain available when camera permission is denied. The gallery action uses the system Photo Picker and does not request photo or storage permission.

## Image decoding

The picker accepts images only. `ScannerActivity` decodes the selected URI into a software bitmap with a bounded maximum dimension. A shared decoder converts bitmap pixels to `RGBLuminanceSource`, limits ZXing to `BarcodeFormat.QR_CODE`, and returns the decoded text.

The activity returns nonblank decoded text through the existing `EXTRA_RESULT` contract. Main-screen parsing and device actions remain unchanged.

## Errors

The scanner stays open when the user cancels the picker, the image cannot be read, or the image contains no QR code. A localized message distinguishes an unreadable image from an image without a QR code.

## Ads and consent

The Play build uses GMA Next-Gen SDK 1.4.0 and UMP 4.0.0. Debug builds use Google's demo ad IDs. Release builds use the dedicated AdMob app and ad units created for `com.majkeylab.qrscannercreator`.

An inline adaptive banner appears after primary content on the Scan and Create screens. The slot exists only after an ad loads, so failed or blocked ads leave no empty space.

An interstitial can appear only after every fifth completed scan. A process-wide monotonic clock enforces a 300,000 ms cooldown. The app never shows an interstitial at launch, before an action, after a failed scan, or more than once during the cooldown.

The app requests updated UMP consent information at launch. Ads remain blocked until both consent and the Premium entitlement check allow them. About includes a UMP privacy-options action when Google requires it.

## Premium

Premium is a non-consumable Google Play one-time product with ID `qr_scanner_creator_premium`. Billing Library 9.1.0 queries purchases after connecting and whenever the app resumes. Only a matching purchase in `PURCHASED` state grants Premium. Pending, unknown, and unrelated purchases do not grant it. The app acknowledges completed purchases.

Premium has no local unlock switch. Google Play purchase data is the entitlement source. R8 and Google Play automatic protection raise the cost of repackaging, but a client-only application cannot be made fully tamper-proof without a purchase-verification backend.

About shows the localized Play price, purchase, restore, pending, active, unavailable, and error states. Ads disappear only after Play verifies Premium.

## Verification

Automated tests cover:

- Decoding a rendered QR bitmap back to its original content.
- Rejecting an ordinary bitmap without a QR code.
- Rejecting blank or invalid image input at the decoder boundary.

The release gate runs unit tests, Android lint, debug APK assembly, and release AAB assembly. A temporary Android 10 Google Play emulator verifies gallery scanning, test ads, consent UI, dark-theme contrast, Premium states available without a live product, and store screenshots. The AVD is deleted after QA.

## Release

Version 1.1.0 uses `versionCode = 3`. The change updates the privacy policy, third-party notices, changelog, English and Czech store descriptions, Play declarations, and release notes. Delivery uses a feature branch, pull request, GitHub Release, and the existing Google Play testing tracks.

## Store screenshots

The regenerated screenshots use this order:

1. Scan home screen.
2. Scanner controls with **Scan from gallery**.
3. QR creator screen.
4. About dialog with Premium.

The About screenshot stays last because it does not explain the primary scan or create workflows.

## Acceptance criteria

- **Content type** has sufficient contrast in both themes.
- The scanner can select an image without camera permission.
- A valid QR image returns the same result flow as camera scanning.
- Cancel, unreadable image, and no-QR image paths do not close the scanner.
- No photo or storage permission is added.
- Release builds use the QR-specific AdMob IDs and debug builds use only Google demo IDs.
- Banners and interstitials stay hidden until consent and entitlement checks complete.
- Interstitials require five completed scans and a five-minute cooldown.
- A verified Premium purchase removes all ads and restores from the Google Play account.
- The store screenshot order is Scan, Scanner, Create, About.
- CI and release workflows pass for version 1.1.0.
