# QR Code Scanner & Creator Android design

## Goal

Build a free Kotlin Android app for Android 10 and newer. It scans QR codes, turns supported payloads into safe native phone actions, creates common QR payloads, styles them, and shares PNG files.

## Product and UI

- Audience: people who need one direct QR scanner and maker without accounts, ads, or tracking.
- Primary actions: `Scan QR code` and `Create`.
- Structure: bottom navigation with `Scan` and `Create`; top overflow contains `About`.
- Visual direction: white or true dark surfaces, blue accent, restrained borders, 8 dp corners, 16/24 dp spacing, Material 3 typography, no gradients or card grid.
- Accessibility: 48 dp controls, content descriptions, readable contrast, system font scaling, and no required motion.

## Scanner

Use bundled CameraX 1.6.1 with ZXing 3.5.4. Ask for camera permission only when the scanner opens, process frames locally, accept QR codes only, and include manual content entry for camera-less devices and accessibility.

Map typed ML Kit results to native actions:

- URL -> browser, limited to `http` and `https`.
- Wi-Fi -> Android saved-network confirmation on Android 11+; Android 10 suggestion plus Wi-Fi panel.
- Contact -> prefilled Contacts insert screen.
- Phone, SMS, email -> dialer, messages, email app.
- Location -> maps.
- Calendar event -> prefilled Calendar insert screen.
- Other content -> copy or share; never execute arbitrary schemes.

Every external action requires the user to confirm in the target system app. Unsupported or invalid data stays visible and shareable.

## Creator

Support text, website, Wi-Fi, contact, email, phone, SMS, location, and calendar event. Contact values can be entered manually; a contact phone number can also come from Android's permissionless contact-data picker.

Build standard QR payload strings. Generate locally with ZXing 3.5.4 at error correction level H. Appearance options are foreground, finder, and background hex colors plus square, rounded, or dot modules. Reject invalid colors, empty required fields, and low foreground/background contrast. Share a cache PNG through `FileProvider`; request no storage permission.

## Privacy and legal

The app has no account, ads, analytics, tracking, or backend. Scanning and QR creation are offline. Camera frames stay on device and are not saved. Add `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`, a public privacy HTML page, source link, version, and the same Buy Me a Coffee destination used by ScanIt: `https://www.buymeacoffee.com/majkey`.

## Build and release

- Package: `com.majkeylab.qrscannercreator`
- `minSdk = 29`, `targetSdk = 37`, `compileSdk = 37`
- Kotlin Compose, Java 17, Gradle wrapper
- JUnit pure-logic tests, Android lint, debug and release builds
- GitHub Actions badge, signed tag release workflow, APK and SHA-256 asset
- Initial version: `1.0.0`, `versionCode = 1`

## Acceptance

- Unit tests prove payload escaping, validation, and QR rendering inputs.
- Emulator proves launch, both tabs, creator validation, QR preview/share flow, About/privacy/coffee links, scanner launch, one typed native action, and one old workflow after navigation.
- CI passes on `main`.
- Public `v1.0.0` release contains an installable signed APK and checksum.
