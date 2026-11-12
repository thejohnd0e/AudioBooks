package com.fanok.audiobooks.model;

import androidx.annotation.NonNull;
import com.fanok.audiobooks.pojo.BookPOJO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * What is new on the listing of a favourite author or reader, apart from the sites and the database: the books the
 * person's listing shows that were not seen before. The comparison is by the set of book URLs, never by place in the
 * list: a site may order its listing as it likes. Pure.
 */
public final class NewReleases {

    /** A book found longer ago than this is no longer news. */
    public static final long FRESH_MS = TimeUnit.DAYS.toMillis(30);

    /** Books taken from one listing: the first page of a site at most, so a check is one request. */
    public static final int PER_LISTING = 30;

    private NewReleases() {
    }

    /**
     * The books of {@code current} (a listing's first page) whose URL is not in {@code known}, in the listing's order
     * and without repeats. A book without a URL or a name cannot be shown and is left out.
     */
    @NonNull
    public static List<BookPOJO> unseen(@NonNull Set<String> known, @NonNull List<BookPOJO> current) {
        List<BookPOJO> out = new ArrayList<>();
        Set<String> taken = new HashSet<>();
        for (BookPOJO book : current.subList(0, Math.min(current.size(), PER_LISTING))) {
            String url = book.getUrl();
            if (url == null || url.isEmpty() || book.getName() == null || book.getName().trim().isEmpty()
                    || known.contains(url) || !taken.add(url)) {
                continue;
            }
            out.add(book);
        }
        return out;
    }

    /**
     * The key of a person on a site in the set of those already looked at once: the first look makes every book on
     * the listing known (nothing is news yet), only books found after it are.
     */
    @NonNull
    public static String marker(int kind, @NonNull String personKey, @NonNull String site) {
        return kind + "|" + personKey + "|" + site;
    }
}
