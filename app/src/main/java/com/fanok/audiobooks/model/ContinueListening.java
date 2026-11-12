package com.fanok.audiobooks.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fanok.audiobooks.pojo.AudioListPOJO;
import com.fanok.audiobooks.pojo.BookPOJO;
import com.fanok.audiobooks.util.RemainingTime;
import java.util.List;

/**
 * The rules of the "Продолжить" screen, apart from the database: which books are to be continued, and how much of one
 * is left. A book is to be continued when something of it was listened to (a chapter is recorded) and it is not
 * listened to the end; a book whose chapters are not stored here has no percent (-1) but is still offered.
 */
public final class ContinueListening {

    /** How many books the screen offers. */
    public static final int LIMIT = 10;

    private ContinueListening() {
    }

    /** One book to continue. */
    public static final class Item {
        @NonNull
        public final BookPOJO book;
        /** {@link ListeningProgress#percent}, or -1 when the book's chapters are not stored here. */
        public final int percent;
        /** The chapter it stands at (its name), never empty. */
        @NonNull
        public final String chapter;
        /** Real seconds left at the playing speed, or -1 when it cannot be told. */
        public final long leftSeconds;

        public Item(@NonNull BookPOJO book, int percent, @NonNull String chapter, long leftSeconds) {
            this.book = book;
            this.percent = percent;
            this.chapter = chapter;
            this.leftSeconds = leftSeconds;
        }

        /** The book's chapters are known, so the player can resume it without its page. */
        public boolean canResume() {
            return percent >= 0;
        }
    }

    /**
     * Whether a book standing at {@code chapter} with {@code percent} listened is to be continued: it has a recorded
     * place and is not finished. A book not listened to at all has no recorded chapter.
     */
    public static boolean qualifies(@Nullable String chapter, int percent) {
        return chapter != null && !chapter.trim().isEmpty() && percent < 100;
    }

    /**
     * Real seconds left in the book at the chapter named {@code chapter}, {@code seconds} into it, played at
     * {@code speed}; -1 when the chapter is not in the list or a chapter's length is unknown.
     */
    public static long leftSeconds(@NonNull List<AudioListPOJO> chapters, @NonNull String chapter, int seconds,
            float speed) {
        int index = -1;
        int[] lengths = new int[chapters.size()];
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = chapters.get(i).getTime();
            if (index < 0 && chapter.equals(chapters.get(i).getAudioName())) {
                index = i;
            }
        }
        if (index < 0) {
            return -1;
        }
        RemainingTime.Line line = new RemainingTime(lengths).at(index, seconds * 1000L, speed);
        return line.state == RemainingTime.LEFT ? line.left : -1;
    }
}
