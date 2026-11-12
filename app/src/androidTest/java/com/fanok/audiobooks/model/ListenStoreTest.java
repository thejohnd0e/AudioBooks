package com.fanok.audiobooks.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.fanok.audiobooks.room.AppDatabase;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** The statistics in the real database: days add up across writes, a finished book is one row with its first date. */
@RunWith(AndroidJUnit4.class)
public class ListenStoreTest {

    private static final int DAY = 1_999_001; // far from any real day
    private static final int NEXT = DAY + 1;
    private static final String BOOK = "https://example.com/listen-store-test/book";

    private Context context;
    private AppDatabase db;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = AppDatabase.getDatabase(context);
        clean();
    }

    @After
    public void clean() {
        if (db == null) {
            return;
        }
        db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM listen_day WHERE day >= " + DAY);
        db.getOpenHelper().getWritableDatabase().execSQL(
                "DELETE FROM finished_book WHERE url_book = '" + BOOK + "'");
    }

    private static Map<Integer, Long> ms(int day, long value) {
        Map<Integer, Long> map = new HashMap<>();
        map.put(day, value);
        return map;
    }

    @Test
    public void daysAddUpAcrossWrites() {
        ListenStore.add(context, ms(DAY, 30_000));
        ListenStore.add(context, ms(DAY, 45_500));
        Map<Integer, Long> both = new HashMap<>();
        both.put(DAY, 1_000L);
        both.put(NEXT, 7_000L);
        ListenStore.add(context, both);
        Map<Integer, Long> days = ListenStore.days(context);
        assertEquals(Long.valueOf(76_500), days.get(DAY));
        assertEquals(Long.valueOf(7_000), days.get(NEXT));
    }

    @Test
    public void nothingToAddChangesNothing() {
        ListenStore.add(context, new HashMap<>());
        assertNull(ListenStore.days(context).get(DAY));
    }

    @Test
    public void aBookFinishedTwiceIsOneRowWithItsFirstDate() {
        int before = db.finishedBookDao().count();
        ListenStore.finished(context, BOOK, 1_000);
        ListenStore.finished(context, BOOK, 9_000);
        assertEquals(before + 1, db.finishedBookDao().count());
        long first = -1;
        for (com.fanok.audiobooks.room.FinishedBookEntity row : db.finishedBookDao().getAll()) {
            if (BOOK.equals(row.urlBook)) {
                first = row.finishedAt;
            }
        }
        assertEquals(1_000, first);
    }
}
