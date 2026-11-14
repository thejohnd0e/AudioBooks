package com.fanok.audiobooks.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.fanok.audiobooks.R;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * «Что нового» after an update: a short list of the changes in a dialog, once per new build. The text is bundled with
 * the build ({@code assets/whats_new.txt}: a heading {@code [build] version}, then one change per line). A new install
 * shows nothing; an update from a build older than this screen shows what is newer than {@link #BASELINE}.
 */
public final class WhatsNew {

    private static final String ASSET = "whats_new.txt";
    private static final String SEEN = "whats_new_seen_build";
    /** The last build that shipped before this screen existed: an update from older builds shows what came after it. */
    static final int BASELINE = 143;
    static final int MAXIMUM_BUILDS = 5;
    /** A package this young was installed just now, not updated. */
    private static final long FRESH_MS = 60_000L;

    private WhatsNew() {
    }

    /**
     * The notes of every build in (seen, current], newest first, one heading per build and a bullet per change; empty
     * when there are none.
     */
    static String notes(@NonNull String source, int seen, int current) {
        StringBuilder result = new StringBuilder();
        int blocks = 0;
        int build = -1;
        String version = "";
        StringBuilder body = new StringBuilder();
        for (String raw : (source + "\n[0]").split("\r?\n")) {
            String line = raw.trim();
            if (line.startsWith("[") && line.contains("]")) {
                if (build > seen && build <= current && body.length() > 0 && blocks < MAXIMUM_BUILDS) {
                    if (blocks++ > 0) {
                        result.append("\n\n");
                    }
                    result.append(version.isEmpty() ? "Сборка " + build : "Версия " + version).append(body);
                }
                try {
                    build = Integer.parseInt(line.substring(1, line.indexOf(']')).trim());
                } catch (NumberFormatException e) {
                    build = -1;
                }
                version = line.substring(line.indexOf(']') + 1).trim();
                body.setLength(0);
            } else if (build >= 0 && !line.isEmpty()) {
                body.append("\n• ").append(line.startsWith("• ") ? line.substring(2) : line);
            }
        }
        return result.toString();
    }

    /**
     * The build the user is taken to have seen: what was stored; for a store that is empty, the baseline on an update
     * (it shows what is newer) and the current build on a fresh install (it shows nothing).
     */
    static int seenBuild(int stored, int current, boolean freshInstall) {
        if (stored >= 0) {
            return stored;
        }
        return freshInstall ? current : BASELINE;
    }

    /**
     * The notes to show now, or null; marks the current build as seen at once, so they come once. Never throws: a
     * list of changes must not stop the app from starting.
     */
    @Nullable
    public static String pending(@NonNull Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int current = (int) androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info);
            int stored = prefs.getInt(SEEN, -1);
            int seen = seenBuild(stored, current, info.lastUpdateTime - info.firstInstallTime <= FRESH_MS);
            if (stored != current) {
                prefs.edit().putInt(SEEN, current).apply();
            }
            if (seen >= current) {
                return null;
            }
            String text = notes(read(context), seen, current);
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            return null;
        }
    }

    /** Shows the notes in the app's dialog; {@code done} runs when it closes. */
    public static void show(@NonNull Activity activity, @NonNull String notes, @NonNull Runnable done) {
        new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(activity)
                .setTitle(R.string.whats_new)
                .setMessage(notes)
                .setPositiveButton(R.string.ok, null)
                .setOnDismissListener(dialog -> done.run())
                .show();
    }

    private static String read(@NonNull Context context) throws java.io.IOException {
        try (InputStream input = context.getAssets().open(ASSET)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int count; (count = input.read(buffer)) > 0; ) {
                bytes.write(buffer, 0, count);
            }
            return bytes.toString("UTF-8");
        }
    }
}
