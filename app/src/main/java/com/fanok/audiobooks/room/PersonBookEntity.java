package com.fanok.audiobooks.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.fanok.audiobooks.pojo.BookPOJO;

/**
 * A book seen on the listing of a favourite author or reader (search.ReleaseCheck). Every book seen is kept, new or
 * not, since the set of known books is what tells a new one; {@code isNew} marks those found after the person's
 * first look.
 */
@Entity(tableName = "person_book", indices = {
        @Index(value = {"kind", "person_key", "url_book"}, unique = true)})
public class PersonBookEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** FavoritePeople.AUTHOR or READER. */
    @ColumnInfo(name = "kind")
    public int kind;

    @NonNull
    @ColumnInfo(name = "person_key")
    public String personKey = "";

    @NonNull
    @ColumnInfo(name = "url_book")
    public String urlBook = "";

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "photo")
    public String photo;

    @ColumnInfo(name = "author")
    public String author;

    @ColumnInfo(name = "artist")
    public String artist;

    @ColumnInfo(name = "time")
    public String time;

    /** Epoch ms of the check that saw the book. */
    @ColumnInfo(name = "found_at")
    public long foundAt;

    @ColumnInfo(name = "is_new", defaultValue = "0")
    public boolean isNew;

    /** What the card of the book needs; the rest of the book is read when its page opens. */
    @NonNull
    public BookPOJO toPojo() {
        BookPOJO book = new BookPOJO();
        book.setUrl(urlBook);
        book.setName(name);
        book.setPhoto(photo);
        book.setAutor(author);
        book.setArtist(artist);
        book.setTime(time);
        return book;
    }
}
