package com.fanok.audiobooks.activity;

import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
import static com.fanok.audiobooks.Consts.handleUserInput;
import static com.fanok.audiobooks.Consts.isServiceRunning;
import static com.fanok.audiobooks.presenter.BookPresenter.Broadcast_SHOW_TITLE;
import static com.fanok.audiobooks.service.MediaPlayerService.countAudioWereShowingRatingPopUp;

import com.fanok.audiobooks.FontScale;
import com.fanok.audiobooks.AppThemes;
import com.fanok.audiobooks.PaletteBinder;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.arellomobile.mvp.MvpAppCompatActivity;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.fanok.audiobooks.Consts;
import com.fanok.audiobooks.R;
import com.fanok.audiobooks.TvSeekBar;
import com.fanok.audiobooks.Url;
import com.fanok.audiobooks.adapter.AudioAdapter;
import com.fanok.audiobooks.download.DownloadStatus;
import com.fanok.audiobooks.download.DownloadStatusText;
import com.fanok.audiobooks.adapter.SectionsPagerAdapter;
import com.fanok.audiobooks.interface_pacatge.book_content.Activity;
import com.fanok.audiobooks.fragment.BookmarksSheet;
import com.fanok.audiobooks.model.Bookmarks;
import com.fanok.audiobooks.util.ChapterFilter;
import com.fanok.audiobooks.util.Tv;
import com.fanok.audiobooks.room.BookmarkEntity;
import com.fanok.audiobooks.model.FavoritePeople;
import com.fanok.audiobooks.net.Covers;
import com.fanok.audiobooks.pojo.AudioPOJO;
import com.fanok.audiobooks.pojo.BookPOJO;
import com.fanok.audiobooks.pojo.OtherArtistPOJO;
import com.fanok.audiobooks.pojo.StorageUtil;
import com.fanok.audiobooks.policy.DefaultCover;
import com.fanok.audiobooks.policy.ParentalControl;
import com.fanok.audiobooks.presenter.BookPresenter;
import com.fanok.audiobooks.service.MediaPlayerService;
import com.fanok.audiobooks.service.SeekStep;
import com.fanok.audiobooks.util.BookProgress;
import com.fanok.audiobooks.android_equalizer.DialogEqualizerFragment;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.bumptech.glide.Glide;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;

public class BookActivity extends MvpAppCompatActivity implements Activity, BookmarksSheet.Host {

