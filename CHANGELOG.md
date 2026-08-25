# Changelog

## 1.1.2 - 2026-08-25

- Replaced the shared bottom navigation panel with two translucent Scan and Create controls.
- Improved the active-tab contrast and added a matching glass treatment to the top bar.

## 1.1.1 - 2026-08-25

- Fixed a release-only startup crash caused by WorkManager database initialization after R8 optimization.

## 1.1.0 - 2026-08-24

### Added

- QR scanning from images selected with Android Photo Picker.
- Unobtrusive adaptive banner ads and scan-completion interstitials capped at one every five minutes.
- Monthly and lifetime QR Premium purchase options with restore through the user's Google Play account.
- Google UMP consent and in-app ad privacy controls.

### Fixed

- Made the Create screen's content-type label readable in dark mode.
- Added a monochrome launcher icon for themed Android icons.

## 1.0.1 - 2026-08-24

### Changed

- Refined the interface with contextual Material You gradients, compact floating navigation, clearer surfaces, responsive QR preview, and improved scanner controls.

### Fixed

- Improved contrast and touch targets across phone, tablet, light, and dark layouts.

## 1.0.0 - 2026-08-24

### Added

- Local QR scanning with CameraX and ZXing.
- Native Android actions for web, Wi-Fi, contacts, phone, email, SMS, maps, and calendar content.
- QR creation for nine standard content types.
- Custom module, finder, and background colors; shape controls and optional logo.
- Permissionless contact and photo pickers, PNG sharing, English and Czech UI.
- Privacy policy, GitHub Pages site, CI, signed releases, and Google Play metadata.
