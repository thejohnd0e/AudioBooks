package com.fanok.audiobooks;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayout;

/**
 * Binds active palette colors (Background, Surface, Accent) directly to window, toolbar,
 * and key controls to eliminate all background seams and support dynamic custom colors.
 */
public final class PaletteBinder {

    private PaletteBinder() {
    }

    private static final RecyclerView.OnChildAttachStateChangeListener PREF_ATTACH_LISTENER =
            new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(@NonNull View view) {
                    Context context = view.getContext();
                    String palette = AppThemes.currentPalette(context);
                    int accent;
                    if (AppThemes.PALETTE_CUSTOM.equals(palette)) {
                        accent = AppThemes.getCustomColor(context, AppThemes.SLOT_ACCENT);
                    } else {
                        accent = MaterialColors.getColor(context, R.attr.colorAccentPrimary, 0xFF7EAC91);
                    }
                    tintControls(view, accent);
                }

                @Override
                public void onChildViewDetachedFromWindow(@NonNull View view) {}
            };

    /**
     * Applies the current palette to the activity window, toolbar, and interactive elements.
     */
    public static void bind(@NonNull Activity activity) {
        String palette = AppThemes.currentPalette(activity);
        int bg;
        int surface;
        int accent;

        if (AppThemes.PALETTE_CUSTOM.equals(palette)) {
            bg = AppThemes.getCustomColor(activity, AppThemes.SLOT_BG);
            surface = AppThemes.getCustomColor(activity, AppThemes.SLOT_SURFACE);
            accent = AppThemes.getCustomColor(activity, AppThemes.SLOT_ACCENT);
        } else {
            bg = MaterialColors.getColor(activity, R.attr.colorBackgroundMain, 0xFF141816);
            surface = MaterialColors.getColor(activity, R.attr.colorSurfaceCard, 0xFF1F2622);
            accent = MaterialColors.getColor(activity, R.attr.colorAccentPrimary, 0xFF7EAC91);
        }

        android.util.Log.i("PaletteBinder", "bind: palette=" + palette + ", bg=" + AppThemes.formatHex(bg)
                + ", surface=" + AppThemes.formatHex(surface) + ", accent=" + AppThemes.formatHex(accent));

        // Seamless window and system bars
        activity.getWindow().setBackgroundDrawable(new ColorDrawable(bg));
        activity.getWindow().setStatusBarColor(bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(bg);
        }

        // Seamless toolbar matching window canvas
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(bg);
        }

        // TabLayout indicator
        TabLayout tabs = activity.findViewById(R.id.tabs);
        if (tabs != null) {
            tabs.setBackgroundColor(bg);
            tabs.setSelectedTabIndicatorColor(accent);
            tabs.setTabTextColors(0x99FFFFFF, accent);
        }

        // Main play buttons
        ImageButton playTop = activity.findViewById(R.id.playTop);
        if (playTop != null) {
            ViewCompat.setBackgroundTintList(playTop, ColorStateList.valueOf(accent));
        }

        ImageButton playBottom = activity.findViewById(R.id.playBottom);
        if (playBottom != null) {
            ViewCompat.setBackgroundTintList(playBottom, ColorStateList.valueOf(accent));
        }

        // Player top progress indicator
        ProgressBar playerProgress = activity.findViewById(R.id.progressBar);
        if (playerProgress != null) {
            playerProgress.setProgressTintList(ColorStateList.valueOf(accent));
        }

        // Dynamic controls across the activity
        tintControls(activity.getWindow().getDecorView(), accent);
    }

    /**
     * Binds custom colors to dynamic views within a fragment or view hierarchy.
     */
    public static void bindView(@NonNull View view, @NonNull Context context) {
        String palette = AppThemes.currentPalette(context);
        int bg;
        int surface;
        int accent;

        if (AppThemes.PALETTE_CUSTOM.equals(palette)) {
            bg = AppThemes.getCustomColor(context, AppThemes.SLOT_BG);
            surface = AppThemes.getCustomColor(context, AppThemes.SLOT_SURFACE);
            accent = AppThemes.getCustomColor(context, AppThemes.SLOT_ACCENT);
        } else {
            bg = MaterialColors.getColor(context, R.attr.colorBackgroundMain, 0xFF141816);
            surface = MaterialColors.getColor(context, R.attr.colorSurfaceCard, 0xFF1F2622);
            accent = MaterialColors.getColor(context, R.attr.colorAccentPrimary, 0xFF7EAC91);
        }

        View btnListen = view.findViewById(R.id.btnListenBook);
        if (btnListen instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btnListen)
                    .setBackgroundTintList(ColorStateList.valueOf(accent));
        }

        View genre = view.findViewById(R.id.genre);
        if (genre != null && AppThemes.PALETTE_CUSTOM.equals(palette)) {
            GradientDrawable chip = new GradientDrawable();
            chip.setShape(GradientDrawable.RECTANGLE);
            chip.setCornerRadius(14 * context.getResources().getDisplayMetrics().density);
            chip.setColor(surface);
            chip.setStroke((int) (1.5f * context.getResources().getDisplayMetrics().density), accent);
            genre.setBackground(chip);
        }

        tintControls(view, accent);
    }

    /**
     * Binds and tints PreferenceFragmentCompat RecyclerView items as they attach.
     */
    public static void bindPreferences(@NonNull PreferenceFragmentCompat fragment) {
        Context context = fragment.getContext();
        if (context == null) return;
        String palette = AppThemes.currentPalette(context);
        int accent;
        if (AppThemes.PALETTE_CUSTOM.equals(palette)) {
            accent = AppThemes.getCustomColor(context, AppThemes.SLOT_ACCENT);
        } else {
            accent = MaterialColors.getColor(context, R.attr.colorAccentPrimary, 0xFF7EAC91);
        }

        try {
            RecyclerView list = fragment.getListView();
            if (list != null) {
                tintControls(list, accent);
                list.removeOnChildAttachStateChangeListener(PREF_ATTACH_LISTENER);
                list.addOnChildAttachStateChangeListener(PREF_ATTACH_LISTENER);
            }
        } catch (IllegalStateException ignored) {
            // list view not yet created
        }
    }

    /**
     * Recursively traverses a view hierarchy and applies accent color to SeekBars,
     * CompoundButtons (CheckBox/Switch/RadioButton), and ProgressBars.
     */
    public static void tintControls(@Nullable View view, int accent) {
        if (view == null) return;

        ColorStateList accentList = ColorStateList.valueOf(accent);

        if (view instanceof com.google.android.material.slider.Slider) {
            com.google.android.material.slider.Slider slider = (com.google.android.material.slider.Slider) view;
            slider.setThumbTintList(accentList);
            slider.setTrackActiveTintList(accentList);
            slider.setHaloTintList(ColorStateList.valueOf((accent & 0x00FFFFFF) | 0x33000000));
            slider.setTrackInactiveTintList(ColorStateList.valueOf((accent & 0x00FFFFFF) | 0x44000000));
        } else if (view instanceof SeekBar) {
            SeekBar sb = (SeekBar) view;
            sb.setProgressTintList(accentList);
            sb.setThumbTintList(accentList);
        } else if (view instanceof CompoundButton) {
            CompoundButton cb = (CompoundButton) view;
            cb.setButtonTintList(accentList);
            if (cb instanceof SwitchCompat) {
                SwitchCompat sc = (SwitchCompat) cb;
                sc.setThumbTintList(accentList);
                sc.setTrackTintList(accentList);
            }
        } else if (view instanceof ProgressBar) {
            ProgressBar pb = (ProgressBar) view;
            pb.setProgressTintList(accentList);
            pb.setIndeterminateTintList(accentList);
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            ColorStateList tint = iv.getImageTintList();
            if (matchesThemeAccent(view.getContext(), tint)) {
                iv.setImageTintList(accentList);
            }
        }

        // anything coloured by a theme accent attribute keeps the palette's built-in colour,
        // because theme attributes cannot hold a colour chosen at runtime; replace it here
        if (view instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton mb =
                    (com.google.android.material.button.MaterialButton) view;
            if (mb.getIcon() != null && matchesThemeAccent(mb.getContext(), mb.getIconTint())) {
                mb.setIconTint(accentList);
            }
            if (matchesThemeAccent(mb.getContext(), mb.getStrokeColor())) {
                mb.setStrokeColor(accentList);
            }
        }

        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            ColorStateList colors = tv.getTextColors();
            if (colors != null && colors.isStateful() && isPaletteAccent(tv.getContext(), colors.getDefaultColor())) {
                tv.setTextColor(accent);
            } else if (tv.getCurrentTextColor() != accent && isPaletteAccent(tv.getContext(), tv.getCurrentTextColor())) {
                tv.setTextColor(accent);
            }
        }

        if (view.getBackground() != null && view.getBackgroundTintList() != null
                && isPaletteAccent(view.getContext(), view.getBackgroundTintList().getDefaultColor())) {
            view.setBackgroundTintList(accentList);
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                tintControls(vg.getChildAt(i), accent);
            }
        }
    }

    private static boolean matchesThemeAccent(@NonNull Context context, @Nullable ColorStateList tint) {
        return tint != null && isPaletteAccent(context, tint.getDefaultColor());
    }

    /** Whether {@code color} is one of the theme's accent attributes (i.e. the palette's built-in accent). */
    private static boolean isPaletteAccent(@NonNull Context context, int color) {
        int[] accents = new int[] {
                MaterialColors.getColor(context, android.R.attr.colorAccent, 0),
                MaterialColors.getColor(context, R.attr.colorAccentPrimary, 0),
                MaterialColors.getColor(context, R.attr.hrefColor, 0)
        };
        for (int accent : accents) {
            if (accent != 0 && accent == color) {
                return true;
            }
        }
        return false;
    }

    /**
     * Paints a dialog with the active palette: its surface (Material would lighten it by the
     * dialog's elevation) and its accent-coloured buttons and labels.
     */
    public static void applyPaletteToDialog(@NonNull android.app.Dialog dialog) {
        android.view.Window window = dialog.getWindow();
        if (window == null) return;

        Context context = dialog.getContext();
        int surface = AppThemes.PALETTE_CUSTOM.equals(AppThemes.currentPalette(context))
                ? AppThemes.getCustomColor(context, AppThemes.SLOT_SURFACE)
                : MaterialColors.getColor(context, R.attr.colorSurfaceCard, 0xFF1F2622);
        int accent = AppThemes.PALETTE_CUSTOM.equals(AppThemes.currentPalette(context))
                ? AppThemes.getCustomColor(context, AppThemes.SLOT_ACCENT)
                : MaterialColors.getColor(context, R.attr.colorAccentPrimary, 0xFF7EAC91);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(16f * context.getResources().getDisplayMetrics().density);
        background.setColor(surface);

        window.setBackgroundDrawable(background);
        tintControls(window.getDecorView(), accent);
    }
}
