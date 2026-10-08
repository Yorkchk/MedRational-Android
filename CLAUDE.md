# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MedRational-Android is a Jetpack Compose client for the MedRational backend, a Spring Boot (Maven) REST API located in the sibling repository `../MedRational`.

Users browse hierarchical educational materials: **Categories → Reasonings → StudyFiles**, search files, favorite them, and download them. Users with administrative privileges can also create and delete categories, reasonings, and files (including multipart file uploads) directly within the user-facing screens.

## Commands

Run these from the repository root (use `gradlew.bat` on Windows):

```bash
./gradlew assembleDebug                 # Build debug APK
./gradlew installDebug                  # Install debug APK on connected device/emulator
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest          # Instrumented tests (needs connected device/emulator)
./gradlew lint                          # Run Android lint
./gradlew test --tests "com.example.medrational_android.ExampleUnitTest" # Run single test class
```

> **Note on Testing:** The repository currently only contains default boilerplate template tests (`ExampleUnitTest` and `ExampleInstrumentedTest`). When adding new features or fixing bugs, write unit tests for ViewModels and StateFlow emissions.

## Repository Structure

Package root: `app/src/main/java/com/example/medrational_android`

```text
├── data/
│   ├── api/             # Retrofit interfaces, ApiClient singleton, AuthInterceptor
│   ├── download/        # AndroidDownloader wrapping Android DownloadManager
│   ├── model/           # DTOs and API responses (Models.kt)
│   └── prefs/           # TokenManager (EncryptedSharedPreferences)
├── ui/
│   ├── components/      # Reusable composables (TopBar, SearchBar, Dialogs, Cards)
│   ├── screens/         # Feature screen composables (Auth, Categories, Reasonings, Favorites, Search)
│   └── theme/           # Color, Shape, Type, Theme definitions
├── viewmodel/           # StateFlow-driven ViewModels and UiState sealed interfaces
└── MainActivity.kt      # Entry point, single NavHost routing, ApiClient initialization
```

## Backend Connection & Networking

- **Local Host Mapping:** `ApiClient.BASE_URL` comes from `BuildConfig.BASE_URL`, set per build type in `app/build.gradle.kts` from `local.properties` (`api.baseUrl`, `api.releaseBaseUrl`). It defaults to `http://10.0.2.2:8080/` (reaches host machine from Android emulator). For physical devices, set `api.baseUrl` to the host machine's LAN IP (e.g., `http://192.168.x.x:8080/`). Ensure the Spring Boot backend in `../MedRational` is running.
- **Cleartext Traffic:** Enabled in `AndroidManifest.xml` (`android:usesCleartextTraffic="true"`) for local HTTP development.
- **DTO Mirroring:** Models in `data/model/Models.kt` mirror the Spring backend DTOs field-for-field using Gson serialization. Paginated endpoints return `PageResponse<T>` (extract records via `.content`). Always verify target endpoints in `../MedRational/src` before altering models.
- **File Downloads:** Downloads bypass Retrofit. UI components assemble download URLs (`${ApiClient.BASE_URL}api/v1/downloads/file/{id}` or `.../downloads/category/{id}/zip`) and pass them to `AndroidDownloader`, which delegates to the system's `DownloadManager` with bearer token authentication headers.
- **Multipart Uploads:** Admin file creation uses `MultipartBody.Part` for binary files alongside `RequestBody` for metadata fields (`title`, `description`, `reasoningId`).

## Architecture & State Management

Single-module (`:app`) project following MVVM without external DI frameworks (no Hilt/Koin):

