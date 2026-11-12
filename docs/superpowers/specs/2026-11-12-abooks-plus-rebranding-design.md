# Design: Rebranding to ABooks++ and New Adaptive Icon

**Date:** 2026-11-12  
**Status:** Pending Approval  

---

## 1. Intent & Context
The app is a customized fork of AudioBooks featuring modern themes, expanded audio sources, and an in-app updater. To visually and conceptually represent this enhanced edition, the app is being rebranded to **ABooks++** with a custom modern dark icon featuring a listener mascot (headphones, open book, and `+ +` eyes).

---

## 2. Goals & Non-Goals

### Goals
- Rebrand the app title to **`ABooks++`** across launcher labels, action bar / toolbar references, and user-facing dialogs where appropriate.
- Replace the legacy vinyl disc launcher icon with the approved modern design:
  - Adaptive icon (`ic_launcher.xml` and `ic_launcher_round.xml` in `mipmap-anydpi-v26/`): dark matte background (`#18191C`) with a crisp vector emblem foreground (`ic_launcher_foreground.xml`) featuring the headphones, book, and `+ +` eyes inside the 72dp safe area.
  - Legacy launcher icons (`ic_launcher.png`, `ic_launcher_round.png`) across all densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) matching the squircle and circular forms.
- Keep the existing `applicationId` (`link.intertop.audiobooks`) to preserve in-app update capability (`Updater.java`) and avoid losing user data.
- Verify building, tests, and package signing.

### Non-Goals
- Changing internal code package names (`com.fanok.audiobooks.*`), file providers, or database schemas.
- Changing the update server URLs or repository routes at this time.

### Anti-Goals
- Cropping the `+ +` or headphones on circular launcher masks (all essential details must fit comfortably within the 72dp adaptive icon safe zone).
- Distorting icon aspect ratio or introducing blurred raster scaling.

---

## 3. Constraints
- **Android Adaptive Icon Spec:** Full canvas is 108dp x 108dp; guaranteed visible safe zone is the centered inner 72dp circle.
- **Gradle & Build:** Maintain Java 8 / Android SDK 35 compatibility. Follow `CLAUDE.md` and `android-apk-signing` guidelines (no parallel gradle daemons, deliver APK to `dist/`).

---

## 4. Approaches Evaluated

1. **Approach 1: Full Modern Adaptive Icon (Vector Foreground + Solid/Gradient Background) + Rendered Mipmaps (Chosen)**
   - *Why chosen:* Vector foreground provides infinite resolution and crisp rendering across all screen densities and launcher animations. Renders raster mipmaps for older API levels (< Android 8.0).
   - *Tradeoffs:* Requires generating clean, balanced vector paths for the emblem.

2. **Approach 2: Pure Raster Adaptive Icon (PNG layers)**
   - *Tradeoffs:* Simpler to produce directly from raster image, but lacks the crisp sharpness of vector geometry on high-density displays (xxxhdpi) and launcher scale animations. Rejected in favor of Approach 1.

---

## 5. Detailed Implementation Plan

### 5.1 App Name Update
- Update `res/values/strings.xml`:
  ```xml
  <string name="app_name" translatable="false">ABooks++</string>
  ```
- Check references in `AndroidManifest.xml`, notification titles, and about screens to ensure proper layout and display.

### 5.2 Icon Assets Generation
- Source artwork: `C:\Users\la\Downloads\AudioBooks-icon-eyes-higher.jpg`.
- **Vector Foreground (`res/drawable/ic_launcher_foreground.xml`):**
  - Viewport: 108 x 108 dp.
  - Geometry: Headband arch, earcups with subtle metallic silver shades/accents, open book pages with spine curve, and two centered plus symbols `+ +` positioned in the upper arch. All content scaled and centered within the 72dp circle.
- **Background (`res/values/ic_launcher_background.xml` & `res/drawable/ic_launcher_background.xml`):**
  - Dark matte color: `#18191C` (matching the tile from the design).
- **Legacy Mipmaps (`res/mipmap-*/`):**
  - Generate square/squircle `ic_launcher.png` and circular `ic_launcher_round.png` for `mdpi` (48px), `hdpi` (72px), `xhdpi` (96px), `xxhdpi` (144px), `xxxhdpi` (192px).

### 5.3 Verification & Build
- Run unit tests: `:app:testDebugUnitTest`.
- Build signed release APK and debug APK.
- Copy deliverables to `dist/`.
- Verify the APK launcher label and icon resources with `aapt2` / `apksigner`.