    /** The screen at the chosen font scale (FontScale). */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(FontScale.wrap(newBase));
    }

    private static final String TAG = "BookActivity";

    public static final String Broadcast_SET_IMAGE = "SetImage";
    public static final String Broadcast_SET_PROGRESS = "SetProgress";
    public static final String Broadcast_SET_SELECTION = "SetSelection";
    public static final String Broadcast_SET_TITLE = "SetTitle";
    public static final String Broadcast_SHOW_RATING = "ShowRating";
    public static final String Broadcast_UPDATE_ADAPTER = "UpdateAdapter";
    /** A mark was stored: extras id, url, bookName, name (the chapter) and time (as text). */
    public static final String Broadcast_BOOKMARK_ADDED = "BookmarkAdded";
    /** On a TV: open on the player, not on the book's page. */
    private static final String EXTRA_TV_PLAYER = "tvPlayer";

    public static final String Broadcast_SHOW_EQUALIZER = "ShowEqualizer";


    private static String showingView;
    /** The screen that set {@link #showingView} last: only it clears it, not one destroyed after the next resumed. */
    private static Object sShowingOwner;
    private final Object mShowingToken = new Object();


    private ImageButton mButtonCollapse;
    private LinearLayout mTopButtonsControls;
    private LinearLayout mPlayer;
    /** On a TV: the player is a screen of its own, shown over the book's page (see {@link #showTvPlayer}). */
    private boolean mTvPlayerShown;
    /** A TV only: the black cover of «Погасить экран». */
    private com.fanok.audiobooks.util.ScreenOff mScreenOff;
    private ImageView mPlayerCover;
    private TextView mRadioAll;
    private ImageButton mSelectionClose;
    /** The top row shows how many chapters are chosen, not the playing chapter (bindSelectionBar). */
    private boolean mShowingCount;
    private ImageButton mDelete;
    private ImageButton mDownland;

    public static String getShowingView() {
        return showingView;
    }

    private static final String ARG_BOOK = "arg_book";
    /** Far beyond the system long press (~500 ms), so a slow tap never switches books by accident. */
    private static final long SWITCH_BOOK_HOLD_MS = 2500;
    private static final String PREF_SEEK_BUTTONS = "seekButtonsPref";
    private static final String PREF_SAFE_NAVIGATION = "safeNavigationPref";
    /**
     * Holding rewind/forward scrubs at {@code START_SPEED} times real time, doubling every {@code DOUBLING_MS} up to
     * {@code MAX_SPEED}; a chapter of many hours allows up to {@code MAX_SPEED_LONG}, crossing it in about
     * {@code CROSSING_MS}. The scrub stops {@code END_MARGIN_MS} short of the end, so a release there does not end
     * the chapter.
     */
    private static final double SEEK_HOLD_START_SPEED = 10;
    private static final double SEEK_HOLD_DOUBLING_MS = 1500;
    private static final double SEEK_HOLD_MAX_SPEED = 300;
    private static final double SEEK_HOLD_MAX_SPEED_LONG = 600;
    private static final double SEEK_HOLD_CROSSING_MS = 20_000;
    private static final int SEEK_HOLD_END_MARGIN_MS = 1000;
    /** After a scrub's seek, progress this far from its target and this soon is a tick sent before the seek. */
    private static final long SEEK_COMMIT_SETTLE_MS = 1000;
    private static final int SEEK_COMMIT_TOLERANCE_MS = 1500;
    /** Menu items that can be toolbar icons, in toolbar order, and the setting of each; the favourite pair shares one. */
    private static final int[] TOOLBAR_ITEMS = {R.id.addFavorite, R.id.favoriteAuthor, R.id.favoriteReader, R.id.share,
            R.id.playbackSpeed, R.id.download_delete, R.id.sleep_timer, R.id.car_mode, R.id.equalizer, R.id.settings,
            R.id.openSite, R.id.addMainScreen, R.id.refresh};
    private static final String[] TOOLBAR_KEYS = {"toolbar_favorite", "toolbar_favorite_author",
            "toolbar_favorite_reader", "toolbar_share", "toolbar_speed", "toolbar_download", "toolbar_sleep_timer",
            "toolbar_car_mode", "toolbar_equalizer", "toolbar_settings", "toolbar_open_site", "toolbar_main_screen",
            "toolbar_refresh"};
    /** The toolbar icons by default, as preferences.xml has them; the other actions stay in the overflow menu. */
    private static final List<String> TOOLBAR_DEFAULT = Arrays.asList("toolbar_favorite", "toolbar_speed",
            "toolbar_download", "toolbar_sleep_timer", "toolbar_car_mode", "toolbar_equalizer", "toolbar_main_screen");
    /** Set once {@link #newToolbarDefaults} ran. */
    private static final String PREF_TOOLBAR_DEFAULTS = "toolbar_defaults_8_5";
    /**
     * Widths in the book toolbar: the back arrow, an action icon (styles.xml Widget.App.ActionButton), the speed as
     * text (action_book_speed), the overflow.
     */
    private static final int TOOLBAR_NAV_DP = 56;
    private static final int TOOLBAR_ICON_DP = 32;
    private static final int TOOLBAR_SPEED_DP = 44;
    private static final int TOOLBAR_OVERFLOW_DP = 40;
    /** Below this width left by the icons the title and author are not shown at all. */
    private static final int TOOLBAR_TITLE_MIN_DP = 120;
    /** Set once the hint on holding play and the seek buttons was shown; it is shown only once. */
    private static final String PREF_HINT_SWITCH_BOOK = "hint_switch_book_shown";
    /** The toolbar's height from the layout, for when title and author are shown. */
    private int mToolbarHeight;
    private boolean mSwitchHintShowing;
    @InjectPresenter
    BookPresenter mPresenter;
    private TextView mSpeed;
    private RecyclerView mList;
    private TextView mNameCurent;
    private String mLastChapterTitle = "";
    private ProgressBar mProgressBar;
    private View mDownloadStatus;
    private TextView mDownloadStatusText;
    private ProgressBar mDownloadStatusBar;
    /** The book's download status as the chapter list last computed it; drives the status row and the menu title. */
    private DownloadStatus mLastDownloadStatus;
    private ImageButton mPreviousTop;
    private ImageButton mPlayTop;
    private ImageButton mNextTop;
    /** The collapsed player's buttons beside play rewind/forward instead of changing chapters ("Кнопки плеера"). */
    private boolean mTopSeek;
    /** The rewind/forward button being held: it shows its own position meanwhile. */
    @Nullable
    private SeekHold mActiveSeekHold;
    /** The position the last hold sought to, and when. */
    private int mSeekCommitTarget;
    private long mSeekCommitAt;
    private TextView mTimeStart;
    private AppCompatSeekBar mSeekBar;
    private TextView mTimeEnd;
    private ImageButton mRewind;
    private ImageButton mPreviousBottom;
    private ImageButton mPlayBottom;
    private ImageButton mNextBottom;
    private ImageButton mForward;
    private TabLayout tabs;
    private SectionsPagerAdapter sectionsPagerAdapter;
    /** The comments fetch that decides whether their tab shows. */
    private io.reactivex.disposables.Disposable comentsCount;
    private BookPOJO mBookPOJO;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private LinearLayoutManager mLinearLayoutManager;
    private SharedPreferences pref;

    /** The options menu while it exists: its favourite and speed items follow their state in place. */
    @Nullable
    private Menu mMenu;
    /** A remote's focus was on the toolbar's speed text as {@link #rebuildMenu} ran: its new view takes it back. */
    private boolean mSpeedFocused;
    /** The book's and its people's favourite state, shared by the toolbar, the ★ tab and the description. */
    private BookFavorites mFavorites;
    private final BookFavorites.Listener mFavoritesListener = this::onBookFavoritesChanged;

    private boolean mNotificationClick;

    @SuppressLint("UnsafeOptInUsageError")
    private AudioAdapter mAudioAdapter;
    private final BroadcastReceiver setImage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("id", R.drawable.ic_play);
            mPresenter.setImageDrawable(id);
        }
    };

    private final BroadcastReceiver showRating = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showRatingDialog();
        }
    };

    private final BroadcastReceiver setProgress = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int timeCurrent = intent.getIntExtra("timeCurrent", 0);
            int timeEnd = intent.getIntExtra("timeEnd", 0);
            int buffered = intent.getIntExtra("buffered", 0);
            mPresenter.updateTime(timeCurrent, timeEnd, buffered);
        }
    };
    private final BroadcastReceiver setSelectionBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int pos = intent.getIntExtra("postion", -1);
            String name = intent.getStringExtra("name");
            if (name != null) {
                mPresenter.setSelected(pos, name);
            }
        }
    };
    private final BroadcastReceiver setTitleBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra("title");
            if (title != null) {
                mPresenter.showTitle(title);
            }
        }
    };


    /** The player's audio session for an equalizer dialog that arrived while this screen was not resumed, or 0. */
    private int pendingEqualizerSession;

    private final BroadcastReceiver showEqualizer = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sessionId = intent.getIntExtra("id", 0);
            if (sessionId <= 0) {
                return;
            }
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                showEqualizer(sessionId);
            } else {
                //a dialog cannot be shown now: onResume shows it
                pendingEqualizerSession = sessionId;
            }
        }
    };

    private void showEqualizer(int sessionId) {
        try {
            int textColor = com.google.android.material.color.MaterialColors.getColor(
                    this, R.attr.colorPrimaryText, android.graphics.Color.WHITE);
            DialogEqualizerFragment fragment = DialogEqualizerFragment.newBuilder()
                    .setAudioSessionId(sessionId)
                    .themeColor(PaletteBinder.surface(this))
                    .textColor(textColor)
                    .setAccentColor(PaletteBinder.accent(this))
                    .build();

            fragment.show(getSupportFragmentManager(), "eq");
        } catch (IllegalStateException ignored) {
        }
    }


    private final BroadcastReceiver updateAdapter = new BroadcastReceiver() {
        @UnstableApi
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAdapter();
        }
    };


    public static void startNewActivity(@NonNull Context context, @NonNull BookPOJO bookPOJO) {
        startNewActivity(context, bookPOJO, false);
    }

    public static void startNewActivity(@NonNull Context context, @NonNull BookPOJO bookPOJO,
            boolean notificationClick) {
        startNewActivity(context, bookPOJO, notificationClick, false);
    }

    /** {@code tvPlayer}: on a TV the book opens on its player, not on its page (the book playing now, «Продолжить»). */
    public static void startNewActivity(@NonNull Context context, @NonNull BookPOJO bookPOJO,
            boolean notificationClick, boolean tvPlayer) {
        Intent intent = new Intent(context, BookActivity.class);
        intent.putExtra("notificationClick", notificationClick);
        intent.putExtra(EXTRA_TV_PLAYER, tvPlayer);
        String json = new GsonBuilder().serializeNulls().create().toJson(bookPOJO);
        intent.putExtra(ARG_BOOK, json);
        //one policy for every way in; a "notificationClick" extra is never a reason to skip it
        if (!ParentalControl.isAllowed(context, bookPOJO)) {

            XmlPullParser parser = context.getResources().getXml(R.xml.pin_entry);
            try {
                parser.next();
                parser.nextTag();
            } catch (Exception e) {
                Log.d(TAG, "startNewActivity: ", e);
            }

            AttributeSet attr = Xml.asAttributeSet(parser);
            PinEntryEditText editText = new PinEntryEditText(context, attr);


            AlertDialog.Builder builder = new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(context);
            builder.setTitle(R.string.enter_password)
                    .setCancelable(false)
                    .setView(editText)
                    .setNeutralButton(R.string.cancel, null)
                    .setPositiveButton(R.string.confirm, null);

            AlertDialog dialog = builder.create();
            if (context instanceof LoadBook) {
                // its blank loading screen has nothing else to show, whether the book opens or not
                dialog.setOnDismissListener(d -> ((LoadBook) context).finish());
            }
            dialog.setOnShowListener(dialogInterface -> {
                PaletteBinder.applyPaletteToDialog(dialog);
                Objects.requireNonNull(editText.getText()).clear();
                editText.requestFocus();
                Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setEnabled(false);
                b.setOnClickListener(view -> openWithPin(context, editText, dialog, intent));
            });

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    int size = Objects.requireNonNull(editText.getText()).toString().length();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(size == 4);
                }
            });

            editText.setOnPinEnteredListener(str -> openWithPin(context, editText, dialog, intent));

            dialog.show();

        } else {
            context.startActivity(intent);
        }
    }

    /** Opens the book when the PIN is right; otherwise says why not (wrong, or locked for a while). */
    private static void openWithPin(@NonNull Context context, @NonNull PinEntryEditText editText,
            @NonNull AlertDialog dialog, @NonNull Intent intent) {
        ParentalControl.Verdict verdict = ParentalControl.verifyPin(context,
                Objects.requireNonNull(editText.getText()).toString());
        if (verdict.result == ParentalControl.Result.OK) {
            dialog.cancel();
            context.startActivity(intent);
        } else {
            Toast.makeText(context, ParentalControlActivity.pinMessage(context, verdict), Toast.LENGTH_SHORT).show();
            editText.getText().clear();
        }
    }

    /**
     * The player notification's content intent: the book screen from the saved book itself, no page fetch. Single
     * top, so tapping it while this book's screen is on top only brings it forward.
     */
    @NonNull
    public static Intent notificationIntent(@NonNull Context context, @NonNull BookPOJO bookPOJO) {
        Intent intent = new Intent(context, BookActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("notificationClick", true);
        intent.putExtra(ARG_BOOK, new GsonBuilder().serializeNulls().create().toJson(bookPOJO));
        return intent;
    }

    private static void refreshActivity(@NonNull Context context, @NonNull BookPOJO bookPOJO,
            boolean notificationClick) {
        Intent intent = new Intent(context, BookActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("notificationClick", notificationClick);
        String json = new GsonBuilder().serializeNulls().create().toJson(bookPOJO);
        intent.putExtra(ARG_BOOK, json);
        context.startActivity(intent);
        ((AppCompatActivity) context).finish();
    }


    @ProvidePresenter
    BookPresenter provideBookPresenter() {
        String json = getIntent().getStringExtra(ARG_BOOK);
        if (json == null) throw new NullPointerException();
        return new BookPresenter(BookPOJO.parceJsonToBookPojo(json), getApplicationContext());
    }

    @UnstableApi
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppThemes.apply(this, R.style.AppTheme_NoActionBar, R.style.LightAppTheme_NoActionBar,
                R.style.AppThemeBlack_NoActionBar);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called");

        Intent intent = getIntent();
        String json = intent.getStringExtra(ARG_BOOK);
        if (json == null) throw new NullPointerException();
        mBookPOJO = BookPOJO.parceJsonToBookPojo(json);
        mFavorites = new BookFavorites(this, mBookPOJO);
        mFavorites.addListener(mFavoritesListener);

        pref = PreferenceManager
                .getDefaultSharedPreferences(this);

        UiModeManager uiModeManager = (UiModeManager) getApplicationContext().getSystemService(UI_MODE_SERVICE);
        setContentView(R.layout.activity_book);
        mButtonCollapse = findViewById(R.id.buttonCollapse);
        mTopButtonsControls = findViewById(R.id.topButtonsControls);
        mPlayer = findViewById(R.id.player);
        mTvPlayerShown = false;
        mPlayerCover = findViewById(R.id.playerCover);
        bindPlayerCover();
        TextView playerTitle = findViewById(R.id.playerBookTitle);
        TextView playerAuthor = findViewById(R.id.playerBookAuthor);
        if (playerTitle != null && mBookPOJO != null) {
            playerTitle.setText(mBookPOJO.getName());
            playerAuthor.setText(mBookPOJO.getAutor());
            if (android.text.TextUtils.isEmpty(mBookPOJO.getAutor())) playerAuthor.setVisibility(View.GONE);
        }
        mRadioAll = findViewById(R.id.radioAll);
        mSelectionClose = findViewById(R.id.selectionClose);
        mDelete = findViewById(R.id.delete);
        mDownland = findViewById(R.id.dowland);
        mSpeed = findViewById(R.id.player_action_speed);
        mList = findViewById(R.id.list);
        mNameCurent = findViewById(R.id.name_curent);
        mProgressBar = findViewById(R.id.progressBar);
        mDownloadStatus = findViewById(R.id.downloadStatus);
        mDownloadStatusText = findViewById(R.id.downloadStatusText);
        mDownloadStatusBar = findViewById(R.id.downloadStatusBar);
        if (mDownloadStatus != null) {
            mDownloadStatus.setOnClickListener(view -> ClearSavedActivity.startActivity(this));
        }
        mPreviousTop = findViewById(R.id.previousTop);
        mPlayTop = findViewById(R.id.playTop);
        mNextTop = findViewById(R.id.nextTop);
        mTimeStart = findViewById(R.id.timeStart);
        mSeekBar = findViewById(R.id.seekBar);
        mTimeEnd = findViewById(R.id.timeEnd);
        mRewind = findViewById(R.id.rewind);
        mPreviousBottom = findViewById(R.id.previousBottom);
        mPlayBottom = findViewById(R.id.playBottom);
        mNextBottom = findViewById(R.id.nextBottom);
        mForward = findViewById(R.id.forward);

        MainActivity.setCloseApp(false);

        mNotificationClick = intent.getBooleanExtra("notificationClick", false);

        if (mBookPOJO != null && mBookPOJO.getName() != null) {
            setTitle(mBookPOJO.getName().trim());
        }

        sectionsPagerAdapter = new SectionsPagerAdapter(this, Objects.requireNonNull(mBookPOJO));
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        tabs = findViewById(R.id.tabs);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // the ★ tab is an icon in the colours of the tabs' titles; ОБЗОР before it stays the page shown first
        tabs.setTabIconTint(tabs.getTabTextColors());
        new TabLayoutMediator(tabs, viewPager, (tab, position) -> {
            if (sectionsPagerAdapter.kindAt(position) == SectionsPagerAdapter.FAVORITES) {
                tab.setIcon(favoritesTabIcon());
                tab.setContentDescription(R.string.menu_favorite);
            } else {
                tab.setText(sectionsPagerAdapter.getPageTitle(position));
            }
        }).attach();
        PaletteBinder.bind(this);
        // the comments tab shows only when there are comments; its list reuses this fetch
        comentsCount = com.fanok.audiobooks.model.ComentsModel.shared(mBookPOJO.getUrl(), savedInstanceState == null)
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(comments -> {
                    if (!comments.isEmpty()) {
                        sectionsPagerAdapter.showComents(comments.size());
                    }
                }, e -> Log.d(TAG, "no comments tab: " + e));


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (mBookPOJO != null && mBookPOJO.getAutor() != null) {
                actionBar.setSubtitle(mBookPOJO.getAutor().trim());
            }
        }

        if (mButtonCollapse != null && mButtonCollapse.getVisibility() != View.INVISIBLE) {
            mButtonCollapse.setVisibility(View.INVISIBLE);
        }





        // decided by the inflated layout, not the screen size: a tablet in portrait has the phones' bottom sheet too
        ViewGroup.LayoutParams playerParams = mPlayer.getLayoutParams();
        if (playerParams instanceof CoordinatorLayout.LayoutParams
                && ((CoordinatorLayout.LayoutParams) playerParams).getBehavior() instanceof BottomSheetBehavior
                && (uiModeManager == null
                || uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_TELEVISION)) {
            bottomSheetBehavior = BottomSheetBehavior.from(mPlayer);


            bottomSheetBehavior.addBottomSheetCallback(
                    new BottomSheetBehavior.BottomSheetCallback() {
                        @Override
                        public void onStateChanged(@NonNull View bottomSheet, int newState) {
                            if (actionBar != null) {
                                if (BottomSheetBehavior.STATE_COLLAPSED == newState) {
                                    mPresenter.stateCollapsed();
                                }
                            }

                            if (BottomSheetBehavior.STATE_EXPANDED == newState) {
                                if (mPlayer != null && mPlayer.getParent() instanceof View) {
                                    View parent = (View) mPlayer.getParent();
                                    parent.bringToFront();
                                    androidx.core.view.ViewCompat.setZ(parent, 32f);
                                }
                                mPresenter.stateExpanded();
                            } else {
                                mPresenter.stateElse();
                            }
                        }


                        @Override
                        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                            float alpha = 1 - slideOffset * 2;
                            if (mTopButtonsControls != null) {
                                mTopButtonsControls.animate().alpha(alpha).setDuration(0);
                            }
                            if (mProgressBar != null) {
                                mProgressBar.animate().alpha(alpha).setDuration(0);
                            }
                            mNameCurent.animate().alpha(alpha).setDuration(0);
                            if (alpha <= 0.0) {
                                mTopButtonsControls.setVisibility(View.GONE);
                                mProgressBar.setVisibility(View.INVISIBLE);
                            } else {
                                mTopButtonsControls.setVisibility(View.VISIBLE);
                                mProgressBar.setVisibility(View.VISIBLE);
                            }

                            if (slideOffset > Consts.COLLAPS_BUTTON_VISIBLE) {
                                bindSelectionBar(true);
                                if (mButtonCollapse != null) {
                                    mButtonCollapse.setRotation(0);
                                    mButtonCollapse.setContentDescription(getString(R.string.collapse));
                                }
                                float alphaCollapse = (float) ((slideOffset - Consts.COLLAPS_BUTTON_VISIBLE)
                                        / Consts.COLLAPS_BUTTON_VISIBLE_STEP);
                                for (View view : new View[] {mButtonCollapse, mSelectionClose, mRadioAll, mDownland,
                                        mDelete, mNameCurent}) {
                                    if (view != null) {
                                        view.animate().alpha(alphaCollapse).setDuration(0);
                                    }
                                }
                            } else {
                                showExpandHint(Math.max(0, alpha));
                                bindSelectionBar(false);
                            }
                        }
                    });
            mPlayer.setOnClickListener(
                    view -> {
                        if (BottomSheetBehavior.STATE_COLLAPSED == bottomSheetBehavior.getState()) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    });


            mButtonCollapse.setOnClickListener(view -> {
                if (BottomSheetBehavior.STATE_EXPANDED == bottomSheetBehavior.getState()) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else if (BottomSheetBehavior.STATE_COLLAPSED == bottomSheetBehavior.getState()) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            });
            showExpandHint(1);
        }


        // the chapter selection: all <-> none, close, and the actions, which end it
        mRadioAll.setOnClickListener(view -> mAudioAdapter.setAllSelected(!mAudioAdapter.isAllSelected()));
        mSelectionClose.setOnClickListener(view -> mAudioAdapter.endSelection());
        mDownland.setOnClickListener(view -> {
            if (mAudioAdapter.getSelectedItemsSize() > 0) {
                mPresenter.dowland(mAudioAdapter.getSelectedItems());
                mAudioAdapter.endSelection();
            }
        });

        mDelete.setOnClickListener(view -> {
            if (mAudioAdapter != null && mAudioAdapter.getSelectedItemsSize() > 0) {
                final java.util.HashSet<String> chosen = mAudioAdapter.getSelectedItems();
                new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(this)
                        .setTitle("Удаление загрузок")
                        .setMessage("Вы уверены, что хотите удалить выбранные файлы (" + chosen.size() + ")?")
                        .setPositiveButton("ОК", (dialog, which) -> {
                            mPresenter.delete(chosen);
                            mAudioAdapter.endSelection();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        });

        mAudioAdapter = new AudioAdapter();
        mAudioAdapter.setBookUrl(mBookPOJO.getUrl());
        mAudioAdapter.setListener((view, position) -> mPresenter.onItemSelected(view, position));
        mAudioAdapter.setStatusListener(this::showDownloadStatus);
        mList.setAdapter(mAudioAdapter);
        mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mList.setLayoutManager(mLinearLayoutManager);
        mAudioAdapter.setSelectedListner(() -> bindSelectionBar(bottomSheetBehavior == null
                || bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED));


        mSeekBar.setMax(100);
        if (mProgressBar != null) {
            mProgressBar.setMax(100);
        }

        if (mSeekBar instanceof TvSeekBar) {
            // --- Логика для ТВ ---
            // Безопасно приводим тип и устанавливаем кастомный слушатель
            ((TvSeekBar) mSeekBar).setOnSeekBarChangedByUserListener(new TvSeekBar.OnSeekBarChangedByUserListener() {
                @Override
                public void onProgressChangedByUser(TvSeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        // Этот код сработает ОДИН РАЗ, когда фокус уйдет с SeekBar на ТВ
                        mPresenter.seekChange(progress);
                    }
                }
            });
        } else {
            // --- Логика для Телефона ---
            // Если это обычный SeekBar, устанавливаем стандартный слушатель
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Можно оставить пустым
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // the drag wins over a button held meanwhile
                    if (mActiveSeekHold != null) {
                        mActiveSeekHold.finish(false);
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Этот код сработает, когда пользователь отпустит палец от SeekBar на телефоне
                    mPresenter.seekChange(seekBar.getProgress());
                }
            });
        }


        if (mPlayTop != null) {
            mPlayTop.setOnClickListener(view -> mPresenter.buttomPlayClick(view));
            new SwitchBookHold().attach(mPlayTop);
        }
        //icons and descriptions of these two follow the setting in onResume
        if (mPreviousTop != null) {
            mPreviousTop.setOnClickListener(view -> {
                if (mTopSeek) {
                    mPresenter.buttomRewindClick(view);
                } else {
                    jumpChapter(view, false);
                }
            });
            new SeekHold(mPreviousTop, false, true);
        }
        if (mNextTop != null) {
            mNextTop.setOnClickListener(view -> {
                if (mTopSeek) {
                    mPresenter.buttomForwardClick(view);
                } else {
                    jumpChapter(view, true);
                }
            });
            new SeekHold(mNextTop, true, true);
        }
        mPlayBottom.setOnClickListener(view -> mPresenter.buttomPlayClick(view));
        new SwitchBookHold().attach(mPlayBottom);
        mPreviousBottom.setOnClickListener(view -> jumpChapter(view, false));
        mNextBottom.setOnClickListener(view -> jumpChapter(view, true));
        mRewind.setOnClickListener(view -> mPresenter.buttomRewindClick(view));
        mForward.setOnClickListener(view -> mPresenter.buttomForwardClick(view));
        new SeekHold(mRewind, false, false);
        new SeekHold(mForward, true, false);
        if (mSpeed != null) {
            mSpeed.setOnClickListener(v -> mPresenter.buttonSpeedClick(v));
        }
        findViewById(R.id.player_action_timer).setOnClickListener(v -> performPlayerAction(R.id.sleep_timer));
        findViewById(R.id.player_action_equalizer).setOnClickListener(v -> performPlayerAction(R.id.equalizer));
        findViewById(R.id.player_action_car).setOnClickListener(v -> performPlayerAction(R.id.car_mode));
        if (com.fanok.audiobooks.util.Tv.is(this)) {
            // a car mode on a living-room TV is nonsense: its place is the button that blacks the screen out
            mScreenOff = new com.fanok.audiobooks.util.ScreenOff(this);
            com.google.android.material.button.MaterialButton screenOff = findViewById(R.id.player_action_car);
            screenOff.setIconResource(R.drawable.ic_screen_off);
            screenOff.setContentDescription(getString(R.string.screen_off));
            screenOff.setOnClickListener(v -> mScreenOff.show());
        }
        findViewById(R.id.player_action_bookmark).setOnClickListener(v -> addBookmarkHere());
        findViewById(R.id.player_action_bookmark).setOnLongClickListener(v -> {
            showBookmarks();
            return true;
        });
        setUpChapterSearch();
        if (Tv.is(this) && getIntent().getBooleanExtra(EXTRA_TV_PLAYER, false)) {
            showTvPlayer(true); // the book playing now: its player, not its page
        }
        getSystemService(AUDIO_SERVICE);




        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mAudioAdapter.isSelecting()) {
                    mAudioAdapter.endSelection();
                    return;
                }
                if (mTvPlayerShown) {
                    showTvPlayer(false);
                    return;
                }
                if (bottomSheetBehavior != null
                        && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return;
                }

                // only a book alone in its task starts the main screen; screens below it are returned to
                if (mNotificationClick && isTaskRoot()) {
                    Intent intent = new Intent(BookActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("notificationClick", true);
                    startActivity(intent);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });



    }

    /**
     * The player's cover, shown above the chapters: the book's cover, the mask the parental control puts on it, or the
     * default cover of a site whose covers are never requested; hidden when the book has no cover at all.
     */
    private void bindPlayerCover() {
        if (mPlayerCover == null) {
            return;
        }
        String photo = mBookPOJO == null ? null : mBookPOJO.getPhoto();
        String bookUrl = mBookPOJO == null ? null : mBookPOJO.getUrl();
        boolean empty = photo == null || photo.trim().isEmpty();
        if (DefaultCover.replaces(photo) || empty && DefaultCover.replaces(bookUrl)) {
            mPlayerCover.setVisibility(View.VISIBLE);
            mPlayerCover.setImageDrawable(DefaultCover.drawable(this, bookUrl));
            return;
        }
        if (empty) {
            mPlayerCover.setVisibility(View.GONE);
            return;
        }
        mPlayerCover.setVisibility(View.VISIBLE);
        Drawable mask = ParentalControl.coverMask(this, mBookPOJO);
        if (mask != null) {
            Glide.with(this).load(mask).into(mPlayerCover);
        } else if (photo.startsWith("content://")) {
            Glide.with(this).load(Uri.parse(photo)).into(mPlayerCover);
        } else if (photo.startsWith("/") || photo.contains(":\\")) {
            Glide.with(this).load(new java.io.File(photo)).into(mPlayerCover);
        } else {
            Glide.with(this).load(Covers.glide(photo)).into(mPlayerCover);
        }
    }

    @Override
    public void stateCollapsed() {
        if (mTopButtonsControls != null && mTopButtonsControls.getVisibility() != View.GONE) {
            mTopButtonsControls.setVisibility(View.GONE);
        }
        if (mProgressBar != null && mProgressBar.getVisibility() != View.VISIBLE) {
            mProgressBar.setVisibility(
                    View.VISIBLE);
        }
        if (bottomSheetBehavior != null) {
            showExpandHint(1);
        } else if (mButtonCollapse != null && mButtonCollapse.getVisibility() != View.INVISIBLE) {
            mButtonCollapse.setVisibility(View.INVISIBLE);
        }

        mAudioAdapter.endSelection();
        bindSelectionBar(false);
        broadcastSend(new Intent(Broadcast_SHOW_TITLE));
    }

    @UnstableApi
    @Override
    public void stateExpanded() {
        if (mButtonCollapse != null) {
            mButtonCollapse.setRotation(0);
            mButtonCollapse.setContentDescription(getString(R.string.collapse));
        }
        if (mTopButtonsControls != null && mTopButtonsControls.getVisibility() != View.GONE) {
            mTopButtonsControls.setVisibility(View.GONE);
        }
        if (mProgressBar != null && mProgressBar.getVisibility() != View.INVISIBLE) {
            mProgressBar.setVisibility(
                    View.INVISIBLE);
        }
        if (mButtonCollapse != null && mButtonCollapse.getVisibility() != View.VISIBLE
                && !mAudioAdapter.isSelecting()) {
            mButtonCollapse.setVisibility(View.VISIBLE);
        }
        bindSelectionBar(true);

        int selected = mAudioAdapter.getIndexSelected();
        boolean b = pref.getBoolean("scrollToSelectedPref", false);
        int row = selected == -1 ? -1 : mAudioAdapter.displayOf(selected);
        if (row != -1 && b) {
            mLinearLayoutManager.scrollToPositionWithOffset(row, 0);
        }

    }

    @Override
    public void stateElse() {
        if (mTopButtonsControls != null) {
            mTopButtonsControls.setVisibility(View.VISIBLE);
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    /** The favourites of this book and its people; the ★ tab, the description and the cover's sheet use it too. */
    @NonNull
    public BookFavorites favorites() {
        return mFavorites;
    }

    /** The book or one of its people was marked or unmarked: the ★ tab's icon and the toolbar's items follow. */
    private void onBookFavoritesChanged() {
        TabLayout.Tab tab = tabs == null ? null
                : tabs.getTabAt(sectionsPagerAdapter.positionOfKind(SectionsPagerAdapter.FAVORITES));
        if (tab != null) {
            tab.setIcon(favoritesTabIcon());
        }
        if (mMenu != null && bindFavoriteItems(mMenu)) {
            rebuildMenu(); //an author or reader item came or went: the toolbar is laid out anew
        }
    }

    /** invalidateOptionsMenu; a remote's focus on the toolbar's speed text goes to the rebuilt menu's new one. */
    private void rebuildMenu() {
        // left on the old view, the focus would drop to the toolbar's Up arrow
        MenuItem speed = mMenu == null ? null : mMenu.findItem(R.id.playbackSpeed);
        mSpeedFocused = speed != null && speed.getActionView() != null && speed.getActionView().isFocused();
        invalidateOptionsMenu();
    }

    private int favoritesTabIcon() {
        return mFavorites.isBook() ? R.drawable.ic_favorite_remove : R.drawable.ic_favorite_add;
    }

    /**
     * The book's add/remove pair, and the author and reader items: checked (filled icon) when a person of the field
     * is a favourite, hidden when the book names nobody. True when one of these two was shown or hidden.
     */
    private boolean bindFavoriteItems(@NonNull Menu menu) {
        boolean book = mFavorites.isBook();
        menu.findItem(R.id.addFavorite).setVisible(!book);
        menu.findItem(R.id.removeFavorite).setVisible(book);
        return bindPersonItem(menu.findItem(R.id.favoriteAuthor), FavoritePeople.AUTHOR, R.drawable.ic_favorite_author,
                R.drawable.ic_favorite_author_on)
                | bindPersonItem(menu.findItem(R.id.favoriteReader), FavoritePeople.READER,
                R.drawable.ic_favorite_reader, R.drawable.ic_favorite_reader_on);
    }

    private boolean bindPersonItem(@NonNull MenuItem item, int kind, int offIcon, int onIcon) {
        boolean shown = !mFavorites.people(kind).isEmpty();
        boolean changed = item.isVisible() != shown;
        item.setVisible(shown);
        boolean on = mFavorites.any(kind);
        if (item.isChecked() != on) {
            item.setChecked(on);
            item.setIcon(on ? onIcon : offIcon);
        }
        return changed;
    }

    /**
     * The author or reader item: one person is toggled at once, with a snackbar that can undo it; several are listed
     * in the favourites sheet, one row each.
     */
    private void togglePerson(int kind) {
        List<String> people = mFavorites.people(kind);
        if (people.size() > 1) {
            mFavorites.showSheet(this);
            return;
        }
        if (people.isEmpty()) {
            return;
        }
        String name = people.get(0);
        boolean on = mFavorites.toggle(kind, name);
        Snackbar bar = Snackbar.make(snackbarHost(),
                getString(on ? R.string.book_fav_added : R.string.book_fav_removed, name), Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> mFavorites.undo(kind, name));
        //above the collapsed player, which is drawn over the screen's bottom
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bar.setAnchorView(mPlayer);
        }
        bar.show();
    }

    /** The speed item: "Скорость · 1,5×" in the overflow, the text "1,5×" on the toolbar; opens the speed dialog. */
    private void bindSpeedItem(@NonNull Menu menu) {
        MenuItem item = menu.findItem(R.id.playbackSpeed);
        String speed = BookPresenter.speedText(BookPresenter.getSpeed());
        item.setTitle(getString(R.string.book_toolbar_speed, speed));
        View action = item.getActionView();
        if (action instanceof TextView) {
            ((TextView) action).setText(speed);
            action.setContentDescription(getString(R.string.book_toolbar_speed_desc, speed));
            action.setOnClickListener(v -> mPresenter.buttonSpeedClick(mSpeed));
        }
    }

    /** The player's speed chip: icon + "1,5×", opening the speed dialog. */
    private void bindSpeedButton() {
        if (mSpeed == null) {
            return;
        }
        String speed = BookPresenter.speedText(BookPresenter.getSpeed());
        mSpeed.setText(speed);
        mSpeed.setContentDescription(getString(R.string.book_toolbar_speed_desc, speed));
    }

    @Override
    public void showSpeed(float speed) {
        bindSpeedButton();
        if (mMenu != null) {
            bindSpeedItem(mMenu);
        }
        if (mBookPOJO != null && mBookPOJO.getUrl() != null) {
            BookProgress.refresh(mBookPOJO.getUrl()); // the description's time left is at this speed
        }
    }


    public void showSiries() {
        sectionsPagerAdapter.showTab(SectionsPagerAdapter.SERIES);
    }

    @Override
    public void showOtherSource() {
        sectionsPagerAdapter.setArtistPOJO(mPresenter.getOtherArtistPOJOS());
        sectionsPagerAdapter.showTab(SectionsPagerAdapter.OTHER_SOURCE);
    }

    /** The description says other narrations exist: the presenter loads them and shows the tab only if any. */
    public void loadOtherArtist() {
        mPresenter.loadOtherArtists();
    }

    /** The description already found the other narrations: shown through the presenter's view state, not searched again. */
    public void otherArtistsFound(@NonNull ArrayList<OtherArtistPOJO> artists) {
        mPresenter.otherArtistsLoaded(artists);
    }

    @Override
    public void showOtherArtist(@NonNull ArrayList<OtherArtistPOJO> artists) {
        sectionsPagerAdapter.setOtherArtists(artists);
        sectionsPagerAdapter.showTab(SectionsPagerAdapter.OTHER_ARTIST);
    }

    @UnstableApi
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.addFavorite || id == R.id.removeFavorite) {
            mFavorites.toggleBook();
            return true;
        } else if (id == R.id.favoriteAuthor || id == R.id.favoriteReader) {
            togglePerson(id == R.id.favoriteAuthor ? FavoritePeople.AUTHOR : FavoritePeople.READER);
            return true;
        } else if (id == R.id.bookmarks) {
            showBookmarks();
            return true;
        } else if (id == R.id.playbackSpeed) {
            //the player's own speed button is the dialog's anchor: a view of this screen's theme, not the toolbar's
            mPresenter.buttonSpeedClick(mSpeed);
            return true;
        }
        if (id == android.R.id.home) {
            if (mNotificationClick && isTaskRoot()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("notificationClick", true); // as Back does: not a launch, no last book over it
                startActivity(intent);
            } else {
                finish();
            }
        } else if (menuItem.getItemId() == R.id.download_delete) {
            openDownloadSelection();
        }

        mPresenter.onOptionsMenuItemSelected(menuItem);
        return (super.onOptionsItemSelected(menuItem));
    }

    /** The description's listen button acts on this book, even while the bottom player holds another. */
    public void playThisBook() {
        mPresenter.playThisBook();
        if (Tv.is(this)) {
            showTvPlayer(true);
        }
    }

    /**
     * On a TV the book's page and the player are two screens: the page chooses (listen, favourites, bookmarks,
     * download), the player listens (the whole screen, the focus on Play). Back from the player returns to the page.
     */
    public void showTvPlayer(boolean shown) {
        View appBar = findViewById(R.id.appBar);
        View pager = findViewById(R.id.view_pager);
        if (mPlayer == null || appBar == null || pager == null) {
            return;
        }
        mTvPlayerShown = shown;
        // the side buttons are for the touch: the remote winds with «left»/«right» on Play and changes the chapter by
        // the list or the media keys, so the focus never lands on a button it cannot leave
        for (int id : new int[] {R.id.rewind, R.id.forward, R.id.previousBottom, R.id.nextBottom}) {
            View side = findViewById(id);
            if (side != null) {
                side.setFocusable(!shown);
            }
        }
        mPlayer.setVisibility(shown ? View.VISIBLE : View.GONE);
        appBar.setVisibility(shown ? View.GONE : View.VISIBLE);
        pager.setVisibility(shown ? View.GONE : View.VISIBLE);
        View listen = findViewById(R.id.btnListenBook);
        View target = shown ? findViewById(R.id.playBottom) : listen != null ? listen : pager;
        if (target != null) {
            target.post(target::requestFocus);
        }
    }

    private void performPlayerAction(int id) {
        mPresenter.onPlayerActionSelected(id);
    }

    /** Opens the bottom player sheet and begins chapter selection for download or deletion. */
    public void openDownloadSelection() {
        if (bottomSheetBehavior != null &&
                bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        if (mAudioAdapter != null) {
            mAudioAdapter.startSelection();
        }
    }

    /** A mark the service stored: said with its place, and "Заметка" to write on it. */
    private final BroadcastReceiver bookmarkAdded = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String name = intent.getStringExtra("name");
            String time = intent.getStringExtra("time");
            String url = intent.getStringExtra("url");
            String bookName = intent.getStringExtra("bookName");
            int id = intent.getIntExtra("id", -1);
            String chapter = name != null && !name.isEmpty() && android.text.TextUtils.isDigitsOnly(name)
                    ? getString(R.string.player_chapter_number, name) : (name == null ? "" : name);
            // a mark of the book playing, which may not be the one open: then its book is named
            boolean other = url != null && mBookPOJO != null && !url.equals(mBookPOJO.getUrl());
            String text = other
                    ? getString(R.string.bookmark_added_other, chapter, time, bookName == null ? "" : bookName)
                    : getString(R.string.bookmark_added, chapter, time);
            Snackbar bar = Snackbar.make(snackbarHost(), text, Snackbar.LENGTH_LONG)
                    .setDuration(6000);
            if (id >= 0) {
                bar.setAction(R.string.bookmark_note, v -> BookmarksSheet.editNote(BookActivity.this, id, null));
            }
            //above the collapsed player, which is drawn over the screen's bottom
            if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bar.setAnchorView(mPlayer);
            }
            bar.show();
        }
    };

    private int mTvUndoChapter;
    private long mTvUndoMs;
    private long mTvUndoUntil;
    private boolean mTvMenuTaken;

    @Override
    public void showSeekUndo(int chapter, long positionMs, String chapterName) {
        String name = chapterName == null ? "" : chapterName;
        String chapterText = !name.isEmpty() && android.text.TextUtils.isDigitsOnly(name)
                ? getString(R.string.player_chapter_number, name) : name;
        String text = getString(R.string.seek_undo, Bookmarks.time(positionMs), chapterText);
        if (Tv.is(this)) {
            // the remote cannot press a bar: its «Меню» key is the undo, for as long as the bar is shown
            mTvUndoChapter = chapter;
            mTvUndoMs = positionMs;
            mTvUndoUntil = android.os.SystemClock.elapsedRealtime() + 8000;
            text = getString(R.string.seek_undo_tv, text);
        }
        Snackbar bar = Snackbar.make(snackbarHost(), text, 8000)
                .setAction(R.string.seek_undo_action, v -> mPresenter.undoSeek(chapter, positionMs));
        //above the collapsed player, which is drawn over the screen's bottom
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            bar.setAnchorView(mPlayer);
        }
        bar.show();
    }

    /** The player's own layer is drawn over the screen: a bar made on the screen would be hidden behind the open player. */
    private View snackbarHost() {
        boolean open = bottomSheetBehavior != null && mPlayer != null
                && bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED
                && bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
        return open || mTvPlayerShown ? mPlayer : findViewById(R.id.view_pager);
    }

    /** Marks the place the player is at; with nothing loaded there is no place to mark. */
    @Override
    public void addBookmarkHere() {
        if (MediaPlayerService.currentBookUrl() == null) {
            showToast(R.string.bookmark_none_playing);
            return;
        }
        broadcastSend(new Intent(BookPresenter.Broadcast_ADD_BOOKMARK));
    }

    /** The book's bookmarks: from the menu, the book page's button and a long press on the player's. */
    public void showBookmarks() {
        if (mBookPOJO == null || mBookPOJO.getUrl() == null || isFinishing()
                || getSupportFragmentManager().findFragmentByTag(BookmarksSheet.TAG) != null) {
            return;
        }
        BookmarksSheet.newInstance(mBookPOJO.getUrl()).show(getSupportFragmentManager(), BookmarksSheet.TAG);
    }

    @Override
    public void onBookmarkChosen(@NonNull BookmarkEntity mark) {
        mPresenter.playBookmark(mark);
    }

    /** The player UI listens only while visible; on the way back the service resends chapter, icon and progress. */
    @Override
    protected void onStart() {
        super.onStart();
        registerInternalReceiver(setImage, Broadcast_SET_IMAGE);
        registerInternalReceiver(setProgress, Broadcast_SET_PROGRESS);
        registerInternalReceiver(setSelectionBroadcast, Broadcast_SET_SELECTION);
        registerInternalReceiver(setTitleBroadcast, Broadcast_SET_TITLE);
        registerInternalReceiver(showRating, Broadcast_SHOW_RATING);
        registerInternalReceiver(updateAdapter, Broadcast_UPDATE_ADAPTER);
        registerInternalReceiver(showEqualizer, Broadcast_SHOW_EQUALIZER);
        registerInternalReceiver(bookmarkAdded, Broadcast_BOOKMARK_ADDED);
        broadcastSend(new Intent(Broadcast_SHOW_TITLE));
        if (mAudioAdapter != null) mAudioAdapter.startLive();
    }

    @Override
    protected void onStop() {
        com.fanok.audiobooks.util.WatchNext.sync(this); // the TV home screen's «Продолжить»
        if (mAudioAdapter != null) mAudioAdapter.stopLive();
        unregisterReceiver(setImage);
        unregisterReceiver(setProgress);
        unregisterReceiver(setSelectionBroadcast);
        unregisterReceiver(setTitleBroadcast);
        unregisterReceiver(showRating);
        unregisterReceiver(updateAdapter);
        unregisterReceiver(showEqualizer);
        unregisterReceiver(bookmarkAdded);
        mPresenter.dismissSpeedDialog();
        mPresenter.onStop();
        super.onStop();
    }

    /** App-internal actions are never exported: other apps must not be able to feed or read the player UI. */
    private void registerInternalReceiver(BroadcastReceiver receiver, String action) {
        ContextCompat.registerReceiver(this, receiver, new IntentFilter(action),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        if (mScreenOff != null) {
            mScreenOff.hide();
            mScreenOff.disarm();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // a new screen of this same book was resumed first («Обновить», a passed site check): its comments fetch,
        // which replaced this one's under the same URL, must go on
        boolean replaced = sShowingOwner != mShowingToken && Objects.equals(mBookPOJO.getUrl(), showingView);
        if (sShowingOwner == mShowingToken) {
            showingView = "";
            sShowingOwner = null;
        }
        mFavorites.release();
        comentsCount.dispose();
        if (isFinishing() && !replaced) {
            com.fanok.audiobooks.model.ComentsModel.cancel(mBookPOJO.getUrl());
        }
        //Moxy destroys the presenter (and its player binding) when this screen is finished for good
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.book_activity_options_menu, menu);
        mMenu = menu;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            menu.findItem(R.id.addMainScreen).setVisible(false);
        }
        bindFavoriteItems(menu);
        bindSpeedItem(menu);
        View speed = menu.findItem(R.id.playbackSpeed).getActionView();
        if (mSpeedFocused && speed != null) {
            speed.post(speed::requestFocus); // once the rebuilt toolbar holds it, after the old one went
        }
        mSpeedFocused = false;

        mPresenter.onCreateOptionsMenu(menu);
        layoutToolbar(menu);

        return true;
    }

    /**
     * Makes the actions chosen in the settings toolbar icons, as many as fit beside the back arrow; the others stay
     * in the overflow menu, which disappears when it is empty. Title and author take the width left, and are not
     * shown at all when that is too little to read (or when they are not wanted).
     */
    private void layoutToolbar(@NonNull Menu menu) {
        int wanted = 0;
        boolean others = false;
        for (int i = 0; i < TOOLBAR_ITEMS.length; i++) {
            MenuItem item = menu.findItem(TOOLBAR_ITEMS[i]);
            //one of the favourite pair is visible, whichever it is
            if (item == null || !item.isVisible() && TOOLBAR_ITEMS[i] != R.id.addFavorite) {
                continue;
            }
            if (onToolbar(TOOLBAR_KEYS[i])) {
                wanted += toolbarWidth(TOOLBAR_ITEMS[i]);
            } else {
                others = true;
            }
        }
        int room = getResources().getConfiguration().screenWidthDp - TOOLBAR_NAV_DP;
        if (wanted > room || others) {
            room -= TOOLBAR_OVERFLOW_DP;
        }
        //the chosen ones in order, each while it still fits
        for (int i = 0; i < TOOLBAR_ITEMS.length; i++) {
            MenuItem item = menu.findItem(TOOLBAR_ITEMS[i]);
            if (item == null || !item.isVisible() && TOOLBAR_ITEMS[i] != R.id.addFavorite) {
                continue;
            }
            boolean icon = false; // Book actions live in the overflow, not in a row of toolbar icons.
            if (icon) {
                room -= toolbarWidth(TOOLBAR_ITEMS[i]);
            }
            int action = icon ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER;
            item.setShowAsAction(action);
            if (TOOLBAR_ITEMS[i] == R.id.addFavorite) {
                menu.findItem(R.id.removeFavorite).setShowAsAction(action);
            }
        }
        boolean title = pref.getBoolean("toolbar_title", false) && room >= TOOLBAR_TITLE_MIN_DP;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(title);
        }
        // without title and author the bar is only as tall as its icons, not an empty band above the tabs
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            ViewGroup.LayoutParams params = toolbar.getLayoutParams();
            if (mToolbarHeight == 0) {
                mToolbarHeight = params.height;
            }
            int height = title ? mToolbarHeight : getResources().getDimensionPixelSize(R.dimen.book_toolbar_compact);
            if (params.height != height) {
                params.height = height;
                toolbar.setLayoutParams(params);
            }
        }
    }

    private boolean onToolbar(@NonNull String key) {
        return pref.getBoolean(key, TOOLBAR_DEFAULT.contains(key));
    }

    /**
     * Once, at the start of the app: a toolbar left as the default of 8.4 and before had it (title and author,
     * favourite and share) gets the new default: no title, the icons of {@link #TOOLBAR_DEFAULT}; one the user chose
     * stays as it showed, its keys never stored kept at their old defaults, not the new ones.
     */
    public static void newToolbarDefaults(@NonNull SharedPreferences prefs) {
        if (prefs.getBoolean(PREF_TOOLBAR_DEFAULTS, false)) {
            return;
        }
        boolean title = prefs.getBoolean("toolbar_title", true);
        boolean untouched = title;
        boolean[] shown = new boolean[TOOLBAR_KEYS.length];
        for (int i = 0; i < TOOLBAR_KEYS.length; i++) {
            boolean old = "toolbar_favorite".equals(TOOLBAR_KEYS[i]) || "toolbar_share".equals(TOOLBAR_KEYS[i]);
            shown[i] = prefs.getBoolean(TOOLBAR_KEYS[i], old);
            untouched &= shown[i] == old;
        }
        SharedPreferences.Editor edit = prefs.edit().putBoolean(PREF_TOOLBAR_DEFAULTS, true)
                .putBoolean("toolbar_title", !untouched && title);
        for (int i = 0; i < TOOLBAR_KEYS.length; i++) {
            edit.putBoolean(TOOLBAR_KEYS[i], untouched ? TOOLBAR_DEFAULT.contains(TOOLBAR_KEYS[i]) : shown[i]);
        }
        edit.apply();
    }

    private static int toolbarWidth(int itemId) {
        return itemId == R.id.playbackSpeed ? TOOLBAR_SPEED_DP : TOOLBAR_ICON_DP;
    }

    /** In the collapsed player the collapse button, turned upwards, shows that the chapter list opens. */
    private void showExpandHint(float alpha) {
        if (mButtonCollapse == null) {
            return;
        }
        mButtonCollapse.setRotation(180);
        mButtonCollapse.setContentDescription(getString(R.string.expand));
        mButtonCollapse.animate().alpha(alpha).setDuration(0);
        mButtonCollapse.setVisibility(View.VISIBLE);
    }

    /**
     * The previous or next chapter. With "Безопасная навигация" on (the default) only after a confirmation: a
     * stray tap would lose the place in what may be a chapter of hours.
     */
    private void jumpChapter(@NonNull View view, boolean next) {
        Runnable jump = () -> {
            if (next) {
                mPresenter.buttomNextClick(view);
            } else {
                mPresenter.buttomPreviousClick(view);
            }
        };
        if (!pref.getBoolean(PREF_SAFE_NAVIGATION, true)) {
            jump.run();
            return;
        }
        new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(this)
                .setMessage(next ? R.string.confirm_next_chapter : R.string.confirm_previous_chapter)
                .setPositiveButton(R.string.go_to, (dialog, which) -> jump.run())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** The collapsed player's buttons beside play: chapters, or rewind/forward as "Кнопки плеера" asks. */
    private void bindTopSideButtons() {
        mTopSeek = pref.getBoolean(PREF_SEEK_BUTTONS, true);
        if (mPreviousTop != null) {
            mPreviousTop.setImageResource(mTopSeek ? R.drawable.ic_fast_rewind : R.drawable.ic_skip_previous);
            mPreviousTop.setContentDescription(getString(mTopSeek ? R.string.rewind_back : R.string.previous));
        }
        if (mNextTop != null) {
            mNextTop.setImageResource(mTopSeek ? R.drawable.ic_fast_forward : R.drawable.ic_skip_next);
            mNextTop.setContentDescription(getString(mTopSeek ? R.string.fast_forward : R.string.next));
        }
        //a tap seeks by the step set in the settings, and the icons show it
        mRewind.setImageResource(SeekStep.rewindIcon(this));
        mRewind.setContentDescription(SeekStep.rewindLabel(this));
        mForward.setImageResource(SeekStep.forwardIcon(this));
        mForward.setContentDescription(SeekStep.forwardLabel(this));
    }

    @Override
    public void setTabPostion(String title) {
        TabLayout.Tab tab = tabs.getTabAt(sectionsPagerAdapter.positionOf(title));
        if (tab != null) {
            tab.select();
        }
    }

    @Override
    public void refreshActivity() {
        BookActivity.refreshActivity(this, mBookPOJO, mNotificationClick);
    }

    /** The browser check of the book's site, which refused the app: passed, the book screen loads again. */
    private final androidx.activity.result.ActivityResultLauncher<Intent> mSiteCheck = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshActivity();
                }
            });
    /** The «Пройти» offer on screen: the description and the chapters fail on the same page, one offer does. */
    @Nullable
    private Snackbar mSiteCheckOffer;

    @Override
    public void showSiteCheckOffer(@NonNull com.fanok.audiobooks.search.Provider site) {
        if (mSiteCheckOffer != null && mSiteCheckOffer.isShownOrQueued()) {
            return;
        }
        mSiteCheckOffer = Snackbar.make(findViewById(R.id.view_pager), getString(R.string.site_check_offer, site.title),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.site_check_action, v -> mSiteCheck.launch(ActivityCookies.intent(this, site, mBookPOJO.getUrl(), true)));
        //above the collapsed player, which is drawn over the screen's bottom
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            mSiteCheckOffer.setAnchorView(mPlayer);
        }
        mSiteCheckOffer.show();
    }

    @Override
    public void shareTextUrl() {
        BookLinks.show(this, mBookPOJO);
    }


    /** The shortcut's cover load: Picasso holds its target only weakly, a collected one would never deliver. */
    @Nullable
    private Target mShortcutTarget;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void addToMainScreen(BookPOJO pojo) {
        String photo = pojo.getPhoto();
        if (photo == null || photo.trim().isEmpty() || DefaultCover.replaces(photo)) {
            pinShortcut(pojo, null); // no cover, or one that is never requested: the app's icon
            return;
        }
        mShortcutTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                pinShortcut(pojo, bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                pinShortcut(pojo, null); // offline or a broken cover: the shortcut still comes, with the app's icon
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        (photo.startsWith("/") ? Picasso.get().load(new java.io.File(photo)) : Picasso.get().load(photo))
                .into(mShortcutTarget);
    }

    /** The book's shortcut with its cover, or the app's icon when {@code cover} is null; pinned where supported. */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pinShortcut(@NonNull BookPOJO pojo, @Nullable Bitmap cover) {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        Intent intent = new Intent(getApplicationContext(), LoadBook.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("url", pojo.getUrl());
        intent.putExtra("notificationClick", true);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(getApplicationContext(),
                pojo.getUrl())
                .setShortLabel(pojo.getName())
                .setLongLabel(pojo.getName())
                .setIcon(cover != null ? Icon.createWithBitmap(cover)
                        : Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build();

        if (shortcutManager != null) {
            shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
            if (shortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo pinShortcutInfo = new ShortcutInfo
                        .Builder(getApplicationContext(), pojo.getUrl())
                        .build();
                Intent pinnedShortcutCallbackIntent =
                        shortcutManager.createShortcutResultIntent(pinShortcutInfo);


                PendingIntent successCallback = PendingIntent.getBroadcast(
                        getApplicationContext(), 0,
                        pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE);
                shortcutManager.requestPinShortcut(pinShortcutInfo,
                        successCallback.getIntentSender());
            }
        }
    }


    @Override
    public void showToast(int id) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoadError(String message, String report) {
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_error_reports", true)) {
            showToast(message);
            return;
        }
        String full = message + "\n\n" + report;
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        // what happened, "Скопировать отчёт", then the details: three dialog buttons would not fit on one line
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextIsSelectable(true);
        text.setPadding(pad, pad / 2, pad, 0);
        Button copy = new Button(this, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        copy.setText(R.string.error_report_copy);
        copy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                        getString(R.string.error_report_title), full));
                showToast(R.string.copied);
            }
        });
        TextView details = new TextView(this);
        details.setText(report);
        details.setTextIsSelectable(true);
        details.setPadding(pad, 0, pad, 0);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(pad / 2);
        body.addView(copy, lp);
        body.addView(details);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(this)
                .setTitle(R.string.error_report_title)
                .setView(scroll)
                .setPositiveButton(R.string.retry, (d, which) -> mPresenter.getAudio())
                .setNegativeButton(R.string.close, null)
                .show();
    }

    @Override
    public void showLongToast(int id) {
        Toast.makeText(this, id, Toast.LENGTH_LONG).show();
    }

    @UnstableApi
    @Override
    public void updateAdapter() {
        if (mAudioAdapter != null) {
            mAudioAdapter.refresh();
        }
    }

    /**
     * The row above the chapter list: what the book's download is doing (state, chapters, bytes, speed, time left),
     * hidden while nothing is selected for download. Tapping it opens the Downloads screen.
     */
    @Nullable
    public DownloadStatus downloadStatus() {
        return mLastDownloadStatus;
    }

    private void showDownloadStatus(@NonNull DownloadStatus status) {
        mLastDownloadStatus = status;
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof com.fanok.audiobooks.fragment.DescriptionBookFragment) {
                ((com.fanok.audiobooks.fragment.DescriptionBookFragment) fragment).onDownloadsChanged();
            }
        }
        if (mDownloadStatus == null) return;
        String line = DownloadStatusText.line(this, status);
        if (line.isEmpty()) {
            mDownloadStatus.setVisibility(View.GONE);
            return;
        }
        mDownloadStatusText.setText(line);
        mDownloadStatusBar.setProgress(status.percent());
        mDownloadStatusBar.setVisibility(status.state == DownloadStatus.COMPLETE ? View.GONE : View.VISIBLE);
        mDownloadStatus.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem download = menu.findItem(R.id.download_delete);
        if (download != null) {
            String brief = mLastDownloadStatus == null ? null : DownloadStatusText.brief(this, mLastDownloadStatus);
            download.setTitle(brief == null ? getString(R.string.dowland_delete)
                    : getString(R.string.download_menu_status, brief));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /** "Настройки панели…": the settings open at Оформление → Панель книги, Back walks up to the root. */
    @Override
    public void startMainActivity(int fragmentId) {
        MainActivity.startMainActivity(this, fragmentId, "book_toolbar");
    }


    public void showRatingDialog() {
        new StorageUtil(this).storeShowRating(false);
    }

    @Override
    public void showProgres(boolean b) {

    }

    @Override
    public void showTitle(@NotNull @NonNull String name) {
        if (!name.isEmpty()) {
            mLastChapterTitle = name;
        }
        if (!mShowingCount) {
            String raw = name.isEmpty() ? mLastChapterTitle : name;
            String chapter = !raw.isEmpty() && android.text.TextUtils.isDigitsOnly(raw)
                    ? getString(R.string.player_chapter_number, raw) : raw;
            BookPOJO playing = MediaPlayerService.currentBook();
            String shown = collapsedPlayerTitle(playing == null ? null : playing.getName(),
                    MediaPlayerService.currentBookUrl(),
                    mBookPOJO == null ? null : mBookPOJO.getName(),
                    mBookPOJO == null ? null : mBookPOJO.getUrl(), chapter);
            mNameCurent.setText(shown == null ? "" : shown);
        }
    }

    /** When another book is in the player, prefix its chapter with that book's name so the row identifies what plays. */
    @Nullable
    public static String collapsedPlayerTitle(@Nullable String activeBookName, @Nullable String activeBookUrl,
            @Nullable String openBookName, @Nullable String openBookUrl, @Nullable String chapter) {
        String cleanChapter = chapter == null ? "" : chapter.trim();
        String cleanActive = activeBookName == null ? "" : activeBookName.trim();
        boolean differentBook = activeBookUrl != null && !activeBookUrl.equals(openBookUrl)
                || activeBookUrl == null && !cleanActive.isEmpty() && !cleanActive.equals(openBookName == null ? "" : openBookName.trim());
        if (differentBook && !cleanActive.isEmpty()) {
            return cleanChapter.isEmpty() ? cleanActive : cleanActive + " · " + cleanChapter;
        }
        return cleanChapter.isEmpty() ? null : cleanChapter;
    }

    /**
     * The top row of the chapter list while chapters are chosen for download/delete (the Download/Delete menu or a
     * long press): close, all/some/none chosen, "N из M" and download and delete stand in place of the collapse
     * button and the playing chapter. Choosing goes on until it is closed, Back, an action or the sheet collapsing,
     * however many chapters are chosen. {@code shown}: the list is open, not a sheet at its collapsed height.
     */
    private void bindSelectionBar(boolean shown) {
        boolean selecting = shown && mAudioAdapter != null && mAudioAdapter.isSelecting();
        int bar = selecting ? View.VISIBLE : View.GONE;
        for (View view : new View[] {mSelectionClose, mRadioAll, mDownland, mDelete}) {
            if (view != null && view.getVisibility() != bar) {
                view.setVisibility(bar);
            }
        }
        if (mButtonCollapse != null && shown && bottomSheetBehavior != null) {
            mButtonCollapse.setVisibility(selecting ? View.GONE : View.VISIBLE);
        }
        if (!selecting) {
            if (mShowingCount) {
                mShowingCount = false;
                broadcastSend(new Intent(Broadcast_SHOW_TITLE));
            }
            return;
        }
        int chosen = mAudioAdapter.getSelectedChapters();
        int total = mAudioAdapter.getItemCount();
        mShowingCount = true;
        mNameCurent.setText(getString(R.string.selection_count, chosen, total));
        boolean all = total > 0 && chosen == total;
        mRadioAll.setCompoundDrawablesRelativeWithIntrinsicBounds(all ? R.drawable.ic_check_circle_outline
                : chosen == 0 ? R.drawable.ic_checkbox_blank_circle_outline : R.drawable.ic_selection_partial, 0, 0, 0);
        mRadioAll.setContentDescription(getString(all ? R.string.selection_clear_all : R.string.selection_select_all));
        boolean any = mAudioAdapter.getSelectedItemsSize() > 0;
        for (ImageButton action : new ImageButton[] {mDownland, mDelete}) {
            action.setEnabled(any);
            action.setImageAlpha(any ? 255 : 97);
        }
    }

    /** The search field of a long chapter list: shown from {@link ChapterFilter#SEARCH_FROM} chapters. */
    private void bindChapterSearch(int chapters) {
        View field = findViewById(R.id.chapterSearch);
        if (field == null) {
            return; // a layout without it (the TV's)
        }
        boolean show = chapters > ChapterFilter.SEARCH_FROM;
        field.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show && field instanceof EditText && ((EditText) field).length() > 0) {
            ((EditText) field).setText(""); // the filter goes with the field
        }
    }

    private void setUpChapterSearch() {
        View field = findViewById(R.id.chapterSearch);
        if (!(field instanceof EditText)) {
            return;
        }
        ((EditText) field).addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (mAudioAdapter != null) {
                    mAudioAdapter.setFilter(s.toString());
                }
            }
        });
    }

    @UnstableApi
    @Override
    public void showData(@NonNull ArrayList<AudioPOJO> data, @NonNull String bookUrl, int chaptersTotal) {
        if (mAudioAdapter != null) {
            mAudioAdapter.setData(data, chaptersTotal);
            bindChapterSearch(data.size());
        }
    }

    /**
     * Says once what the hidden gestures do while another book keeps playing: holding play switches the player to
     * this book ({@link SwitchBookHold}), holding a seek button speeds up ({@link SeekHold}). Spent when dismissed, so
     * a rotation that drops the dialog unread does not spend it.
     */
    @Override
    public void showSwitchBookHint() {
        if (mSwitchHintShowing || pref.getBoolean(PREF_HINT_SWITCH_BOOK, false) || isFinishing()) {
            return;
        }
        mSwitchHintShowing = true; // a reload of the chapters while it is open does not stack a second one
        new com.fanok.audiobooks.dialogs.PaletteDialogBuilder(this)
                .setTitle(R.string.hint_switch_book_title)
                .setMessage(R.string.hint_switch_book)
                .setPositiveButton(R.string.got_it, null)
                .setOnDismissListener(dialog -> pref.edit().putBoolean(PREF_HINT_SWITCH_BOOK, true).apply())
                .show();
    }

    @Override
    public void updateTime(int timeCurent, int timeEnd, int buffered) {
        // the chapter played to its end under a hold: the position held belongs to the one before
        if (mActiveSeekHold != null && timeEnd != mSeekBar.getMax()) {
            mActiveSeekHold.finish(false);
        }
        // a hold shows its own position; a tick sent before its seek arrived would jump back for a moment
        if (mActiveSeekHold != null || (SystemClock.uptimeMillis() - mSeekCommitAt < SEEK_COMMIT_SETTLE_MS
                && Math.abs(timeCurent - mSeekCommitTarget) > SEEK_COMMIT_TOLERANCE_MS)) {
            return;
        }
        showTime(timeCurent, timeEnd, buffered);
    }

    private void showTime(int timeCurent, int timeEnd, int buffered) {
        if (mProgressBar != null) {
            mProgressBar.setMax(timeEnd);
            mProgressBar.setProgress(timeCurent);
            mProgressBar.setSecondaryProgress(buffered);
        }
        mSeekBar.setMax(timeEnd);
        mSeekBar.setProgress(timeCurent);
        mSeekBar.setSecondaryProgress(buffered);

        timeCurent /= 1000;
        timeEnd /= 1000;
        int minutes = timeCurent / 60;
        int seconds = timeCurent % 60;
        String timeString = String.format(Locale.forLanguageTag("UK"), "%02d:%02d", minutes,
                seconds);

        mTimeStart.setText(timeString);

        minutes = timeEnd / 60;
        seconds = timeEnd % 60;
        timeString = String.format(Locale.forLanguageTag("UK"), "%02d:%02d", minutes, seconds);
        mTimeEnd.setText(timeString);
    }

    @Override
    public void setTimeEnd(int timeEnd) {

    }

    @Override
    public void setImageDrawable(int id) {
        String label = getString(id == R.drawable.ic_pause ? R.string.pause : R.string.play);
        if (mPlayTop != null) {
            mPlayTop.setImageResource(id);
            mPlayTop.setContentDescription(label);
        }
        mPlayBottom.setImageResource(id);
        mPlayBottom.setContentDescription(label);
    }

    @UnstableApi
    @Override
    public void setSelected(int id, String name) {
        if (mAudioAdapter != null) {
            if (id >= 0 && id < mAudioAdapter.getDataCount()) {
                if (mAudioAdapter.getData(id).getName().equals(name)) {
                    mAudioAdapter.setIndexSelected(id);
                }
            }
        }
    }

    @Override
    public void broadcastSend(@NotNull Intent intent) {
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void activityStart(@NotNull Intent intent) {
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        View currentFocus = getCurrentFocus();
        // the TV's player works as a video player does: on Play, «left» and «right» wind the book by the step (held
        // down, they go on), OK pauses, «up» and «down» move through the chapters, the progress and the buttons
        if (mTvPlayerShown && currentFocus != null) {
            int id = currentFocus.getId();
            boolean inList = mList != null && (currentFocus == mList || mList.findContainingViewHolder(currentFocus) != null);
            boolean inActions = id == R.id.player_action_timer || id == R.id.player_action_equalizer
                    || id == R.id.player_action_speed || id == R.id.player_action_car
                    || id == R.id.player_action_bookmark;
            View to = null;
            if (inList && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                to = mPlayBottom; // out of the long list at once, to the button OK pauses with
            } else if (id == R.id.playBottom && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                to = findViewById(R.id.player_action_speed);
            } else if (inActions && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                to = mPlayBottom;
            }
            if (to != null && to.requestFocus()) {
                return true;
            }
        }
        if (mTvPlayerShown && currentFocus != null && currentFocus.getId() == R.id.playBottom
                && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mPresenter.buttomRewindClick(currentFocus);
            } else {
                mPresenter.buttomForwardClick(currentFocus);
            }
            return true;
        }
        if (currentFocus != null && tabs != null && !mTvPlayerShown) {
            // the tabs sit between the toolbar's buttons and the page: «down» and «right» from «Back» come to them,
            // «up» from a tab goes back to it (the beam of the focus search jumps over the tabs)
            View toolbar = findViewById(R.id.toolbar);
            View strip = tabs.getChildAt(0);
            boolean inToolbar = toolbar != null && currentFocus != toolbar && isInside(currentFocus, toolbar);
            // «down» from any button of the toolbar («Back», the three dots) comes to the tabs; «right» and «left»
            // between the toolbar's buttons stay the ordinary focus search, so the three dots can be reached
            if (inToolbar && (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)) {
                // the focus search would jump from the toolbar's buttons to the tabs: go along the toolbar instead
                java.util.ArrayList<View> stops = new java.util.ArrayList<>();
                for (View stop : toolbar.getFocusables(View.FOCUS_DOWN)) {
                    if (stop.isShown()) {
                        stops.add(stop);
                    }
                }
                int at = stops.indexOf(currentFocus);
                int to = at + (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                if (at >= 0 && to >= 0 && to < stops.size() && stops.get(to).requestFocus()) {
                    return true;
                }
            }
            if (inToolbar && keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                    && strip instanceof ViewGroup && ((ViewGroup) strip).getChildCount() > 0) {
                View tab = ((ViewGroup) strip).getChildAt(Math.max(0, tabs.getSelectedTabPosition()));
                if (tab.requestFocus()) {
                    return true;
                }
            }
            if (isTabView(currentFocus) && keyCode == KeyEvent.KEYCODE_DPAD_UP && toolbar != null) {
                java.util.ArrayList<View> stops = toolbar.getFocusables(View.FOCUS_DOWN);
                for (View stop : stops) {
                    if (stop.isShown() && stop.requestFocus()) {
                        return true;
                    }
                }
            }
        }
        if (currentFocus!=null){
            if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // --- НАЧАЛО ИСПРАВЛЕНИЯ ---
                // Проверяем, является ли текущий сфокусированный View дочерним элементом нашего TabLayout
                if (isTabView(currentFocus)) {
                    // Находим ViewPager2
                    ViewPager2 viewPager = findViewById(R.id.view_pager);
                    if (viewPager != null) {
                        // Получаем текущую позицию ViewPager2
                        int currentItem = viewPager.getCurrentItem();
                        // Получаем адаптер ViewPager2
                        RecyclerView.Adapter adapter = viewPager.getAdapter();

                        // Проверяем, что адаптер - это наш SectionsPagerAdapter
                        if (adapter instanceof SectionsPagerAdapter) {
                            // Получаем ID фрагмента по его позиции
                            long fragmentId = adapter.getItemId(currentItem);
                            // Находим сам фрагмент по его ID
                            Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + fragmentId);

                            // Если фрагмент найден, передаем фокус его корневому View
                            if (currentFragment != null && currentFragment.getView() != null) {
                                currentFragment.getView().requestFocus();
                            }
                        }
                    }
                    // Возвращаем true, т.к. мы обработали событие
                    return true;
                }
            }else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                View downloadBtn = findViewById(R.id.btnDownloadBook);
                // the last button of the row (Favorite, Bookmarks, Download: whichever is the rightmost shown)
                boolean noDownload = downloadBtn == null || downloadBtn.getVisibility() != View.VISIBLE;
                boolean favoriteIsRightmost = currentFocus.getId() == R.id.btnBookmarksBook && noDownload;
                if (currentFocus.getId()==R.id.genre||currentFocus.getId()==R.id.autorConteiner||currentFocus.getId()==R.id.author||
                        currentFocus.getId()==R.id.artistConteiner||currentFocus.getId()==R.id.artist||currentFocus.getId()==R.id.seriesConteiner||
                        currentFocus.getId()==R.id.series||currentFocus.getId()==R.id.showMore||
                        currentFocus.getId()==R.id.authorLabel||currentFocus.getId()==R.id.artistLabel||
                        currentFocus.getId()==R.id.seriesLabel||currentFocus.getId()==R.id.btnListenBook||
                        currentFocus.getId()==R.id.btnDownloadBook||favoriteIsRightmost) {
                    // the page has nothing to the right of these (the player is its own screen now): the focus stays
                    return true;
                }
            }
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !mTvPlayerShown && tabs != null && currentFocus != null
                && !isTabView(currentFocus)) {
            // «up» from the top of the page: to the tabs (the focus search ends there, or goes past them)
            View up = currentFocus.focusSearch(View.FOCUS_UP);
            View strip = tabs.getChildAt(0);
            if ((up == null || up == currentFocus) && strip instanceof ViewGroup
                    && ((ViewGroup) strip).getChildCount() > 0) {
                View tab = ((ViewGroup) strip).getChildAt(Math.max(0, tabs.getSelectedTabPosition()));
                if (tab.requestFocus()) {
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Holding a play button (touch, or the D-pad centre / Enter key) for {@link #SWITCH_BOOK_HOLD_MS} while another
     * book plays switches the player to this one; a shorter press stays an ordinary click.
     */
    private final class SwitchBookHold implements View.OnTouchListener, View.OnKeyListener, Runnable {

        private View mButton;

        private boolean mArmed;

        private boolean mFired;

        void attach(@NonNull View button) {
            mButton = button;
            button.setOnTouchListener(this);
            button.setOnKeyListener(this);
        }

        private void arm() {
            disarm();
            mFired = false;
            if (mPresenter.canSwitchToThisBook()) {
                mArmed = true;
                mButton.postDelayed(this, SWITCH_BOOK_HOLD_MS);
            }
        }

        private void disarm() {
            mArmed = false;
            mButton.removeCallbacks(this);
        }

        /** Ends the press; true when the hold fired, so the click that would follow is swallowed. */
        private boolean release() {
            disarm();
            if (!mFired) {
                return false;
            }
            mFired = false;
            mButton.setPressed(false);
            return true;
        }

        @Override
        public void run() {
            boolean armed = mArmed;
            mArmed = false;
            // still held: a press whose release was missed (focus moved, screen left) ends unpressed
            if (!armed || !mButton.isPressed() || !mButton.hasWindowFocus() || !mPresenter.canSwitchToThisBook()) {
                return;
            }
            mFired = true;
            mButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            showToast(R.string.switched_to_this_book);
            mPresenter.switchToThisBook();
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    arm();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (event.getX() < 0 || event.getY() < 0 || event.getX() > v.getWidth()
                            || event.getY() > v.getHeight()) {
                        disarm();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return release();
                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_ENTER) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    arm();
                }
                return mFired;
            }
            return event.getAction() == KeyEvent.ACTION_UP && release();
        }
    }

    /**
     * Holding a rewind/forward button (touch, or the D-pad centre) scrubs the shown position, faster and faster
     * the longer it is held, as hardware players do; the player seeks once, on release, so a stream does not
     * rebuffer on the way. A tap stays the ordinary click. The collapsed player's buttons do it only in seek mode.
     */
    private final class SeekHold implements View.OnLongClickListener, View.OnTouchListener, View.OnKeyListener,
            Runnable {

        private final View mButton;

        private final boolean mForward;

        private final boolean mTop;

        /** The position shown (ms) and the top speed of this hold. */
        private double mPosition;

        private double mMaxSpeed;

        /** The hold's start and its last frame, uptime ms. */
        private long mStart;

        private long mLast;

        private boolean mAtEdge;

        SeekHold(@NonNull View button, boolean forward, boolean top) {
            mButton = button;
            mForward = forward;
            mTop = top;
            button.setOnLongClickListener(this);
            button.setOnTouchListener(this);
            button.setOnKeyListener(this);
        }

        /** Any end of the press (a cancel too: the sheet took the gesture) seeks to the position the user saw. */
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                finish(true);
            }
            return false;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                finish(true);
            }
            return false;
        }

        @Override
        public boolean onLongClick(View v) {
            int duration = mSeekBar.getMax();
            //no chapter length known yet: nothing to scrub, a tap still seeks
            if ((mTop && !mTopSeek) || duration <= SEEK_HOLD_END_MARGIN_MS) {
                return false;
            }
            if (mActiveSeekHold != null) {
                mActiveSeekHold.finish(true);
            }
            mActiveSeekHold = this;
            mPosition = mSeekBar.getProgress();
            mMaxSpeed = Math.max(SEEK_HOLD_MAX_SPEED,
                    Math.min(SEEK_HOLD_MAX_SPEED_LONG, duration / SEEK_HOLD_CROSSING_MS));
            mStart = SystemClock.uptimeMillis();
            mLast = mStart;
            mAtEdge = false;
            mButton.postOnAnimation(this);
            return true;
        }

        /** One frame: the position moves by the speed reached so far, times the time since the last frame. */
        @Override
        public void run() {
            if (mActiveSeekHold != this) {
                return;
            }
            //released, slid off, or the screen left while held: the press is over
            if (!mButton.isPressed() || !mButton.hasWindowFocus()) {
                finish(true);
                return;
            }
            long now = SystemClock.uptimeMillis();
            double speed = Math.min(mMaxSpeed,
                    SEEK_HOLD_START_SPEED * Math.pow(2, (now - mStart) / SEEK_HOLD_DOUBLING_MS));
            double next = mPosition + (mForward ? speed : -speed) * (now - mLast);
            mLast = now;
            int end = Math.max(0, mSeekBar.getMax() - SEEK_HOLD_END_MARGIN_MS);
            mPosition = Math.max(0, Math.min(end, next));
            boolean atEdge = next <= 0 || next >= end;
            if (atEdge && !mAtEdge) {
                mButton.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
            mAtEdge = atEdge;
            showTime((int) mPosition, mSeekBar.getMax(), mSeekBar.getSecondaryProgress());
            mButton.postOnAnimation(this);
        }

        /** Ends the hold: {@code commit} seeks to the position shown, else the player's own position returns. */
        void finish(boolean commit) {
            if (mActiveSeekHold != this) {
                return;
            }
            mActiveSeekHold = null;
            mButton.removeCallbacks(this);
            if (commit) {
                mSeekCommitTarget = (int) mPosition;
                mSeekCommitAt = SystemClock.uptimeMillis();
                mPresenter.seekChange(mSeekCommitTarget);
                mButton.announceForAccessibility(mTimeStart.getText());
            }
        }
    }

    private static boolean isInside(View view, View ancestor) {
        for (ViewParent p = view.getParent(); p != null; p = p.getParent()) {
            if (p == ancestor) {
                return true;
            }
        }
        return false;
    }

    private boolean isTabView(View view) {
        if (view == null || tabs == null) return false;
        // Каждая вкладка (TabView) является дочерним элементом контейнера внутри TabLayout.
        // Проверяем, совпадает ли родительский View нашего focusedView с контейнером вкладок.
        ViewParent parent = view.getParent();
        // TabLayout содержит дочерний SlidingTabIndicator, который в свою очередь содержит TabView
        return parent != null && parent.getParent() == tabs;
    }

    /** The notification of another book reached this single-top screen: open that book's own screen. */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        String json = intent.getStringExtra(ARG_BOOK);
        BookPOJO other = json != null ? BookPOJO.parceJsonToBookPojo(json) : null;
        if (other != null && other.getUrl() != null && !other.getUrl().equals(mBookPOJO.getUrl())) {
            Intent fresh = new Intent(intent);
            fresh.setFlags(intent.getFlags() & ~(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            startActivity(fresh);
        }
    }

    @UnstableApi
    @Override
    protected void onResume() {
        super.onResume();
        if (mScreenOff != null) {
            mScreenOff.arm();
        }
        if (pendingEqualizerSession > 0) {
            int sessionId = pendingEqualizerSession;
            pendingEqualizerSession = 0;
            showEqualizer(sessionId);
        }
        showingView = mBookPOJO.getUrl();
        sShowingOwner = mShowingToken;
        mFavorites.refresh(); //the book may have been (un)marked on another screen meanwhile
        bindTopSideButtons();
        rebuildMenu(); //the toolbar settings may have changed meanwhile
        bindSpeedButton(); //the speed may have changed on another book's screen
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        mAudioAdapter.refresh();

        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(mPresenter.getState());
        } else {
            bindSelectionBar(true);
        }
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // on a TV the screen covered black (the player's button, or by itself) comes back with any key
        if (mScreenOff != null && mScreenOff.consume(event)) {
            return true;
        }
        // on a TV the «Меню» key undoes a jump while its bar is shown (the system would open the book's menu instead)
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && Tv.is(this)) {
            boolean offered = android.os.SystemClock.elapsedRealtime() < mTvUndoUntil;
            if (event.getAction() == KeyEvent.ACTION_DOWN && offered && event.getRepeatCount() == 0) {
                mTvUndoUntil = 0;
                mTvMenuTaken = true;
                mPresenter.undoSeek(mTvUndoChapter, mTvUndoMs);
                return true;
            }
            if (mTvMenuTaken) { // the press the undo took: its release must not open the menu
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mTvMenuTaken = false;
                }
                return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0
                && (event.getKeyCode() == KEYCODE_MEDIA_PLAY || event.getKeyCode() == KEYCODE_MEDIA_PLAY_PAUSE)
                && !isServiceRunning(getApplicationContext(), MediaPlayerService.class)) {
            mPresenter.buttomPlayClick(mPlayTop);
            return true;
        }
        return handleUserInput(this, event) || super.dispatchKeyEvent(event);
    }


}
