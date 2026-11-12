package com.fanok.audiobooks.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.fanok.audiobooks.model.AudioListDBModel;
import com.fanok.audiobooks.model.BooksDBModel;
import com.fanok.audiobooks.pojo.AudioListPOJO;
import com.fanok.audiobooks.pojo.BookPOJO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The library a car sees, through the real service and a real media browser: the root's lists, a list that holds only
 * the books this player can start (stored chapters), and an unknown parent that holds nothing.
 */
@RunWith(AndroidJUnit4.class)
public class BrowseLibraryTest {

    private static final String PLAYABLE = "https://example.com/browse-test/playable";
    private static final String NO_CHAPTERS = "https://example.com/browse-test/no-chapters";

    private Context context;
    private MediaBrowserCompat browser;
    private BooksDBModel books;
    private AudioListDBModel chapters;

    private static BookPOJO book(String url, String name) {
        BookPOJO book = new BookPOJO();
        book.setUrl(url);
        book.setName(name);
        return book;
    }

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        books = new BooksDBModel(context);
        chapters = new AudioListDBModel(context);
        books.addHistory(book(PLAYABLE, "Книга со главами"), System.currentTimeMillis());
        books.addHistory(book(NO_CHAPTERS, "Книга без глав"), System.currentTimeMillis());
        AudioListPOJO chapter = new AudioListPOJO();
        chapter.setBookUrl(PLAYABLE);
        chapter.setBookName("Книга со главами");
        chapter.setAudioName("Глава 1");
        chapter.setAudioUrl("https://example.com/browse-test/1.mp3");
        chapter.setTime(60);
        chapters.add(chapter);

        CountDownLatch connected = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            browser = new MediaBrowserCompat(context, new ComponentName(context, MediaPlayerService.class),
                    new MediaBrowserCompat.ConnectionCallback() {
                        @Override
                        public void onConnected() {
                            connected.countDown();
                        }
                    }, null);
            browser.connect();
        });
        assertTrue("the browser did not connect", connected.await(15, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            if (browser != null) {
                browser.disconnect();
            }
        });
        books.removeHistory(book(PLAYABLE, "x"));
        books.removeHistory(book(NO_CHAPTERS, "x"));
        chapters.remove(PLAYABLE);
    }

    /** The ids of the children of {@code parent}, read through the browser. */
    private List<String> children(String parent) throws Exception {
        AtomicReference<List<MediaBrowserCompat.MediaItem>> got = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> browser.subscribe(parent, new MediaBrowserCompat.SubscriptionCallback() {
                    @Override
                    public void onChildrenLoaded(String id, List<MediaBrowserCompat.MediaItem> items) {
                        got.set(items);
                        done.countDown();
                    }

                    @Override
                    public void onError(String id) {
                        done.countDown();
                    }
                }));
        assertTrue("no answer for " + parent, done.await(15, TimeUnit.SECONDS));
        assertNotNull("error for " + parent, got.get());
        List<String> ids = new ArrayList<>();
        for (MediaBrowserCompat.MediaItem item : got.get()) {
            ids.add(item.getMediaId());
        }
        return ids;
    }

    @Test
    public void rootHoldsTheListsAndNoCurrentBookWhileNothingIsLoaded() throws Exception {
        List<String> root = children(BrowseTree.ROOT);
        assertTrue(root.contains(BrowseTree.NODE_CONTINUE));
        assertTrue(root.contains(BrowseTree.NODE_HISTORY));
        assertTrue(root.contains(BrowseTree.NODE_FAVORITE));
        assertTrue(root.contains(BrowseTree.NODE_SAVED));
        if (!MediaPlayerService.isRunning() || MediaPlayerService.currentBookUrl() == null) {
            assertFalse(root.contains(BrowseTree.NODE_CURRENT));
        }
    }

    @Test
    public void historyListsOnlyBooksWithStoredChapters() throws Exception {
        List<String> history = children(BrowseTree.NODE_HISTORY);
        assertTrue(history.contains(BrowseTree.bookId(PLAYABLE)));
        assertFalse(history.contains(BrowseTree.bookId(NO_CHAPTERS)));
        assertTrue(history.size() <= BrowseTree.MAX_ITEMS);
    }

    @Test
    public void everyListAnswersAndAnUnknownParentIsEmpty() throws Exception {
        children(BrowseTree.NODE_CONTINUE);
        children(BrowseTree.NODE_FAVORITE);
        children(BrowseTree.NODE_SAVED);
        assertEquals(0, children("node:nothing").size());
    }
}
