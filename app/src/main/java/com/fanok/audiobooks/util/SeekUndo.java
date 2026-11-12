package com.fanok.audiobooks.util;

import androidx.annotation.Nullable;

/**
 * Takes back a seek made by mistake: after a jump (the scrub bar dragged far, another chapter chosen) the place before
 * it is offered for a few seconds. Small seeks (the step buttons, a nudge of the bar) are never offered. Jumps made
 * one after another within the offer's time keep the first place: undoing goes back to where the listener was, not
 * to the middle of the scrubbing. Pure; the screen shows the offer.
 */
public final class SeekUndo {

    /** A move of the position within one chapter by more than this is a jump. */
    public static final long JUMP_MS = 60_000;

    /** How long the way back is offered. */
    public static final long WINDOW_MS = 8_000;

    /** A place in the book: the chapter's index in the list and the position in it. */
    public static final class Place {
        public final int chapter;
        public final long positionMs;

        Place(int chapter, long positionMs) {
            this.chapter = chapter;
            this.positionMs = positionMs;
        }
    }

    @Nullable
    private Place origin;
    private long offeredAt;

    /** Whether a move from one place to another is a jump: another chapter, or far within the same one. */
    public static boolean isJump(int fromChapter, long fromMs, int toChapter, long toMs) {
        return fromChapter != toChapter || Math.abs(toMs - fromMs) > JUMP_MS;
    }

    /**
     * A move from {@code (fromChapter, fromMs)} to {@code (toChapter, toMs)} at {@code now}. Returns the place to offer
     * going back to, or null when there is nothing to offer: the move is no jump, or it came back to where an earlier
     * jump started.
     */
    @Nullable
    public Place move(long now, int fromChapter, long fromMs, int toChapter, long toMs) {
        if (!isJump(fromChapter, fromMs, toChapter, toMs)) {
            return null;
        }
        Place from = origin != null && now - offeredAt < WINDOW_MS ? origin : new Place(fromChapter, fromMs);
        offeredAt = now;
        origin = from;
        if (!isJump(from.chapter, from.positionMs, toChapter, toMs)) {
            origin = null; // back at the start of the scrubbing: nothing to take back
            return null;
        }
        return from;
    }

    /** The offer is gone: taken, or the listener moved on. */
    public void clear() {
        origin = null;
    }
}
