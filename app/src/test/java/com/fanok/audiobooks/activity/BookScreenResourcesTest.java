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

    @Test public void expandedPlayersIncludePlaybackActions() throws Exception {
        for (String path : new String[] {"layout/player.xml", "layout-sw600dp-land/player.xml", "layout-television/player.xml"}) {
            Document doc = resource(path);
            NodeList includes = doc.getElementsByTagName("include");
            boolean found = false;
            for (int i = 0; i < includes.getLength(); i++) {
                found |= "@layout/player_actions".equals(((Element) includes.item(i)).getAttribute("layout"));
            }
            assertTrue(path + " lacks expanded player actions", found);
            NodeList texts = doc.getElementsByTagName("TextView");
            for (int i = 0; i < texts.getLength(); i++) {
                assertFalse(path + " still duplicates @id/speed next to seekBar",
                        "@+id/speed".equals(((Element) texts.item(i)).getAttribute("android:id")));
            }
        }
        Document actions = resource("layout/player_actions.xml");
        assertEquals("horizontal", actions.getDocumentElement().getAttribute("android:orientation"));
        NodeList buttons = actions.getElementsByTagName("com.google.android.material.button.MaterialButton");
        assertEquals(5, buttons.getLength()); // speed, timer, equalizer, car mode, bookmark
        for (int i = 0; i < buttons.getLength(); i++) {
            Element btn = (Element) buttons.item(i);
            assertEquals("@style/Widget.MaterialComponents.Button.OutlinedButton", btn.getAttribute("style"));
            assertTrue(btn.getAttribute("app:cornerRadius").endsWith("dp"));
            assertTrue("Player action button lacks icon", btn.getAttribute("app:icon").startsWith("@drawable/"));
        }
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

    @Test public void dialogAndCarModeStylesUsePaletteAttributes() throws Exception {
        NodeList styles = resource("values/styles.xml").getElementsByTagName("style");
        boolean foundNeutral = false;
        for (int i = 0; i < styles.getLength(); i++) {
            Element style = (Element) styles.item(i);
            if ("ThemeOverlay.AudioBooks.Dialog.Alert".equals(style.getAttribute("name"))) {
                NodeList items = style.getElementsByTagName("item");
                for (int j = 0; j < items.getLength(); j++) {
                    if ("buttonBarNeutralButtonStyle".equals(((Element) items.item(j)).getAttribute("name"))) {
                        foundNeutral = true;
                    }
                }
            }
        }
        assertTrue("Dialog alert theme lacks neutral button style", foundNeutral);

        boolean foundPopupSurface = false;
        for (int i = 0; i < styles.getLength(); i++) {
            Element style = (Element) styles.item(i);
            if ("AppTheme.PopupOverlay".equals(style.getAttribute("name"))) {
                NodeList items = style.getElementsByTagName("item");
                for (int j = 0; j < items.getLength(); j++) {
                    Element item = (Element) items.item(j);
                    if ("android:popupBackground".equals(item.getAttribute("name"))
                            && "@drawable/bg_popup_menu".equals(item.getTextContent())) {
                        foundPopupSurface = true;
                    }
                }
            }
        }
        assertTrue("PopupOverlay lacks palette popup background", foundPopupSurface);
        assertTrue(new File("src/main/res/drawable/bg_popup_menu.xml").exists());

        NodeList carStyles = resource("values/styles_car_mode.xml").getElementsByTagName("style");
        for (int i = 0; i < carStyles.getLength(); i++) {
            Element style = (Element) carStyles.item(i);
            if ("AppTheme.CarMode".equals(style.getAttribute("name"))) {
                NodeList items = style.getElementsByTagName("item");
                for (int j = 0; j < items.getLength(); j++) {
                    Element item = (Element) items.item(j);
                    if ("carBackground".equals(item.getAttribute("name"))) {
                        assertEquals("?attr/colorBackgroundMain", item.getTextContent());
                    } else if ("carAccent".equals(item.getAttribute("name"))) {
                        assertEquals("?attr/colorAccentPrimary", item.getTextContent());
                    }
                }
            }
        }
    }

    @Test public void allThemedActivitiesBindActivePalette() throws Exception {
        String[] activities = {
            "MainActivity.java", "BookActivity.java", "SearchableActivity.java",
            "SleepTimerActivity.java", "CarModeActivity.java", "ClearSavedActivity.java",
            "SourceActivity.java", "NarratorActivity.java", "ParentalControlActivity.java",
            "ActivityImport.java", "ActivitySendEmail.java", "ActivityCookies.java",
            "ForumLoginActivity.java", "LoadBook.java"
        };
        for (String name : activities) {
            File file = new File("src/main/java/com/fanok/audiobooks/activity/" + name);
            String source = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(name + " lacks AppThemes.apply", source.contains("AppThemes.apply("));
            assertTrue(name + " lacks PaletteBinder.bind", source.contains("PaletteBinder.bind("));
        }
    }
}
