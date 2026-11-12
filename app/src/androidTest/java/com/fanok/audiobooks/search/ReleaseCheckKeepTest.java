package com.fanok.audiobooks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.fanok.audiobooks.model.BooksDBModel;
import com.fanok.audiobooks.model.FavoritePeople;
import com.fanok.audiobooks.model.NewReleases;
import com.fanok.audiobooks.pojo.BookPOJO;
import com.fanok.audiobooks.room.AppDatabase;
import com.fanok.audiobooks.room.PersonBookDao;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * What a look at a person's listing leaves in the database: the first look only learns the books, a later one keeps the
 * books found since as news, and a book opened meanwhile is news no more.
 */
@RunWith(AndroidJUnit4.class)
public class ReleaseCheckKeepTest {

    private static final String KEY = "release-test-person";
    private static final int KIND = FavoritePeople.AUTHOR;

    private Context context;
    private SharedPreferences prefs;
    private PersonBookDao dao;
    private final Provider provider = Provider.values()[0];
    private final ReleaseCheck.Job job = new ReleaseCheck.Job(KIND, KEY, "https://example.com/person");

    private static BookPOJO book(String url, String name) {
        BookPOJO book = new BookPOJO();
        book.setUrl(url);
        book.setName(name);
        return book;
    }

    private static long since() {
        return System.currentTimeMillis() - NewReleases.FRESH_MS;
    }

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        dao = AppDatabase.getDatabase(context).personBookDao();
        clean();
    }

    @After
    public void clean() {
        if (dao == null) {
            return;
        }
        dao.deletePerson(KIND, KEY);
        BooksDBModel books = new BooksDBModel(context);
        books.removeHistory(book("https://example.com/release-test/c", "C"));
        java.util.Set<String> looked = new java.util.HashSet<>(
                prefs.getStringSet(ReleaseCheck.PREF_BASELINED, new java.util.HashSet<String>()));
        looked.remove(NewReleases.marker(KIND, KEY, provider.prefValue));
        prefs.edit().putStringSet(ReleaseCheck.PREF_BASELINED, looked).apply();
    }

    private List<BookPOJO> listing(String... names) {
        BookPOJO[] books = new BookPOJO[names.length];
        for (int i = 0; i < names.length; i++) {
            books[i] = book("https://example.com/release-test/" + names[i].toLowerCase(), names[i]);
        }
        return Arrays.asList(books);
    }

    @Test
    public void theFirstLookLearnsAndALaterOneFindsTheNewBooks() {
        ReleaseCheck.keep(context, prefs, provider, job, listing("A", "B"));
        assertEquals(2, dao.getKnownUrls(KIND, KEY).size());
        assertTrue("the first look makes nothing news", dao.getNew(since(), 50).isEmpty());

        ReleaseCheck.keep(context, prefs, provider, job, listing("C", "A", "B"));
        assertEquals(3, dao.getKnownUrls(KIND, KEY).size());
        assertEquals(1, dao.countNew(KIND, KEY, since()));
        assertEquals("C", dao.getNew(since(), 50).get(0).name);

        // the same listing again changes nothing
        ReleaseCheck.keep(context, prefs, provider, job, listing("C", "A", "B"));
        assertEquals(3, dao.getKnownUrls(KIND, KEY).size());
        assertEquals(1, dao.countNew(KIND, KEY, since()));
    }

    @Test
    public void aBookOpenedIsNewsNoMore() {
        ReleaseCheck.keep(context, prefs, provider, job, listing("A"));
        ReleaseCheck.keep(context, prefs, provider, job, listing("C", "A"));
        assertEquals(1, dao.countNew(KIND, KEY, since()));
        new BooksDBModel(context).addHistory(book("https://example.com/release-test/c", "C"),
                System.currentTimeMillis());
        assertEquals(0, dao.countNew(KIND, KEY, since()));
        assertTrue(dao.getNew(since(), 50).isEmpty());
    }

    @Test
    public void newsExpiresButTheBookStaysKnown() {
        ReleaseCheck.keep(context, prefs, provider, job, listing("A"));
        ReleaseCheck.keep(context, prefs, provider, job, listing("C", "A"));
        dao.expireNew(System.currentTimeMillis() + 1_000);
        assertTrue(dao.getNew(0, 50).isEmpty());
        assertEquals(2, dao.getKnownUrls(KIND, KEY).size()); // not shown as new again by a later look
        ReleaseCheck.keep(context, prefs, provider, job, listing("C", "A"));
        assertTrue(dao.getNew(0, 50).isEmpty());
    }
}
