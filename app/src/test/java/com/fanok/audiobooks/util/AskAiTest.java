package com.fanok.audiobooks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The question about a book and the choice of the AI app: only the title and the author leave the app. */
public class AskAiTest {

    private static final String TEMPLATE = "О книге «%1$s», автор: %2$s.";

    @Test
    public void theQuestionHoldsTheTitleAndTheAuthor() {
        assertEquals("О книге «Дюна», автор: Герберт.", AskAi.question(TEMPLATE, " Дюна ", "Герберт"));
    }

    @Test
    public void anUnknownAuthorIsADash() {
        assertEquals("О книге «Дюна», автор: —.", AskAi.question(TEMPLATE, "Дюна", null));
        assertEquals("О книге «Дюна», автор: —.", AskAi.question(TEMPLATE, "Дюна", "  "));
    }

    @Test
    public void anUnknownAppIsTheDefault() {
        assertEquals(AskAi.App.CHATGPT, AskAi.App.parse(null));
        assertEquals(AskAi.App.CHATGPT, AskAi.App.parse("bard"));
        for (AskAi.App app : AskAi.App.values()) {
            assertEquals(app, AskAi.App.parse(app.id));
        }
    }

    @Test
    public void theSiteAddressCarriesTheEncodedQuestion() {
        String address = AskAi.webAddress(AskAi.App.CLAUDE, "О чём? А&Б");
        assertTrue(address, address.startsWith("https://claude.ai/new?q="));
        assertTrue(address, address.endsWith("%D0%9E+%D1%87%D1%91%D0%BC%3F+%D0%90%26%D0%91"));
    }
}
