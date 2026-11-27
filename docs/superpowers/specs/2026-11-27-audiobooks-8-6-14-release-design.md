# AudioBooks 8.6.14 Release

- **Intent:** Publish the takedown-warning dialog changes as the next compatible AudioBooks release.
- **Scope:** Increment `versionName` from `8.6.13` to `8.6.14` and `versionCode` from `147` to `148`; retain the existing package ID and signing identity.
- **Approach:** Follow the existing tag-triggered GitHub Actions release workflow: commit on `master`, push tag `v8.6.14`, and publish its signed APK, AAB, and checksums.
- **Constraints:** Preserve unrelated working-tree content (including `tv_bugs.md`); do not publish if tests, release build, or signature verification fail.
- **Success:** The tag workflow completes and the GitHub release contains the expected signed assets.
