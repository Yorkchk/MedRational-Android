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
- **Suggestions:** One-way (`/api/v1/suggestions`): users only send one (`POST`, multipart) and see a thank-you, with no list or thread. Admins read the inbox (`GET ?status=PENDING`), open one to see the sender's name, email and phone (`GET /{id}`, which also clears its unread marker), and delete it (`DELETE /{id}`). The backend still has replies (`/messages`), closing (`/close`) and `/mine`, but the app never calls them. The `suggestions` route shows the inbox to admins and the form to users. The backend takes the caller from the JWT and returns 413/415 for attachments over 10 MB or of an unsupported type. Attachments are never public: download them through `.../suggestions/{id}/attachment` with `AndroidDownloader`. The detail page previews them with `AttachmentPreviewCard` (`ui/files/`, shared with workshop documents) from `.../suggestions/{id}/attachment/preview`: images load through Coil, and PDFs (Office files converted by the backend, which doesn't keep the converted copy) are cached as `previews/suggestion-{id}.pdf`. Only admins may call the inbox, detail, preview and delete, because a 403 logs the user out. Picked files are read through `data/upload/PickedFiles.kt`.
- **Novelties:** The "What's new" feed (`/api/v1/novelties`) takes the caller from the JWT, so only call it when signed in. A file is `isNew` when uploaded after the user's `noveltiesSeenAt` baseline (account creation until the first visit). Logging in doesn't move the baseline. `NoveltiesViewModel` loads once per visit and then calls `mark-seen`, so highlights survive opening a file and coming back. The home badge comes from `/novelties/badge-count`.
- **Ratings:** Files are rated 1–5 through `/api/v1/files/{fileId}/ratings` (GET summary, POST `{"score"}`, DELETE). The backend takes the rater from the JWT. Every call returns `FileRatingSummary` (`avgRating`, `totalRatings`, `userRating`). List DTOs carry `avgRating`/`totalRatings` too. The process-wide `data/rating/FileRatingStore` keeps the latest summary per file, so a change shows on every screen without a refetch:
  - List ViewModels `seed` it from each network response. Seed in the ViewModel, never from a screen's `LaunchedEffect`, which would re-seed stale data when navigating back.
  - Rows show `RatingBadge(known = ratings[id], …)`.
  - `FileRatingViewModel` updates it optimistically and rolls back on failure.
  - It is cleared on sign-out.
- **Workshops ("Projects"):** `/api/v1/workshops` needs a signed-in user. Admins pick the type (FIRST_COME_FIRST_SERVED or MOTIVATION) only when creating a project; editing keeps it. Both types work the same in the app, since the committee's selection for MOTIVATION happens on the external form. Only the project page's explanation (`ProjectType.description()`) and the backend's notification wording differ. The backend scheduler (second 0 of every minute) moves UPCOMING → OPEN → CLOSED at `registrationOpensAt`/`registrationClosesAt`, and admins can override it with `PATCH /{id}/status`. A manual status stays until the next date still ahead: the `*Sent` flags mark passed moments as handled, so the scheduler doesn't undo it. The list and detail ViewModels refetch a few seconds after the next opening/closing time (`nextStatusChangeMillis`), retrying while the backend is late. Admins edit a project with `PUT /{id}`: changing its dates recomputes the status and resends the reminders, while unchanged dates keep a manually set status. The create form uploads picked files one by one after the create; files that fail are reported, and the project is kept. `formLink` is null for non-admins unless OPEN. The app only opens the link, because sign-ups happen on the external form. Create, attachments, status and delete are admin only, since a 403 logs the user out. Attachments are private: their `fileUrl` is the R2 endpoint, which answers 400, so never open it. Download them with `AndroidDownloader` from `ApiClient.workshopAttachmentDownloadUrl(id)`, and preview them from `.../preview` (PDF and images as-is, Office files converted to PDF) through `WorkshopAttachmentPreviewsViewModel`, which caches PDFs as `previews/workshop-{id}.pdf`. Images there load through Coil, whose client (`ApiClient.imageHttpClient`, set in `MainActivity`) adds the JWT for `BASE_URL` only. Without it the backend refuses them, since workshop endpoints need sign-in, unlike study file previews. `AndroidDownloader` sends the JWT only to `BASE_URL`. Dates are zone-less `LocalDateTime`, so `data/model/Dates.kt` reads them in the device zone.
- **Push notifications (FCM):** workshop alerts are server pushes, not on-device alarms.
  - Creating a project pushes to the FCM topic `workshops` right away (`WORKSHOP_CREATED`), so students hear about it while it is still UPCOMING.
  - The backend scheduler publishes to the FCM topic `workshops` 10 minutes before opening, at opening, 10 minutes before closing and at closing (`PushNotificationService`). It needs `FIREBASE_CREDENTIALS` (path to the secret service-account key) in the backend `.env`. Without it, pushes are skipped with a warning.
  - A manual status change pushes too: OPEN sends the opened alert (`WORKSHOP_LAUNCH`) and CLOSED the closed one (`WORKSHOP_CLOSED`). Back to UPCOMING sends nothing. The scheduler skips alerts whose moment already passed, and closes a project whose whole window is past without alerting. A new project must open in the future, checked in both the app and the backend. Workshop times are always shown and picked in 24-hour format (e.g. "20 Oct 2026, 19:30"), whatever the phone's setting. Notifications only use the `NotifType` values; add a new value to the enum when one is needed.
  - The app subscribes to the topic at every start in `MainActivity.onCreate`, signed in or not (`data/push/WorkshopPush.kt`). It never unsubscribes: signing out or a 401/403 must not make users miss a registration opening.
  - The app asks for the Android 13+ notification permission at startup. Tapping a push opens `workshops/{projectId}`. When signed out it opens `login` first, and the project opens after sign-in (`routeForPushTap`, `pendingPushProjectId` in `MedRationalApp`).
  - Push needs `app/google-services.json` from the Firebase console (not secret, committed). Without it the app builds with push off.
  - The topic, the channel (`workshop_reminders`) and the data key (`projectId`) must match the backend.
