package com.fanok.audiobooks.service;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Every sound of the sleep timer's warning can be played and stopped on a real device, a missing or broken picked sound
 * falls back to the beep instead of failing, and a sound chosen is the one the warning uses.
 */
@RunWith(AndroidJUnit4.class)
public class SleepSoundPlayerTest {

    private Context context;
    private SharedPreferences prefs;
    private SleepSoundPlayer player;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> player = new SleepSoundPlayer(context, new Handler(Looper.getMainLooper())));
    }

    @After
    public void tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> player.stop());
        prefs.edit().remove(SleepSound.PREF).remove(SleepSound.PREF_URI).apply();
    }

    @Test
    public void everyKindPlaysAndStopsWithoutFailing() throws Exception {
        for (SleepSound.Kind kind : SleepSound.Kind.values()) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                player.preview(kind, null);
                player.stop();
            });
        }
    }

    @Test
    public void aBrokenPickedSoundFallsBackWithoutFailing() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            player.preview(SleepSound.Kind.CUSTOM, Uri.parse("content://nothing.here/sound/1"));
            player.stop();
            player.preview(SleepSound.Kind.CUSTOM, null);
            player.stop();
        });
    }

    @Test
    public void theBurstPlaysTheChosenSoundAndTheChoiceIsKept() {
        prefs.edit().putString(SleepSound.PREF, SleepSound.Kind.SOFT.id).apply();
        assertEquals(SleepSound.Kind.SOFT, SleepSound.effective(prefs));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            player.play(4);
            player.stop();
        });
        // a picked sound without an address is the beep, not a failure
        prefs.edit().putString(SleepSound.PREF, SleepSound.Kind.CUSTOM.id).remove(SleepSound.PREF_URI).apply();
        assertEquals(SleepSound.Kind.BEEP, SleepSound.effective(prefs));
    }
}
