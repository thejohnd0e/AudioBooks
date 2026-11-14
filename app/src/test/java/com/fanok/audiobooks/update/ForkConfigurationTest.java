package com.fanok.audiobooks.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

public class ForkConfigurationTest {
    private String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test public void releasesComeFromFork() {
        assertEquals("https://github.com/thejohnd0e/AudioBooks/releases/latest", UpdateSources.GITHUB_LATEST);
    }

    @Test public void updaterDoesNotRouteToOriginalSources() throws Exception {
        String source = read("src/main/java/com/fanok/audiobooks/update/Updater.java");
        assertFalse(source.contains("sites.add(Site.MIRROR)"));
        assertFalse(source.contains("sites.add(Site.DISK)"));
        assertFalse(source.contains("sites.add(Site.FORUM)"));
        assertFalse(source.contains("UpdateSources.routeOrder("));
        assertTrue(source.contains("sites.add(Site.GITHUB)"));
    }

    @Test public void syncKeepsGoogleButNotYandex() throws Exception {
        String sync = read("src/main/java/com/fanok/audiobooks/sync/CloudSync.java");
        assertTrue(sync.contains("new GoogleDrive.Service()"));
        assertFalse(sync.contains("new YandexDisk.Service()"));
    }

    @Test public void releasePackageMatchesPreviouslyDistributedApk() throws Exception {
        assertTrue(read("build.gradle").contains("applicationId \"link.intertop.audiobooks\""));
    }

    @Test public void aboutShowsOnlyForkLinks() throws Exception {
        String about = read("src/main/res/xml/about_preferences.xml");
        assertFalse(about.contains("android:key=\"open_4pda\""));
        assertFalse(about.contains("android:key=\"open_telegram\""));
        assertFalse(about.contains("android:key=\"write_autor\""));
        assertTrue(read("src/main/java/com/fanok/audiobooks/fragment/AboutFragment.java")
                .contains("https://github.com/thejohnd0e/AudioBooks"));
        assertFalse(read("src/main/java/com/fanok/audiobooks/fragment/AboutFragment.java")
                .contains("[by thejohnd0e]"));
        assertTrue(read("src/main/res/values/strings.xml").contains("android-dev@intertop.link"));
    }
}
