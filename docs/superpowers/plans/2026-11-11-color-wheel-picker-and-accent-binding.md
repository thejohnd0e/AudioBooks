# Color Wheel Picker & Dynamic Custom Accent Binding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the static swatches color picker with an interactive HSV Color Wheel View with brightness slider and HEX input, and fix custom accent color propagation so that sliders (e.g. font scale in Settings), switches, and controls dynamically inherit the chosen accent instead of staying green.

**Architecture:** Create an Android Canvas-based `ColorWheelView` (SweepGradient Hue + RadialGradient Saturation) integrated into `ColorPickerDialog` with an interactive Brightness slider and live HEX sync. Extend `PaletteBinder` to dynamically tint `SeekBar`, `CompoundButton`, `ProgressBar`, and `MaterialButton` instances across activities and `SettingsFragment`.

**Tech Stack:** Java 8, Android SDK 36 (Platform), Android Canvas 2D graphics, MaterialComponents, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-11-color-wheel-picker-and-accent-binding-design.md`

## Global Constraints
- Namespace: `com.fanok.audiobooks`, fork application ID: `link.intertop.audiobooks`.
- Zero new third-party dependencies added (Canvas-based custom view).
- Darkness Guard: HSV Value ≤ 0.38 for Background (slot 0) and Surface (slot 1); unconstrained (0.0–1.0) for Accent (slot 2).
- One Gradle build at a time; preserve existing release keystores and build caches.

## Review Focus
1. Touching outside the circular wheel boundary clamps radius to 1.0 (does not produce NaN or out-of-range saturation).
2. Selecting pure black (`#000000`) or pure white (`#FFFFFF`) does not corrupt Hue angle for subsequent slider moves.
3. Rapidly typing in the HEX text field updates the color wheel selector position without causing circular text watcher loops.
4. Setting a custom accent color in Settings immediately re-tints the font scale slider and controls without requiring an app restart.
5. Background and Surface color picking restricts maximum brightness slider value to 38%.

---

### Task 1: Create `ColorWheelView` Custom View

**Files:**
- Create: `app/src/main/java/com/fanok/audiobooks/view/ColorWheelView.java`
- Test: `app/src/test/java/com/fanok/audiobooks/AppThemesTest.java`

**Interfaces:**
- Produces:
  - `ColorWheelView(Context context, AttributeSet attrs)`
  - `void setColor(int color)`
  - `int getColor()`
  - `void setHue(float hue)` / `float getHue()`
  - `void setSaturation(float sat)` / `float getSaturation()`
  - `void setOnColorChangeListener(OnColorChangeListener listener)`
  - `interface OnColorChangeListener { void onColorChanged(float hue, float sat, int rgb); }`

- [ ] **Step 1: Write unit tests for HSV color conversion and wheel touch math in `AppThemesTest.java`**
  Add unit tests validating `Color.colorToHSV` and `Color.HSVToColor` conversions across pure white, pure black, standard hues ($0^\circ, 60^\circ, 120^\circ, 180^\circ, 240^\circ, 300^\circ$), and saturation bounds clamping ($[0.0, 1.0]$).

- [ ] **Step 2: Run test to verify it passes or fails**
  Run: `cmd /c gradlew.bat :app:testDebugUnitTest --tests com.fanok.audiobooks.AppThemesTest`
  Expected: PASS

- [ ] **Step 3: Implement `ColorWheelView` in `app/src/main/java/com/fanok/audiobooks/view/ColorWheelView.java`**
  - Implement custom View subclassing `View`.
  - In `onSizeChanged`: generate `SweepGradient` with 7 rainbow colors for Hue, composed with a `RadialGradient` (white to transparent) for Saturation.
  - In `onDraw`: draw the composite wheel circle, and draw a thumb indicator (circle with stroke and shadow) at the current `(hue, sat)` coordinates.
  - In `onTouchEvent`: calculate angle $\theta = \text{atan2}(dy, dx)$ to derive Hue ($0^\circ..360^\circ$) and distance $r = \sqrt{dx^2 + dy^2} / R$ clamped to $[0.0, 1.0]$ for Saturation.
  - Notify `OnColorChangeListener` when user touches or drags.

