# Gallery, ads, and Premium implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Release version 1.1.0 with gallery QR decoding, fixed dark-theme contrast, consent-gated ads, and Google Play Premium that removes ads.

**Architecture:** Keep image loading in `ScannerActivity` and put QR pixel decoding in a testable Kotlin function. Port the proven ScanIt beta monetization pattern into small policy, ads, billing, and Premium UI files. Google Play purchases are the entitlement source, UMP gates ads, and a five-scan plus five-minute policy gates interstitials.

**Tech stack:** Kotlin, Jetpack Compose, ZXing 3.5.4, GMA Next-Gen SDK 1.4.0, UMP 4.0.0, Play Billing 9.1.0, JUnit 4, Gradle, GitHub Actions, AdMob, Google Play Console.

---

### Task 1: Add test-first gallery decoding

**Files:**
- Create: `app/src/main/java/com/majkeylab/qrscannercreator/QrImageDecoder.kt`
- Create: `app/src/test/java/com/majkeylab/qrscannercreator/QrImageDecoderTest.kt`
- Modify: `app/src/main/java/com/majkeylab/qrscannercreator/ScannerActivity.kt`

- [ ] Write tests proving that rendered QR pixels decode, a plain image returns `null`, and invalid pixel dimensions throw `IllegalArgumentException`.
- [ ] Run `./gradlew testDebugUnitTest --tests '*QrImageDecoderTest' --console=plain` and verify compilation fails because `decodeQrPixels` is missing.
- [ ] Implement `decodeQrPixels(width, height, pixels)` with `RGBLuminanceSource`, `HybridBinarizer`, QR-only ZXing hints, and `ReaderException -> null`.
- [ ] Register `ActivityResultContracts.PickVisualMedia` in `ScannerActivity`, load a software bitmap capped at 2048 px, and pass its pixels to `decodeQrPixels`.
- [ ] Keep cancel silent, keep the scanner open on error, and return successful text through `EXTRA_RESULT`.
- [ ] Run the focused test and commit with `feat: scan QR codes from gallery`.

### Task 2: Define ad and Premium policies with RED-GREEN tests

**Files:**
- Create: `app/src/main/java/com/majkeylab/qrscannercreator/MonetizationPolicy.kt`
- Create: `app/src/test/java/com/majkeylab/qrscannercreator/MonetizationPolicyTest.kt`

- [ ] Write failing tests for these rules:

```kotlin
assertFalse(isInterstitialDue(4, 300_000, null))
assertTrue(isInterstitialDue(5, 300_000, null))
assertFalse(isInterstitialDue(10, 299_999, 0))
assertTrue(isInterstitialDue(10, 300_000, 0))
assertTrue(shouldShowAds(premium = false, entitlementVerified = true, canRequestAds = true))
assertFalse(shouldShowAds(premium = true, entitlementVerified = true, canRequestAds = true))
assertTrue(hasPremiumEntitlement(listOf(PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), Purchased))))
assertFalse(hasPremiumEntitlement(listOf(PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), Pending))))
```

- [ ] Run the focused test and verify RED because the policy symbols do not exist.
- [ ] Implement:

```kotlin
internal const val PREMIUM_PRODUCT_ID = "qr_scanner_creator_premium"
private const val INTERSTITIAL_COOLDOWN_MILLIS = 300_000L

internal fun isInterstitialDue(completedScans: Int, now: Long, last: Long?): Boolean =
    completedScans > 0 && completedScans % 5 == 0 &&
        (last == null || now - last >= INTERSTITIAL_COOLDOWN_MILLIS)

internal fun shouldShowAds(premium: Boolean, entitlementVerified: Boolean, canRequestAds: Boolean) =
    !premium && entitlementVerified && canRequestAds
```

- [ ] Add the purchased, pending, and unknown entitlement model. Only a matching purchased product grants Premium.
- [ ] Run the tests and verify GREEN. Commit with `feat: define monetization policy`.

### Task 3: Integrate AdMob consent, banners, and interstitials

