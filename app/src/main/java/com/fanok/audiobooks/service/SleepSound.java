package com.fanok.audiobooks.service;

import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.fanok.audiobooks.R;

/**
 * The sound of the sleep timer's warning, chosen on the timer screen: the old beep, a softer tone, or any sound the user picks. Kept in the preferences by id; an unknown or missing id is
 * the old beep, and a picked sound that is gone (no address kept) falls back to it too. Pure: the sounds are played by
 * {@link SleepSoundPlayer}.
 */
public final class SleepSound {

    public static final String PREF = "sleep_timer_warn_sound";
    /** The address of the picked sound, for {@link Kind#CUSTOM}. */
    public static final String PREF_URI = "sleep_timer_warn_sound_uri";

    /** The sounds on offer, in the order of the list. */
    public enum Kind {
        BEEP("beep", R.string.sleep_sound_beep),
        SOFT("soft", R.string.sleep_sound_soft),
        CUSTOM("custom", R.string.sleep_sound_custom);

        public final String id;
        @StringRes
        public final int title;

        Kind(String id, @StringRes int title) {
            this.id = id;
            this.title = title;
        }

        /** The kind of {@code id}; anything else is the old beep. */
        @NonNull
        public static Kind parse(@Nullable String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return BEEP;
        }
    }

    private SleepSound() {
    }

    /** The picked sound's address, or null when none is kept. */
    @Nullable
    public static Uri customUri(@NonNull SharedPreferences prefs) {
        String uri = prefs.getString(PREF_URI, null);
        return uri == null || uri.isEmpty() ? null : Uri.parse(uri);
    }

    /** The kind to play: the chosen one, the beep when a picked sound has no address. */
    @NonNull
    public static Kind effective(@NonNull SharedPreferences prefs) {
        Kind kind = Kind.parse(prefs.getString(PREF, null));
        return kind == Kind.CUSTOM && customUri(prefs) == null ? Kind.BEEP : kind;
    }
}
