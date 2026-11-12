package com.fanok.audiobooks.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A place the listener marked in a book: the chapter (by number and by name, since the sites' chapter lists get
 * reordered and renamed), the position in it and an optional note.
 */
@Entity(tableName = "bookmark", indices = {@Index("url_book")})
public class BookmarkEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "url_book")
    public String urlBook = "";

    /** The chapter's place in the list when the mark was made. */
    @ColumnInfo(name = "chapter_index")
    public int chapterIndex;

    /** The chapter's name then: the way back when the list changed. */
    @NonNull
    @ColumnInfo(name = "chapter_name")
    public String chapterName = "";

    @ColumnInfo(name = "position_ms")
    public long positionMs;

    @ColumnInfo(name = "note")
    public String note;

    /** Epoch ms: the order of marks made at one place, and what a cloud sync compares. */
    @ColumnInfo(name = "created_at")
    public long createdAt;
}