- **Profile:** `/api/v1/users/me` (GET, PUT) takes the user from the JWT and is reached from the side menu when signed in. `ProfileViewModels.kt` holds its ViewModels.
  - First name, last name and phone save without a code. The backend keeps `fullName` as "first last", and `TokenManager.saveUserName` updates the menu header.
  - Password: `POST .../password/code` emails a code to the current address, then `PUT .../password {code, newPassword}`. It signs every other device out and returns new tokens for this one, saved with `saveRenewedSession`.
  - Email: `POST .../email/code {newEmail}` emails a code to the new address, then `PUT .../email {code}` returns a new `AuthResponse`. The JWT subject is the email, so the app must save that token with `saveAuth`, because the old one stops working.
  - Wrong or expired codes, and an email already in use, answer 400, never 401/403, so they don't sign the user out.
  - Delete account (`profile/delete`, `DeleteAccountViewModel`): `POST .../delete/code` emails a code to the current address, then `DELETE /api/v1/users/me {code}` (204, sent with `@HTTP(hasBody = true)`). The token stops working right away, so the screen calls the shared `signOut()` in `MedRationalApp`, the same one the side menu uses. The backend deletes what identifies the user and keeps download/view counts, ratings and an admin's projects and replies with no author, so `WorkshopProject.authorId`/`authorEmail` can be null. The backend's public `/delete-account` page is the deletion link for the Play Console.
- **Statistics (admin):** `StatisticsScreen` (`ui/statistics/`) shows `GET /api/v1/admin/analytics/dashboard` in one call. It's admin only, so the side menu lists it for admins only, because a 403 logs the user out.
  - "Week" means the last 7 days, today included, and students are non-admin accounts. The backend computes every figure; the app only formats them, with pure helpers in `StatisticsMath.kt` (`weekDelta`, `percentOf`, `chartCeiling`).
  - Charts are drawn with Compose `Canvas` (`Charts.kt`); there is no chart library.
  - `FileDetailViewModel` calls `POST /api/v1/files/{id}/views` once per opened file, for signed-in non-admins only (`recordViews`), and ignores failures. Active students come from the backend's own per-day tracking of authenticated requests, so the app doesn't record activity.
- **Multipart Uploads:** Admin file creation uses `MultipartBody.Part` for binary files alongside `RequestBody` for metadata fields (`title`, `description`, `reasoningId`).

## Architecture & State Management

Single-module (`:app`) project following MVVM without external DI frameworks (no Hilt/Koin):

