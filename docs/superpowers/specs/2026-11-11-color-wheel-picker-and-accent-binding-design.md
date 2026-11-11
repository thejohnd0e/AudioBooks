# Design Spec: Color Wheel Picker & Dynamic Custom Accent Binding

## 1. Intent & Context
In the 3-color palette architecture (released in v8.5.0), users can choose "Своя палитра" (Custom Palette) and customize Background, Surface, and Accent colors.
Two critical UX gaps were discovered:
1. **Color Picker UX**: The initial picker dialog relied on a small static grid of circles + HEX input, which was unintuitive and made fine-tuning colors difficult.
2. **Custom Accent Not Propagating**: While custom Accent was saved to `pref_custom_color_accent`, system widgets (such as `SeekBarPreference` font scale slider, switches, and checkboxes in Settings) remained hardcoded to the sage green (`#7EAC91`) defined in the static XML `ThemeOverlay.AudioBooks.Palette.Custom`.

The user requested:
- Replacing the color selection mechanism with **Variant 1: Color Wheel (HSV wheel + Brightness slider + HEX input)**.
- Ensuring the chosen custom accent color reliably and dynamically tints all controls, sliders, and buttons across the app (including the Settings screen).

## 2. Goals & Non-Goals
### Goals
- Implement an interactive, native `ColorWheelView` (HSV circle with Hue/Saturation touch tracking, Brightness/Value slider, live comparison preview, and direct HEX `#RRGGBB` input).
- Maintain the **Darkness Guard** (HSV Value ≤ 0.38) when choosing Background or Surface, and full freedom (Value 0.0–1.0) when choosing Accent.
- Zero external libraries added (implemented via lightweight Android Canvas / gradients for optimal performance and no dependency bloat).
- Expand `PaletteBinder` to dynamically tint `SeekBar`, `CompoundButton` (CheckBox/Switch/RadioButton), and `MaterialButton` controls in `SettingsFragment` and activities, eliminating the lingering green theme tint when a custom accent is active.
- Immediate visual refresh: when user applies a custom color in the picker, the active settings screen instantly reflects the new color.

### Non-Goals
- Adding complex alpha/opacity transparency channels (RGB `#RRGGBB` is sufficient and prevents translucent text contrast issues).
- Redesigning the built-in preset palettes (Sage, Umbra, Steel, Onyx remain as designed).

## 3. Approaches Considered
- **Option 1: Classic HSV Color Wheel (Chosen)**
  - *Pros*: Natural, intuitive, single-touch Hue + Saturation selection, separate Brightness slider, fits cleanly on both phones and tablets, universally familiar in Android audio players.
  - *Cons*: Requires custom touch math (`atan2`, radial distance), easily handled with clean Canvas math.
- **Option 2: 2D Rectangular Spectrum (Photoshop/Figma style)**
  - *Pros*: Precise rectangular mapping.
  - *Cons*: More cluttered with extra sliders; less compact in standard dialogs.
- **Option 3: Material Palette Grid + Spectrum**
  - *Pros*: Curated 100-900 material tones.
  - *Cons*: Two-tab navigation, more clicks to reach custom hues.
- **Option 4: 3 Linear Sliders (Hue/Sat/Val)**
  - *Pros*: Simple math.
  - *Cons*: Harder to conceptualize a color than touching a visual circle.

## 4. Architecture & Implementation Details

### A. `ColorWheelView` (Custom View)
- A self-contained custom `View`:
  - `onDraw`: Draws an HSV color wheel using `SweepGradient` for 360° hues and a `RadialGradient` for white saturation falloff.
  - Touch handler: Converts `(x, y)` relative to wheel center into Hue ($0^\circ..360^\circ$) and Saturation ($0.0..1.0$).
  - Draws a circular selector indicator at the currently selected coordinates.
  - Provides listeners `onColorChanged(int color)` and methods `setColor(int color)` / `getColor()`.

### B. `ColorPickerDialog` Layout & Logic
- Incorporates `ColorWheelView`, an interactive Brightness `SeekBar` (gradient track from black to current Hue/Sat color), preview box (Current vs New), and `TextInputEditText` for HEX input.
- Automatically synchronizes touch events on the wheel with the brightness slider and the HEX text input.
- Enforces Darkness Guard on Background & Surface slots by clamping the brightness slider max to 38%.

### C. Dynamic Custom Accent Propagation in `PaletteBinder`
- Extend `PaletteBinder`:
  - `bind(Activity)`: In addition to window, toolbar, tab layout, and play controls, bind general controls.
  - `tintControls(View rootView, int accent, int surface, int bg)`: Recursively or targetedly tints:
    - `SeekBar` (`progressTintList`, `thumbTintList`);
    - `CompoundButton` (`buttonTintList`);
    - `ProgressBar` (`progressTintList`);
    - `MaterialButton` (accent background / stroke).
  - In `SettingsFragment`:
    - In `onViewCreated` and after palette changes, invoke `PaletteBinder.tintControls(getView(), accent, surface, bg)`.
    - When a custom color is saved, update the settings list items and call `App.recreateActivities()` so that the entire app and current settings immediately display the new accent.

## 5. Verification Plan
- Unit tests: Add tests for HSV conversions, hex parsing/formatting with color wheel values.
- Build test: `gradlew :app:testDebugUnitTest`.
- Functional / UI verification:
  - Run on Android emulator / device.
  - Open Settings -> Appearance -> Настроить палитру.
  - Select "Акцентный цвет" -> verify Color Wheel opens with Hue circle and brightness slider.
  - Select an accent (e.g. Amber `#FFB703` or Coral `#E07A5F` or Blue `#3A86FF`) -> verify font scale slider, switches, and book buttons change to that accent (no longer green).