- [ ] **Step 4: Verify compilation and tests**
  Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/com/fanok/audiobooks/view/ColorWheelView.java app/src/test/java/com/fanok/audiobooks/AppThemesTest.java
  git commit -m "feat(view): add ColorWheelView for interactive HSV hue and saturation picking"
  ```

---

### Task 2: Redesign `ColorPickerDialog` with Color Wheel, Brightness Slider & HEX Input

**Files:**
- Modify: `app/src/main/res/layout/dialog_color_picker.xml`
- Modify: `app/src/main/java/com/fanok/audiobooks/dialogs/ColorPickerDialog.java`

**Interfaces:**
- Consumes: `ColorWheelView` from Task 1, `AppThemes.formatHex`, `AppThemes.parseHexColor`.
- Produces: `ColorPickerDialog.show(Context, String title, int initialColor, boolean isDarkClamped, OnColorPickedListener)`

- [ ] **Step 1: Update layout `dialog_color_picker.xml`**
  - Replace the old `swatchesGrid` with:
    - `ColorWheelView` (width 220dp, height 220dp, centered).
    - Brightness `SeekBar` (`id/brightnessSlider`) with a custom track gradient or standard Material styling.
    - Preserved live preview box (Old vs New) and outlined `TextInputLayout` with `TextInputEditText` (`id/hexInput`).

- [ ] **Step 2: Update `ColorPickerDialog.java` logic**
  - Initialize `ColorWheelView` with `initialColor`'s Hue and Saturation.
  - Initialize brightness `SeekBar` (0–100 for Accent, clamped to max 38 for Background and Surface when `isDarkClamped` is true).
  - Sync `ColorWheelView` drag events -> updates brightness slider gradient track + updates `hexInput.setText(...)` + updates `previewNew.setBackgroundColor(...)`.
  - Sync brightness `SeekBar` progress -> recomputes HSV RGB -> updates `hexInput` and `previewNew`.
  - Sync `hexInput` typing -> if valid HEX, updates `ColorWheelView` thumb position and brightness slider without recursive loops.
  - On dialog OK -> invoke `listener.onColorPicked(selectedColor)`.

- [ ] **Step 3: Run unit tests to verify no regressions**
  Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/res/layout/dialog_color_picker.xml app/src/main/java/com/fanok/audiobooks/dialogs/ColorPickerDialog.java
  git commit -m "feat(picker): integrate ColorWheelView and brightness slider into ColorPickerDialog"
  ```

---

### Task 3: Dynamic Accent Tinting in `PaletteBinder` & `SettingsFragment`

**Files:**
- Modify: `app/src/main/java/com/fanok/audiobooks/PaletteBinder.java`
- Modify: `app/src/main/java/com/fanok/audiobooks/fragment/SettingsFragment.java`
- Modify: `app/src/main/res/values/styles.xml`

**Interfaces:**
- Consumes: `AppThemes.getCustomColor`, `AppThemes.currentPalette`.
- Produces: `PaletteBinder.tintControls(View rootView, int accent, int surface, int bg)`

- [ ] **Step 1: Add dynamic view hierarchy control tinting to `PaletteBinder.java`**
  - Add `PaletteBinder.tintControls(View root, int accent, int surface, int bg)`:
    - Recursively traverse child views or target known control types:
    - If `SeekBar`: apply `progressTintList = ColorStateList.valueOf(accent)` and `thumbTintList = ColorStateList.valueOf(accent)`.
    - If `CompoundButton` (CheckBox, RadioButton, Switch): apply `buttonTintList = ColorStateList.valueOf(accent)`.
    - If `ProgressBar`: apply `progressTintList = ColorStateList.valueOf(accent)`.
  - In `PaletteBinder.bind(Activity)`: call `tintControls(activity.getWindow().getDecorView(), accent, surface, bg)`.

- [ ] **Step 2: Bind controls in `SettingsFragment.java`**
  - In `onViewCreated(View view, Bundle savedInstanceState)`: call `PaletteBinder.bindView(view, requireContext())` and `PaletteBinder.tintControls(view, accent, surface, bg)`.
  - In `showCustomPaletteMenu()` when color is chosen:
    - Store the new color via `AppThemes.setCustomColor(context, slot, newColor)`.
    - Immediately re-tint current fragment views: `PaletteBinder.tintControls(getView(), accent, surface, bg)`.
    - Post `App::recreateActivities` so all screens update.

- [ ] **Step 3: Run unit tests**
  Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
  ```bash
  git add app/src/main/java/com/fanok/audiobooks/PaletteBinder.java app/src/main/java/com/fanok/audiobooks/fragment/SettingsFragment.java app/src/main/res/values/styles.xml
  git commit -m "fix(palette): propagate custom accent color dynamically to SeekBars and controls"
  ```

---

### Task 4: Full App Build & End-to-End Emulator Verification

**Files:**
- Verify: Full release build `assembleRelease`
- Live emulator testing on Pixel / tablet emulator

- [ ] **Step 1: Run full unit test suite**
  Run: `cmd /c gradlew.bat :app:testDebugUnitTest`
  Expected: 0 failures, all tests PASS.

- [ ] **Step 2: Assemble signed release APK**
  Run release packaging with test credentials.
  Expected: `app/build/outputs/apk/release/AudioBooks-8.5.0-release.apk` built successfully.

- [ ] **Step 3: Test on live Android emulator**
  - Launch emulator.
  - Install release APK.
  - Open Settings -> Appearance -> Настроить палитру -> Акцентный цвет.
  - Interact with Color Wheel circle and brightness slider.
  - Set accent to Amber `#FFB703` or Coral `#E07A5F`.
  - Confirm the Font Scale slider (`SeekBarPreference`) and checkboxes immediately become Amber/Coral (not green).
  - Clean up emulator and stop Gradle daemon.

- [ ] **Step 4: Commit and finalize SDD ledger**
  ```bash
  git add .
  git commit -m "chore: complete color wheel picker and custom accent tinting verification"
  ```
