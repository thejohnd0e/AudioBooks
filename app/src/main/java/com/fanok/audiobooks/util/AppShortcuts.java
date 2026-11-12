package com.fanok.audiobooks.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.PreferenceManager;
import com.fanok.audiobooks.Consts;
import com.fanok.audiobooks.R;
import com.fanok.audiobooks.activity.MainActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * The shortcuts of a long press on the app's icon: the Continue screen, the history and the downloaded books. They are
 * made here, not in a shortcuts.xml, since the package of the app differs between its builds and a shortcut's intent
 * names it. Published once per version of this list; the launcher keeps them.
 */
public final class AppShortcuts {

    /** Raised when the list below changes: the shortcuts are then published again. */
    static final int VERSION = 1;
    private static final String PREF = "app_shortcuts_version";
    private static final String TAG = "AppShortcuts";

    private AppShortcuts() {
    }

    /** Publishes the shortcuts if this version of them is not published yet; never fails. */
    public static void install(@NonNull Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (prefs.getInt(PREF, 0) >= VERSION) {
            return;
        }
        try {
            List<ShortcutInfoCompat> shortcuts = new ArrayList<>();
            shortcuts.add(shortcut(app, "continue", R.string.menu_home, R.string.home_continue, Consts.FRAGMENT_HOME));
            shortcuts.add(shortcut(app, "history", R.string.menu_history, R.string.menu_history,
                    Consts.FRAGMENT_HISTORY));
            shortcuts.add(shortcut(app, "saved", R.string.menu_saved, R.string.menu_saved, Consts.FRAGMENT_SAVED));
            if (ShortcutManagerCompat.setDynamicShortcuts(app, shortcuts)) {
                prefs.edit().putInt(PREF, VERSION).apply();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "cannot publish the shortcuts", e);
        }
    }

    @NonNull
    private static ShortcutInfoCompat shortcut(Context app, String id, int shortLabel, int longLabel, int section) {
        Intent intent = MainActivity.sectionIntent(app, section);
        return new ShortcutInfoCompat.Builder(app, id)
                .setShortLabel(app.getString(shortLabel))
                .setLongLabel(app.getString(longLabel))
                .setIcon(IconCompat.createWithResource(app, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build();
    }
}
