package com.fanok.audiobooks;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;

/**
 * Paints the app with the active palette: window, system bars, toolbar, cards, dialogs and every
 * control or label the theme colours by attribute.
 *
 * <p>The three colours of a custom palette live in the preferences. An XML theme is a compiled
 * resource and cannot hold them, so the theme overlay keeps placeholder colours of its own and this
 * class replaces them at runtime, everywhere and every time a screen or a list row is drawn. That is
 * why a custom palette wins over any preset: the preset's colours never reach the screen.
 */
public final class PaletteBinder {

    private PaletteBinder() {
    }

    /** The palette's background colour; a custom palette always wins over the theme's. */
    public static int background(@NonNull Context context) {
        return AppThemes.PALETTE_CUSTOM.equals(AppThemes.currentPalette(context))
                ? AppThemes.getCustomColor(context, AppThemes.SLOT_BG)
                : MaterialColors.getColor(context, R.attr.colorBackgroundMain, 0xFF101412);
    }

    /** The palette's surface colour: cards, list rows, dialogs. */
    public static int surface(@NonNull Context context) {
        return AppThemes.PALETTE_CUSTOM.equals(AppThemes.currentPalette(context))
                ? AppThemes.getCustomColor(context, AppThemes.SLOT_SURFACE)
                : MaterialColors.getColor(context, R.attr.colorSurfaceCard, 0xFF18201B);
    }

    /** The palette's accent colour. */
    public static int accent(@NonNull Context context) {
        return AppThemes.PALETTE_CUSTOM.equals(AppThemes.currentPalette(context))
                ? AppThemes.getCustomColor(context, AppThemes.SLOT_ACCENT)
                : MaterialColors.getColor(context, R.attr.colorAccentPrimary, 0xFF7EAC91);
    }

    private static final RecyclerView.OnChildAttachStateChangeListener PREF_ATTACH_LISTENER =
            new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(@NonNull View view) {
                    Context context = view.getContext();
                    recolorSurfaces(view, background(context), surface(context));
                    tintControls(view, accent(context));
                }

