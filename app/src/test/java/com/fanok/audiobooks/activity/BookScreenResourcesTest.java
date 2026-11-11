package com.fanok.audiobooks.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class BookScreenResourcesTest {
    private Document resource(String path) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("src/main/res/" + path));
    }

    @Test public void bookActionsAreConfiguredForOverflow() throws Exception {
        NodeList items = resource("menu/book_activity_options_menu.xml").getElementsByTagName("item");
        assertTrue(items.getLength() > 0);
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            assertEquals(item.getAttribute("android:id"), "never", item.getAttribute("app:showAsAction"));
        }
    }

    @Test public void collapsedPlayerHasMoreVerticalRoom() throws Exception {
        NodeList dimensions = resource("values/dimens.xml").getElementsByTagName("dimen");
        for (int i = 0; i < dimensions.getLength(); i++) {
            Element dimen = (Element) dimensions.item(i);
            if ("player_row_height_redesign".equals(dimen.getAttribute("name"))) {
                assertTrue(Integer.parseInt(dimen.getTextContent().replace("dp", "")) > 68);
                return;
            }
        }
        throw new AssertionError("collapsed player dimension missing");
    }

    @Test public void preferencesKeepOnlyToolbarTitleWithoutDeadIconToggles() throws Exception {
        NodeList screens = resource("xml/preferences.xml").getElementsByTagName("PreferenceScreen");
        for (int i = 0; i < screens.getLength(); i++) {
            assertFalse("book_toolbar".equals(((Element) screens.item(i)).getAttribute("android:key")));
        }
        NodeList checks = resource("xml/preferences.xml").getElementsByTagName("CheckBoxPreference");
        boolean hasTitle = false;
        for (int i = 0; i < checks.getLength(); i++) {
            String key = ((Element) checks.item(i)).getAttribute("android:key");
            if ("toolbar_title".equals(key)) hasTitle = true;
            else assertFalse(key, key.startsWith("toolbar_"));
        }
        assertTrue(hasTitle);
    }

    @Test public void collapsedPlayerTitleIdentifiesActiveBook() {
        assertEquals("Книга А · Глава 01",
                BookActivity.collapsedPlayerTitle("Книга А", "http://a", "Книга Б", "http://b", "Глава 01"));
        assertEquals("Глава 01",
                BookActivity.collapsedPlayerTitle("Книга А", "http://a", "Книга А", "http://a", "Глава 01"));
        assertNull(BookActivity.collapsedPlayerTitle(null, null, "Книга А", "http://a", null));
    }

    @Test public void descriptionLayoutsIncludeActionButtons() throws Exception {
        for (String layout : new String[] {
                "layout/fragment_book_description.xml",
                "layout-land/fragment_book_description.xml",
                "layout-television/fragment_book_description.xml"}) {
            NodeList buttons = resource(layout).getElementsByTagName("com.google.android.material.button.MaterialButton");
            assertEquals(layout, 3, buttons.getLength());
            for (int i = 0; i < buttons.getLength(); i++) {
                Element button = (Element) buttons.item(i);
                assertEquals(layout, "1", button.getAttribute("android:maxLines"));
                assertEquals(layout, "end", button.getAttribute("android:ellipsize"));
            }
        }
    }
}
