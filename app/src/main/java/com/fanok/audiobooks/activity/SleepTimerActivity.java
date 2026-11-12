package com.fanok.audiobooks.activity;

import com.fanok.audiobooks.FontScale;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.Nullable;
import com.fanok.audiobooks.service.SleepSound;
import com.fanok.audiobooks.service.SleepSoundPlayer;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.arellomobile.mvp.MvpAppCompatActivity;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.fanok.audiobooks.Hourglass;
import com.fanok.audiobooks.R;
import com.fanok.audiobooks.interface_pacatge.sleep_timer.SleepTimerView;
import com.fanok.audiobooks.presenter.SleepTimerPresenter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.r0adkll.slidr.Slidr;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class SleepTimerActivity extends MvpAppCompatActivity implements SleepTimerView {

    /** The screen at the chosen font scale (FontScale). */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(FontScale.wrap(newBase));
    }

    /** From the player service. Extras: "time" (remaining ms), "endAt" (epoch ms, 0 for end-of-chapter),
     * "paused" (boolean), "mode" ({@code SleepTimerPresenter.MODE_*}). */
    public static final String bradcast_UpdateTimer = "updateTimer";

    /** From the player service: the timer fired or was cancelled. */
    public static final String bradcast_FinishTimer = "finishTimer";

    /** To the player service: reply with {@link #bradcast_UpdateTimer}. */
    public static final String Broadcast_UPDATE_TIMER_START = "updateTimerStart";

    private static final String CLOCK_PICKER = "sleepTimerClock";

    @InjectPresenter
    SleepTimerPresenter mPresenter;

    private View mSetup;
    private View mRunning;
    private View mCountdown;
    private View mClock;
    private MaterialButtonToggleGroup mMode;
    private NumberPicker mHours;
    private NumberPicker mMinutes;
    private ChipGroup mPresets;
    private TextView mClockTime;
    private TextView mClockCaption;
    private SwitchMaterial mWarn;
    private SwitchMaterial mShake;
    /** The name of the sound chosen for the warning, under its row. */
    private TextView mWarnSoundName;
    /** Plays the sounds on offer as the listener goes through them. */
    private SleepSoundPlayer mPreview;
    private final androidx.activity.result.ActivityResultLauncher<Intent> mSoundPicker = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                Uri picked = data == null ? null
                        : androidx.core.content.IntentCompat.getParcelableExtra(data,
                                RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
                if (result.getResultCode() == RESULT_OK && picked != null) {
                    PreferenceManager.getDefaultSharedPreferences(this).edit()
                            .putString(SleepSound.PREF, SleepSound.Kind.CUSTOM.id)
                            .putString(SleepSound.PREF_URI, picked.toString()).apply();
                    previewSound(SleepSound.Kind.CUSTOM, picked);
                    bindWarnSound();
                }
            });
    private View mStart;
    private TextView mRemaining;
    private TextView mEndCaption;
    private View mPausedCaption;
    private View mExtend;
    private View mCancel;

    /** True while the presenter's state is written into the views: their listeners must not echo it back. */
    private boolean binding;
    private int clockHour;
    private int clockMinute;

    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Keeps "fires in" current while the screen is visible. */
    private final Runnable minuteTick = new Runnable() {
        @Override
        public void run() {
            updateClockCaption();
            handler.postDelayed(this, 60_000 - System.currentTimeMillis() % 60_000);
        }
    };

    private BroadcastReceiver updateTimerBroadcast;

    private BroadcastReceiver finihTimer;

    @ProvidePresenter
    SleepTimerPresenter provideSleepTimerPresenter() {
        return new SleepTimerPresenter(getApplicationContext());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.fanok.audiobooks.AppThemes.apply(this, R.style.AppTheme_SwipeOnClose, R.style.LightAppTheme_SwipeOnClose,
                R.style.AppThemeBlack_SwipeOnClose);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sleep_timer);

        mSetup = findViewById(R.id.setup);
        mRunning = findViewById(R.id.running);
        mCountdown = findViewById(R.id.countdown);
        mClock = findViewById(R.id.clock);
        mMode = findViewById(R.id.mode);
        mHours = findViewById(R.id.hours);
        mMinutes = findViewById(R.id.minutes);
        mPresets = findViewById(R.id.presets);
        mClockTime = findViewById(R.id.clockTime);
        mClockCaption = findViewById(R.id.clockCaption);
        mWarn = findViewById(R.id.warn);
        mShake = findViewById(R.id.shake);
        mWarnSoundName = findViewById(R.id.warnSoundName);
        mStart = findViewById(R.id.start);
        mRemaining = findViewById(R.id.remaining);
        mEndCaption = findViewById(R.id.endCaption);
        mPausedCaption = findViewById(R.id.pausedCaption);
        mExtend = findViewById(R.id.extend);
        mCancel = findViewById(R.id.cancel);

        Slidr.attach(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        com.fanok.audiobooks.PaletteBinder.bind(this);
        stylePaletteControls();
        initButtons();
        SensorManager sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensors == null || sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            findViewById(R.id.shakeRow).setVisibility(View.GONE);
        }
        Fragment picker = getSupportFragmentManager().findFragmentByTag(CLOCK_PICKER);
        if (picker instanceof MaterialTimePicker) {
            //recreated with the screen: its listener was not
            listenToClockPicker((MaterialTimePicker) picker);
        }
        registerdBradcast();
    }

    private void initButtons() {
        mMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && !binding) {
                mPresenter.setMode(checkedId == R.id.modeClock ? SleepTimerPresenter.MODE_CLOCK
                        : SleepTimerPresenter.MODE_COUNTDOWN);
            }
        });
        initPicker(mHours, 23);
        initPicker(mMinutes, 59);
        for (int i = 0; i < mPresets.getChildCount(); i++) {
            mPresets.getChildAt(i).setOnClickListener(view -> {
                int total = Integer.parseInt(String.valueOf(view.getTag()));
                mPresenter.setCountdown(total / 60, total % 60);
            });
        }
        mClockTime.setOnClickListener(view -> showClockPicker());
        findViewById(R.id.endOfChapter).setOnClickListener(view -> mPresenter.endOfChapter());
        mWarn.setOnCheckedChangeListener((view, checked) -> {
            if (!binding) {
                mPresenter.setWarn(checked);
            }
        });
        mShake.setOnCheckedChangeListener((view, checked) -> {
            if (!binding) {
                mPresenter.setShake(checked);
            }
        });
        findViewById(R.id.warnSoundRow).setOnClickListener(view -> showSoundChooser());
        bindWarnSound();
        mStart.setOnClickListener(view -> mPresenter.start());
        mExtend.setOnClickListener(view -> mPresenter.extend());
        mCancel.setOnClickListener(view -> mPresenter.cancel());
    }

    /** The chosen sound's name under the row: the kind's, or the picked sound's own title. */
    private void bindWarnSound() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SleepSound.Kind kind = SleepSound.effective(prefs);
        String name = getString(kind.title);
        Uri custom = SleepSound.customUri(prefs);
        if (kind == SleepSound.Kind.CUSTOM && custom != null) {
            try {
                Ringtone ringtone = RingtoneManager.getRingtone(this, custom);
                if (ringtone != null) {
                    name = getString(R.string.sleep_sound_custom_named, ringtone.getTitle(this));
                }
            } catch (RuntimeException e) {
                // the sound's title is a nicety: the row reads "Своя мелодия…" without it
            }
        }
        mWarnSoundName.setText(name);
    }

    private void previewSound(SleepSound.Kind kind, @Nullable Uri custom) {
        if (mPreview == null) {
            mPreview = new SleepSoundPlayer(this, new Handler(Looper.getMainLooper()));
        }
        mPreview.preview(kind, custom);
    }

    /** The sounds on offer, each heard as it is chosen; "Своя мелодия…" opens the system's list of sounds. */
    private void showSoundChooser() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SleepSound.Kind[] kinds = SleepSound.Kind.values();
        CharSequence[] names = new CharSequence[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            names[i] = getString(kinds[i].title);
        }
        int checked = SleepSound.effective(prefs).ordinal();
        new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(this)
                .setTitle(R.string.sleep_sound_title)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    SleepSound.Kind kind = kinds[which];
                    if (kind == SleepSound.Kind.CUSTOM) {
                        Uri existing = SleepSound.customUri(prefs);
                        Intent picker = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                                        getString(R.string.sleep_sound_picker_title))
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
                        dialog.dismiss();
                        try {
                            mSoundPicker.launch(picker);
                        } catch (RuntimeException e) {
                            Log.w("SleepTimerActivity", "no sound picker", e);
                        }
                        return;
                    }
                    prefs.edit().putString(SleepSound.PREF, kind.id).apply();
                    previewSound(kind, null);
                    bindWarnSound();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.close, null)
                .show();
    }

    @Override
    protected void onStop() {
        if (mPreview != null) {
            mPreview.stop();
        }
        super.onStop();
    }

    private void stylePaletteControls() {
        int accent = com.fanok.audiobooks.PaletteBinder.accent(this);
        int bg = com.fanok.audiobooks.PaletteBinder.background(this);
        int surface = com.fanok.audiobooks.PaletteBinder.surface(this);
        int text = com.google.android.material.color.MaterialColors.getColor(
                this, R.attr.colorPrimaryText, android.graphics.Color.WHITE);
        android.content.res.ColorStateList accentList = android.content.res.ColorStateList.valueOf(accent);
        int[][] states = new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}};
        for (int id : new int[] {R.id.modeCountdown, R.id.modeClock}) {
            View v = findViewById(id);
            if (v instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton b =
                        (com.google.android.material.button.MaterialButton) v;
                b.setBackgroundTintList(new android.content.res.ColorStateList(
                        states, new int[] {(accent & 0x00FFFFFF) | 0x33000000, surface}));
                b.setStrokeColor(new android.content.res.ColorStateList(
                        states, new int[] {accent, 0x55FFFFFF}));
                b.setTextColor(new android.content.res.ColorStateList(
                        states, new int[] {accent, text}));
            }
        }
        if (mStart instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton startBtn =
                    (com.google.android.material.button.MaterialButton) mStart;
            startBtn.setBackgroundTintList(accentList);
            startBtn.setTextColor(bg);
        }
        for (int id : new int[] {R.id.endOfChapter, R.id.cancel}) {
            View v = findViewById(id);
            if (v instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton b =
                        (com.google.android.material.button.MaterialButton) v;
                b.setStrokeColor(accentList);
                b.setTextColor(accent);
            }
        }
        if (mExtend instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) mExtend).setTextColor(accent);
        }
        if (mClockTime != null) {
            mClockTime.setTextColor(accent);
        }
        if (mRemaining != null) {
            mRemaining.setTextColor(accent);
        }
    }

    private void initPicker(@NonNull NumberPicker picker, int max) {
        picker.setMinValue(0);
        picker.setMaxValue(max);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            picker.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28,
                    getResources().getDisplayMetrics()));
            picker.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                    this, R.attr.colorPrimaryText, android.graphics.Color.WHITE));
        }
        picker.setOnValueChangedListener((view, oldValue, newValue) -> {
            if (!binding) {
                mPresenter.setCountdown(mHours.getValue(), mMinutes.getValue());
            }
        });
    }

    private void showClockPicker() {
        if (getSupportFragmentManager().findFragmentByTag(CLOCK_PICKER) != null) {
            return;
        }
        //the dial is for touch: a remote types the time
        boolean tv = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK)
                == Configuration.UI_MODE_TYPE_TELEVISION;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(clockHour)
                .setMinute(clockMinute)
                .setInputMode(tv ? MaterialTimePicker.INPUT_MODE_KEYBOARD : MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTheme(R.style.ThemeOverlay_App_SleepTimer_TimePicker)
                .build();
        listenToClockPicker(picker);
        picker.show(getSupportFragmentManager(), CLOCK_PICKER);
    }

    private void listenToClockPicker(@NonNull MaterialTimePicker picker) {
        picker.addOnPositiveButtonClickListener(view -> mPresenter.setClock(picker.getHour(), picker.getMinute()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(minuteTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(minuteTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateTimerBroadcast);
        unregisterReceiver(finihTimer);
    }

    @Override
    public void showSetup(int mode, int hours, int minutes, int clockHour, int clockMinute, boolean warn,
            boolean shake) {
        binding = true;
        boolean clock = mode == SleepTimerPresenter.MODE_CLOCK;
        mMode.check(clock ? R.id.modeClock : R.id.modeCountdown);
        mCountdown.setVisibility(clock ? View.GONE : View.VISIBLE);
        mClock.setVisibility(clock ? View.VISIBLE : View.GONE);
        mHours.setValue(hours);
        mMinutes.setValue(minutes);
        //a preset chip shows only while the pickers hold its value
        View preset = mPresets.findViewWithTag(String.valueOf(hours * 60 + minutes));
        if (preset != null) {
            mPresets.check(preset.getId());
        } else {
            mPresets.clearCheck();
        }
        this.clockHour = clockHour;
        this.clockMinute = clockMinute;
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, clockHour);
        time.set(Calendar.MINUTE, clockMinute);
        mClockTime.setText(DateFormat.getTimeFormat(this).format(time.getTime()));
        updateClockCaption();
        mWarn.setChecked(warn);
        mShake.setChecked(shake);
        binding = false;
        switchTo(mSetup, mRunning, mStart);
    }

    @Override
    public void showRunning(long remainingMs, long endAt, boolean paused, int mode) {
        long[] hms = Hourglass.split(remainingMs);
        mRemaining.setText(remainingMs < 0 ? "--:--:--"
                : String.format(Locale.ROOT, "%02d:%02d:%02d", hms[0], hms[1], hms[2]));
        String caption = null;
        if (remainingMs >= 0 && mode == SleepTimerPresenter.MODE_END_OF_CHAPTER) {
            String left = hms[0] > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hms[0], hms[1], hms[2])
                    : String.format(Locale.ROOT, "%02d:%02d", hms[1], hms[2]);
            caption = getString(R.string.sleep_timer_chapter_left, left);
        } else if (remainingMs >= 0 && mode >= 0 && !paused) {
            //while paused the end moves with the pause: the paused caption says so instead
            caption = getString(R.string.sleep_timer_stops_at, DateFormat.getTimeFormat(this).format(new Date(endAt)));
        }
        mEndCaption.setText(caption);
        mEndCaption.setVisibility(caption != null ? View.VISIBLE : View.GONE);
        mPausedCaption.setVisibility(paused ? View.VISIBLE : View.GONE);
        mExtend.setVisibility(mode == SleepTimerPresenter.MODE_COUNTDOWN || mode == SleepTimerPresenter.MODE_CLOCK
                ? View.VISIBLE : View.GONE);
        switchTo(mRunning, mSetup, mCancel);
    }

    /** Shows one state; with a D-pad the focus moves to {@code focus} when its control went away. */
    private void switchTo(@NonNull View show, @NonNull View hide, @NonNull View focus) {
        boolean changed = show.getVisibility() != View.VISIBLE;
        hide.setVisibility(View.GONE);
        show.setVisibility(View.VISIBLE);
        if (changed && !focus.isInTouchMode()) {
            focus.requestFocus();
        }
    }

    private void updateClockCaption() {
        Calendar now = Calendar.getInstance();
        long delay = SleepTimerPresenter.nextClockTime(now, clockHour, clockMinute) - now.getTimeInMillis();
        long total = (delay + 59_999) / 60_000;
        long hours = total / 60;
        long minutes = total % 60;
        String text;
        if (hours == 0) {
            text = getString(R.string.sleep_timer_duration_m, minutes);
        } else if (minutes == 0) {
            text = getString(R.string.sleep_timer_duration_h, hours);
        } else {
            text = getString(R.string.sleep_timer_duration_hm, hours, minutes);
        }
        mClockCaption.setText(getString(R.string.sleep_timer_fires_in, text));
    }

    @Override
    public void setStartEnabled(boolean enabled) {
        mStart.setEnabled(enabled);
    }

    @Override
    public void brodcastSend(final Intent intent) {
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void showToast(@NonNull String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    /** Only the player service, in this app, sends these: never exported. */
    private void registerdBradcast() {
        updateTimerBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mPresenter.onTimerUpdate(intent.getLongExtra("time", 0), intent.getLongExtra("endAt", 0),
                        intent.getBooleanExtra("paused", false),
                        intent.getIntExtra("mode", SleepTimerPresenter.MODE_COUNTDOWN));
            }
        };
        ContextCompat.registerReceiver(this, updateTimerBroadcast, new IntentFilter(bradcast_UpdateTimer),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        finihTimer = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mPresenter.finish();
            }
        };
        ContextCompat.registerReceiver(this, finihTimer, new IntentFilter(bradcast_FinishTimer),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
