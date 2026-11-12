package com.fanok.audiobooks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ListeningClockTest {

    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    @Test
    public void theDayIsLocalAndNegativeTimesFloor() {
        assertEquals(0, ListeningClock.dayOf(0, 0));
        assertEquals(1, ListeningClock.dayOf(DAY, 0));
        // 23:30 UTC on day 5 is already day 6 at UTC+3
        assertEquals(6, ListeningClock.dayOf(5 * DAY + TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(30),
                TimeUnit.HOURS.toMillis(3)));
        assertEquals(-1, ListeningClock.dayOf(-1, 0));
    }

    @Test
    public void ticksAddTheTimeBetweenThem() {
        ListeningClock clock = new ListeningClock();
        long wall = 10 * DAY + 5_000;
        clock.tick(1_000, wall, 0); // the first tick only starts the stretch
        clock.tick(1_500, wall + 500, 0);
        clock.tick(2_000, wall + 1_000, 0);
        assertEquals(1_000, clock.pendingMs());
        Map<Integer, Long> day = clock.drain();
        assertEquals(Long.valueOf(1_000), day.get(10));
        assertEquals(0, clock.pendingMs());
        assertTrue(clock.drain().isEmpty());
    }

    @Test
    public void aLongGapIsNotListening() {
        ListeningClock clock = new ListeningClock();
        clock.tick(1_000, 10 * DAY, 0);
        clock.tick(1_000 + ListeningClock.MAX_GAP_MS + 1, 10 * DAY + 3_000, 0); // the process slept
        assertEquals(0, clock.pendingMs());
        clock.tick(1_000 + ListeningClock.MAX_GAP_MS + 1 + 500, 10 * DAY + 3_500, 0); // counting again
        assertEquals(500, clock.pendingMs());
    }

    @Test
    public void aPauseCountsNothingAndTheNextStretchStartsAnew() {
        ListeningClock clock = new ListeningClock();
        clock.tick(1_000, 10 * DAY, 0);
        clock.tick(1_500, 10 * DAY + 500, 0);
        clock.stop();
        clock.tick(9_000, 10 * DAY + 8_000, 0); // after the pause: only starts
        assertEquals(500, clock.pendingMs());
        clock.tick(9_500, 10 * DAY + 8_500, 0);
        assertEquals(1_000, clock.pendingMs());
    }

    @Test
    public void midnightSplitsATickBetweenTheTwoDays() {
        ListeningClock clock = new ListeningClock();
        long midnight = 11 * DAY; // UTC
        clock.tick(1_000, midnight - 300, 0);
        clock.tick(1_500, midnight + 200, 0); // 500 ms: 300 before midnight, 200 after
        Map<Integer, Long> days = clock.drain();
        assertEquals(Long.valueOf(300), days.get(10));
        assertEquals(Long.valueOf(200), days.get(11));
    }

    @Test
    public void midnightFollowsTheZoneNotUtc() {
        ListeningClock clock = new ListeningClock();
        long offset = TimeUnit.HOURS.toMillis(3);
        long localMidnight = 11 * DAY - offset; // the start of local day 11
        clock.tick(1_000, localMidnight - 100, offset);
        clock.tick(1_400, localMidnight + 300, offset);
        Map<Integer, Long> days = clock.drain();
        assertEquals(Long.valueOf(100), days.get(10));
        assertEquals(Long.valueOf(300), days.get(11));
    }

    @Test
    public void aClockGoingBackCountsNothing() {
        ListeningClock clock = new ListeningClock();
        clock.tick(5_000, 10 * DAY, 0);
        clock.tick(4_000, 10 * DAY + 500, 0);
        assertEquals(0, clock.pendingMs());
    }
}