**Files:**
- Create: `app/src/main/java/com/majkeylab/qrscannercreator/Ads.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Modify: `app/proguard-rules.pro`

- [ ] Add dependencies pinned to the verified ScanIt and Google versions:

```kotlin
implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0")
implementation("com.google.android.ump:user-messaging-platform:4.0.0")
implementation("com.android.billingclient:billing:9.1.0")
```

- [ ] Add `INTERNET` and the AdMob application metadata. Debug uses Google's demo app and ad-unit IDs. Release uses:

```text
app: ca-app-pub-6991329209066655~5561017627
banner: ca-app-pub-6991329209066655/6914512227
interstitial: ca-app-pub-6991329209066655/1662185545
```

- [ ] Port the ScanIt `ConsentGate`: call `requestConsentInfoUpdate` at launch, call `loadAndShowConsentFormIfRequired`, expose `canRequestAds`, and expose the privacy-options requirement.
- [ ] Implement an inline adaptive banner that creates no layout gap before load and destroys its `AdView` on disposal.
- [ ] Implement an interstitial preloader. Record completed scans in bounded integer preferences. Poll and show only when `isInterstitialDue` returns true.
- [ ] Ensure every interstitial completion path invokes the original action exactly once.
- [ ] Use Google demo IDs for `BuildConfig.DEBUG`; never request production ads in debug.

### Task 4: Integrate Play Billing and Premium UI

**Files:**
- Create: `app/src/main/java/com/majkeylab/qrscannercreator/PremiumBilling.kt`
- Create: `app/src/main/java/com/majkeylab/qrscannercreator/PremiumUi.kt`
- Modify: `app/src/main/java/com/majkeylab/qrscannercreator/MainActivity.kt`
- Modify: `app/src/main/java/com/majkeylab/qrscannercreator/AppUi.kt`
- Modify: `app/src/main/java/com/majkeylab/qrscannercreator/CreatorUi.kt`

- [ ] Port the ScanIt Billing 9.1.0 controller with one active `BillingClient`, auto reconnection, pending purchases, product-detail queries, purchase queries, and acknowledgement.
- [ ] Refresh purchase state at startup and `onResume`. Never persist a local Premium boolean.
- [ ] Block ads until entitlement verification finishes. Purchased Premium hides banner and interstitial ads.
- [ ] Add a Premium panel to About with localized price, buy, restore, account link, pending, active, unavailable, and error states.
- [ ] Add the UMP privacy-options link below the privacy policy when required.
- [ ] Add one inline banner after primary content on Scan and Create. Do not place ads next to a destructive or primary tap target.
- [ ] After every fifth successful scan, call the interstitial wrapper before showing the parsed result. Failed and canceled scans do not increment the count.
- [ ] Fix **Content type** with `MaterialTheme.colorScheme.onSurface`.
- [ ] Add **Scan from gallery** above manual entry in both camera states.

### Task 5: Update legal text, store assets, and version 1.1.0

**Files:**
- Modify: `docs/privacy.html`
- Modify: `THIRD_PARTY_NOTICES.md`
- Modify: `CHANGELOG.md`
- Modify: `fastlane/metadata/android/en-US/full_description.txt`
- Modify: `fastlane/metadata/android/cs-CZ/full_description.txt`
- Create: `fastlane/metadata/android/en-US/changelogs/3.txt`
- Create: `fastlane/metadata/android/cs-CZ/changelogs/3.txt`
- Modify: `app/build.gradle.kts`
- Replace: `fastlane/metadata/android/*/images/phoneScreenshots/*.png`

- [ ] Bump to `versionCode = 3` and `versionName = "1.1.0"`.
- [ ] State that AdMob and partners process consent choices, device identifiers, app interactions, and diagnostics for advertising. State that Google Play processes purchase and account data for Premium.
- [ ] Document GMA Next-Gen 1.4.0, UMP 4.0.0, and Billing 9.1.0 in third-party notices.
- [ ] Recreate one temporary Android 10 Google Play AVD from the installed SDK. Test only this app and delete the AVD after QA.
- [ ] Capture 1080x1920 screenshots in this order: Scan, scanner gallery controls, Create, About with Premium. Keep About last.
- [ ] Run unit tests, lint, debug APK, and release AAB. Commit with `chore: prepare 1.1.0 release`.

### Task 6: Release and Play configuration

**Files:**
- Modify Play Console and AdMob only through their authenticated UIs.

- [ ] Push the feature branch, open a PR, wait for CI, review the diff, and merge only after green checks.
- [ ] Tag `v1.1.0`, wait for the signed GitHub Release, download assets, and verify hashes and signatures.
- [ ] Create non-consumable product `qr_scanner_creator_premium` after the Play merchant account is available. Use a one-time purchase option and activate it.
- [ ] Upload the AAB to internal and Alpha tracks with the existing testers and all countries.
- [ ] Reorder Play screenshots to Scan, Scanner, Create, About.
- [ ] Change the ads declaration to **Yes**. Update Advertising ID, Data safety, privacy policy, and store descriptions to match AdMob and Billing.
- [ ] Submit changes for Google review and record exact external blockers. The merchant-account screen is a human financial gate if Google requests payment, tax, or identity data.

### Acceptance evidence

- Unit tests prove gallery decoding, five-minute ad cooldown, consent gating, and Premium entitlement rules.
- Debug uses Google demo ads. Release contains only the QR-specific AdMob IDs.
- UMP message is published for both ScanIt and QR apps with the QR privacy URL.
- Premium has no local unlock flag and only a purchased matching Play product removes ads.
- Screenshots match version 1.1.0 and About is last.
- The temporary emulator is removed after QA.
- GitHub and Play publication states are linked in the final report.
