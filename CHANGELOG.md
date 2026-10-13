# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Statistics for admins, reached from the home screen's side menu:
  - Students: registered students and new sign-ups this week, students active in the last 7 days and today, stickiness, and activation (students who opened or downloaded at least one file). Weekly sign-up and active-student charts cover 12 weeks.
  - Usage: downloads and file views this week, compared with the previous week, and a 30-day chart of both.
  - Trending files and categories: what rose most against the previous week, to spot exam-season spikes.
  - Most downloaded, viewed and favorited files, activity per category, and the most downloaded hashtags, over the last 7 days or all time.
  - Engagement and community: favorites and ratings given, average rating, highest rated files, new files and files never downloaded, open suggestions and projects.
  - Tap a file to open it, and use the refresh button for the latest figures.
- Opening a file is counted as a view for these statistics (signed-in students only; reopening it within 30 minutes counts once).
- The app is in French by default, and English can be picked in the Profile screen (on Android 13+ also in the system's app language settings). Everything the app writes is translated, including errors, dates, file sizes and workshop push notifications; what admins write (titles, descriptions, hashtags, projects) is shown as written.
- Profile, reached from the home screen's side menu when signed in: see your first name, last name, email and phone, and edit your names and phone directly. Changing your password sends a 6-digit code to your email, and changing your email sends a code to the new address; you stay signed in after either change.
- Projects (workshops), reached from the home screen's side menu when signed in. A project is either first come, first served, or by motivation letter, where students apply on the registration form and a committee picks who is accepted; admins choose the type when creating it. Each project shows its description, capacity, registration dates and documents (each with a preview of its first page or image, full screen with zoom on tap, and a Download button), with an Upcoming, Open or Closed status, filterable by tabs. The status changes on its own at the opening and closing times, and an open project page or list updates without a refresh. While registration is open a "Register now" button opens the organisers' sign-up form; it is hidden before opening and locked once closed. Push notifications, even while the app is closed, when a project's registration opens and closes, and 10 minutes before each. Tap one to open the project. Every phone with the app gets them, even when signed out or when the session has expired; tapping one then asks you to sign in and opens the project afterwards. Admins can create projects (title, description, registration link, optional capacity, opening and closing date and time, and documents to attach right away), edit them later (changing the dates moves the status and reminders to match, while a status set by hand is kept when the dates stay the same), attach and remove documents, force a project's status (for example to close it once the form is full; a forced status stays until the next opening or closing time still ahead), and delete projects.
- File ratings: rate any file from 1 to 5 stars on its page, change your rating, or remove it. Each file's average and number of ratings show on its page and next to it in category, search, favorites, What's new and recent downloads lists, and a change shows up on every screen at once.
- Hashtags on the file detail screen. Admins can add, rename, and remove a file's hashtags; other users see them read-only.
- Hashtag autocomplete for admins: existing tags are suggested, most used first, with how many files use each.
- Hashtags can be chosen when uploading a file.
- Bulk hashtag editing: admins long-press files on a category screen to select several, then add or remove tags on all of them.
- Recent downloads screen, opened from the clock icon on the home screen: your most recently downloaded files, each listed once, newest first. Tap one to open its file page.
- Suggestions: users send the admins a suggestion with a title, a description and an optional file (up to 10 MB), then follow the conversation with the admins, who can reply with messages and files of their own. Admins get a Suggestions inbox showing who sent each one, with unread markers, and can close a suggestion, which archives it and makes it read-only for both sides. Reached from the home screen's side menu.
- What's new: a side-menu section listing the latest uploads, newest first, with each file's category, reasoning, upload time, type and size. Files uploaded since your last visit are shown in bold with a NEW badge, and a badge on the menu shows how many are waiting. Tap a file to open it.
- `docs/CI_AND_TESTS.md` and `docs/GITHUB_REPO_SETUP.md` guides, linked from the README.
- GitHub Actions CI (lint, unit tests, debug build), Dependabot, and a PR template.
- Backend URL configurable per build type through `local.properties`.

### Changed
- The home screen has a ☰ side menu with Home, Search, What's new, Projects, Favorites, Recent downloads, Suggestions, Statistics (admins), Profile and Sign out, plus the signed-in user's name and email. Only Search stays in the top bar.
- Release builds are minified and resource-shrunk with R8.
- HTTP bodies are logged in debug builds only.

### Fixed
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
