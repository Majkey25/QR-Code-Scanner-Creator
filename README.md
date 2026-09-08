# QR Code Scanner & Creator

[![Android CI](https://github.com/Majkey25/QR-Code-Scanner-Creator/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Majkey25/QR-Code-Scanner-Creator/actions/workflows/android-ci.yml)
[![Latest release](https://img.shields.io/github/v/release/Majkey25/QR-Code-Scanner-Creator)](https://github.com/Majkey25/QR-Code-Scanner-Creator/releases/latest)
[![Android 10+](https://img.shields.io/badge/Android-10%2B-0B57F0)](https://developer.android.com/about/versions/10)
[![License](https://img.shields.io/badge/license-source--visible-lightgrey)](LICENSE)

An Android QR scanner and creator. Scan with CameraX or from the gallery, act on QR content, create styled codes, and share PNG files. Processing runs locally; chosen Android or sharing actions pass content to the receiving app.

## What it does

- Scans QR codes on device with CameraX, Android Photo Picker, and ZXing.
- Opens selected web links in your browser and passes Wi-Fi, contacts, calls, email, SMS, maps, and calendar events to Android apps. QR recognition does not prove a destination is safe.
- Creates text, website, Wi-Fi, contact, email, phone, SMS, location, and event QR codes.
- Changes module, finder, and background colors; supports square, rounded, or dot modules and an optional logo.
- Shares generated PNG files without storage permission.
- Shows restrained ads with consent controls; monthly or lifetime QR Premium removes every ad through Google Play.
- Includes English and Czech UI.

## Install

Download the signed APK from [GitHub Releases](https://github.com/Majkey25/QR-Code-Scanner-Creator/releases/latest). Android may ask you to allow installs from your browser or file manager.

Premium uses Google Play and the purchasing account. A sideloaded APK does not guarantee purchase availability.

## Build

Requirements: JDK 17 and Android SDK 37.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

Scanning and creation run locally. Camera frames, selected images, and QR content are not sent to the ad service. Free installs use Google ads; see the [privacy policy](https://majkey25.github.io/QR-Code-Scanner-Creator/privacy.html) and [third-party notices](THIRD_PARTY_NOTICES.md).

## Terms and policies

- [Terms and conditions](https://majkey25.github.io/QR-Code-Scanner-Creator/terms.html)
- [Refund policy](https://majkey25.github.io/QR-Code-Scanner-Creator/refunds.html)
- [Cookie policy](https://majkey25.github.io/QR-Code-Scanner-Creator/cookies.html)

## Support

[Support me with coffee](https://www.buymeacoffee.com/majkey)

Coffee contributions do not remove ads or activate Premium. For private support, email [majkeylab@gmail.com](mailto:majkeylab@gmail.com).

Copyright © 2026 Majkey25.
