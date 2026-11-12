package com.fanok.audiobooks.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fanok.audiobooks.pojo.AudioListPOJO;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ContinueListeningTest {

    private static List<AudioListPOJO> book(int... lengths) {
        List<AudioListPOJO> list = new ArrayList<>();
        for (int i = 0; i < lengths.length; i++) {
            AudioListPOJO chapter = new AudioListPOJO();
            chapter.setAudioName("Глава " + (i + 1));
            chapter.setTime(lengths[i]);
            list.add(chapter);
        }
        return list;
    }

    @Test
    public void aBookIsContinuedWhenAPlaceIsRecordedAndItIsNotFinished() {
        assertTrue(ContinueListening.qualifies("Глава 2", 0)); // a few seconds in is still a place
        assertTrue(ContinueListening.qualifies("Глава 2", 40));
        assertTrue(ContinueListening.qualifies("Глава 2", -1)); // chapters not stored here: still offered
        assertFalse(ContinueListening.qualifies("Глава 2", 100));
        assertFalse(ContinueListening.qualifies("", 40)); // never listened
        assertFalse(ContinueListening.qualifies(null, 40));
        assertFalse(ContinueListening.qualifies("  ", 40));
    }

    @Test
    public void leftIsTheRestOfTheBookAtTheSpeed() {
        List<AudioListPOJO> chapters = book(600, 600, 600); // 30 min
        // 5 min into chapter 2: 600 + 300 behind, 900 s left
        assertEquals(900, ContinueListening.leftSeconds(chapters, "Глава 2", 300, 1f));
        assertEquals(450, ContinueListening.leftSeconds(chapters, "Глава 2", 300, 2f));
    }

    @Test
    public void leftIsUnknownForAMissingChapterOrAnUnknownLength() {
        assertEquals(-1, ContinueListening.leftSeconds(book(600, 600), "Другая", 10, 1f));
        assertEquals(-1, ContinueListening.leftSeconds(book(600, 0, 600), "Глава 1", 100, 1f));
        assertEquals(-1, ContinueListening.leftSeconds(new ArrayList<>(), "Глава 1", 100, 1f));
    }

    @Test
    public void leftIsUnknownWhenTheBookIsAlmostOver() {
        // under a minute left counts as listened (RemainingTime.DONE), not as a remainder
        assertEquals(-1, ContinueListening.leftSeconds(book(600), "Глава 1", 570, 1f));
    }
}
