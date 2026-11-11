package com.fanok.audiobooks;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * The app's theme preference ({@code pref_theme}) and palette preference ({@code pref_palette})
 * in one place: night mode goes through {@link AppCompatDelegate#setDefaultNightMode} (never by
 * editing a {@code Configuration}), and an activity's style and palette overlay are chosen once,
 * before {@code setContentView}.
 */
public final class AppThemes {

    public static final String PREF_PALETTE = "pref_palette";
    public static final String PALETTE_SAGE = "sage";
    public static final String PALETTE_UMBRA = "umbra";
    public static final String PALETTE_STEEL = "steel";
    public static final String PALETTE_ONYX = "onyx";
    public static final String PALETTE_CUSTOM = "custom";
    public static final String DEFAULT_PALETTE = PALETTE_SAGE;

    public static final int SLOT_BG = 0;
    public static final int SLOT_SURFACE = 1;
    public static final int SLOT_ACCENT = 2;

    public static final String PREF_CUSTOM_BG = "pref_custom_color_bg";
    public static final String PREF_CUSTOM_SURFACE = "pref_custom_color_surface";
    public static final String PREF_CUSTOM_ACCENT = "pref_custom_color_accent";

    public static final int DEFAULT_CUSTOM_BG = 0xFF141816;
    public static final int DEFAULT_CUSTOM_SURFACE = 0xFF1F2622;
    public static final int DEFAULT_CUSTOM_ACCENT = 0xFF7EAC91;

    private AppThemes() {
    }

    /** Formats a 32-bit color int to a hex string with a leading '#'. */
    @NonNull
    public static String formatHex(int color) {
        return String.format("#%06x", (0xFFFFFF & color));
    }

    /** Parses a hex color string (with or without '#') into an ARGB color int. */
    public static int parseHex(String hex, int defaultColor) {
        Integer parsed = parseHexColor(hex);
        return parsed != null ? parsed : defaultColor;
    }

    /** Parses a hex color string (with or without '#') into an ARGB color Integer, or null if invalid. */
    public static Integer parseHexColor(String hex) {
        if (hex == null) return null;
        String clean = hex.trim();
        if (clean.startsWith("#")) clean = clean.substring(1);
        if (clean.length() != 6 && clean.length() != 8) return null;
        try {
            if (clean.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(clean, 16));
            } else {
                return (int) Long.parseLong(clean, 16);
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Returns the stored custom color for the given slot. */
    public static int getCustomColor(@NonNull Context context, int slot) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        switch (slot) {
            case SLOT_SURFACE:
                return pref.getInt(PREF_CUSTOM_SURFACE, DEFAULT_CUSTOM_SURFACE);
            case SLOT_ACCENT:
                return pref.getInt(PREF_CUSTOM_ACCENT, DEFAULT_CUSTOM_ACCENT);
            case SLOT_BG:
            default:
                return pref.getInt(PREF_CUSTOM_BG, DEFAULT_CUSTOM_BG);
        }
    }

    /** Stores the custom color for the given slot. */
    public static void setCustomColor(@NonNull Context context, int slot, int color) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String key;
        switch (slot) {
            case SLOT_SURFACE:
                key = PREF_CUSTOM_SURFACE;
                break;
            case SLOT_ACCENT:
                key = PREF_CUSTOM_ACCENT;
                break;
            case SLOT_BG:
            default:
                key = PREF_CUSTOM_BG;
                break;
        }
        pref.edit().putInt(key, color).apply();
    }

    /** Migrates legacy pref_theme setting to pref_palette if pref_palette is not yet present. */
    public static void migrateLegacyTheme(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (!pref.contains(PREF_PALETTE) && pref.contains("pref_theme")) {
            String legacy = pref.getString("pref_theme", "");
            if ("black".equals(legacy)) {
                pref.edit().putString(PREF_PALETTE, PALETTE_ONYX).apply();
            } else {
                pref.edit().putString(PREF_PALETTE, PALETTE_SAGE).apply();
            }
        }
    }

    /** The stored theme value (dark by default). */
    @NonNull
    public static String current(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString("pref_theme", context.getString(R.string.theme_dark_value));
    }

    /** The stored palette value (sage by default). */
    @NonNull
    public static String currentPalette(@NonNull Context context) {
        migrateLegacyTheme(context);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return normalizePalette(pref.getString(PREF_PALETTE, DEFAULT_PALETTE));
    }

    /** Normalizes a palette value to one of the supported palette IDs, defaulting to sage. */
    @NonNull
    public static String normalizePalette(String palette) {
        if (PALETTE_UMBRA.equals(palette)) {
            return PALETTE_UMBRA;
        }
        if (PALETTE_STEEL.equals(palette)) {
            return PALETTE_STEEL;
        }
        if (PALETTE_ONYX.equals(palette)) {
            return PALETTE_ONYX;
        }
        if (PALETTE_CUSTOM.equals(palette)) {
            return PALETTE_CUSTOM;
        }
        return PALETTE_SAGE;
    }

    /** The theme overlay resource matching the current palette. */
    @StyleRes
    public static int paletteStyle(@NonNull Context context) {
        String palette = currentPalette(context);
        if (PALETTE_UMBRA.equals(palette)) {
            return R.style.ThemeOverlay_AudioBooks_Palette_Umbra;
        }
        if (PALETTE_STEEL.equals(palette)) {
            return R.style.ThemeOverlay_AudioBooks_Palette_Steel;
        }
        if (PALETTE_ONYX.equals(palette)) {
            return R.style.ThemeOverlay_AudioBooks_Palette_Onyx;
        }
        if (PALETTE_CUSTOM.equals(palette)) {
            return R.style.ThemeOverlay_AudioBooks_Palette_Custom;
        }
        return R.style.ThemeOverlay_AudioBooks_Palette_Sage;
    }

    /** Applies the current palette overlay to a theme. */
    public static void applyPalette(@NonNull Context context, @NonNull Resources.Theme theme) {
        int style = paletteStyle(context);
        android.util.Log.i("AppThemes", "applyPalette: context=" + context.getClass().getSimpleName()
                + ", palette=" + currentPalette(context) + ", overlayStyle=" + Integer.toHexString(style));
        theme.applyStyle(style, true);
    }

    /** Applies the night mode the preference implies; called at startup and when the preference changes. */
    public static void applyNightMode(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        // Android 12+ draws the starting window before the app runs: told the app's mode, it takes the matching color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            UiModeManager uiMode = context.getApplicationContext().getSystemService(UiModeManager.class);
            if (uiMode != null) {
                uiMode.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES);
            }
        }
    }

    /** The one of the three styles that matches the preference. */
    @StyleRes
    public static int style(@NonNull Context context, @StyleRes int dark, @StyleRes int light, @StyleRes int black) {
        return dark;
    }

    /** Sets the matching style and palette overlay on {@code activity}; call before {@code super.onCreate}/{@code setContentView}. */
    public static void apply(@NonNull Activity activity, @StyleRes int dark, @StyleRes int light, @StyleRes int black) {
        activity.setTheme(style(activity, dark, light, black));
        applyPalette(activity, activity.getTheme());
    }
}
