# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- `docs/CI_AND_TESTS.md` and `docs/GITHUB_REPO_SETUP.md` guides, linked from the README.
- GitHub Actions CI (lint, unit tests, debug build), Dependabot, and a PR template.
- Backend URL configurable per build type through `local.properties`.

### Changed
- Release builds are minified and resource-shrunk with R8.
- HTTP bodies are logged in debug builds only.

## [0.1.0] - 2026-10-08

### Added
- Welcome, sign-up, login, and forgot-password flows with email one-time codes.
- Category and clinical reasoning browsing with image preview and file downloads (single file, reasoning ZIP, category ZIP).
- Admin create/delete for categories, reasonings, and file uploads.
- File search with category filter and hashtags.
- Favorites: like/unlike files from the reasoning page and a dedicated favorites screen.
