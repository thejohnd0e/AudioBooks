package com.fanok.audiobooks.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fanok.audiobooks.room.BookmarkEntity;
import java.util.List;
import java.util.Locale;

/**
 * The rules of a book's marks, apart from the screen and the database: finding the marked chapter again in a list
 * that changed, the one key a mark has across devices, and its time as text.
 */
public final class Bookmarks {

    /** Longer notes are cut: a mark is a pointer, not a document. */
    public static final int MAX_NOTE = 500;

    private Bookmarks() {
    }

    /**
     * The mark of the place now: chapter {@code index} named {@code chapterName}, {@code positionMs} into it.
     * The note is trimmed; an empty one is none.
     */
    @NonNull
    public static BookmarkEntity create(@NonNull String bookUrl, int index, @Nullable String chapterName,
            long positionMs, @Nullable String note, long now) {
        BookmarkEntity mark = new BookmarkEntity();
        mark.urlBook = bookUrl;
        mark.chapterIndex = Math.max(0, index);
        mark.chapterName = chapterName == null ? "" : chapterName;
        mark.positionMs = Math.max(0, positionMs);
        mark.note = cleanNote(note);
        mark.createdAt = now;
        return mark;
    }

    /** The note trimmed and cut to {@link #MAX_NOTE}; null when nothing is left. */
    @Nullable
    public static String cleanNote(@Nullable String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > MAX_NOTE ? trimmed.substring(0, MAX_NOTE) : trimmed;
    }

    /**
     * Where the marked chapter is in {@code names} now: the place it had, when a chapter of that name is still
     * there; else the first chapter of that name; else -1. A chapter is never found by its number alone: after a
     * reorder the same number is another chapter, and a wrong jump is worse than none.
     */
    public static int resolve(@NonNull List<String> names, @NonNull BookmarkEntity mark) {
        if (mark.chapterName.isEmpty()) {
            return -1;
        }
        if (mark.chapterIndex >= 0 && mark.chapterIndex < names.size()
                && mark.chapterName.equals(names.get(mark.chapterIndex))) {
            return mark.chapterIndex;
        }
        return names.indexOf(mark.chapterName);
    }

    /** The key of a mark among devices: the same place of the same book is one mark however often it was made. */
    @NonNull
    public static String key(@NonNull BookmarkEntity mark) {
        return mark.urlBook + "|" + mark.chapterName + "|" + mark.positionMs;
    }

    /** {@code 7:05} or, from an hour, {@code 1:07:05}. */
    @NonNull
    public static String time(long positionMs) {
        long total = Math.max(0, positionMs) / 1000;
        long hours = total / 3600;
        long minutes = total % 3600 / 60;
        long seconds = total % 60;
        return hours > 0
                ? String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.US, "%d:%02d", minutes, seconds);
    }
}
