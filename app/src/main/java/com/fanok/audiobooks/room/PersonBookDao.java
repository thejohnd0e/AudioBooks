package com.fanok.audiobooks.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PersonBookDao {

    /** A book already seen on the person's listing stays as it was. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PersonBookEntity entity);

    @Query("SELECT url_book FROM person_book WHERE kind = :kind AND person_key = :key")
    List<String> getKnownUrls(int kind, String key);

    /**
     * New books found since {@code since} that are not in the history (opened, so seen), one row per book, newest
     * first.
     */
    @Query("SELECT * FROM person_book WHERE is_new = 1 AND found_at >= :since"
            + " AND url_book NOT IN (SELECT url_book FROM history) GROUP BY url_book ORDER BY found_at DESC LIMIT :limit")
    List<PersonBookEntity> getNew(long since, int limit);

    @Query("SELECT COUNT(DISTINCT url_book) FROM person_book WHERE is_new = 1 AND found_at >= :since"
            + " AND kind = :kind AND person_key = :key AND url_book NOT IN (SELECT url_book FROM history)")
    int countNew(int kind, String key, long since);

    /** A book found before {@code before} is no longer news; it stays known. */
    @Query("UPDATE person_book SET is_new = 0 WHERE found_at < :before")
    void expireNew(long before);

    @Query("SELECT DISTINCT person_key FROM person_book WHERE kind = :kind")
    List<String> getPersonKeys(int kind);

    @Query("DELETE FROM person_book WHERE kind = :kind AND person_key = :key")
    void deletePerson(int kind, String key);

    @Query("DELETE FROM person_book")
    void deleteAll();
}
