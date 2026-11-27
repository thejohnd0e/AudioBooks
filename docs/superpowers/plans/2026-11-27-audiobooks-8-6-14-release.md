# AudioBooks 8.6.14 Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish a signed, update-compatible AudioBooks 8.6.14 release with the prominent takedown dialog.

**Architecture:** Use the existing Gradle release configuration and GitHub Actions workflow. A versioned commit on `master` plus tag `v8.6.14` triggers CI tests, signed APK/AAB builds, signature verification, and GitHub Release publication.

**Tech Stack:** Android Gradle Plugin, Gradle, Git, GitHub Actions, GitHub CLI.

**Spec:** `docs/superpowers/specs/2026-11-27-audiobooks-8-6-14-release-design.md`

## Global Constraints

- `versionName` is `8.6.14`; `versionCode` is `148`.
- Preserve package ID `link.intertop.audiobooks` and the established signing certificate.
- Publish only after tests/build/signature checks pass.
- Leave unrelated `tv_bugs.md` untouched and uncommitted.

## Review Focus

- Tag and Gradle version mismatch — validate the tag name and built APK metadata.
- Wrong application identity or signer — inspect package ID and certificate before publication.
- Unrelated local work accidentally included — review staged files before commit.
- CI release workflow failure — confirm all expected release assets exist before reporting success.
- Duplicate version tag — verify `v8.6.14` does not already exist locally or remotely.

---

### Task 1: Prepare and verify the release commit

**Files:**
- Modify: `app/build.gradle` (version fields)
- Include: only release-relevant source changes and release records
- Preserve: unrelated `tv_bugs.md`

- [x] Confirm tag `v8.6.14` is unused and inspect staged changes.
- [x] Set version fields to `8.6.14` and `148`.
- [x] Run `.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease :app:bundleRelease --offline`.
- [x] Verify release APK package ID, version, and signing certificate.
- [ ] Commit the reviewed release changes on `master`.

### Task 2: Publish and verify

**Files:** Git tag `v8.6.14`; GitHub Release assets produced by `.github/workflows/release.yml`.

- [ ] Push the release commit and `v8.6.14` tag to the configured remote.
- [ ] Wait for the tag-triggered Release workflow to complete successfully.
- [ ] Verify the GitHub release contains the signed APK, AAB, and SHA256 checksums.
