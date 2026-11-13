package com.fanok.audiobooks.util;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;

/**
 * Whether the app runs on a TV (Android TV, a set-top box): the one answer for the whole app, asked once. A phone or a
 * tablet is never a TV, whatever its screen size; the remote's behaviour (the focus ring, the first focus of a dialog)
 * is only for the TV.
 */
public final class Tv {

    private static Boolean sTv;

    private Tv() {
    }

    /** True on a TV. The first answer is kept: the device does not change its mode while the app runs. */
    public static boolean is(@NonNull Context context) {
        if (sTv == null) {
            UiModeManager modes = (UiModeManager) context.getApplicationContext()
                    .getSystemService(Context.UI_MODE_SERVICE);
            sTv = modes != null && modes.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        }
        return sTv;
    }
}
