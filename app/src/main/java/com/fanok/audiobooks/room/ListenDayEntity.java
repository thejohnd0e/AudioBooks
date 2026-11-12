package com.fanok.audiobooks.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * How long the listener really listened on one local day (the screen "Статистика"). Real time, not the time of the
 * book that passed: an hour at double speed is an hour. Milliseconds, so that many short writes lose nothing.
 */
@Entity(tableName = "listen_day")
public class ListenDayEntity {
    /** The local day: whole days since 1970-01-01 in the time zone of the day (util.ListeningClock#dayOf). */
    @PrimaryKey
    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "listened_ms")
    public long listenedMs;
}
