package com.fanok.audiobooks.pojo;

/** A book listened to its end in a backup (model.ListenStore). */
public class FinishedBookPOJO {
    private String urlBook;
    private long finishedAt;

    public FinishedBookPOJO() {
    }

    public FinishedBookPOJO(String urlBook, long finishedAt) {
        this.urlBook = urlBook;
        this.finishedAt = finishedAt;
    }

    public String getUrlBook() {
        return urlBook;
    }

    public long getFinishedAt() {
        return finishedAt;
    }
}
