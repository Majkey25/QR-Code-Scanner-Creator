# QR Code Scanner & Creator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and publish the Android 10+ scanner and creator defined by the approved design.

**Architecture:** One Compose activity owns launchers and app navigation. Pure Kotlin payload/style logic feeds a ZXing bitmap renderer; one Android helper maps typed ML Kit results to explicit safe intents.

**Tech Stack:** Kotlin 2.4.10, Compose Material 3, AGP 9.3.1, CameraX 1.6.1, ZXing core 3.5.4, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-24-android-app-design.md`

## Global constraints

- Package `com.majkeylab.qrscannercreator`; `minSdk = 29`, `targetSdk = 37`, `compileSdk = 37`.
- No account, ads, analytics, backend, or storage permission; camera permission is requested only inside the scanner.
- External content can open only typed Android actions; raw URI schemes never execute.
- UI uses English defaults plus Czech translations.

---

### Task 1: Project, payload contract, and tests

**Files:** Gradle files, manifest/resources, `QrPayload.kt`, `QrPayloadTest.kt`.

**Interfaces:** `QrDraft.payload(): Result<String>`, `QrStyle.parse(): Result<ParsedQrStyle>`.

- [ ] Write table-driven JUnit tests for valid payloads, escaping, empty data, malformed coordinates, invalid colors, and low contrast.
- [ ] Run `gradlew.bat testDebugUnitTest --tests "*QrPayloadTest"` and confirm failure because production types do not exist.
- [ ] Add the minimum pure Kotlin models and builders.
- [ ] Re-run the focused test and confirm pass.

### Task 2: QR renderer and sharing

**Files:** `QrRenderer.kt`, `QrRendererTest.kt`, FileProvider XML.

**Interfaces:** `QrRenderer.render(payload, style, size): Bitmap`, `shareQr(context, bitmap)`.

- [ ] Write a renderer test that rejects invalid size and proves a valid matrix has dark and light pixels.
- [ ] Run the focused test and confirm failure before `QrRenderer` exists.
- [ ] Render ZXing matrices with a four-module quiet zone, finder color, module style, error correction H, and cache PNG sharing.
- [ ] Re-run renderer and payload tests.

### Task 3: Scanner and native actions

**Files:** `DeviceActions.kt`, `MainActivity.kt`.

**Interfaces:** `ScanResult.from(Barcode)`, `DeviceActions.perform(context, result): ActionOutcome`.

- [ ] Add tests for safe URL scheme selection and Android-independent result labels; confirm red.
- [ ] Map ML Kit URL, Wi-Fi, contact, phone, SMS, email, geo, calendar, and raw text to explicit actions.
- [ ] Use `ACTION_WIFI_ADD_NETWORKS` on API 30+ and a network suggestion plus Wi-Fi panel on API 29.
- [ ] Run unit tests and compile.

### Task 4: Minimal Compose UI and About

**Files:** `AppUi.kt`, theme/resources, launcher icons.

**Interfaces:** `QrApp(onScan, onShare, onAction, onPickPhone)`.

- [ ] Add two-tab Scan/Create UI, typed forms, style inputs, preview, loading/error states, and overflow About dialog.
- [ ] Add privacy, notices, source, Buy Me a Coffee, version, and Czech strings.
- [ ] Run unit tests, `lintDebug`, `assembleDebug`, and install on `emulator-5554`.
- [ ] Verify happy, edge, failure, and regression flows with UI-tree coordinates, screenshots, and logcat.

### Task 5: GitHub publication and release

**Files:** README, license/notices/privacy, Pages privacy HTML, workflows, changelog.

- [ ] Add Android CI and signed tag-release workflows, badges, install/use docs, and curated changelog.
- [ ] Run the full local gate and inspect the staged diff for secrets and unrelated files.
- [ ] Push `main` to the new public repo, configure signing secrets and Pages, tag `v1.0.0`, then wait for Actions.
- [ ] Download the live APK/checksum, verify SHA-256 and signing certificate, install it on the emulator, and run a smoke flow.
