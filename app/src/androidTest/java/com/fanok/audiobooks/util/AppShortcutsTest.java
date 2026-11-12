package com.fanok.audiobooks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.fanok.audiobooks.Consts;
import com.fanok.audiobooks.activity.MainActivity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** The icon's shortcuts are published with intents that open this app's main screen on the right section. */
@RunWith(AndroidJUnit4.class)
public class AppShortcutsTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("app_shortcuts_version").apply();
        ShortcutManagerCompat.removeAllDynamicShortcuts(context);
    }

    @Test
    public void theShortcutsAreMadeWithIntentsToTheSections() {
        AppShortcuts.install(context);
        List<ShortcutInfoCompat> shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context);
        Map<String, Intent> byId = new HashMap<>();
        for (ShortcutInfoCompat shortcut : shortcuts) {
            byId.put(shortcut.getId(), shortcut.getIntent());
        }
        assertEquals(3, byId.size());
        assertSection(byId.get("continue"), Consts.FRAGMENT_HOME);
        assertSection(byId.get("history"), Consts.FRAGMENT_HISTORY);
        assertSection(byId.get("saved"), Consts.FRAGMENT_SAVED);
    }

    @Test
    public void installingAgainDoesNotPublishTwice() {
        AppShortcuts.install(context);
        ShortcutManagerCompat.removeAllDynamicShortcuts(context);
        AppShortcuts.install(context); // this version is published already: nothing is made again
        assertTrue(ShortcutManagerCompat.getDynamicShortcuts(context).isEmpty());
    }

    private void assertSection(Intent intent, int section) {
        assertNotNull(intent);
        assertEquals(context.getPackageName(), intent.getComponent().getPackageName());
        assertEquals(MainActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(section, intent.getIntExtra("startFragment", -1));
    }
}
