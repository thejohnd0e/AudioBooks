package com.fanok.audiobooks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The decisions behind the TV's focus ring: its colour, its inset and its shape (the drawing itself is on a TV). */
public class TvFocusTest {

    private static final float DENSITY = 2f; // a 1280x720 TV is about 213 dpi
    private static final float COMMON = 12 * DENSITY;

    @Test public void ringIsWhiteOnADarkThemeAndBlackOnALightOne() {
        assertEquals(0xFFFFFFFF, TvFocus.ringColor(true));
        assertEquals(0xFF000000, TvFocus.ringColor(false));
        assertTrue(TvFocus.isDark(0xFF14161A));
        assertTrue(TvFocus.isDark(0xFF000000));
        assertFalse(TvFocus.isDark(0xFFFFFFFF));
        assertFalse(TvFocus.isDark(0xFFF2F2F2));
    }

    @Test public void rowFillIsAVeilOfTheRingColour() {
        assertEquals(0x22FFFFFF, TvFocus.fillColor(true));
        assertEquals(0x22000000, TvFocus.fillColor(false));
        assertNotEquals(TvFocus.fillColor(true), TvFocus.fillColor(false));
    }

    @Test public void strokeSitsInsideTheView() {
        assertEquals(1, TvFocus.ringInset(2f));
        assertEquals(1, TvFocus.ringInset(1f));
        assertEquals(2, TvFocus.ringInset(3f));
    }

    @Test public void pillButtonKeepsItsOwnRadiusUpToHalfItsHeight() {
        float half = 50 / 2f - 1f * DENSITY / 2f;
        assertEquals(half, TvFocus.ringRadius(true, 100f, false, 900, 50, DENSITY), 0.001f);
        assertEquals(10f, TvFocus.ringRadius(true, 10f, false, 900, 50, DENSITY), 0.001f);
    }

    @Test public void buttonWithoutARadiusGetsTheCommonOne() {
        assertEquals(COMMON, TvFocus.ringRadius(true, 0f, false, 400, 200, DENSITY), 0.001f);
    }

    @Test public void roundIconButtonIsACircle() {
        float half = 44 / 2f - 1f * DENSITY / 2f;
        assertEquals(half, TvFocus.ringRadius(false, 0f, true, 44, 44, DENSITY), 0.001f);
        // not quite square: still a circle
        assertEquals(half, TvFocus.ringRadius(false, 0f, true, 44, 46, DENSITY), 0.001f);
    }

    @Test public void largeOrNonSquareImagesGetTheCommonRadius() {
        assertEquals(COMMON, TvFocus.ringRadius(false, 0f, true, 300, 300, DENSITY), 0.001f); // a cover
        assertEquals(COMMON, TvFocus.ringRadius(false, 0f, true, 60, 30, DENSITY), 0.001f);   // not round
        assertEquals(COMMON, TvFocus.ringRadius(false, 0f, false, 44, 44, DENSITY), 0.001f);  // not an image
    }

    @Test public void buttonRadiusNeverExceedsHalfOfATinyView() {
        float half = 10 / 2f - 1f * DENSITY / 2f;
        assertEquals(half, TvFocus.ringRadius(true, 0f, false, 10, 10, DENSITY), 0.001f);
    }
}
