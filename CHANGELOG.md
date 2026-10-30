# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Search bars on the home screen (categories) and on a category's page (reasonings) filter the list as you type, accents ignored. Their Advanced button opens the full search with what you typed, on that category.
- Testers can install the UAT app ("SWISRA UAT") next to the real one. It talks to the test server and only gets its workshop notifications, so testing never notifies students.
- You stay signed in: the app renews your session in the background, so you only sign in again after 60 days without opening it. Being offline never signs you out.
  - Signing out ends the session on the server too.
  - Changing your password or email signs your other devices out, but keeps this one signed in.
- Share files to your email, Google Drive, OneDrive, Quick Share or any other app on your phone, to open them on your laptop or tablet:
  - One file: the Share button on its page, or Share in its ⋮ menu.
  - Several files: long-press a file (or pick Select in its ⋮ menu), tick the others, then tap Share. They are sent as separate files.
  - A whole reasoning: Share all (zip) in its ⋮ menu. A whole category: the Share icon at the top, which sends one zip with a folder per reasoning.
  - Shared files (not zips) also appear in Recent downloads.
- A push notification announces each new project as soon as an admin creates it, while it is still upcoming. Tap it to open the project; the registration link appears there once registration opens.
- Statistics for admins, reached from the home screen's side menu, kept short so they read at a glance:
  - This week: active students (and their share of registered students), new students, downloads and file views, each compared with the previous 7 days, and a 30-day chart of downloads and views.
  - Most popular: the most downloaded files and the busiest categories, over the last 7 days or all time. Tap a file to open it.
  - Needs attention: unread suggestions, projects open for registration and files never downloaded.
  - Use the refresh button for the latest figures.
