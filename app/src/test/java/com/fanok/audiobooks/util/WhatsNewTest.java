package com.fanok.audiobooks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** «Что нового»: which builds' notes are shown, in which order, and when nothing is. */
public class WhatsNewTest {

    private static final String SOURCE = "[146] 8.6.12\nТретье.\n\n[145] 8.6.11\nВторое.\nЕщё второе.\n\n"
            + "[144] 8.6.10\n• Первое.\n";

    @Test public void newerBuildsComeNewestFirst() {
        String text = WhatsNew.notes(SOURCE, 143, 146);
        assertEquals("Версия 8.6.12\n• Третье.\n\nВерсия 8.6.11\n• Второе.\n• Ещё второе.\n\nВерсия 8.6.10\n• Первое.",
                text);
    }

    @Test public void onlyWhatIsNewerThanSeenAndNotNewerThanCurrentIsShown() {
        assertEquals("Версия 8.6.11\n• Второе.\n• Ещё второе.", WhatsNew.notes(SOURCE, 144, 145));
        assertEquals("", WhatsNew.notes(SOURCE, 146, 146));
        assertEquals("", WhatsNew.notes(SOURCE, 100, 99));
    }

    @Test public void aHeadingWithoutAVersionNamesTheBuild() {
        assertEquals("Сборка 7\n• Что-то.", WhatsNew.notes("[7]\nЧто-то.\n", 0, 7));
    }

    @Test public void noMoreThanTheLastFewBuildsAreShown() {
        StringBuilder source = new StringBuilder();
        for (int build = 10; build >= 1; build--) {
            source.append('[').append(build).append("] v").append(build).append("\nИзменение ").append(build).append('\n');
        }
        String text = WhatsNew.notes(source.toString(), 0, 10);
        assertTrue(text.contains("Версия v10"));
        assertTrue(text.contains("Версия v6"));
        assertFalse(text.contains("Версия v5"));
    }

    @Test public void aNewInstallSeesNothingAndAnUpdateSeesWhatIsNewerThanTheBaseline() {
        assertEquals(150, WhatsNew.seenBuild(-1, 150, true));
        assertEquals(WhatsNew.BASELINE, WhatsNew.seenBuild(-1, 150, false));
        assertEquals(148, WhatsNew.seenBuild(148, 150, true));
        assertEquals(148, WhatsNew.seenBuild(148, 150, false));
    }

    @Test public void theBundledNotesStartWithTheCurrentVersion() throws Exception {
        java.nio.file.Path asset = java.nio.file.Paths.get("src/main/assets/whats_new.txt");
        String text = new String(java.nio.file.Files.readAllBytes(asset), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(WhatsNew.notes(text, WhatsNew.BASELINE - 1, Integer.MAX_VALUE).contains("Версия 8.6.9"));
        assertTrue(WhatsNew.notes(text, WhatsNew.BASELINE, Integer.MAX_VALUE).contains("Версия 8.6.10"));
        assertTrue(WhatsNew.notes(text, WhatsNew.BASELINE + 1, Integer.MAX_VALUE).startsWith("Версия 8.6.11"));
    }
}
