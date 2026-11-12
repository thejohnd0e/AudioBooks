package com.fanok.audiobooks.util;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Counts the time the listener really listened to, per local day, from the ticks of the player while it plays. The
 * player's position is no measure of it (speed, seeks, skipped silence): the wall time between two ticks is. A gap
 * longer than {@link #MAX_GAP_MS} between ticks is not counted (the process slept, the player stalled), and a stretch
 * that crosses midnight is split between the two days. Pure: the service feeds the clocks and stores what {@link
 * #drain} gives.
 */
public final class ListeningClock {

    static final long DAY_MS = TimeUnit.DAYS.toMillis(1);

    /** The player ticks every half second: a longer silence between ticks was not listening. */
    public static final long MAX_GAP_MS = 2_000;

    private long lastElapsed = -1;
    /** Pending, by local day. */
    private final Map<Integer, Long> pending = new HashMap<>();
    private long pendingMs;

    /** The local day of {@code wallMs}, with {@code utcOffsetMs} the zone's offset at that time. */
    public static int dayOf(long wallMs, long utcOffsetMs) {
        return (int) Math.floorDiv(wallMs + utcOffsetMs, DAY_MS);
    }

    /**
     * A tick of the playing player: {@code elapsedMs} on a monotonic clock, {@code wallMs} the date and time now, and
     * the zone's {@code utcOffsetMs}. Adds the time since the previous tick, if it was near enough.
     */
    public void tick(long elapsedMs, long wallMs, long utcOffsetMs) {
        long last = lastElapsed;
        lastElapsed = elapsedMs;
        if (last < 0) {
            return;
        }
        long delta = elapsedMs - last;
        if (delta <= 0 || delta > MAX_GAP_MS) {
            return;
        }
        int today = dayOf(wallMs, utcOffsetMs);
        int before = dayOf(wallMs - delta, utcOffsetMs);
        if (before == today) {
            add(today, delta);
        } else {
            long midnight = (long) today * DAY_MS - utcOffsetMs; // the start of today, in wall time
            long early = Math.max(0, Math.min(delta, midnight - (wallMs - delta)));
            add(before, early);
            add(today, delta - early);
        }
    }

    /** The player stopped playing: the next tick starts a new stretch, counting nothing for the pause. */
    public void stop() {
        lastElapsed = -1;
    }

    private void add(int day, long ms) {
        if (ms <= 0) {
            return;
        }
        Long was = pending.get(day);
        pending.put(day, (was == null ? 0 : was) + ms);
        pendingMs += ms;
    }

    /** How much is waiting to be stored. */
    public long pendingMs() {
        return pendingMs;
    }

    /** What is waiting, by local day, and nothing waiting after. */
    @NonNull
    public Map<Integer, Long> drain() {
        Map<Integer, Long> out = new HashMap<>(pending);
        pending.clear();
        pendingMs = 0;
        return out;
    }
}
