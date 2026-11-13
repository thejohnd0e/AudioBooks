package com.fanok.audiobooks.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;
import org.junit.Test;

/** What counts as a TV: only the television UI mode; a phone, a tablet or a car never does. */
public class TvTest {

    @Test public void televisionModeIsATv() {
        assertTrue(Tv.isTelevisionMode(Configuration.UI_MODE_TYPE_TELEVISION));
    }

    @Test public void otherModesAreNotATv() {
        int[] others = {Configuration.UI_MODE_TYPE_NORMAL, Configuration.UI_MODE_TYPE_DESK,
                Configuration.UI_MODE_TYPE_CAR, Configuration.UI_MODE_TYPE_APPLIANCE,
                Configuration.UI_MODE_TYPE_WATCH, Configuration.UI_MODE_TYPE_VR_HEADSET,
                Configuration.UI_MODE_TYPE_UNDEFINED};
        for (int mode : others) {
            assertFalse("mode " + mode, Tv.isTelevisionMode(mode));
        }
    }
}
