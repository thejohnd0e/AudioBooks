# Three-Color Palette & Custom Color Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the legacy theme selector (`pref_theme`), add an Onyx (AMOLED) preset, and implement a 3-color palette system (Background, Surface, Accent) with custom HEX color picking and zero gray AppCompat artifacts.

**Architecture:** `AppThemes` becomes the single authority for visual styling. Predefined palettes (Sage, Umbra, Steel, Onyx) apply via compiled XML overlays (`ThemeOverlay.AudioBooks.Palette.*`), while custom user colors are stored as 3 HEX values and bound across Activities via a lightweight `PaletteBinder`. The legacy `pref_theme` setting is migrated to `pref_palette` and deleted from `preferences.xml`.

**Tech Stack:** Android Java (source level 8 with desugaring), Android SDK platform 36, AndroidX Material Components, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-11-three-color-palette-system-design.md`

## Global Constraints

- Preserve independent fork identity: applicationId `link.intertop.audiobooks`.
- No new external library dependencies: color picker is self-contained.
- Single Gradle build at a time; no `gradlew clean`.
- Emulator testing only with explicit approval.
- Dark luminance guard: Background and Surface colors in the custom picker enforce HSV Value <= 0.35 to ensure static white/light text remains 100% readable.

## Review Focus

1. **Legacy preference migration:** existing `pref_theme = "black"` cleanly maps to `pref_palette = "onyx"`, and `"dark"` / `"light"` map to `"sage"` without crash.
2. **Custom HEX formatting:** invalid HEX strings (e.g. `"#xyz"`, empty) gracefully fallback to defaults without throwing `IllegalArgumentException`.
3. **Dialog styling:** `AlertDialog` instances inherit the palette's Surface color instead of the legacy `#303030` AppCompat gray.
4. **Seamless headers:** Toolbar and Status Bar match Background color with no mismatched horizontal stripes.
5. **Palette recreation:** Changing palette re-creates the Activity smoothly with immediate visual update.

---

### Task 1: AppThemes and Preference Migration