- **`ApiClient`:** Singleton object holding OkHttp and Retrofit instances. `MainActivity.onCreate()` must call `ApiClient.initialize(context)` before any network dispatch.
- **Authentication & Interception:** the auth interceptor in `ApiClient` attaches `Authorization: Bearer <jwt>` via `TokenManager`.
  - **401** (missing or expired JWT, which lasts 1 hour): `TokenRefreshAuthenticator` asks `SessionRenewer` (`data/auth/`) to trade the refresh token at `POST /auth/refresh` through `SessionApi` (a bare client, so a refused renewal can't trigger another), stores the new pair and retries once. Requests that fail together renew once. Only a refused renewal (401) clears the session; offline or a server error keeps it, so being offline never signs anyone out.
  - **403** (signed in but not allowed, e.g. a student on an admin endpoint) clears the stored session, forcing a re-login on the next app cycle.
  - Coil's `imageHttpClient` renews too, but leaves the sign-out to the API client.
  - `AndroidDownloader` hands the token to the system `DownloadManager`, which can't renew it, so it runs in the background and calls `ApiClient.freshAccessToken()` first (renews when the JWT expires within 5 minutes, read from its `exp` claim).
  - Signing out calls `ApiClient.endSessionOnServer()` (`POST /auth/logout`, best effort) before clearing locally.
- **`TokenManager`:** Persists JWT, refresh token, roles, `userId`, `email`, and `name` using `EncryptedSharedPreferences`; it is the `SessionStore` the renewer uses. Admin role check is `isAdmin()` matching either `ROLE_ADMIN` or `ADMIN`.
- **UI RBAC:** Role-based access control is handled client-side. Screens query `TokenManager.isAdmin()` to conditionally display admin CRUD buttons, dialogs, and deletion actions. There are no standalone admin dashboards.
- **ViewModels & State:**
  - ViewModels expose immutable `StateFlow<*UiState>` collected with `collectAsStateWithLifecycle()` or `collectAsState()` in Compose.
  - State follows sealed hierarchies: `interface/sealed class *UiState { object Loading; data class Success(...); data class Error(val message: String); }`.
  - When instantiating newer ViewModels (`AuthViewModel`, `FavoriteViewModel`, `SearchViewModel`), pass dependencies via constructor (`TokenManager`, `MedRationalApi`) using `viewModel { ... }` factories. Older ViewModels (`CategoryViewModel`, `ReasoningViewModel`) access `ApiClient.api` directly and are activity-scoped in `MainActivity`.
- **Navigation:** Controlled by a single `NavHost` inside `MainActivity.MedRationalApp()`.
  - Routes: `welcome`, `login`, `signup`, `forgot_password`, `categories`, `favorites`, `downloads`, `search`, `reasonings/{categoryId}/{categoryName}`, `file/{reasoningId}/{fileId}`, `suggestions`, `suggestions/{suggestionId}`, `novelties`, `workshops`, `workshops/new`, `workshops/{workshopId}`, `workshops/{workshopId}/edit`, `profile`, `profile/email`, `profile/password`, `profile/delete`, `statistics`.
  - Startup routing: checks `TokenManager.hasToken()`. If valid, routes directly to `categories`; otherwise routes to `welcome`.
  - Secondary sections (Projects, Favorites, Recent downloads, Suggestions, Statistics, Profile, Sign out) are reached from the home screen's ☰ side menu (`ui/categories/HomeDrawer.kt`); add new sections there rather than to the top bar. Other screens use a back arrow.

## Localization (French / English)

- French is the default whatever the phone's language (`res/values/`, marked `tools:locale="fr"`), and English lives in `res/values-en/`. Strings are split by feature (`strings_<feature>.xml`). Every key needs both languages with the same placeholders; `StringResourcesParityTest` and lint's `MissingTranslation` check it.
- `data/locale/AppLanguage` stores the choice through AppCompat (`MainActivity` is an `AppCompatActivity`, and the manifest's `AppLocalesMetadataHolderService` keeps it on Android 12 and older). `ensureDefault()` sets French on first launch, and the Profile screen's language card switches it, which recreates the activity.
- Never hardcode user-facing text: composables use `stringResource`/`pluralStringResource`, and ViewModels expose `ui/text/UiText` (a resource id plus args, resolved with `asString()` when shown), never a `String`. Tests check the text with `TestStrings.en(...)`/`fr(...)`, which read the XML files.
- Show the app's own messages, chosen from the HTTP status, never the backend's (English) message text.
- Dates and sizes take the screen's locale from `ui/text/appLocale()`.
- Only translate what the app writes. What admins enter (titles, descriptions, hashtags, projects) is shown as written.
- Workshop pushes name a string (`push_{event}_{fcfs|motivation}_title`/`_body` in `push_strings.xml`, with the project title as `%1$s`) that must match `WorkshopNotificationServiceImpl` in the backend. Language splits are disabled in the App Bundle, so both languages ship in every install.

## Core Workflows

- **Auth:**
  - Sign-in is email + password only: `/auth/login` returns the user payload + JWT right away, persisted with its `refreshToken` via `TokenManager.saveAuth(...)`. The device then stays signed in until the session is unused for 60 days, ended by sign-out, or ended by a password change, password reset or email change elsewhere. 401 means wrong credentials, and 403 means the sign-up code was never entered (no token is stored yet, so it logs nobody out).
  - Emailed 6-digit codes are only for sign-up (`/auth/user/register` → `/auth/user/verify-registration`), forgot password (`/auth/forgot-password` → `/auth/reset-password`) and the profile flows (password, email change, account deletion). Signing up with the email of a confirmed account answers 409.
- **Optimistic Favorites:**
  - Toggling favorites immediately mutates local UI state in `FavoriteViewModel`, followed by the network call to `/api/v1/users/{userId}/favorites`. Rolls back or resyncs on failure.

## Code Conventions & Guidelines

- **SWISRA look (`ui/theme/`, `ui/components/`):** the app is branded SWISRA (Study With Scome RAbat); logo, colors and fonts are described in `docs/brand/README.md`.
  - Wrap UI in `SwisraTheme`. It has light and dark palettes, and dynamic color is off on purpose so the brand stays. Light is the default: `data/settings/AppTheme` stores the choice made in the profile (Light, Dark, Like the phone) and applies it through AppCompat's night mode in `MainActivity.onCreate`, so `isSystemInDarkTheme()` and the `values-night` resources follow it.
  - Use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes` (`Shape.kt`) and `Spacing` (`Dimens.kt`). No raw colors, `RoundedCornerShape` literals or font sizes in screens. White cards are `surfaceContainerLowest` on the `background`.
  - Reuse the shared components: `LoadingState`, `ErrorState`, `EmptyState`, `StatusBanner`, `PrimaryButton`/`SecondaryButton`, `SwisraTopBar`, `SectionHeader`, `ListRowCard`, `FileTypeBadge`, `MonogramTile`, `SwisraLogo`/`BrandHeader`. Auth screens use `AuthLayout` and `AuthTextField`.
  - Decorative tints (category monograms, file-type badges) come from `SwisraTints`, so they match the theme.
  - Destructive actions (deleting a category, reasoning or file) always confirm in a dialog.
  - Logo assets are generated from `docs/brand/swisra-logo.svg`; regenerate every drawable and mipmap together when it changes.
- **Kotlin & Compose Standards:**
  - Write pure declarative Jetpack Compose UI without XML layouts.
  - Prefix event callback parameters with `on` (e.g., `onCategoryClick: (Long) -> Unit`, `onDismissRequest: () -> Unit`).
  - Pass `Modifier` as the first optional parameter to custom composables and chain modifications cleanly.
- **Error Handling:**
  - Wrap network operations inside `viewModelScope.launch` with `try-catch` blocks capturing `IOException` and `HttpException`.
  - Expose descriptive error strings or resource IDs through `UiState.Error` rather than crashing or swallowing exceptions.
- **Imports:** Do not use wildcard imports (`import foo.bar.*`). Maintain clean, explicit imports.

## Common Pitfalls & Troubleshooting

- **Connection Refused (`ECONNREFUSED`):** Verify the Spring Boot backend is running locally on port 8080. If testing on a physical device, confirm the device and computer share the same Wi-Fi subnet and set `api.baseUrl` in `local.properties` to the computer's LAN IP.
- **Unexpected Logouts:** A 403 Forbidden (missing endpoint permissions) or a refused session renewal (401 from `/auth/refresh`) clears the stored session. Check backend security filters and Spring logs if requests trigger logout loops.
- **Context Initialization Crash:** If `ApiClient.api` throws `IllegalStateException` or `UninitializedPropertyAccessException`, verify `ApiClient.initialize(applicationContext)` ran inside `MainActivity.onCreate()`.

## Git workflow

`main` is protected: changes go through short-lived branches (`feat/…`, `fix/…`, `chore/…`, `docs/…`) and pull requests that must pass the Android CI workflow (`.github/workflows/android-ci.yml`: `lint testDebugUnitTest assembleDebug`). PRs are squash-merged, so PR titles use Conventional Commits (`feat(favorites): …`). Record user-facing changes under `[Unreleased]` in `CHANGELOG.md`.

## Testing policy

Every new feature or behavior change ships with tests in the same PR, and every bug fix includes a regression test that fails without the fix. Untested features are not considered done. See `docs/CI_AND_TESTS.md` §10 for test patterns and examples.
