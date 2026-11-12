package com.fanok.audiobooks.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds chapters of a long book by what the listener types: a part of the name (case and ё/е do not matter), or a
 * number, which is the chapter of that number. Pure.
 */
public final class ChapterFilter {

    /** More chapters than this make the search field worth showing. */
    public static final int SEARCH_FROM = 30;

    private ChapterFilter() {
    }

    /** The text the way names are compared: lower case, ё as е, no outer blanks. */
    @NonNull
    static String fold(@Nullable String text) {
        return text == null ? "" : text.trim().toLowerCase(new Locale("ru")).replace('ё', 'е');
    }

    /**
     * The indexes of the chapters of {@code names} that match {@code query}, in list order; null for an empty query,
     * which is every chapter. A query of digits also matches the chapter of that number (its place in the list + 1),
     * besides the names that hold the digits.
     */
    @Nullable
    public static int[] match(@NonNull List<String> names, @Nullable String query) {
        String needle = fold(query);
        if (needle.isEmpty()) {
            return null;
        }
        int number = -1;
        if (needle.length() <= 6 && needle.chars().allMatch(Character::isDigit)) {
            number = Integer.parseInt(needle);
        }
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            if (i + 1 == number || fold(names.get(i)).contains(needle)) {
                found.add(i);
            }
        }
        int[] out = new int[found.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = found.get(i);
        }
        return out;
    }
}
