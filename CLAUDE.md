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
- **File Downloads:** Downloads bypass Retrofit. UI components assemble download URLs (`${ApiClient.BASE_URL}api/v1/downloads/file/{id}` or `.../downloads/category/{id}/zip`) and pass them to `AndroidDownloader`, which delegates to the system's `DownloadManager` with bearer token authentication headers. The backend uses that token to record single-file downloads for the Recent downloads screen (`GET /api/v1/users/{userId}/recent-downloads`), so the app never records them itself.
- **Suggestions:** Private threads between the user who sent a suggestion and the admins (`/api/v1/suggestions`). The backend takes the caller from the JWT and returns 403 to anyone else, 409 once a suggestion is closed, and 413/415 for attachments over 10 MB or of an unsupported type. Attachments are never public: download them through `.../suggestions/{id}/attachment` or `.../messages/{messageId}/attachment` with `AndroidDownloader`. Only admins may call the inbox (`GET /api/v1/suggestions`), because a 403 logs the user out. Picked files are read through `data/upload/PickedFiles.kt`.
- **Novelties:** The "What's new" feed (`/api/v1/novelties`) takes the caller from the JWT, so only call it when signed in. A file is `isNew` when uploaded after the user's `noveltiesSeenAt` baseline (account creation until the first visit). Logging in doesn't move the baseline. `NoveltiesViewModel` loads once per visit and then calls `mark-seen`, so highlights survive opening a file and coming back. The home badge comes from `/novelties/badge-count`.
- **Ratings:** Files are rated 1–5 through `/api/v1/files/{fileId}/ratings` (GET summary, POST `{"score"}`, DELETE). The backend takes the rater from the JWT. Every call returns `FileRatingSummary` (`avgRating`, `totalRatings`, `userRating`). List DTOs carry `avgRating`/`totalRatings` too. The process-wide `data/rating/FileRatingStore` keeps the latest summary per file, so a change shows on every screen without a refetch:
  - List ViewModels `seed` it from each network response. Seed in the ViewModel, never from a screen's `LaunchedEffect`, which would re-seed stale data when navigating back.
  - Rows show `RatingBadge(known = ratings[id], …)`.
  - `FileRatingViewModel` updates it optimistically and rolls back on failure.
  - It is cleared on sign-out.
- **Workshops ("Projects"):** `/api/v1/workshops` needs a signed-in user. Admins pick the type (FIRST_COME_FIRST_SERVED or MOTIVATION) only when creating a project; editing keeps it. Both types work the same in the app, since the committee's selection for MOTIVATION happens on the external form. Only the project page's explanation (`ProjectType.description()`) and the backend's notification wording differ. The backend scheduler (second 0 of every minute) moves UPCOMING → OPEN → CLOSED at `registrationOpensAt`/`registrationClosesAt`, and admins can override it with `PATCH /{id}/status`. A manual status stays until the next date still ahead: the `*Sent` flags mark passed moments as handled, so the scheduler doesn't undo it. The list and detail ViewModels refetch a few seconds after the next opening/closing time (`nextStatusChangeMillis`), retrying while the backend is late. Admins edit a project with `PUT /{id}`: changing its dates recomputes the status and resends the reminders, while unchanged dates keep a manually set status. The create form uploads picked files one by one after the create; files that fail are reported, and the project is kept. `formLink` is null for non-admins unless OPEN. The app only opens the link, because sign-ups happen on the external form. Create, attachments, status and delete are admin only, since a 403 logs the user out. Attachments are private: their `fileUrl` is the R2 endpoint, which answers 400, so never open it. Download them with `AndroidDownloader` from `ApiClient.workshopAttachmentDownloadUrl(id)`, and preview them from `.../preview` (PDF and images as-is, Office files converted to PDF) through `WorkshopAttachmentPreviewsViewModel`, which caches PDFs as `previews/workshop-{id}.pdf`. `AndroidDownloader` sends the JWT only to `BASE_URL`. Dates are zone-less `LocalDateTime`, so `data/model/Dates.kt` reads them in the device zone.
- **Push notifications (FCM):** workshop alerts are server pushes, not on-device alarms.
  - The backend scheduler publishes to the FCM topic `workshops` 10 minutes before opening, at opening, 10 minutes before closing and at closing (`PushNotificationService`). It needs `FIREBASE_CREDENTIALS` (path to the secret service-account key) in the backend `.env`. Without it, pushes are skipped with a warning.
  - A manual status change pushes too: OPEN sends the opened alert (`WORKSHOP_LAUNCH`) and CLOSED the closed one (`WORKSHOP_CLOSED`). Back to UPCOMING sends nothing. The scheduler skips alerts whose moment already passed, and closes a project whose whole window is past without alerting. A new project must open in the future, checked in both the app and the backend. Workshop times follow the phone's 12/24-hour setting (`is24HourClock()`), including the time picker. Notifications only use the `NotifType` values; add a new value to the enum when one is needed.
  - The app subscribes to the topic at every start in `MainActivity.onCreate`, signed in or not (`data/push/WorkshopPush.kt`). It never unsubscribes: signing out or a 401/403 must not make users miss a registration opening.
  - The app asks for the Android 13+ notification permission at startup. Tapping a push opens `workshops/{projectId}`. When signed out it opens `login` first, and the project opens after sign-in (`routeForPushTap`, `pendingPushProjectId` in `MedRationalApp`).
  - Push needs `app/google-services.json` from the Firebase console (not secret, committed). Without it the app builds with push off.
  - The topic, the channel (`workshop_reminders`) and the data key (`projectId`) must match the backend.
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
  - Routes: `welcome`, `login`, `signup`, `forgot_password`, `categories`, `favorites`, `downloads`, `search`, `reasonings/{categoryId}/{categoryName}`, `file/{reasoningId}/{fileId}`, `suggestions`, `suggestions/new`, `suggestions/{suggestionId}`, `novelties`, `workshops`, `workshops/new`, `workshops/{workshopId}`, `workshops/{workshopId}/edit`.
  - Startup routing: checks `TokenManager.hasToken()`. If valid, routes directly to `categories`; otherwise routes to `welcome`.
  - Secondary sections (Projects, Favorites, Recent downloads, Suggestions, Sign out) are reached from the home screen's ☰ side menu (`ui/categories/HomeDrawer.kt`); add new sections there rather than to the top bar. Other screens use a back arrow.

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
