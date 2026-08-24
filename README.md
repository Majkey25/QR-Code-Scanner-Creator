# QR Code Scanner & Creator

[![Android CI](https://github.com/Majkey25/QR-Code-Scanner-Creator/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Majkey25/QR-Code-Scanner-Creator/actions/workflows/android-ci.yml)
[![Latest release](https://img.shields.io/github/v/release/Majkey25/QR-Code-Scanner-Creator)](https://github.com/Majkey25/QR-Code-Scanner-Creator/releases/latest)
[![Android 10+](https://img.shields.io/badge/Android-10%2B-0B57F0)](https://developer.android.com/about/versions/10)
[![License](https://img.shields.io/badge/license-source--visible-lightgrey)](LICENSE)

A modern Android QR scanner and creator. Scan with CameraX or from the gallery, act on standard QR content, create styled codes, and share PNG files. QR content stays on your device.

## What it does

- Scans QR codes on device with CameraX, Android Photo Picker, and ZXing.
- Opens safe web links and hands Wi-Fi, contacts, calls, email, SMS, maps, and calendar events to Android system apps for confirmation.
- Creates text, website, Wi-Fi, contact, email, phone, SMS, location, and event QR codes.
- Changes module, finder, and background colors; supports square, rounded, or dot modules and an optional logo.
- Shares generated PNG files without storage permission.
- Shows restrained ads with consent controls; monthly or lifetime QR Premium removes every ad through Google Play.
- Includes English and Czech UI.

## Install

Download the signed APK from [GitHub Releases](https://github.com/Majkey25/QR-Code-Scanner-Creator/releases/latest). Android may ask you to allow installs from your browser or file manager.

## Build

Requirements: JDK 17 and Android SDK 37.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

Scanning and creation run locally. Camera frames, selected images, and QR content are not sent to the ad service. Free installs use Google ads; see the [privacy policy](https://majkey25.github.io/QR-Code-Scanner-Creator/privacy.html) and [third-party notices](THIRD_PARTY_NOTICES.md).

## Support

[Support me with coffee](https://www.buymeacoffee.com/majkey)

Copyright © 2026 Majkey25.
