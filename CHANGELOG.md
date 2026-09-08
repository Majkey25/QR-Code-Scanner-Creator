# Changelog

## 1.1.5 - 2026-09-08

- Fixed monthly and lifetime Premium restoration when one Google Play product query fails.
- Ignore outdated purchase queries and preserve existing Premium while another purchase is pending.
- Block another purchase until Google Play confirms ownership, including while a purchase is pending or Premium is already active.
- Added subscription management and cancellation in About, including when Premium is active.
- Explain optional monthly renewal and one-time lifetime billing before purchase.
- Stop ad preloading after Premium activation and invalidate cached ads when privacy choices change.
- Keep late ad callbacks from restoring a dismissed banner.
- Added a private privacy-contact address and clarified SDK storage and subscription management.
- Added linked terms, refund and cookie policies, keyboard navigation and light/dark contrast checks for the website.
- Clarified that coffee contributions do not activate Premium and that QR recognition does not verify a destination's safety.

## 1.1.4 - 2026-08-25

- Made the floating navigation controls fully opaque for consistent readability.
- Moved banner ads below completed content so they no longer interrupt Scan or Create workflows.

## 1.1.3 - 2026-08-25

- Removed the reserved app-bar regions so content scrolls naturally beneath the translucent controls.

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
