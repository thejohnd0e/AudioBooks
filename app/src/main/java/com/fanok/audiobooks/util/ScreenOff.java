package com.fanok.audiobooks.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.preference.PreferenceManager;

import com.fanok.audiobooks.service.MediaPlayerService;

/**
 * «Погасить экран» for a TV or a TV box: an app cannot switch a panel off, so the screen is covered by black and
 * the window brightness is lowered, while the book plays on. Any key of the remote brings the screen back (the media
 * keys do not: they only control the playing). It comes on by the button of the player, or by itself after
 * {@link #PREF_MINUTES} minutes without a key while a book plays.
 */
public final class ScreenOff {

    public static final String PREF_MINUTES = "tv_screen_off_minutes";

    private final Activity mActivity;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mAuto = this::onIdle;
    private View mCover;
    private boolean mSwallowUp;

    public ScreenOff(Activity activity) {
        mActivity = activity;
    }

    public boolean isShown() {
        return mCover != null;
    }

    /** The idle minutes chosen in the settings, 0 when it is off. */
    public int minutes() {
        try {
            String value = PreferenceManager.getDefaultSharedPreferences(mActivity).getString(PREF_MINUTES, "0");
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public void show() {
        if (mCover != null) {
            return;
        }
        FrameLayout content = mActivity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        View cover = new View(mActivity);
        cover.setBackgroundColor(0xFF000000);
        cover.setClickable(true);
        cover.setFocusable(false);
        content.addView(cover, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mCover = cover;
        WindowManager.LayoutParams params = mActivity.getWindow().getAttributes();
        params.screenBrightness = 0.01f;
        mActivity.getWindow().setAttributes(params);
        mHandler.removeCallbacks(mAuto);
    }

    public void hide() {
        if (mCover == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) mCover.getParent();
        if (parent != null) {
            parent.removeView(mCover);
        }
        mCover = null;
        WindowManager.LayoutParams params = mActivity.getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        mActivity.getWindow().setAttributes(params);
        arm();
    }

    /** Starts the idle count anew (every key, and the screen coming back, do it). */
    public void arm() {
        mHandler.removeCallbacks(mAuto);
        int minutes = minutes();
        if (minutes > 0 && mCover == null) {
            mHandler.postDelayed(mAuto, minutes * 60_000L);
        }
    }

    public void disarm() {
        mHandler.removeCallbacks(mAuto);
    }

    private void onIdle() {
        if (MediaPlayerService.isPlay()) {
            show();
        } else {
            arm(); // nothing plays: the screen stays, the count goes on
        }
    }

    /** True when the key was spent on bringing the screen back (or on the key's end after that). */
    public boolean consume(KeyEvent event) {
        if (mCover == null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                arm();
            }
            if (event.getAction() == KeyEvent.ACTION_UP && mSwallowUp) {
                mSwallowUp = false;
                return true;
            }
            return false;
        }
        if (isMediaKey(event.getKeyCode())) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mSwallowUp = true;
            hide();
        }
        return true;
    }

    private static boolean isMediaKey(int keyCode) {
        return (keyCode >= KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && keyCode <= KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE;
    }
}
