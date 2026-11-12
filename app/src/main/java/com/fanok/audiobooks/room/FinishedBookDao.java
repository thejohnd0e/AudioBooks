package com.fanok.audiobooks.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FinishedBookDao {

    /** A book finished before keeps its first date. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(FinishedBookEntity entity);

    @Query("SELECT * FROM finished_book ORDER BY finished_at")
    List<FinishedBookEntity> getAll();

    @Query("SELECT COUNT(id) FROM finished_book")
    int count();

    @Query("SELECT COUNT(id) FROM finished_book WHERE finished_at >= :since")
    int countSince(long since);

    @Query("DELETE FROM finished_book")
    void deleteAll();
}
