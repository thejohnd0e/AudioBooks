package com.fanok.audiobooks.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * akniga's {@code hres}: the player script's {@code getHres} tries three passphrases in turn (its {@code assets3},
 * {@code assets} and the legacy {@code assets2}), and the site moved to the first one on 2026-11-26. The blob below is
 * a live {@code /ajax/b/114409} answer.
 */
public class AknigaTracksTest {

    @Test
    public void decryptsTheCurrentHresBlob() {
        String hres = "{\"ct\":\"oXXt4IrCEQOhU0xGux2Xt9IeZmRCMztSiJpH9tXeiG4F36w1l5CASJGRqGBcFkboO26MZkJ4lyYS"
                + "9OxkeG5fsgmu+zm5ZO5AvFAX5j/0r1dhNbCOxpStf8RDNXYwxQkj\",\"iv\":\"b7b66271d1ec99dd07573b4ac9681b87\","
                + "\"s\":\"ad4d599c00adede8\"}";
        String hls = Akniga.decryptHres(hres);
        assertTrue(hls, hls.startsWith("https://r1.akniga.club/b/114409/pl.m3u8"));
    }

    @Test
    public void stillDecryptsTheLegacyPassphrase() {
        String hres = CryptoJs.encrypt(new com.google.gson.JsonPrimitive("https://r1.akniga.club/b/1/pl.m3u8")
                .toString(), "EKxtcg46V");
        assertEquals("https://r1.akniga.club/b/1/pl.m3u8", Akniga.decryptHres(hres));
    }
}
