package com.fanok.audiobooks.pojo;

import androidx.annotation.NonNull;
import com.fanok.audiobooks.room.BookmarkEntity;
import java.util.ArrayList;
import java.util.List;

/** A mark in a book in a backup (model.Bookmarks): the place by chapter name and number, the note and the time. */
public class BookmarkPOJO {
    private String urlBook;
    private int chapterIndex;
    private String chapterName;
    private long positionMs;
    private String note;
    private long createdAt;

    public BookmarkPOJO() {
    }

    @NonNull
    public static BookmarkPOJO of(@NonNull BookmarkEntity entity) {
        BookmarkPOJO pojo = new BookmarkPOJO();
        pojo.urlBook = entity.urlBook;
        pojo.chapterIndex = entity.chapterIndex;
        pojo.chapterName = entity.chapterName;
        pojo.positionMs = entity.positionMs;
        pojo.note = entity.note;
        pojo.createdAt = entity.createdAt;
        return pojo;
    }

    @NonNull
    public static ArrayList<BookmarkPOJO> ofAll(@NonNull List<BookmarkEntity> entities) {
        ArrayList<BookmarkPOJO> out = new ArrayList<>(entities.size());
        for (BookmarkEntity entity : entities) {
            out.add(of(entity));
        }
        return out;
    }

    /** A row of a new id: the backup's own ids mean nothing in the database it is restored into. */
    @NonNull
    public BookmarkEntity toEntity() {
        BookmarkEntity entity = new BookmarkEntity();
        entity.urlBook = urlBook == null ? "" : urlBook;
        entity.chapterIndex = chapterIndex;
        entity.chapterName = chapterName == null ? "" : chapterName;
        entity.positionMs = positionMs;
        entity.note = note;
        entity.createdAt = createdAt;
        return entity;
    }

    public String getUrlBook() {
        return urlBook;
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public String getChapterName() {
        return chapterName;
    }

    public long getPositionMs() {
        return positionMs;
    }

    public String getNote() {
        return note;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
