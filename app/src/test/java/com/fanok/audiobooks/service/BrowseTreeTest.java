package com.fanok.audiobooks.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class BrowseTreeTest {

    @Test
    public void everyListHasItsKind() {
        assertEquals(BrowseTree.Kind.ROOT, BrowseTree.kind("Root"));
        assertEquals(BrowseTree.Kind.CONTINUE, BrowseTree.kind(BrowseTree.NODE_CONTINUE));
        assertEquals(BrowseTree.Kind.CURRENT, BrowseTree.kind(BrowseTree.NODE_CURRENT));
        assertEquals(BrowseTree.Kind.HISTORY, BrowseTree.kind(BrowseTree.NODE_HISTORY));
        assertEquals(BrowseTree.Kind.FAVORITE, BrowseTree.kind(BrowseTree.NODE_FAVORITE));
        assertEquals(BrowseTree.Kind.SAVED, BrowseTree.kind(BrowseTree.NODE_SAVED));
    }

    @Test
    public void anythingElseIsUnknown() {
        assertEquals(BrowseTree.Kind.UNKNOWN, BrowseTree.kind(null));
        assertEquals(BrowseTree.Kind.UNKNOWN, BrowseTree.kind(""));
        assertEquals(BrowseTree.Kind.UNKNOWN, BrowseTree.kind("3"));
        assertEquals(BrowseTree.Kind.UNKNOWN, BrowseTree.kind("book:https://a.b/c"));
    }

    @Test
    public void aBookIdGivesTheUrlBack() {
        String url = "https://akniga.org/book?x=1&y=2";
        assertEquals(url, BrowseTree.bookUrl(BrowseTree.bookId(url)));
    }

    @Test
    public void onlyABookIdWithAWebUrlIsABook() {
        assertNull(BrowseTree.bookUrl(null));
        assertNull(BrowseTree.bookUrl("7")); // a chapter's number
        assertNull(BrowseTree.bookUrl("https://a.b/c")); // a bare url is no book id
        assertNull(BrowseTree.bookUrl("book:"));
        assertNull(BrowseTree.bookUrl("book:file:///sdcard/x"));
        assertNull(BrowseTree.bookUrl(BrowseTree.NODE_HISTORY));
    }

    @Test
    public void aListIsCutToTheLimitAndAShortOneIsKept() {
        List<Integer> many = new ArrayList<>();
        for (int i = 0; i < BrowseTree.MAX_ITEMS + 10; i++) {
            many.add(i);
        }
        assertEquals(BrowseTree.MAX_ITEMS, BrowseTree.limit(many).size());
        assertEquals(Integer.valueOf(0), BrowseTree.limit(many).get(0));
        List<Integer> few = new ArrayList<>(many.subList(0, 3));
        assertSame(few, BrowseTree.limit(few));
    }
}
