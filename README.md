# MedRational Android

[![Android CI](https://github.com/Yorkchk/MedRational-Android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Yorkchk/MedRational-Android/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Android client for **MedRational**, a study platform for clinical reasoning. Students browse medical categories, read step-by-step clinical reasonings, and download or bookmark the study files attached to them. Admins manage the content from the same app.

Backend (Spring Boot REST API): **[Yorkchk/MedRational](https://github.com/Yorkchk/MedRational)**

<!--
## Screenshots
Add 3–4 screenshots to docs/screenshots/ and uncomment:

| Categories | Reasoning | Favorites | Search |
|---|---|---|---|
| <img src="docs/screenshots/categories.png" width="200"> | <img src="docs/screenshots/reasoning.png" width="200"> | <img src="docs/screenshots/favorites.png" width="200"> | <img src="docs/screenshots/search.png" width="200"> |
-->

## Features

- **Accounts:** sign up, login, and password reset, each confirmed with a one-time code sent by email. Sessions use JWTs stored in encrypted preferences.
- **Browse:** categories → clinical reasonings → attached study files, with in-app image preview.
- **Download:** a single file, all files of a reasoning as a ZIP, or a whole category as a ZIP.
- **Search:** full-text file search with category filters and clickable hashtags.
- **Favorites:** like a file with the heart icon; liked files appear in a dedicated favorites screen.
- **Admin mode:** admins create and delete categories and reasonings and upload files from the same screens.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM (ViewModel + `StateFlow` UI state) |
| Networking | Retrofit, OkHttp, Gson |
| Images | Coil |
| Security | AndroidX Security (`EncryptedSharedPreferences`) |
| CI | GitHub Actions (lint, unit tests, build) |

## Architecture

```
Compose screens ──► ViewModels (StateFlow UI state) ──► ApiClient (Retrofit)
                                                          │  JWT added by an OkHttp interceptor
                                                          ▼
                                   MedRational backend (Spring Boot) ──► PostgreSQL · Cloudflare R2 · SMTP
```

- `data/api`: Retrofit interface and the `ApiClient` singleton. A 401/403 from the server clears the stored session.
- `data/auth`: `TokenManager`, encrypted storage for the JWT, role, and user info.
- `viewmodel`: one ViewModel per feature, each exposing a sealed `Loading / Success / Error` state.
- `ui`: Compose screens grouped by feature; navigation lives in `MainActivity`.

## Getting started

**Requirements:** Android Studio (latest stable), JDK 17, and a running [MedRational backend](https://github.com/Yorkchk/MedRational) on port 8080.

1. Clone the repo and open it in Android Studio.
2. Optionally set the backend URL in `local.properties` (not committed):
   ```properties
   # Default: http://10.0.2.2:8080/ (your computer, as seen from the emulator)
   api.baseUrl=http://192.168.1.20:8080/
   # URL baked into release builds (defaults to api.baseUrl)
   api.releaseBaseUrl=https://api.example.com/
   ```
   On a physical device, use your computer's LAN IP; the phone must be on the same network.
3. Run the `app` configuration, or from the command line:
   ```bash
   ./gradlew installDebug
   ```

### Useful commands

```bash
./gradlew assembleDebug        # build a debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lint                 # Android lint
./gradlew assembleRelease      # minified release build (needs a signing config to install)
```

## Contributing

`main` is protected and always releasable. Work happens on short-lived branches (`feat/…`, `fix/…`, `chore/…`) merged through pull requests once CI passes. Commit messages and PR titles follow [Conventional Commits](https://www.conventionalcommits.org/). See [CHANGELOG.md](CHANGELOG.md) for release notes.

## Documentation

- [CI and tests](docs/CI_AND_TESTS.md): what runs on every pull request, what it checks, how to read failures, and how to add tests.
- [GitHub repository setup guide](docs/GITHUB_REPO_SETUP.md): the branching, PR, CI, and release workflow this repo follows.

## License

[MIT](LICENSE)
