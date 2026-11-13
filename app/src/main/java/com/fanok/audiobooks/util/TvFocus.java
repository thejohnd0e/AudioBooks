package com.fanok.audiobooks.util;

import android.app.Activity;
import android.app.Application;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
