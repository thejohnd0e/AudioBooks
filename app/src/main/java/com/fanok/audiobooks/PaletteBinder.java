package com.fanok.audiobooks;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayout;

/**
 * Binds active palette colors (Background, Surface, Accent) directly to window, toolbar,
 * and key controls to eliminate all background seams and support dynamic custom colors.
 */
public final class PaletteBinder {

    private PaletteBinder() {
    }

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
    }
}
