package com.fanok.audiobooks.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.fanok.audiobooks.room.BookmarkEntity;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class BookmarksTest {

    private static BookmarkEntity mark(int index, String name, long ms) {
        return Bookmarks.create("http://book", index, name, ms, null, 1L);
    }

    @Test
    public void resolveKeepsThePlaceWhenTheNameIsStillThere() {
        List<String> names = Arrays.asList("Глава 1", "Глава 2", "Глава 3");
        assertEquals(1, Bookmarks.resolve(names, mark(1, "Глава 2", 5000)));
    }

    @Test
    public void resolveFollowsTheNameAfterAReorder() {
        List<String> names = Arrays.asList("Пролог", "Глава 1", "Глава 2");
        assertEquals(2, Bookmarks.resolve(names, mark(1, "Глава 2", 5000)));
    }

    @Test
    public void resolveNeverGuessesByNumberAlone() {
        List<String> names = Arrays.asList("Другая 1", "Другая 2");
        assertEquals(-1, Bookmarks.resolve(names, mark(1, "Глава 2", 5000)));
    }

    @Test
    public void resolveWithoutAChapterNameFindsNothing() {
        assertEquals(-1, Bookmarks.resolve(Arrays.asList("1", "2"), mark(1, "", 0)));
    }

    @Test
    public void resolvePrefersTheOwnPlaceAmongEqualNames() {
        List<String> names = Arrays.asList("01", "01", "01");
        assertEquals(2, Bookmarks.resolve(names, mark(2, "01", 0)));
    }

    @Test
    public void resolveOutOfRangeFallsBackToTheFirstOfThatName() {
        List<String> names = Arrays.asList("A", "B");
        assertEquals(1, Bookmarks.resolve(names, mark(9, "B", 0)));
    }

    @Test
    public void noteIsTrimmedAndEmptyIsNone() {
        assertNull(Bookmarks.cleanNote(null));
        assertNull(Bookmarks.cleanNote("   "));
        assertEquals("слово", Bookmarks.cleanNote("  слово \n"));
    }

    @Test
    public void noteIsCut() {
        StringBuilder long_ = new StringBuilder();
        for (int i = 0; i < Bookmarks.MAX_NOTE + 50; i++) {
            long_.append('x');
        }
        assertEquals(Bookmarks.MAX_NOTE, Bookmarks.cleanNote(long_.toString()).length());
    }

    @Test
    public void createClampsNegativeValues() {
        BookmarkEntity m = Bookmarks.create("u", -3, null, -5, null, 7L);
        assertEquals(0, m.chapterIndex);
        assertEquals(0, m.positionMs);
        assertEquals("", m.chapterName);
        assertEquals(7L, m.createdAt);
    }

    @Test
    public void keyIsThePlaceNotTheMoment() {
        BookmarkEntity a = Bookmarks.create("u", 1, "Глава", 1000, "а", 1L);
        BookmarkEntity b = Bookmarks.create("u", 4, "Глава", 1000, "б", 99L);
        assertEquals(Bookmarks.key(a), Bookmarks.key(b));
        assertNotEquals(Bookmarks.key(a), Bookmarks.key(Bookmarks.create("u", 1, "Глава", 2000, null, 1L)));
        assertNotEquals(Bookmarks.key(a), Bookmarks.key(Bookmarks.create("v", 1, "Глава", 1000, null, 1L)));
    }

    @Test
    public void timeIsMinutesThenHours() {
        assertEquals("0:00", Bookmarks.time(0));
        assertEquals("7:05", Bookmarks.time(425_900));
        assertEquals("1:07:05", Bookmarks.time(4_025_000));
        assertEquals("0:00", Bookmarks.time(-1));
    }
}
