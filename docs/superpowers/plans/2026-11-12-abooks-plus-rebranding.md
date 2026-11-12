# ABooks++ Rebranding and Adaptive Icon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebrand the application to ABooks++ and deploy the new dark-metallic adaptive launcher icon with listener mascot and `+ +` eyes.

**Architecture:** Update string resources for app title; construct clean Android adaptive icon layers (vector foreground with 72dp safe zone + matte dark background) and generate matching high-resolution legacy mipmaps for pre-Oreo systems; build and sign release packages.

**Tech Stack:** Android XML Vector Drawables, Gradle, Java 8, Android SDK 35 build tools, Python Pillow / Node canvas for asset generation.

**Spec:** `docs/superpowers/specs/2026-11-12-abooks-plus-rebranding-design.md`

## Global Constraints

- Retain `applicationId "link.intertop.audiobooks"` without alterations to guarantee in-app update compatibility via `Updater.java`.
- Do not run Gradle commands concurrently; follow single-invocation rules with `--no-daemon` and proper environment setup.
- Do not launch the emulator or run adb commands without explicit user instruction (per `CLAUDE.md`).
- Ensure all essential icon elements (headphones, book, `+ +` eyes) fit within the centered 72dp circular safe area of the 108dp adaptive canvas.

## Review Focus

- Circular icon masking: ensure neither the top headband nor the `+ +` eyes nor the book base are clipped by a round launcher mask.
- Legacy mipmap sizing: ensure all density folders (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) contain valid, correctly sized square/squircle and circular PNG assets.
- App name truncation: verify that `ABooks++` displays cleanly without XML parsing errors or entity issues.
- Build & test integrity: ensure `:app:testDebugUnitTest` passes without regression.
- Deliverable preservation: output signed release APK directly into `dist/`.

---

### Task 1: Rebrand App Title to ABooks++

**Files:**
- Modify: `app/src/main/res/values/strings.xml:2`
- Test: `app/src/test/java/com/fanok/audiobooks/RebrandingTest.java`

**Interfaces:**
- Consumes: `R.string.app_name`
- Produces: Updated application display title `ABooks++`

- [ ] **Step 1: Write unit test verifying the app name resource value**

Create `app/src/test/java/com/fanok/audiobooks/RebrandingTest.java` that reads and verifies `res/values/strings.xml` defines `<string name="app_name" translatable="false">ABooks++</string>`.

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -NoProfile -File C:/Users/user/AppData/Local/audiobook-audit-tools/g.ps1 D:/MyProjects/AudioBooks :app:testDebugUnitTest --tests com.fanok.audiobooks.RebrandingTest` (or via direct gradlew with loaded environment)
Expected: FAIL (currently reads "AudioBooks")

- [ ] **Step 3: Update `app_name` in `app/src/main/res/values/strings.xml`**

Change `<string name="app_name" translatable="false">AudioBooks</string>` to `<string name="app_name" translatable="false">ABooks++</string>`.

- [ ] **Step 4: Run test to verify it passes**

Run the same test command.
Expected: PASS

---

### Task 2: Vector Adaptive Icon Foreground & Background

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/values/ic_launcher_background.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_background.xml`
- Reference: `C:\Users\la\Downloads\AudioBooks-icon-eyes-higher.jpg`

**Interfaces:**
- Consumes: Approved artwork design
- Produces: Vector XML foreground with viewport 108x108 and safe area <= 72dp; dark background color `#18191C`

- [ ] **Step 1: Update background color and drawable**

Set `ic_launcher_background` in `res/values/ic_launcher_background.xml` and `res/drawable/ic_launcher_background.xml` to `#18191C`.

- [ ] **Step 2: Generate vector paths for `ic_launcher_foreground.xml`**

Construct clean vector paths in `ic_launcher_foreground.xml` (viewport 108x108):
- Headphone arch (outer radius ~23dp, inner radius ~20dp, center (54, 46))
- Headphone ear cushions on left (x=33-39) and right (x=69-75) with soft silver gradient/color tones
- Two `+ +` eye crosses centered horizontally at y~40, under the headband arch
- Open book pages with curved spine base at y=68-76
- Ensure all coordinates stay inside the `r=36` circle centered at (54, 54).

- [ ] **Step 3: Verify XML parsing and rendering via aapt2 / build check**

Run resource validation step to ensure vector drawable compiles with zero AAPT errors.

---

### Task 3: Render Legacy Mipmaps for All Densities

**Files:**
- Source: `C:\Users\la\Downloads\AudioBooks-icon-eyes-higher.jpg`
- Modify: `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48x48) & `ic_launcher_round.png` (48x48)
- Modify: `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72x72) & `ic_launcher_round.png` (72x72)
- Modify: `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96x96) & `ic_launcher_round.png` (96x96)
- Modify: `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144x144) & `ic_launcher_round.png` (144x144)
- Modify: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192x192) & `ic_launcher_round.png` (192x192)

- [ ] **Step 1: Write an automated Python/Node image processing script**

Script crops the central squircle tile from `AudioBooks-icon-eyes-higher.jpg`, generates:
1. `ic_launcher.png` (squircle with smooth alpha corners) at 48, 72, 96, 144, 192 px.
2. `ic_launcher_round.png` (circular mask with smooth anti-aliased edge) at 48, 72, 96, 144, 192 px.

- [ ] **Step 2: Execute script and output PNG assets into `app/src/main/res/mipmap-*/`**

Verify that all 10 files exist and have exact dimensions and non-zero sizes.

---

### Task 4: Complete Build, Verification & Packaging

**Files:**
- Output: `dist/ABooks++-release.apk` (and debug APK)

- [ ] **Step 1: Run unit test suite**

Run `:app:testDebugUnitTest` to guarantee all tests pass.

- [ ] **Step 2: Assemble signed release APK**

Run `assembleRelease` or use the project release signing script per `android-apk-signing`.

- [ ] **Step 3: Inspect APK manifest and resources**

Verify with Android build-tools `aapt2 dump badging <apk>` that:
- `applicationId`: `link.intertop.audiobooks`
- `application-label`: `'ABooks++'`
- `application-icon`: points to `res/mipmap-anydpi-v26/ic_launcher.xml`
- APK signature is valid.

- [ ] **Step 4: Copy deliverable to `dist/` and compute SHA256**
