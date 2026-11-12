package com.fanok.audiobooks.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BookmarkDao {

    /** @return the new row's id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BookmarkEntity entity);

    @Query("UPDATE bookmark SET note = :note WHERE id = :id")
    void updateNote(int id, String note);

    @Query("DELETE FROM bookmark WHERE id = :id")
    void delete(int id);

    @Query("DELETE FROM bookmark WHERE url_book = :urlBook")
    void deleteByBook(String urlBook);

    @Query("DELETE FROM bookmark")
    void deleteAll();

    @Query("SELECT * FROM bookmark WHERE id = :id")
    BookmarkEntity get(int id);

    /** In book order: chapter, then position, then the order they were made in. */
    @Query("SELECT * FROM bookmark WHERE url_book = :urlBook ORDER BY chapter_index, position_ms, created_at")
    List<BookmarkEntity> getByBook(String urlBook);

    @Query("SELECT COUNT(id) FROM bookmark WHERE url_book = :urlBook")
    int count(String urlBook);

    @Query("SELECT * FROM bookmark ORDER BY url_book, chapter_index, position_ms, created_at")
    List<BookmarkEntity> getAll();
}
