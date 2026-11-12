package com.fanok.audiobooks.pojo;

/** A day of listening in a backup (model.ListenStore): the local day number and the milliseconds listened. */
public class ListenDayPOJO {
    private int day;
    private long listenedMs;

    public ListenDayPOJO() {
    }

    public ListenDayPOJO(int day, long listenedMs) {
        this.day = day;
        this.listenedMs = listenedMs;
    }

    public int getDay() {
        return day;
    }

    public long getListenedMs() {
        return listenedMs;
    }
}
