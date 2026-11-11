# Repository guidance

- Read `CLAUDE.md` for the owner's operational rules, especially emulator/ADB safety, browser impersonation, Gradle concurrency, and Git. Its machine-specific paths may need adapting to the current checkout.
- Single Android module `:app`: Gradle wrapper 8.13, JDK 17 and Android SDK platform 36; Java source level 8 with desugaring. The namespace is `com.fanok.audiobooks`; the fork's release application ID is **`link.intertop.audiobooks`**, and debug uses `link.intertop.audiobooks.debug`. Older branches may differ.

## Build and verification

- From the repository root, CI runs `./gradlew :app:testDebugUnitTest`, then `:app:lintDebug`, then `:app:assembleDebug`. On Windows use `gradlew.bat`; run only one Gradle build at a time. For a focused JVM test: `./gradlew :app:testDebugUnitTest --tests '*SourceContractTest'`.
- Do not run `gradlew clean`: preserve existing build artifacts. Release packaging needs the ignored root `keystore.properties`; do not read or print its passwords. `assembleRelease` can clear the release APK output directory; preserve needed APKs elsewhere first.
- Emulator/device testing is only on explicit owner request. Follow `CLAUDE.md`'s exclusive, sequential procedure; never uninstall, clear app data, wipe the emulator, or install an incompatible debug build without permission. Its `emu.ps1 smoke` targets a different app and must not be used here.

## Code boundaries and invariants

- Java sources are under `app/src/main/java/com/fanok/audiobooks/`. `App` sets up shared services; `search/Provider` registers sites, `source/SourceParser` and site adapters implement them, `net/` owns HTTP/proxy handling, `service/` owns playback, and `room/` owns persistence.
- For new or changed sources, follow `docs/howto.md` § “Adding an audiobook site”: test parsers against `app/src/test/resources/fixtures/`, register the adapter in `Provider`, and keep site-specific behavior out of shared code. Never renumber/reuse a source ID or rename its download folder: persisted settings and downloads depend on them. Live site dumps are opt-in, not ordinary CI tests.
- Room exports schemas to `app/schemas/`, also used as instrumentation-test assets; retain schema history when changing the database. Binary region assets must remain uncompressed (`app/build.gradle` uses `noCompress` for `.bin`).
- Browser-like request headers are an intentional owner decision. Do not weaken or expand impersonation without a request; keep `net/BrowserHeaders` and `Consts.USER_AGENT` consistent. See `docs/http-clients.md`.
- Keep English and Russian READMEs in sync. Commit or push only when requested; release tags must match `versionName` in `app/build.gradle` and publish via `.github/workflows/release.yml`.