**Files:**
- Modify: `app/src/main/java/com/fanok/audiobooks/AppThemes.java`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/arrays.xml`
- Test: `app/src/test/java/com/fanok/audiobooks/AppThemesTest.java`

**Interfaces:**
- Produces:
  - `AppThemes.PALETTE_ONYX = "onyx"`
  - `AppThemes.PALETTE_CUSTOM = "custom"`
  - `AppThemes.getCustomColor(Context, int slot)` (slots: `SLOT_BG`, `SLOT_SURFACE`, `SLOT_ACCENT`)
  - `AppThemes.setCustomColor(Context, int slot, int color)`
  - `AppThemes.migrateLegacyTheme(Context)`

- [ ] **Step 1: Write failing unit tests for migration and custom color storage**

```java
@Test public void migratesLegacyBlackThemeToOnyx() {
    // Tests that if pref_palette is unset and pref_theme == "black", currentPalette returns "onyx"
}
@Test public void customColorStoragePreservesHex() {
    // Tests that setting and getting custom colors returns expected integer ARGB values
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cmd /c gradlew.bat :app:testDebugUnitTest --tests com.fanok.audiobooks.AppThemesTest`
Expected: FAIL

- [ ] **Step 3: Implement Onyx, Custom slot constants, and migration in `AppThemes.java`**

Implement constants `PALETTE_ONYX`, `PALETTE_CUSTOM`, helper methods `getCustomColor`, `setCustomColor`, and `migrateLegacyTheme`. Update `arrays.xml` and `strings.xml` with "Оникс (AMOLED)" and "Своя палитра".

- [ ] **Step 4: Run test to verify it passes**

Run: `cmd /c gradlew.bat :app:testDebugUnitTest --tests com.fanok.audiobooks.AppThemesTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fanok/audiobooks/AppThemes.java app/src/main/res/values/strings.xml app/src/main/res/values/arrays.xml app/src/test/java/com/fanok/audiobooks/AppThemesTest.java
git commit -m "feat(themes): add onyx and custom palette slots with legacy migration"
```

---

### Task 2: XML Palettes and Dialog Surface Cleanup

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values-night/colors.xml`
- Modify: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/layout/books_recycler_item.xml`

**Interfaces:**
- Produces:
  - `ThemeOverlay.AudioBooks.Palette.Onyx` (pure `#000000` background, `#121212` surface, `#7EAC91` or `#FFFFFF` accent)
  - `ThemeOverlay.AudioBooks.Dialog` mapped to `colorSurfaceCard`
  - Removal of hardcoded/dirty gray backgrounds from list items

- [ ] **Step 1: Define Onyx colors and Dialog overlay in `colors.xml` and `styles.xml`**

Define `palette_onyx_background` (`#000000`), `palette_onyx_surface` (`#121212`), `palette_onyx_accent` (`#7EAC91`). Update `ThemeOverlay.AudioBooks.Palette.Onyx` and ensure all palette overlays set `alertDialogTheme` to use `colorSurfaceCard`.

- [ ] **Step 2: Clean up catalog item background**

Ensure `books_recycler_item.xml` and `books_recycler_item_big.xml` use seamless background or card surface (`?attr/colorSurfaceCard`) instead of legacy dark gray box.

- [ ] **Step 3: Verify build compiles and resources merge**

Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values-night/colors.xml app/src/main/res/values/styles.xml app/src/main/res/layout/books_recycler_item.xml
git commit -m "style(palette): add onyx amoled overlay and eliminate gray dialog surfaces"
```

---

### Task 3: PaletteBinder for Dynamic Colors

**Files:**
- Create: `app/src/main/java/com/fanok/audiobooks/PaletteBinder.java`
- Modify: `app/src/main/java/com/fanok/audiobooks/FontScale.java`
- Modify: `app/src/main/java/com/fanok/audiobooks/activity/BookActivity.java`
- Modify: `app/src/main/java/com/fanok/audiobooks/activity/MainActivity.java`

**Interfaces:**
- Produces:
  - `PaletteBinder.bind(Activity activity)`: tints window background, toolbar, status bar, and key custom components if custom palette is active.

- [ ] **Step 1: Write `PaletteBinder.java`**

Extract and apply Background, Surface, and Accent.
- Sets `activity.getWindow().setBackgroundDrawable(new ColorDrawable(bg))`
- Sets Status Bar color to `bg`
- Updates Toolbar background to `bg`
- Tints cards and bottom player play buttons with `accent`

- [ ] **Step 2: Connect `PaletteBinder` to base activities**

Call `PaletteBinder.bind(this)` in `MainActivity.onCreate` and `BookActivity.onCreate`.

- [ ] **Step 3: Run unit tests**

Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/fanok/audiobooks/PaletteBinder.java app/src/main/java/com/fanok/audiobooks/activity/MainActivity.java app/src/main/java/com/fanok/audiobooks/activity/BookActivity.java
git commit -m "feat(themes): add PaletteBinder for dynamic custom palette styling"
```

---

### Task 4: Color Picker Dialog & Settings Cleanup

**Files:**
- Create: `app/src/main/java/com/fanok/audiobooks/dialogs/ColorPickerDialog.java`
- Create: `app/src/main/res/layout/dialog_color_picker.xml`
- Modify: `app/src/main/res/xml/preferences.xml`
- Modify: `app/src/main/java/com/fanok/audiobooks/fragment/SettingsFragment.java`

**Interfaces:**
- Produces:
  - `ColorPickerDialog`: allows picking from preset swatches or entering `#RRGGBB` with HSV Value clamp for dark backgrounds.
  - Removal of `theme` ListPreference from `preferences.xml`.
  - When "Своя палитра" is selected, launches custom 3-color customization.

- [ ] **Step 1: Create `dialog_color_picker.xml` layout**

Layout containing:
- Preview square of current vs new color
- Grid of preset swatches
- EditText for HEX input (`#RRGGBB`)
- Cancel and OK buttons

- [ ] **Step 2: Implement `ColorPickerDialog.java`**

Includes HEX text watcher with `#` prefix enforcement, parsing validation, and darkness guard for Background/Surface slots.

- [ ] **Step 3: Update `SettingsFragment.java` and `preferences.xml`**

Remove the `theme` ListPreference. Connect `pref_palette` change listener so selecting `custom` opens the 3-slot color chooser (Фон, Поверхность, Акцент).

- [ ] **Step 4: Verify compilation and tests**

Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fanok/audiobooks/dialogs/ColorPickerDialog.java app/src/main/res/layout/dialog_color_picker.xml app/src/main/res/xml/preferences.xml app/src/main/java/com/fanok/audiobooks/fragment/SettingsFragment.java
git commit -m "feat(settings): replace legacy theme setting with 3-color custom palette picker"
```

---

### Task 5: Verification & End-to-End Build

**Files:**
- Test: emulator verification across all 4 presets + custom palette
- Build: `:app:assembleRelease`

- [ ] **Step 1: Run full unit test suite**

Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Build release APK**

Run release build with signing key and confirm APK creation:
`app/build/outputs/apk/release/AudioBooks-8.5.0-release.apk`

- [ ] **Step 3: Verify on Android emulator**

Test palette selection:
- Verify legacy "Тема" dialog is gone from Settings.
- Verify "Оникс (AMOLED)" produces pitch-black `#000000` background.
- Verify "Своя палитра" opens color picker, accepts custom HEX, and applies cleanly without gray background leaks.
- Capture screenshots for visual confirmation.

- [ ] **Step 4: Final commit and branch review**

```bash
git status
git commit -m "feat: complete three-color palette system and custom color engine"
```
