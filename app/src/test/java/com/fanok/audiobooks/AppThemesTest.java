package com.fanok.audiobooks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Tests for theme and palette validation in {@link AppThemes}. */
public class AppThemesTest {

    @Test
    public void validPalettesArePreserved() {
        assertEquals(AppThemes.PALETTE_SAGE, AppThemes.normalizePalette("sage"));
        assertEquals(AppThemes.PALETTE_UMBRA, AppThemes.normalizePalette("umbra"));
        assertEquals(AppThemes.PALETTE_STEEL, AppThemes.normalizePalette("steel"));
        assertEquals(AppThemes.PALETTE_ONYX, AppThemes.normalizePalette("onyx"));
        assertEquals(AppThemes.PALETTE_CUSTOM, AppThemes.normalizePalette("custom"));
    }

    @Test
    public void hexParsingAndFormatting() {
        assertEquals("#161b18", AppThemes.formatHex(0xFF161B18));
        assertEquals(0xFF161B18, AppThemes.parseHex("#161b18", 0));
        assertEquals(0xFF161B18, AppThemes.parseHex("161B18", 0));
        assertEquals(0xFFFFFFFF, AppThemes.parseHex("invalid", 0xFFFFFFFF));
        assertEquals(0xFFFFFFFF, AppThemes.parseHex(null, 0xFFFFFFFF));

        assertEquals(Integer.valueOf(0xFFFFFFFF), AppThemes.parseHexColor("#FFFFFF"));
        assertEquals(Integer.valueOf(0xFFFFFFFF), AppThemes.parseHexColor("ffffff"));
        assertEquals(Integer.valueOf(0xFF000000), AppThemes.parseHexColor("#000000"));
        org.junit.Assert.assertNull(AppThemes.parseHexColor("invalid"));
        org.junit.Assert.assertNull(AppThemes.parseHexColor(null));
    }

    @Test
    public void unknownOrNullPaletteDefaultsToSage() {
        assertEquals(AppThemes.PALETTE_SAGE, AppThemes.normalizePalette(null));
        assertEquals(AppThemes.PALETTE_SAGE, AppThemes.normalizePalette(""));
        assertEquals(AppThemes.PALETTE_SAGE, AppThemes.normalizePalette("neon_pink"));
        assertEquals(AppThemes.PALETTE_SAGE, AppThemes.normalizePalette("random"));
    }
}
