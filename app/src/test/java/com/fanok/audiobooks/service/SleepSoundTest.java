package com.fanok.audiobooks.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SleepSoundTest {

    @Test
    public void everyIdParsesBackToItsKind() {
        for (SleepSound.Kind kind : SleepSound.Kind.values()) {
            assertEquals(kind, SleepSound.Kind.parse(kind.id));
        }
    }

    @Test
    public void anUnknownOrMissingIdIsTheOldBeep() {
        assertEquals(SleepSound.Kind.BEEP, SleepSound.Kind.parse(null));
        assertEquals(SleepSound.Kind.BEEP, SleepSound.Kind.parse(""));
        assertEquals(SleepSound.Kind.BEEP, SleepSound.Kind.parse("gong"));
    }

    @Test
    public void idsAndTitlesAreDistinctAndTheOldBeepComesFirst() {
        Set<String> ids = new HashSet<>();
        Set<Integer> titles = new HashSet<>();
        for (SleepSound.Kind kind : SleepSound.Kind.values()) {
            ids.add(kind.id);
            titles.add(kind.title);
        }
        assertEquals(SleepSound.Kind.values().length, ids.size());
        assertEquals(SleepSound.Kind.values().length, titles.size());
        assertEquals(SleepSound.Kind.BEEP, SleepSound.Kind.values()[0]);
        assertNotEquals(SleepSound.Kind.BEEP.id, SleepSound.Kind.CUSTOM.id);
    }
}