- **`ApiClient`:** Singleton object holding OkHttp and Retrofit instances. `MainActivity.onCreate()` must call `ApiClient.initialize(context)` before any network dispatch.
- **Authentication & Interception:** `AuthInterceptor` attaches `Authorization: Bearer <jwt>` via `TokenManager`. On a **401 or 403 response**, all stored tokens and sessions are immediately invalidated, forcing a re-login on the next app cycle.
- **`TokenManager`:** Persists JWT, roles, `userId`, `email`, and `name` using `EncryptedSharedPreferences`. Admin role check is `isAdmin()` matching either `ROLE_ADMIN` or `ADMIN`.
- **UI RBAC:** Role-based access control is handled client-side. Screens query `TokenManager.isAdmin()` to conditionally display admin CRUD buttons, dialogs, and deletion actions. There are no standalone admin dashboards.
- **ViewModels & State:**
  - ViewModels expose immutable `StateFlow<*UiState>` collected with `collectAsStateWithLifecycle()` or `collectAsState()` in Compose.
  - State follows sealed hierarchies: `interface/sealed class *UiState { object Loading; data class Success(...); data class Error(val message: String); }`.
  - When instantiating newer ViewModels (`AuthViewModel`, `FavoriteViewModel`, `SearchViewModel`), pass dependencies via constructor (`TokenManager`, `MedRationalApi`) using `viewModel { ... }` factories. Older ViewModels (`CategoryViewModel`, `ReasoningViewModel`) access `ApiClient.api` directly and are activity-scoped in `MainActivity`.
- **Navigation:** Controlled by a single `NavHost` inside `MainActivity.MedRationalApp()`.
  - Routes: `welcome`, `login`, `signup`, `forgot_password`, `categories`, `favorites`, `search`, `reasonings/{categoryId}/{categoryName}`.
  - Startup routing: checks `TokenManager.hasToken()`. If valid, routes directly to `categories`; otherwise routes to `welcome`.

## Core Workflows

- **Two-Step OTP Auth:**
  1. Trigger step (`/auth/login`, `/auth/register`, `/auth/forgot-password`) sends an OTP code to the provided email.
  2. Verify step (`/auth/verify-otp`) accepts the code and returns user payload + JWT, persisted via `TokenManager.saveAuth(...)`.
- **Optimistic Favorites:**
  - Toggling favorites immediately mutates local UI state in `FavoriteViewModel`, followed by the network call to `/api/v1/users/{userId}/favorites`. Rolls back or resyncs on failure.

## Code Conventions & Guidelines

- **Kotlin & Compose Standards:**
  - Write pure declarative Jetpack Compose UI without XML layouts.
  - Avoid raw color hex values in screens; reference `MaterialTheme.colorScheme` or tokens from `ui/theme/Color.kt`.
  - Prefix event callback parameters with `on` (e.g., `onCategoryClick: (Long) -> Unit`, `onDismissRequest: () -> Unit`).
  - Pass `Modifier` as the first optional parameter to custom composables and chain modifications cleanly.
- **Error Handling:**
  - Wrap network operations inside `viewModelScope.launch` with `try-catch` blocks capturing `IOException` and `HttpException`.
  - Expose descriptive error strings or resource IDs through `UiState.Error` rather than crashing or swallowing exceptions.
- **Imports:** Do not use wildcard imports (`import foo.bar.*`). Maintain clean, explicit imports.

## Common Pitfalls & Troubleshooting

- **Connection Refused (`ECONNREFUSED`):** Verify the Spring Boot backend is running locally on port 8080. If testing on a physical device, confirm the device and computer share the same Wi-Fi subnet and set `api.baseUrl` in `local.properties` to the computer's LAN IP.
- **Unexpected Logouts:** A 403 Forbidden on an expired token or missing endpoint permissions will trigger `TokenManager.clear()`, wiping the stored token. Check backend security filters and Spring logs if requests trigger logout loops.
- **Context Initialization Crash:** If `ApiClient.api` throws `IllegalStateException` or `UninitializedPropertyAccessException`, verify `ApiClient.initialize(applicationContext)` ran inside `MainActivity.onCreate()`.

## Git workflow

`main` is protected: changes go through short-lived branches (`feat/…`, `fix/…`, `chore/…`, `docs/…`) and pull requests that must pass the Android CI workflow (`.github/workflows/android-ci.yml`: `lint testDebugUnitTest assembleDebug`). PRs are squash-merged, so PR titles use Conventional Commits (`feat(favorites): …`). Record user-facing changes under `[Unreleased]` in `CHANGELOG.md`.

## Testing policy

Every new feature or behavior change ships with tests in the same PR, and every bug fix includes a regression test that fails without the fix. Untested features are not considered done. See `docs/CI_AND_TESTS.md` §10 for test patterns and examples.
