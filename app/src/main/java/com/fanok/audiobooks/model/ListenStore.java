package com.fanok.audiobooks.model;

import android.content.Context;
import androidx.annotation.NonNull;
import com.fanok.audiobooks.room.AppDatabase;
import com.fanok.audiobooks.room.FinishedBookEntity;
import com.fanok.audiobooks.room.ListenDayEntity;
import java.util.HashMap;
import java.util.Map;

/** The statistics in the database: the listening time per day and the books listened to the end. */
public final class ListenStore {

    private ListenStore() {
    }

    /** Adds {@code ms} (by local day) to the days stored, in one transaction. */
    public static void add(@NonNull Context context, @NonNull Map<Integer, Long> ms) {
        if (ms.isEmpty()) {
            return;
        }
        AppDatabase db = AppDatabase.getDatabase(context);
        db.runInTransaction(() -> {
            for (Map.Entry<Integer, Long> e : ms.entrySet()) {
                ListenDayEntity row = new ListenDayEntity();
                row.day = e.getKey();
                db.listenDayDao().insertIfAbsent(row);
                db.listenDayDao().add(e.getKey(), e.getValue());
            }
        });
    }

    /** Notes the book as listened to its end; a book finished before keeps its first date. */
    public static void finished(@NonNull Context context, @NonNull String bookUrl, long now) {
        FinishedBookEntity row = new FinishedBookEntity();
        row.urlBook = bookUrl;
        row.finishedAt = now;
        AppDatabase.getDatabase(context).finishedBookDao().insert(row);
    }

    /** Every stored day: the local day → milliseconds. */
    @NonNull
    public static Map<Integer, Long> days(@NonNull Context context) {
        Map<Integer, Long> days = new HashMap<>();
        for (ListenDayEntity row : AppDatabase.getDatabase(context).listenDayDao().getAll()) {
            days.put(row.day, row.listenedMs);
        }
        return days;
    }
}
