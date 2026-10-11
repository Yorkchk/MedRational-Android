# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Hashtags on the file detail screen. Admins can add, rename, and remove a file's hashtags; other users see them read-only.
- Hashtag autocomplete for admins: existing tags are suggested, most used first, with how many files use each.
- Hashtags can be chosen when uploading a file.
- Bulk hashtag editing: admins long-press files on a category screen to select several, then add or remove tags on all of them.
- Recent downloads screen, opened from the clock icon on the home screen: your most recently downloaded files, each listed once, newest first. Tap one to open its file page.
- Suggestions: users send the admins a suggestion with a title, a description and an optional file (up to 10 MB), then follow the conversation with the admins, who can reply with messages and files of their own. Admins get a Suggestions inbox showing who sent each one, with unread markers, and can close a suggestion, which archives it and makes it read-only for both sides. Reached from the home screen's side menu.
- `docs/CI_AND_TESTS.md` and `docs/GITHUB_REPO_SETUP.md` guides, linked from the README.
- GitHub Actions CI (lint, unit tests, debug build), Dependabot, and a PR template.
- Backend URL configurable per build type through `local.properties`.

### Changed
- The home screen has a ☰ side menu with Home, Search, Favorites, Recent downloads, Suggestions and Sign out, plus the signed-in user's name and email. Only Search stays in the top bar.
- Release builds are minified and resource-shrunk with R8.
- HTTP bodies are logged in debug builds only.

### Fixed
- File downloads now send the signed-in user's token, so the backend can record them.

## [0.1.0] - 2026-10-08

### Added
- Welcome, sign-up, login, and forgot-password flows with email one-time codes.
- Category and clinical reasoning browsing with image preview and file downloads (single file, reasoning ZIP, category ZIP).
- Admin create/delete for categories, reasonings, and file uploads.
- File search with category filter and hashtags.
- Favorites: like/unlike files from the reasoning page and a dedicated favorites screen.
