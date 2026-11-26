# Masjid Map

A privacy-first, 100% free, offline-capable Islamic companion app for Android built with Kotlin, Jetpack Compose, and Material Design 3.

No accounts, no ads, no tracking, and **zero proprietary API keys required**.

---

## Features

### Nearby Masjid Finder & Offline Routing
- **Live & Cached Masjid Discovery:** Finds nearby masjids using OpenStreetMap (**Overpass API**) with automatic **Nominatim** and **Photon** fallbacks, supporting custom city/address search when GPS is off.
- **Interactive Map & List Views:** Built with `osmdroid` (no Google Maps API key required), displaying distance, accessibility/parking/women's section tags, Jumu'ah khutbah times, and favorite masjids.
- **Turn-by-Turn Driving & Walking Routes:** Computes routes via **OSRM** and caches masjid lists, road routes, and local OpenStreetMap tiles so map browsing and saved routes keep working completely offline.

### Astronomical Prayer Times & Alerts
- **On-Device Solar Calculations:** Computes Fajr, Sunrise, Dhuhr, Asr, Maghrib, and Isha offline using solar declination and equation-of-time math across **ISNA**, **Muslim World League**, **Umm al-Qura (Makkah)**, **Egyptian General Authority**, and **Karachi** methods, with **Standard** and **Hanafi** Asr support.
- **Night & Fasting Windows:** Tracks Suhoor end, Duha start, Iftar, Islamic Midnight, and the Last Third of the Night.
- **Prayer Notifications & Worship Tools:** Supports pre-prayer lead reminders, optional offline Adhan audio, gentle Fajr fade-in alarm, Suhoor reminders, auto Do-Not-Disturb during prayer, a persistent countdown notification, and an Android **Quick Settings Tile** for next prayer status.
- **Daily Worship Log & Hijri Calendar:** Track daily prayers (In Masjid / On Time / Late / Qada), fasting days, streaks, and Hijri dates with Jumu'ah and White Days highlights.

### Qur'an Reader & Recitation Player
- **Complete Offline Qur'an:** All 114 surahs (6,236 ayahs) with 6 English translations (defaulting to **Sahih International**), Arabic-only reading mode, adjustable Arabic (`Amiri`, `Scheherazade New`, `Noto Naskh Arabic`) and English typography, ayah search, reading progress, and bookmarks.
- **Verse-by-Verse Audio Player:** Supports Arabic reciters (Mishary Alafasy, Al-Husary, Abdul Basit), Urdu audio translation, and English audio narration via a background media playback service with sleep timer (`15m`, `30m`, `60m`, `End of Surah`).
- **Offline Packs:** Download full Qur'an translations, essential surah audio, or individual surah audio packs for 100% offline listening.

### Hadith & Adhkar Library (21 Collections)
- Includes *Sahih al-Bukhari*, *Sahih Muslim*, *Sunan an-Nasa'i*, *Sunan Abi Dawud*, *Jami' at-Tirmidhi*, *Sunan Ibn Majah*, *Muwatta Imam Malik*, *Musnad Ahmad*, *Riyad as-Salihin*, *40 Hadith an-Nawawi*, *40 Hadith Qudsi*, *Hisn al-Muslim*, and *HadeethEnc* collections.
- Supports full offline book downloads, cross-book offline search, bookmarks, and a daily Hadith highlight.

### Daily Utilities & Customizable Home Widget
- **Qibla Compass:** Real-time magnetic + True-North bearing and distance to the Kaaba.
- **Digital Tasbeeh, Adhkar, Duas & Guides:** Presets, lifetime counter, situational duas, and practical worship guides.
- **Zakat Calculator:** Gold and Silver Nisab calculations plus Zakat al-Fitr and Fidyah estimates.
- **Glance Home Screen Widget:** Responsive `4×1`, `4×2`, and `4×3` layouts showing the current prayer prominently, next prayer countdown, elapsed progress bar, 6 visual themes, opacity and corner controls, customizable Quick Tiles (including one-tap home screen **Tasbeeh +1**), and a clean Daily Hadith card without narrator-chain clutter.

---

## Download & Sideloading

Pre-built signed Release APKs (`MasjidMap-release.apk`) and their `SHA-256` checksums are published automatically in the **[Releases](../../releases)** tab.

### Why Android Shows Security Prompts When Sideloading
Because `MasjidMap-release.apk` is installed outside the Google Play Store:
- Android prompts **"Install unknown apps"** for your browser or file manager.
- **Google Play Protect** may show *"Unrecognized app / developer"* until enough users install a signed release or it is distributed through a recognized store.
- Samsung devices with **Auto Blocker** enabled block direct browser APKs by default.

These prompts are standard Android sideload gates—the app requests **no** invasive permissions (no contacts, SMS, call logs, background location, storage sweeps, or overlay permissions) and omits proprietary Google dependency blobs (`dependenciesInfo.includeInApk = false`).

### Verifying the APK Checksum
```bash
sha256sum -c MasjidMap-release.apk.sha256
```

For easier updates without manually downloading APKs each release, you can track this repository's Releases tab with **[Obtainium](https://obtainium.imranr.dev/)**.

---

## Building & Signing From Source

### Requirements
- **JDK 21**
- **Android SDK 36.1** (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`)
- **Gradle 9.3.1**

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest --no-daemon
```

### Build a Signed Release APK
`app/build.gradle.kts` reads your upload keystore via environment variables:

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password

gradle :app:assembleRelease --no-daemon
```

The GitHub Actions workflow in `.github/workflows/build-apk.yml` automatically runs tests, builds and verifies the signed APK (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` repository secrets), and attaches `MasjidMap-release.apk` plus `.sha256` to GitHub Releases on pushes and `v*` tags.

---

## Open Data Sources & Attribution

- **Maps, Masjid Discovery & Routing:** [OpenStreetMap](https://www.openstreetmap.org/), Overpass API, Nominatim, Komoot Photon, and OSRM
- **Qur'an Text & Translations:** `fawazahmed0/quran-api` via jsDelivr / GitHub
- **Qur'an Audio Recitations:** Islamic Network CDN & EveryAyah
- **Hadith & Adhkar Collections:** `fawazahmed0/hadith-api`, `AhmedBaset/hadith-json`, `wafaaelmaandy/Hisn-Muslim-Json`, and [HadeethEnc](https://hadeethenc.com/)

---

## Tech Stack

- **Language & UI:** Kotlin, Jetpack Compose, Material Design 3, Jetpack Glance (`androidx.glance`)
- **Architecture:** MVVM with `StateFlow`, Repository pattern, DataStore Preferences
- **Networking & Serialization:** Retrofit, OkHttp, Moshi
- **Mapping & Location:** `osmdroid`, Fused Location Provider, `SensorManager` (Qibla)
- **Testing:** JUnit 4, Robolectric, Kotlin Coroutines Test
