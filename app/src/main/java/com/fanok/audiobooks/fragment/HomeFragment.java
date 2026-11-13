package com.fanok.audiobooks.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fanok.audiobooks.R;
import com.fanok.audiobooks.activity.MainActivity;
import com.fanok.audiobooks.adapter.CoverBinder;
import com.fanok.audiobooks.model.BooksDBModel;
import com.fanok.audiobooks.model.ContinueListening;
import com.fanok.audiobooks.model.ContinueModel;
import com.fanok.audiobooks.model.NewReleases;
import com.fanok.audiobooks.policy.ParentalControl;
import com.fanok.audiobooks.pojo.BookPOJO;
import com.fanok.audiobooks.room.AppDatabase;
import com.fanok.audiobooks.room.PersonBookEntity;
import com.fanok.audiobooks.service.MediaPlayerService;
import com.fanok.audiobooks.util.RemainingTime;
import com.google.android.material.navigation.NavigationView;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

/**
 * "Продолжить": the books listened to and not finished with a play button that resumes them where they stopped,
 * and the downloaded ones below. Read from the database only, so it opens at once; read again whenever the screen
 * comes back, since listening moves the places. Without either it offers the catalog.
 */
public class HomeFragment extends Fragment {

    public static final String TAG = "home";
    private static final String LOG = "HomeFragment";
    /** New books of the favourite people shown in the lane. */
    private static final int FRESH_LIMIT = 20;

    /** What one read returned. */
    private static final class Content {
        final List<ContinueListening.Item> keep;
        final List<BookPOJO> saved;
        /** New books of the favourite people (search.ReleaseCheck). */
        final List<BookPOJO> fresh;

        Content(List<ContinueListening.Item> keep, List<BookPOJO> saved, List<BookPOJO> fresh) {
            this.keep = keep;
            this.saved = saved;
            this.fresh = fresh;
        }
    }

