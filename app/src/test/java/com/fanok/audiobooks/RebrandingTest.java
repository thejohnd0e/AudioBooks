package com.fanok.audiobooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class RebrandingTest {

    @Test
    public void appNameIsABooksPlus() throws Exception {
        File file = new File("src/main/res/values/strings.xml");
        if (!file.exists()) {
            file = new File("app/src/main/res/values/strings.xml");
        }
        assertTrue("strings.xml must exist", file.exists());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);

        NodeList strings = doc.getElementsByTagName("string");
        String appName = null;
        for (int i = 0; i < strings.getLength(); i++) {
            Element el = (Element) strings.item(i);
            if ("app_name".equals(el.getAttribute("name"))) {
                appName = el.getTextContent();
                break;
            }
        }

        assertNotNull("app_name resource must be present", appName);
        assertEquals("ABooks++", appName);
    }

    @Test
    public void adaptiveIconLayersAreValid() throws Exception {
        File fgFile = new File("src/main/res/drawable/ic_launcher_foreground.xml");
        if (!fgFile.exists()) fgFile = new File("app/src/main/res/drawable/ic_launcher_foreground.xml");
        assertTrue("ic_launcher_foreground.xml must exist", fgFile.exists());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(fgFile);
        Element root = doc.getDocumentElement();
        assertEquals("vector", root.getTagName());
        assertEquals("108", root.getAttribute("android:viewportWidth"));
        assertEquals("108", root.getAttribute("android:viewportHeight"));

        File bgFile = new File("src/main/res/values/ic_launcher_background.xml");
        if (!bgFile.exists()) bgFile = new File("app/src/main/res/values/ic_launcher_background.xml");
        assertTrue("ic_launcher_background.xml must exist", bgFile.exists());
        Document bgDoc = factory.newDocumentBuilder().parse(bgFile);
        NodeList colors = bgDoc.getElementsByTagName("color");
        assertTrue("ic_launcher_background color must be defined", colors.getLength() > 0);
        assertEquals("#18191C", colors.item(0).getTextContent().trim());
    }

    @Test
    public void legacyMipmapsExistAndAreNonEmpty() {
        String base = new File("src/main/res").exists() ? "src/main/res" : "app/src/main/res";
        String[] densities = {"mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"};
        for (String density : densities) {
            File sq = new File(base, density + "/ic_launcher.png");
            File rnd = new File(base, density + "/ic_launcher_round.png");
            assertTrue(sq.getPath() + " must exist", sq.exists());
            assertTrue(sq.getPath() + " must not be empty", sq.length() > 0);
            assertTrue(rnd.getPath() + " must exist", rnd.exists());
            assertTrue(rnd.getPath() + " must not be empty", rnd.length() > 0);
        }
    }
}
