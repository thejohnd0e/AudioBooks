package com.fanok.audiobooks.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ListenDayDao {

    /** Makes the day's row if there is none (with 0); {@link #add} then adds to it. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIfAbsent(ListenDayEntity entity);

    @Query("UPDATE listen_day SET listened_ms = listened_ms + :ms WHERE day = :day")
    void add(int day, long ms);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ListenDayEntity entity);

    @Query("SELECT * FROM listen_day WHERE day >= :since ORDER BY day")
    List<ListenDayEntity> getSince(int since);

    @Query("SELECT * FROM listen_day ORDER BY day")
    List<ListenDayEntity> getAll();

    @Query("SELECT COALESCE(SUM(listened_ms), 0) FROM listen_day")
    long total();

    @Query("DELETE FROM listen_day")
    void deleteAll();
}
