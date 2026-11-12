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
}
