package com.fanok.audiobooks.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

/**
 * The library a car (Android Auto) or another media browser sees: the root holds a few lists, a list holds books, and
 * a book plays by its media id. The ids and the limit live here, apart from the service, so they are tested on the
 * JVM and never drift apart between the lists and the playing. The chapters of the book in the player keep their
 * plain numbers as ids (the service's {@code parseMediaIndex}).
 */
public final class BrowseTree {

    /** The root's id, as the service gave it from the start. */
    public static final String ROOT = "Root";
    public static final String NODE_CONTINUE = "node:continue";
    public static final String NODE_CURRENT = "node:current";
    public static final String NODE_HISTORY = "node:history";
    public static final String NODE_FAVORITE = "node:favorite";
    public static final String NODE_SAVED = "node:saved";

    private static final String BOOK_PREFIX = "book:";

    /** A list in a car is short: more than this is for the phone. */
    public static final int MAX_ITEMS = 50;

    /** What a parent id asks for. */
    public enum Kind {
        ROOT, CONTINUE, CURRENT, HISTORY, FAVORITE, SAVED, UNKNOWN
    }

    private BrowseTree() {
    }

    @NonNull
    public static Kind kind(@Nullable String parentId) {
        if (parentId == null) {
            return Kind.UNKNOWN;
        }
        switch (parentId) {
            case ROOT:
                return Kind.ROOT;
            case NODE_CONTINUE:
                return Kind.CONTINUE;
            case NODE_CURRENT:
                return Kind.CURRENT;
            case NODE_HISTORY:
                return Kind.HISTORY;
            case NODE_FAVORITE:
                return Kind.FAVORITE;
            case NODE_SAVED:
                return Kind.SAVED;
            default:
                return Kind.UNKNOWN;
        }
    }

    /** The media id of the book at {@code url}. */
    @NonNull
    public static String bookId(@NonNull String url) {
        return BOOK_PREFIX + url;
    }

    /** The book's URL of a media id made by {@link #bookId}; null for any other id. */
    @Nullable
    public static String bookUrl(@Nullable String mediaId) {
        if (mediaId == null || !mediaId.startsWith(BOOK_PREFIX)) {
            return null;
        }
        String url = mediaId.substring(BOOK_PREFIX.length());
        return url.startsWith("https://") || url.startsWith("http://") ? url : null;
    }

    /** The first {@link #MAX_ITEMS} of a list. */
    @NonNull
    public static <T> List<T> limit(@NonNull List<T> items) {
        return items.size() <= MAX_ITEMS ? items : items.subList(0, MAX_ITEMS);
    }
}
