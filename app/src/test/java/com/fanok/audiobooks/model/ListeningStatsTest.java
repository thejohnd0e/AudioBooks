package com.fanok.audiobooks.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ListeningStatsTest {

    private static final long MIN = TimeUnit.MINUTES.toMillis(1);

    private static Map<Integer, Long> days(int... dayAndMinutes) {
        Map<Integer, Long> map = new HashMap<>();
        for (int i = 0; i < dayAndMinutes.length; i += 2) {
            map.put(dayAndMinutes[i], dayAndMinutes[i + 1] * MIN);
        }
        return map;
    }

    @Test
    public void lastDaysAreOldestFirstWithZerosForQuietDays() {
        Map<Integer, Long> map = days(98, 10, 100, 30);
        assertArrayEquals(new long[] {0, 10 * MIN, 0, 30 * MIN}, ListeningStats.lastDays(map, 100, 4));
        assertArrayEquals(new long[] {30 * MIN}, ListeningStats.lastDays(map, 100, 1));
    }

    @Test
    public void sumsCoverTheDaysEndingToday() {
        Map<Integer, Long> map = days(90, 5, 94, 10, 100, 30);
        assertEquals(30 * MIN, ListeningStats.sum(map, 100, 1));
        assertEquals(40 * MIN, ListeningStats.sum(map, 100, 7)); // days 94..100
        assertEquals(45 * MIN, ListeningStats.sum(map, 100, 30));
        assertEquals(45 * MIN, ListeningStats.total(map));
    }

    @Test
    public void theStreakCountsDaysInARowEndingTodayOrYesterday() {
        assertEquals(3, ListeningStats.streak(days(98, 5, 99, 5, 100, 5), 100));
        // nothing yet today: yesterday's streak is not broken
        assertEquals(2, ListeningStats.streak(days(98, 5, 99, 5), 100));
        // a gap day breaks it
        assertEquals(2, ListeningStats.streak(days(97, 5, 99, 5, 100, 5), 100));
        assertEquals(0, ListeningStats.streak(days(97, 5), 100));
        assertEquals(0, ListeningStats.streak(new HashMap<>(), 100));
    }

    @Test
    public void aStrayTapDoesNotKeepTheStreakAlive() {
        Map<Integer, Long> map = new HashMap<>();
        map.put(99, 5 * MIN);
        map.put(100, 20_000L); // twenty seconds
        assertEquals(1, ListeningStats.streak(map, 100));
    }

    @Test
    public void theChartScaleIsTheLongestDayAndNeverZero() {
        assertEquals(40L, ListeningStats.scale(new long[] {10, 40, 5}));
        assertEquals(1L, ListeningStats.scale(new long[] {0, 0}));
    }
}
