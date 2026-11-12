package com.fanok.audiobooks.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

/**
 * Plays the sleep timer's warning in the sound the user chose ({@link SleepSound}). A burst of {@code count} beats: a
 * tone sounds on every beat, a sound of the phone's (or one picked) once, cut when the burst ends. The same player
 * does the preview on the timer screen. One sound at a time: a new one stops the old.
 */
public final class SleepSoundPlayer {

    private static final String TAG = "SleepSoundPlayer";

    /** Between the beats of a burst; the service ducks the book for {@code count * PERIOD_MS}. */
    public static final long PERIOD_MS = 500;
    private static final int BEEP_MS = 300;
    /** A long melody is cut at this, however many beats the burst has. */
    private static final long SOUND_MAX_MS = 3_000;

    private final Context context;
    private final Handler handler;
    private final SharedPreferences prefs;
    @Nullable
    private ToneGenerator tone;
    @Nullable
    private Ringtone ringtone;
    @Nullable
    private Runnable cut;

    public SleepSoundPlayer(@NonNull Context context, @NonNull Handler handler) {
        this.context = context.getApplicationContext();
        this.handler = handler;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    /** A burst of {@code count} beats in the chosen sound. */
    public void play(int count) {
        play(SleepSound.effective(prefs), SleepSound.customUri(prefs), count);
    }

    /** The sound of {@code kind} as a short preview, before it is chosen. */
    public void preview(@NonNull SleepSound.Kind kind, @Nullable Uri custom) {
        play(kind == SleepSound.Kind.CUSTOM && custom == null ? SleepSound.Kind.BEEP : kind, custom, 2);
    }

    private void play(@NonNull SleepSound.Kind kind, @Nullable Uri custom, int count) {
        stop();
        switch (kind) {
            case SOFT:
                tones(ToneGenerator.TONE_PROP_PROMPT, 70, count);
                break;
            case NOTIFICATION:
                sound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), count);
                break;
            case ALARM:
                sound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), count);
                break;
            case CUSTOM:
                sound(custom, count);
                break;
            case BEEP:
            default:
                //a DTMF tone lasts as long as asked and stands out of speech: 941 + 1633 Hz, at full tone volume
                tones(ToneGenerator.TONE_DTMF_D, ToneGenerator.MAX_VOLUME, count);
        }
    }

    private void tones(int toneType, int volume, int count) {
        final ToneGenerator generator;
        try {
            generator = new ToneGenerator(AudioManager.STREAM_MUSIC, volume);
        } catch (RuntimeException e) {
            Log.w(TAG, "cannot beep", e);
            return;
        }
        tone = generator;
        for (int i = 0; i < count; i++) {
            handler.postDelayed(() -> {
                if (tone == generator) {
                    generator.startTone(toneType, BEEP_MS);
                }
            }, i * PERIOD_MS);
        }
        schedule(() -> {
            if (tone == generator) {
                stop();
            }
        }, count * PERIOD_MS);
    }

    /** A sound of the system or the user's, once; the beep when it cannot be played. */
    private void sound(@Nullable Uri uri, int count) {
        Ringtone found = null;
        try {
            found = uri == null ? null : RingtoneManager.getRingtone(context, uri);
        } catch (RuntimeException e) {
            Log.w(TAG, "cannot open the sound " + uri, e);
        }
        if (found == null) {
            tones(ToneGenerator.TONE_DTMF_D, ToneGenerator.MAX_VOLUME, count);
            return;
        }
        final Ringtone sound = found;
        try {
            //as media, like the book: the burst ducks it, and the volume the listener set is the one that is heard
            sound.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            sound.play();
        } catch (RuntimeException e) {
            Log.w(TAG, "cannot play the sound " + uri, e);
            tones(ToneGenerator.TONE_DTMF_D, ToneGenerator.MAX_VOLUME, count);
            return;
        }
        ringtone = sound;
        schedule(() -> {
            if (ringtone == sound) {
                stop();
            }
        }, Math.min(SOUND_MAX_MS, count * PERIOD_MS));
    }

    private void schedule(@NonNull Runnable task, long delayMs) {
        cut = task;
        handler.postDelayed(task, delayMs);
    }

    /** Silences what sounds and forgets the beats still to come. */
    public void stop() {
        if (cut != null) {
            handler.removeCallbacks(cut);
            cut = null;
        }
        ToneGenerator generator = tone;
        tone = null; // the beats posted for it see this and do not sound
        if (generator != null) {
            try {
                generator.release();
            } catch (RuntimeException e) {
                Log.w(TAG, "cannot release the beep", e);
            }
        }
        Ringtone sound = ringtone;
        ringtone = null;
        if (sound != null) {
            try {
                sound.stop();
            } catch (RuntimeException e) {
                Log.w(TAG, "cannot stop the sound", e);
            }
        }
    }
}
