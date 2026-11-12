package com.fanok.audiobooks.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A book listened to its end, once (the first time it was): the count on the screen "Статистика". */
@Entity(tableName = "finished_book", indices = {@Index(value = {"url_book"}, unique = true)})
public class FinishedBookEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "url_book")
    public String urlBook = "";

    @ColumnInfo(name = "finished_at")
    public long finishedAt;
}
