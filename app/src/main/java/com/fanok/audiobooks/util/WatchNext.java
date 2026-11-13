package com.fanok.audiobooks.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.fanok.audiobooks.activity.MainActivity;
import com.fanok.audiobooks.model.ContinueListening;
import com.fanok.audiobooks.model.ContinueModel;
import com.fanok.audiobooks.pojo.BookPOJO;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The «Продолжить» row of Android TV's home screen: the books being listened to, each with its progress, taken from
 * the same list as the app's own «Продолжить». Only on a TV (API 26+); a failure only leaves the row as it was.
 */
public final class WatchNext {

    private static final String TAG = "WatchNext";
    public static final String EXTRA_BOOK_URL = "watchNextBookUrl";
    private static final int LIMIT = 5;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    private WatchNext() {
    }

    /** Rewrites the row from the current state of the listening; call when the app goes to the background. */
    public static void sync(@NonNull Context context) {
        final Context app = context.getApplicationContext();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !Tv.is(app)) {
            return;
        }
        WORKER.execute(() -> {
            try {
                List<ContinueListening.Item> items = ContinueModel.load(app, LIMIT);
                Log.i(TAG, "sync: " + items.size() + " books");
                write(app, items);
            } catch (RuntimeException e) {
                Log.w(TAG, "the row was not written", e);
            }
        });
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private static void write(Context app, List<ContinueListening.Item> items) {
        ContentResolver resolver = app.getContentResolver();
        resolver.delete(TvContract.WatchNextPrograms.CONTENT_URI, null, null); // ours only: the provider scopes it
        long now = System.currentTimeMillis();
        for (ContinueListening.Item item : items) {
            BookPOJO book = item.book;
            Intent intent = new Intent(app, MainActivity.class)
                    .putExtra(EXTRA_BOOK_URL, book.getUrl());
            ContentValues row = new ContentValues();
            row.put(TvContract.WatchNextPrograms.COLUMN_TYPE, TvContract.WatchNextPrograms.TYPE_CLIP);
            row.put(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
                    TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE);
            row.put(TvContract.WatchNextPrograms.COLUMN_TITLE, book.getName());
            row.put(TvContract.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION, item.chapter);
            if (book.getPhoto() != null && !book.getPhoto().isEmpty()) {
                row.put(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, Uri.parse(book.getPhoto()).toString());
                row.put(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
                        TvContract.WatchNextPrograms.ASPECT_RATIO_2_3);
            }
            row.put(TvContract.WatchNextPrograms.COLUMN_INTENT_URI, intent.toUri(Intent.URI_INTENT_SCHEME));
            row.put(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, now--);
            if (item.percent >= 0) {
                row.put(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS, 100_000);
                row.put(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, item.percent * 1000);
            }
            Uri added = resolver.insert(TvContract.WatchNextPrograms.CONTENT_URI, row);
            Log.i(TAG, "row for " + book.getName() + ": " + added);
        }
    }
}
