package com.fanok.audiobooks.util;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import com.fanok.audiobooks.PaletteBinder;
import com.fanok.audiobooks.R;

/**
 * The remote's cursor: on a TV the view in focus gets a ring in the palette's accent, on every screen of the app, so one
 * can always see what OK will press. Drawn in the view's overlay (nothing in the layouts changes, nothing moves) and
 * taken off when the focus leaves. Off a TV nothing is installed.
 */
public final class TvFocus {

    private static final int RING_DP = 3;
    private static final int RADIUS_DP = 8;

    private static final ViewTreeObserver.OnGlobalFocusChangeListener LISTENER = (old, now) -> {
        remove(old);
        add(now);
    };

    private TvFocus() {
    }

    /** Call once, at process start. */
    public static void install(@NonNull Application app) {
        if (!Tv.is(app)) {
            return;
        }
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                attach(activity.getWindow().getDecorView());
                if (activity instanceof FragmentActivity) {
                    // every dialog fragment (the settings' lists, the sheets): the ring and a first focus
                    ((FragmentActivity) activity).getSupportFragmentManager()
                            .registerFragmentLifecycleCallbacks(DIALOGS, true);
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    private static final FragmentManager.FragmentLifecycleCallbacks DIALOGS =
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
                    if (fragment instanceof DialogFragment && ((DialogFragment) fragment).getDialog() != null) {
                        prepareDialog(((DialogFragment) fragment).getDialog());
                    }
                }
            };

    /** A dialog on a TV: the ring in its window and the remote on its list (the chosen row) or a button. */
    private static void prepareDialog(@NonNull Dialog dialog) {
        if (dialog.getWindow() == null) {
            return;
        }
        View decor = dialog.getWindow().getDecorView();
        attach(decor);
        decor.post(() -> {
            if (dialog.getCurrentFocus() instanceof Button) {
                return;
            }
            if (dialog instanceof AlertDialog) {
                AlertDialog alert = (AlertDialog) dialog;
                ListView list = alert.getListView();
                if (list != null && list.getCount() > 0) {
                    int checked = list.getCheckedItemPosition();
                    list.setSelector(rowHighlight(list));
                    list.setDrawSelectorOnTop(true); // the rows have a background of their own: over it
                    list.requestFocus();
                    list.setSelection(checked >= 0 ? checked : 0);
                    return;
                }
                for (int which : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE,
                        AlertDialog.BUTTON_NEUTRAL}) {
                    Button button = alert.getButton(which);
                    if (button != null && button.getVisibility() == View.VISIBLE) {
                        button.requestFocus();
                        return;
                    }
                }
            }
        });
    }

    /** The row a remote stands on in a dialog's list: a ring and a light fill, in the list's own selector. */
    private static GradientDrawable rowHighlight(@NonNull View list) {
        float density = list.getResources().getDisplayMetrics().density;
        boolean darkTheme = androidx.core.graphics.ColorUtils.calculateLuminance(
                PaletteBinder.background(list.getContext())) < 0.5;
        GradientDrawable row = new GradientDrawable();
        row.setShape(GradientDrawable.RECTANGLE);
        row.setCornerRadius(RADIUS_DP * density);
        row.setColor(darkTheme ? 0x22FFFFFF : 0x22000000);
        row.setStroke(Math.round(RING_DP * density), darkTheme ? 0xFFFFFFFF : 0xFF000000);
        return row;
    }

    /**
     * The ring for the window whose root is {@code root} (a dialog's: an activity's is attached by {@link #install});
     * nothing off a TV, and attaching the same window twice does no harm.
     */
    public static void attach(@Nullable View root) {
        if (root == null || !Tv.is(root.getContext())) {
            return;
        }
        ViewTreeObserver observer = root.getViewTreeObserver();
        observer.removeOnGlobalFocusChangeListener(LISTENER);
        observer.addOnGlobalFocusChangeListener(LISTENER);
        // the first focus may be taken before the listener is there: the focused view gets its ring now
        add(root.findFocus());
    }

    /** Containers and text fields show their own cursor: the ring is for the things OK presses. */
    private static boolean wantsRing(@NonNull View view) {
        if (view instanceof EditText || view instanceof RecyclerView || view instanceof AbsListView
                || view instanceof ScrollView || view instanceof NestedScrollView
                || view instanceof androidx.viewpager2.widget.ViewPager2 || view.getWidth() <= 0
                || view.getHeight() <= 0) {
            return false;
        }
        // a view that fills most of the window is a screen, not a button: no ring around it
        View root = view.getRootView();
        return root == null || (long) view.getWidth() * view.getHeight() < 0.5 * root.getWidth() * root.getHeight();
    }

    private static void add(@Nullable View view) {
        if (view == null || !wantsRing(view)) {
            return;
        }
        remove(view); // the same view can be told twice: one ring only
        float density = view.getResources().getDisplayMetrics().density;
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.RECTANGLE);
        ring.setCornerRadius(RADIUS_DP * density);
        // white on a dark theme, black on a light one: seen on the screen and on a button filled with the accent
        boolean darkTheme = androidx.core.graphics.ColorUtils.calculateLuminance(
                PaletteBinder.background(view.getContext())) < 0.5;
        ring.setStroke(Math.round(RING_DP * density), darkTheme ? 0xFFFFFFFF : 0xFF000000);
        int top = 0;
        int bottom = view.getHeight();
        if (view instanceof com.google.android.material.button.MaterialButton) {
            // a Material button is drawn inside its own insets: the ring follows what is seen
            top = ((com.google.android.material.button.MaterialButton) view).getInsetTop();
            bottom -= ((com.google.android.material.button.MaterialButton) view).getInsetBottom();
        }
        ring.setBounds(0, top, view.getWidth(), bottom);
        view.getOverlay().add(ring);
        view.setTag(R.id.tv_focus_ring, ring);
    }

    private static void remove(@Nullable View view) {
        if (view == null) {
            return;
        }
        Object ring = view.getTag(R.id.tv_focus_ring);
        if (ring instanceof GradientDrawable) {
            view.getOverlay().remove((GradientDrawable) ring);
            view.setTag(R.id.tv_focus_ring, null);
        }
    }
}