                @Override
                public void onChildViewDetachedFromWindow(@NonNull View view) {}
            };

    private static final androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks FRAGMENT_PALETTE_CALLBACKS =
            new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull androidx.fragment.app.FragmentManager fm,
                        @NonNull androidx.fragment.app.Fragment f, @NonNull View v,
                        @Nullable android.os.Bundle savedInstanceState) {
                    Context ctx = f.getContext();
                    if (ctx == null) return;
                    bindView(v, ctx);
                    watchLists(v);
                    if (f instanceof androidx.fragment.app.DialogFragment) {
                        android.app.Dialog d = ((androidx.fragment.app.DialogFragment) f).getDialog();
                        if (d != null) {
                            applyPaletteToDialog(d);
                        }
                    }
                }
            };

    /** Applies the current palette to the activity window, toolbar, cards and controls. */
    public static void bind(@NonNull Activity activity) {
        int bg = background(activity);
        int surface = surface(activity);
        int accent = accent(activity);

        if (activity instanceof androidx.fragment.app.FragmentActivity) {
            androidx.fragment.app.FragmentManager fm =
                    ((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager();
            fm.unregisterFragmentLifecycleCallbacks(FRAGMENT_PALETTE_CALLBACKS);
            fm.registerFragmentLifecycleCallbacks(FRAGMENT_PALETTE_CALLBACKS, true);
        }

        // Seamless window and system bars
        activity.getWindow().setBackgroundDrawable(new ColorDrawable(bg));
        activity.getWindow().setStatusBarColor(bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.getWindow().setNavigationBarColor(bg);
        }

        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(bg);
            AppThemes.applyPalette(activity, toolbar.getContext().getTheme());
            bindToolbarOverflowPopup(toolbar, activity);
        }

        com.google.android.material.navigation.NavigationView navView = activity.findViewById(R.id.nav_view);
        if (navView != null) {
            navView.setBackgroundColor(surface);
            for (int i = 0; i < navView.getHeaderCount(); i++) {
                paint(navView.getHeaderView(i), bg, surface, accent);
            }
        }

        TabLayout tabs = activity.findViewById(R.id.tabs);
        if (tabs != null) {
            tabs.setBackgroundColor(bg);
            tabs.setSelectedTabIndicatorColor(accent);
            tabs.setTabTextColors(0x99FFFFFF, accent);
        }

        ImageButton playTop = activity.findViewById(R.id.playTop);
        if (playTop != null) {
            ViewCompat.setBackgroundTintList(playTop, ColorStateList.valueOf(accent));
        }

        ImageButton playBottom = activity.findViewById(R.id.playBottom);
        if (playBottom != null) {
            ViewCompat.setBackgroundTintList(playBottom, ColorStateList.valueOf(accent));
        }

        ProgressBar playerProgress = activity.findViewById(R.id.progressBar);
        if (playerProgress != null) {
            playerProgress.setProgressTintList(ColorStateList.valueOf(accent));
        }

        final View decor = activity.getWindow().getDecorView();
        paint(decor, bg, surface, accent);
        // fragments and list rows appear right after this call: one pass once they are laid out
        decor.post(() -> {
            paint(decor, background(activity), surface(activity), accent(activity));
            watchLists(decor);
        });
    }

    /** Repaints a list row as it comes into view; a list has no rows when its screen is built. */
    private static void bindToolbarOverflowPopup(@NonNull Toolbar toolbar, @NonNull Activity activity) {
        toolbar.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            try {
                java.lang.reflect.Field menuViewField = Toolbar.class.getDeclaredField("mMenuView");
                menuViewField.setAccessible(true);
                Object menuView = menuViewField.get(toolbar);
                if (menuView == null) return;
                java.lang.reflect.Field presenterField = menuView.getClass().getDeclaredField("mPresenter");
                presenterField.setAccessible(true);
                Object presenter = presenterField.get(menuView);
                if (presenter == null) return;
                java.lang.reflect.Field popupField = presenter.getClass().getDeclaredField("mOverflowPopup");
                popupField.setAccessible(true);
                Object overflowPopup = popupField.get(presenter);
                if (overflowPopup == null) return;
                java.lang.reflect.Method getPopup = overflowPopup.getClass().getMethod("getPopup");
                Object menuPopup = getPopup.invoke(overflowPopup);
                if (menuPopup == null) return;
                java.lang.reflect.Method getListView = menuPopup.getClass().getMethod("getListView");
                View listView = (View) getListView.invoke(menuPopup);
                if (listView == null) return;
                int surface = surface(activity);
                float density = activity.getResources().getDisplayMetrics().density;
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setCornerRadius(12f * density);
                bg.setColor(surface);
                bg.setStroke(Math.max(1, Math.round(density)), 0x26FFFFFF);
                View target = listView.getParent() instanceof View ? (View) listView.getParent() : listView;
                target.setBackground(bg);
                listView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            } catch (Throwable ignored) {
            }
        });
    }

    private static void watchLists(@NonNull View view) {
        if (view instanceof RecyclerView) {
            RecyclerView list = (RecyclerView) view;
            list.removeOnChildAttachStateChangeListener(PREF_ATTACH_LISTENER);
            list.addOnChildAttachStateChangeListener(PREF_ATTACH_LISTENER);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                watchLists(group.getChildAt(i));
            }
        }
    }

    /** Applies the current palette to a fragment's view hierarchy. */
    public static void bindView(@NonNull View view, @NonNull Context context) {
        int accent = accent(context);

        View btnListen = view.findViewById(R.id.btnListenBook);
        if (btnListen instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) btnListen;
            int bg = background(context);
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(bg);
            button.setIconTint(ColorStateList.valueOf(bg));
        }

        View genre = view.findViewById(R.id.genre);
        if (genre != null && AppThemes.PALETTE_CUSTOM.equals(AppThemes.currentPalette(context))) {
            GradientDrawable chip = new GradientDrawable();
            chip.setShape(GradientDrawable.RECTANGLE);
            chip.setCornerRadius(14 * context.getResources().getDisplayMetrics().density);
            chip.setColor(surface(context));
            chip.setStroke((int) (1.5f * context.getResources().getDisplayMetrics().density), accent);
            genre.setBackground(chip);
        }

        paint(view, background(context), surface(context), accent);
    }

    /** Binds and paints PreferenceFragmentCompat RecyclerView items as they attach. */
    public static void bindPreferences(@NonNull PreferenceFragmentCompat fragment) {
        Context context = fragment.getContext();
        if (context == null) return;

        try {
            RecyclerView list = fragment.getListView();
            if (list != null) {
                paint(list, background(context), surface(context), accent(context));
                list.removeOnChildAttachStateChangeListener(PREF_ATTACH_LISTENER);
                list.addOnChildAttachStateChangeListener(PREF_ATTACH_LISTENER);
            }
        } catch (IllegalStateException ignored) {
            // list view not yet created
        }
    }

    private static void paint(@Nullable View view, int background, int surface, int accent) {
        recolorSurfaces(view, background, surface);
        tintControls(view, accent);
    }

    /**
     * Repaints backgrounds painted with the theme's background or surface colour.
     *
     * <p>Cards, rows and dialog surfaces take their colour from theme attributes, which a palette
     * chosen at runtime cannot change; this pass brings them the palette's own colours, so a black
     * custom palette turns every surface black instead of leaving the theme's grey.
     */
    public static void recolorSurfaces(@Nullable View view, int background, int surface) {
        if (view == null) return;
        Context context = view.getContext();
        int themeBackground = MaterialColors.getColor(context, R.attr.colorBackgroundMain, 0);
        int themeSurface = MaterialColors.getColor(context, R.attr.colorSurfaceCard, 0);
        int materialSurface = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorSurface, 0);
        // the placeholders the custom palette's theme overlay carries: nothing may keep them
        int placeholderBackground = ContextCompat.getColor(context, R.color.palette_custom_background);
        int placeholderSurface = ContextCompat.getColor(context, R.color.palette_custom_surface);
        if (background == themeBackground && surface == themeSurface) {
            return; // a built-in palette: the theme already paints it
        }

        Drawable drawable = view.getBackground();
        int fill = fillOf(drawable);

        if ((fill == themeBackground || fill == placeholderBackground) && background != fill) {
            paintColor(view, drawable, background);
        } else if ((fill == themeSurface || fill == placeholderSurface
                || (materialSurface != 0 && fill == materialSurface)) && surface != fill) {
            paintColor(view, drawable, surface);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                recolorSurfaces(group.getChildAt(i), background, surface);
            }
        }
    }

    /** The solid colour of a drawable, or -1 when it fills with something else. */
    private static int fillOf(@Nullable Drawable drawable) {
        if (drawable instanceof GradientDrawable) {
            ColorStateList colors = ((GradientDrawable) drawable).getColor();
            return colors == null ? -1 : colors.getDefaultColor();
        }
        if (drawable instanceof ColorDrawable) {
            return ((ColorDrawable) drawable).getColor();
        }
        if (drawable instanceof MaterialShapeDrawable) {
            ColorStateList colors = ((MaterialShapeDrawable) drawable).getFillColor();
            return colors == null ? -1 : colors.getDefaultColor();
        }
        return -1;
    }

    /** Paints {@code color} on a copy of {@code drawable}, keeping its shape, corners and stroke. */
    private static void paintColor(@NonNull View view, @Nullable Drawable drawable, int color) {
        if (drawable instanceof GradientDrawable || drawable instanceof MaterialShapeDrawable) {
            Drawable copy = drawable.getConstantState() != null
                    ? drawable.getConstantState().newDrawable().mutate()
                    : drawable.mutate();
            if (copy instanceof GradientDrawable) {
                ((GradientDrawable) copy).setColor(color);
            } else if (copy instanceof MaterialShapeDrawable) {
                ((MaterialShapeDrawable) copy).setFillColor(ColorStateList.valueOf(color));
            }
            view.setBackground(copy);
            return;
        }
        // a flat colour: corners and strokes are not part of it
        view.setBackgroundColor(color);
    }

    /**
     * Tints every control and label the theme colours by an accent attribute: sliders, checkboxes,
     * switches, progress bars, buttons with icons, and text or icon tints resolved from the theme.
     */
    public static void tintControls(@Nullable View view, int accent) {
        if (view == null) return;

        ColorStateList accentList = ColorStateList.valueOf(accent);

        if (view instanceof Slider) {
            Slider slider = (Slider) view;
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
            if (matchesThemeAccent(iv.getContext(), iv.getImageTintList())) {
                iv.setImageTintList(accentList);
            }
        }

        // anything coloured by a theme accent attribute keeps the palette's built-in colour,
        // because theme attributes cannot hold a colour chosen at runtime; replace it here
        if (view instanceof com.google.android.material.chip.Chip) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) view;
            int surfaceColor = surface(chip.getContext());
            int[][] states = new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}};
            chip.setChipBackgroundColor(new ColorStateList(states,
                    new int[] {(accent & 0x00FFFFFF) | 0x33000000, surfaceColor}));
            chip.setChipStrokeColor(new ColorStateList(states,
                    new int[] {accent, 0x44FFFFFF}));
            chip.setChipStrokeWidth(chip.getResources().getDisplayMetrics().density);
            chip.setTextColor(MaterialColors.getColor(chip, R.attr.colorPrimaryText, 0xFFECEDE8));
        } else if (view instanceof MaterialButton) {
            MaterialButton mb = (MaterialButton) view;
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
        for (int value : accents) {
            if (value != 0 && value == color) {
                return true;
            }
        }
        return false;
    }

    /**
     * Paints a dialog with the palette: its surface (Material would lighten it by the dialog's
     * elevation) and its accent-coloured buttons and labels.
     */
    public static void applyPaletteToDialog(@NonNull android.app.Dialog dialog) {
        android.view.Window window = dialog.getWindow();
        if (window == null) return;

        Context context = dialog.getContext();
        int surface = surface(context);
        int accent = accent(context);
        float density = context.getResources().getDisplayMetrics().density;

        GradientDrawable card = new GradientDrawable();
        card.setShape(GradientDrawable.RECTANGLE);
        card.setCornerRadius(16f * density);
        card.setColor(surface);
        card.setStroke(Math.max(1, Math.round(density)), 0x26FFFFFF);

        window.setBackgroundDrawable(card.getConstantState().newDrawable());

        View decor = window.getDecorView();
        // the dialog's own content view carries a Material shape of the theme's surface colour
        View content = window.findViewById(android.R.id.content);
        if (content != null && content != decor) {
            content.setBackground(card.getConstantState().newDrawable());
        }
        paint(decor, background(context), surface, accent);
        styleAlertDialogWidgets(dialog, accent);

        // Material applies its own card while the dialog starts, so paint once more afterwards
        decor.post(() -> {
            window.setBackgroundDrawable(card.getConstantState().newDrawable());
            decor.setBackground(card.getConstantState().newDrawable());
            if (content != null && content != decor) {
                content.setBackground(card.getConstantState().newDrawable());
            }
            paint(decor, background(context), surface, accent(context));
            styleAlertDialogWidgets(dialog, accent(context));
        });
    }

    private static void styleAlertDialogWidgets(@NonNull android.app.Dialog dialog, int accent) {
        if (dialog instanceof androidx.appcompat.app.AlertDialog) {
            androidx.appcompat.app.AlertDialog alert = (androidx.appcompat.app.AlertDialog) dialog;
            for (int which : new int[] {
                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                    androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE,
                    androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL}) {
                android.widget.Button btn = alert.getButton(which);
                if (btn != null) {
                    btn.setTextColor(accent);
                }
            }
            com.fanok.audiobooks.util.TvFocus.attach(alert.getWindow() == null ? null : alert.getWindow().getDecorView());
            focusFirstButtonOnTv(alert);
        }
        View icon = dialog.findViewById(android.R.id.icon);
        if (icon instanceof ImageView) {
            ((ImageView) icon).setImageTintList(ColorStateList.valueOf(accent));
        }
    }

    /** On a TV a dialog opens with the focus on a button, so OK answers it at once; nothing off a TV. */
    private static void focusFirstButtonOnTv(@NonNull androidx.appcompat.app.AlertDialog alert) {
        if (!com.fanok.audiobooks.util.Tv.is(alert.getContext())) {
            return;
        }
        if (com.fanok.audiobooks.util.TvFocus.focusList(alert)) {
            return; // a list of choices: the remote starts on it
        }
        if (alert.getCurrentFocus() instanceof android.widget.Button) { // the message's scroll area has it by default
            return;
        }
        for (int which : new int[] {
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE,
                androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL}) {
            android.widget.Button btn = alert.getButton(which);
            if (btn != null && btn.getVisibility() == View.VISIBLE) {
                btn.requestFocus();
                return;
            }
        }
    }

    public static void bindBottomSheet(@NonNull View content, @NonNull android.app.Dialog dialog) {
        Context context = content.getContext();
        int bg = background(context);
        int surface = surface(context);
        int accent = accent(context);
        content.setBackgroundColor(surface);
        paint(content, bg, surface, accent);
        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundColor(surface);
            }
            paint(content, background(context), surface(context), accent(context));
            if (sheet != null) {
                forRemote(sheet, content);
            }
        });
    }

    /**
     * On a TV a bottom sheet opens whole (a remote cannot drag it up) and its first control has the focus; nothing off
     * a TV. For a {@link com.google.android.material.bottomsheet.BottomSheetDialog} built without bindBottomSheet.
     */
    public static void forRemote(@NonNull com.google.android.material.bottomsheet.BottomSheetDialog dialog,
            @NonNull View content) {
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            forRemote(sheet, content);
        }
    }

    private static void forRemote(@NonNull View sheet, @NonNull View content) {
        if (!com.fanok.audiobooks.util.Tv.is(content.getContext())) {
            return;
        }
        com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        content.postDelayed(() -> {
            if (content.findFocus() == null) {
                // a request for the focus does not take in a window that has just come: the remote's own key does
                View root = content.getRootView();
                root.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN));
                root.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN));
            }
        }, 150);
    }
}