    private final CompositeDisposable mDisposables = new CompositeDisposable();
    private final ContinueAdapter mContinue = new ContinueAdapter();
    private final SavedAdapter mSaved = new SavedAdapter();
    private final SavedAdapter mFresh = new SavedAdapter();
    private View mNewTitle;
    private View mNewList;
    private View mContinueTitle;
    private View mSavedTitle;
    private View mSavedList;
    private View mContinueList;
    private View mEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mContinueTitle = view.findViewById(R.id.homeContinueTitle);
        mSavedTitle = view.findViewById(R.id.homeSavedTitle);
        mEmpty = view.findViewById(R.id.homeEmpty);
        mContinueList = view.findViewById(R.id.homeContinueList);
        mSavedList = view.findViewById(R.id.homeSavedList);
        ((RecyclerView) mContinueList).setAdapter(mContinue);
        RecyclerView saved = (RecyclerView) mSavedList;
        saved.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        saved.setAdapter(mSaved);
        mNewTitle = view.findViewById(R.id.homeNewTitle);
        mNewList = view.findViewById(R.id.homeNewList);
        RecyclerView fresh = (RecyclerView) mNewList;
        fresh.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        fresh.setAdapter(mFresh);
        view.findViewById(R.id.homeOpenCatalog).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openSection(R.id.nav_audiobooks);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.menu_home);
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            NavigationView navigationView = activity.getNavigationView();
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_home);
            } else {
                activity.highlightNav(R.id.layout_nav_home);
            }
        }
        load();
    }

    @Override
    public void onDestroyView() {
        mDisposables.clear();
        super.onDestroyView();
    }

    private void load() {
        final android.content.Context app = requireContext().getApplicationContext();
        mDisposables.add(Single.fromCallable(() -> {
            BooksDBModel books = new BooksDBModel(app);
            try {
                List<BookPOJO> saved = new ArrayList<>();
                for (BookPOJO book : books.getAllSaved()) {
                    if (ParentalControl.isAllowed(app, book)) {
                        saved.add(book);
                    }
                }
                List<BookPOJO> fresh = new ArrayList<>();
                for (PersonBookEntity row : AppDatabase.getDatabase(app).personBookDao().getNew(
                        System.currentTimeMillis() - NewReleases.FRESH_MS, FRESH_LIMIT)) {
                    BookPOJO book = row.toPojo();
                    if (ParentalControl.isAllowed(app, book)) {
                        fresh.add(book);
                    }
                }
                return new Content(ContinueModel.load(app, ContinueListening.LIMIT), saved, fresh);
            } finally {
                books.closeDB();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::show, e -> Log.w(LOG, "cannot read the home screen", e)));
    }

    private void show(@NonNull Content content) {
        if (getView() == null) {
            return;
        }
        mContinue.set(content.keep);
        mSaved.set(content.saved);
        mFresh.set(content.fresh);
        boolean any = !content.keep.isEmpty();
        boolean anySaved = !content.saved.isEmpty();
        boolean anyFresh = !content.fresh.isEmpty();
        mNewTitle.setVisibility(anyFresh ? View.VISIBLE : View.GONE);
        mNewList.setVisibility(anyFresh ? View.VISIBLE : View.GONE);
        mContinueTitle.setVisibility(any ? View.VISIBLE : View.GONE);
        mContinueList.setVisibility(any ? View.VISIBLE : View.GONE);
        mSavedTitle.setVisibility(anySaved ? View.VISIBLE : View.GONE);
        mSavedList.setVisibility(anySaved ? View.VISIBLE : View.GONE);
        mEmpty.setVisibility(any || anySaved || anyFresh ? View.GONE : View.VISIBLE);
    }

    private void open(@NonNull BookPOJO book) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBooksActivity(book);
        }
    }

    /** Starts the book where it stopped, as the lock screen's and the car's play does; the screen stays. */
    private void resume(@NonNull BookPOJO book) {
        String url = book.getUrl();
        if (url == null) {
            return;
        }
        try {
            ContextCompat.startForegroundService(requireContext(),
                    new Intent(requireContext(), MediaPlayerService.class).setAction(MediaPlayerService.ACTION_PLAY)
                            .putExtra(MediaPlayerService.EXTRA_BOOK_URL, url));
            Toast.makeText(requireContext(), getString(R.string.home_resuming, book.getName()), Toast.LENGTH_SHORT)
                    .show();
        } catch (RuntimeException e) { // ForegroundServiceStartNotAllowedException is an IllegalStateException
            Log.w(LOG, "resume: the player cannot start", e);
            open(book);
        }
    }

    private final class ContinueAdapter extends RecyclerView.Adapter<ContinueAdapter.Holder> {
        private final List<ContinueListening.Item> mItems = new ArrayList<>();

        void set(@NonNull List<ContinueListening.Item> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_continue, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            private final ImageView cover = itemView.findViewById(R.id.continueCover);
            private final TextView title = itemView.findViewById(R.id.continueTitle);
            private final TextView meta = itemView.findViewById(R.id.continueMeta);
            private final ProgressBar progress = itemView.findViewById(R.id.continueProgress);
            private final ImageButton play = itemView.findViewById(R.id.continuePlay);

            Holder(@NonNull View view) {
                super(view);
            }

            void bind(@NonNull ContinueListening.Item item) {
                BookPOJO book = item.book;
                title.setText(book.getName());
                String chapter = !item.chapter.isEmpty() && android.text.TextUtils.isDigitsOnly(item.chapter)
                        ? getString(R.string.player_chapter_number, item.chapter) : item.chapter;
                meta.setText(item.leftSeconds >= 0
                        ? getString(R.string.home_left, chapter, RemainingTime.format(item.leftSeconds)) : chapter);
                progress.setVisibility(item.percent >= 0 ? View.VISIBLE : View.GONE);
                progress.setProgress(Math.max(0, item.percent));
                CoverBinder.bind(cover, book);
                itemView.findViewById(R.id.continueCard).setOnClickListener(v -> open(book));
                play.setVisibility(item.canResume() ? View.VISIBLE : View.GONE);
                play.setContentDescription(getString(R.string.home_play_book, book.getName()));
                play.setOnClickListener(v -> resume(book));
                if (com.fanok.audiobooks.util.Tv.is(requireContext())) {
                    // the whole plate is one stop for the remote, with one ring; OK opens the book
                    play.setFocusable(false);
                    View card = itemView.findViewById(R.id.continueCard);
                    card.setFocusable(false);
                    card.setClickable(false);
                    itemView.setFocusable(true);
                    itemView.setClickable(true);
                    itemView.setOnClickListener(v -> open(book));
                }
            }
        }
    }

    private final class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.Holder> {
        private final List<BookPOJO> mBooks = new ArrayList<>();

        void set(@NonNull List<BookPOJO> books) {
            mBooks.clear();
            mBooks.addAll(books);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_saved, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            BookPOJO book = mBooks.get(position);
            holder.title.setText(book.getName());
            CoverBinder.bind(holder.cover, book);
            holder.itemView.setOnClickListener(v -> open(book));
        }

        @Override
        public int getItemCount() {
            return mBooks.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            private final ImageView cover = itemView.findViewById(R.id.savedCover);
            private final TextView title = itemView.findViewById(R.id.savedTitle);

            Holder(@NonNull View view) {
                super(view);
            }
        }
    }
}
