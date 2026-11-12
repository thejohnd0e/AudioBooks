package com.fanok.audiobooks.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ChapterFilterTest {

    private static final List<String> NAMES = Arrays.asList(
            "Пролог", "Глава 1. Ёлка", "Глава 2", "Глава 10", "Глава 21", "Эпилог");

    @Test
    public void anEmptyQueryIsEveryChapter() {
        assertNull(ChapterFilter.match(NAMES, null));
        assertNull(ChapterFilter.match(NAMES, ""));
        assertNull(ChapterFilter.match(NAMES, "   "));
    }

    @Test
    public void aPartOfTheNameMatchesWithoutCaseOrYo() {
        assertArrayEquals(new int[] {1}, ChapterFilter.match(NAMES, "елка"));
        assertArrayEquals(new int[] {1}, ChapterFilter.match(NAMES, "ЁЛКА"));
        assertArrayEquals(new int[] {0}, ChapterFilter.match(NAMES, " пролог "));
        assertArrayEquals(new int[] {1, 2, 3, 4}, ChapterFilter.match(NAMES, "глава"));
    }

    @Test
    public void aNumberIsTheChapterOfThatNumberAndTheNamesWithTheDigits() {
        // "2": chapter number 2 (index 1) and the names holding a 2
        assertArrayEquals(new int[] {1, 2, 4}, ChapterFilter.match(NAMES, "2"));
        assertArrayEquals(new int[] {3}, ChapterFilter.match(NAMES, "10"));
        assertArrayEquals(new int[] {0, 1, 3, 4}, ChapterFilter.match(NAMES, "1")); // the 1st chapter and the names with a 1
    }

    @Test
    public void nothingFoundIsAnEmptyList() {
        assertArrayEquals(new int[0], ChapterFilter.match(NAMES, "нет такой"));
        assertArrayEquals(new int[0], ChapterFilter.match(NAMES, "99"));
    }

    @Test
    public void aLongDigitStringIsOnlyAPartOfTheName() {
        assertArrayEquals(new int[0], ChapterFilter.match(NAMES, "1234567"));
    }
}