- Opening a file is counted as a view for these statistics (signed-in students only; reopening it within 30 minutes counts once).
- The app is in French by default, and English can be picked in the Profile screen (on Android 13+ also in the system's app language settings). Everything the app writes is translated, including errors, dates, file sizes and workshop push notifications; what admins write (titles, descriptions, hashtags, projects) is shown as written.
- Profile, reached from the home screen's side menu when signed in: see your first name, last name, email and phone, and edit your names and phone directly. Changing your password sends a 6-digit code to your email, and changing your email sends a code to the new address; you stay signed in after either change.
- Delete your account from the Profile screen: a 6-digit code is sent to your email, and a final confirmation deletes your account, favorites, recent downloads, suggestions and notifications, then signs you out. Download and view counts and your ratings are kept with no link to you. The same can be done without the app on the backend's `/delete-account` page.
- Projects (workshops), reached from the home screen's side menu when signed in. A project is either first come, first served, or by motivation letter, where students apply on the registration form and a committee picks who is accepted; admins choose the type when creating it. Each project shows its description, capacity, registration dates and documents (each with a preview of its first page or image, full screen with zoom on tap, and a Download button), with an Upcoming, Open or Closed status, filterable by tabs. The status changes on its own at the opening and closing times, and an open project page or list updates without a refresh. While registration is open a "Register now" button opens the organisers' sign-up form; it is hidden before opening and locked once closed. Push notifications, even while the app is closed, when a project's registration opens and closes, and 10 minutes before each. Tap one to open the project. Every phone with the app gets them, even when signed out or when the session has expired; tapping one then asks you to sign in and opens the project afterwards. Admins can create projects (title, description, registration link, optional capacity, opening and closing date and time, and documents to attach right away), edit them later (changing the dates moves the status and reminders to match, while a status set by hand is kept when the dates stay the same), attach and remove documents, force a project's status (for example to close it once the form is full; a forced status stays until the next opening or closing time still ahead), and delete projects.
- File ratings: rate any file from 1 to 5 stars on its page, change your rating, or remove it. Each file's average and number of ratings show on its page and next to it in category, search, favorites, What's new and recent downloads lists, and a change shows up on every screen at once.
- Hashtags on the file detail screen. Admins can add, rename, and remove a file's hashtags; other users see them read-only.
- Hashtag autocomplete for admins: existing tags are suggested, most used first, with how many files use each.
- Hashtags can be chosen when uploading a file.
- Bulk hashtag editing: admins long-press files on a category screen to select several, then add or remove tags on all of them.
- Recent downloads screen, opened from the clock icon on the home screen: your most recently downloaded files, each listed once, newest first. Tap one to open its file page.
- Suggestions, reached from the home screen's side menu: users send the admins a suggestion with a title, a description and an optional file (up to 10 MB), and a thank-you message confirms it was received. Admins get a Suggestions inbox with unread markers; each suggestion shows the sender's name, email and phone, the description and a preview of the attached file (first page of a PDF or Office document, or the image, full screen with zoom on tap, plus a Download button), and can be deleted.
- What's new: a side-menu section listing the latest uploads, newest first, with each file's category, reasoning, upload time, type and size. Files uploaded since your last visit are shown in bold with a NEW badge, and a badge on the menu shows how many are waiting. Tap a file to open it.
- `docs/CI_AND_TESTS.md` and `docs/GITHUB_REPO_SETUP.md` guides, linked from the README.
- GitHub Actions CI (lint, unit tests, debug build), Dependabot, and a PR template.
- Backend URL configurable per build type through `local.properties`.

### Changed
- Clearer search: the home search bar just says "Search files", and the search screen tells you that you can search by file name, category, reasoning or #hashtag.
- On a file's page, Download, Favorite and Share now fit on one line, even on small phones. Favorite is a heart button, filled when the file is in your favorites.
- Picking several files to share them is easier to find: each reasoning has a Select button above its files, which shows a checkbox on every file. Long-pressing a file still works.
- Tablets and large screens: pages stay centered at a comfortable width (forms narrower), the lists of favorites, downloads, novelties, projects, suggestions and search results show two columns (a category's reasonings stay in one centered column, easier to read in order), a lone result is centered instead of sitting in half the screen, and a file's page shows its details next to the preview in landscape.
- The home screen lists categories one per line (two on tablets) with their description underneath, and a category's page shows its full description at the top, in an "About this category" card.
- Signing in only needs your email and password; no more code by email each time. Codes are still sent when you sign up, reset a forgotten password, change your password or email, or delete your account.
- Signing up with an email that already has an account now says so, instead of sending a code. Signing in before entering your sign-up code asks you to sign up again to get a new one.
- The app is now **SWISRA** (Study With Scome Rabat), with its own logo as the launcher icon (also as a themed icon on Android 13+) and on a new splash screen.
- New look: SWISRA red and slate colors, Plus Jakarta Sans and Inter fonts, rounded cards, and a light and a dark look. The app opens in light mode; pick Light, Dark or Like the phone under Appearance in the profile. Screens now draw behind the status and navigation bars.
- Redesigned welcome screen, with the logo and what the app offers.
- Redesigned sign-in, sign-up and password reset: the brand at the top, icons in the fields, and the keyboard's Next and Done keys move through the form and submit it. Errors show in a banner.
- Redesigned home screen: a greeting with your first name, a search bar, a "What's new" banner when files are waiting, and categories as a grid of cards.
- The ☰ side menu shows the logo and groups its sections under Explore (Home, Search, What's new, Projects), My space (Favorites, Recent downloads, Suggestions, Profile) and Administration (Statistics, for admins), with Sign out at the bottom.
- Redesigned reasoning screen: each reasoning is a card whose long text folds behind "Read more", and each file shows its type, rating and size, with a favorite button and a menu to download it.
- Redesigned file page: a header with the file's type, reasoning, details and Download and Favorite buttons, then the rating, hashtags and preview in cards.
- Loading, error and empty screens look the same everywhere, with a Retry button on errors.
- Release builds are minified and resource-shrunk with R8.
- HTTP bodies are logged in debug builds only.

### Fixed
- Signing in, signing up and resetting a password no longer jump back to the first step while the emailed code is being checked or after a wrong code; the code step stays open so you can correct it.
- Deleting a category, reasoning or file now asks for confirmation first. It used to happen on a single tap.
- Image previews and thumbnails (JPG, PNG and other images) now load. They were requested from private storage and came back blank. Images now also cover BMP, HEIC/HEIF and AVIF, GIFs animate, and an image that can't be displayed shows a Download button instead of an empty box.
- The app no longer crashes on launch when its saved sign-in can't be read (for example after restoring the phone from a backup). You're signed out and can log in again.
- File downloads now send the signed-in user's token, so the backend can record them.

## [0.1.0] - 2026-10-08

### Added
- Welcome, sign-up, login, and forgot-password flows with email one-time codes.
- Category and clinical reasoning browsing with image preview and file downloads (single file, reasoning ZIP, category ZIP).
- Admin create/delete for categories, reasonings, and file uploads.
- File search with category filter and hashtags.
- Favorites: like/unlike files from the reasoning page and a dedicated favorites screen.
