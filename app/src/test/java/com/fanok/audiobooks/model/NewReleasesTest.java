package com.fanok.audiobooks.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.fanok.audiobooks.pojo.BookPOJO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class NewReleasesTest {

    private static BookPOJO book(String url, String name) {
        BookPOJO book = new BookPOJO();
        book.setUrl(url);
        book.setName(name);
        return book;
    }

    private static List<String> urls(List<BookPOJO> books) {
        List<String> out = new ArrayList<>();
        for (BookPOJO book : books) {
            out.add(book.getUrl());
        }
        return out;
    }

    @Test
    public void onlyBooksNotKnownAreUnseenAndTheListingsOrderIsKept() {
        List<BookPOJO> listing = Arrays.asList(book("u3", "C"), book("u1", "A"), book("u2", "B"));
        assertEquals(Arrays.asList("u3", "u2"),
                urls(NewReleases.unseen(new HashSet<>(Collections.singletonList("u1")), listing)));
    }

    @Test
    public void nothingIsNewWhenAllAreKnownOrTheListingIsEmpty() {
        List<BookPOJO> listing = Arrays.asList(book("u1", "A"), book("u2", "B"));
        assertTrue(NewReleases.unseen(new HashSet<>(Arrays.asList("u1", "u2", "u9")), listing).isEmpty());
        assertTrue(NewReleases.unseen(new HashSet<>(), new ArrayList<>()).isEmpty());
    }

    @Test
    public void aRepeatedBookCountsOnceAndAnUnshowableOneIsLeftOut() {
        List<BookPOJO> listing = Arrays.asList(book("u1", "A"), book("u1", "A again"), book("", "no url"),
                book("u2", "  "), book("u3", null), book("u4", "D"));
        assertEquals(Arrays.asList("u1", "u4"), urls(NewReleases.unseen(new HashSet<>(), listing)));
    }

    @Test
    public void onlyTheFirstBooksOfALongListingAreLooked_at() {
        List<BookPOJO> listing = new ArrayList<>();
        for (int i = 0; i < NewReleases.PER_LISTING + 20; i++) {
            listing.add(book("u" + i, "B" + i));
        }
        assertEquals(NewReleases.PER_LISTING, NewReleases.unseen(new HashSet<>(), listing).size());
    }

    @Test
    public void theMarkerSeparatesPersonsKindsAndSites() {
        String a = NewReleases.marker(0, "king", "akniga");
        assertNotEquals(a, NewReleases.marker(1, "king", "akniga")); // an author and a reader of one name
        assertNotEquals(a, NewReleases.marker(0, "king", "izibuk"));
        assertNotEquals(a, NewReleases.marker(0, "poe", "akniga"));
        assertEquals(a, NewReleases.marker(0, "king", "akniga"));
    }
}
