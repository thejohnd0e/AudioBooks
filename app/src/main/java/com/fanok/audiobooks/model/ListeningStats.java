package com.fanok.audiobooks.model;

import androidx.annotation.NonNull;
import java.util.Map;

/**
 * The numbers of the screen "Статистика" from the days stored: pure, so the figures are tested apart from the screen.
 * Days are the local day numbers of {@link com.fanok.audiobooks.util.ListeningClock#dayOf}.
 */
public final class ListeningStats {

    /** The days of the chart. */
    public static final int CHART_DAYS = 30;

    /** A day counts for the streak from this much listening: a minute, not a stray tap on Play. */
    static final long STREAK_MIN_MS = 60_000;

    private ListeningStats() {
    }

    private static long of(@NonNull Map<Integer, Long> days, int day) {
        Long ms = days.get(day);
        return ms == null ? 0 : ms;
    }

    /** The {@code count} days ending at {@code today}, oldest first; a day without listening is 0. */
    @NonNull
    public static long[] lastDays(@NonNull Map<Integer, Long> days, int today, int count) {
        long[] out = new long[count];
        for (int i = 0; i < count; i++) {
            out[i] = of(days, today - (count - 1 - i));
        }
        return out;
    }

    /** The total of the {@code count} days ending at {@code today}. */
    public static long sum(@NonNull Map<Integer, Long> days, int today, int count) {
        long total = 0;
        for (long ms : lastDays(days, today, count)) {
            total += ms;
        }
        return total;
    }

    /** Everything stored. */
    public static long total(@NonNull Map<Integer, Long> days) {
        long total = 0;
        for (long ms : days.values()) {
            total += ms;
        }
        return total;
    }

    /**
     * The days in a row with listening, ending today; a streak that did not reach today yet (nothing listened so far
     * today) still counts up to yesterday, since the day is not over.
     */
    public static int streak(@NonNull Map<Integer, Long> days, int today) {
        int day = of(days, today) >= STREAK_MIN_MS ? today : today - 1;
        int count = 0;
        while (of(days, day) >= STREAK_MIN_MS) {
            count++;
            day--;
        }
        return count;
    }

    /** The longest of the {@code values}: the chart's scale (at least 1, so a bar is never divided by 0). */
    public static long scale(@NonNull long[] values) {
        long max = 1;
        for (long value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}
