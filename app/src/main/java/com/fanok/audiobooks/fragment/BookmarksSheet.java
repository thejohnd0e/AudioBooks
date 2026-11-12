package com.fanok.audiobooks.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.fanok.audiobooks.R;
import com.fanok.audiobooks.dialogs.PaletteDialogBuilder;
import com.fanok.audiobooks.model.Bookmarks;
import com.fanok.audiobooks.room.AppDatabase;
import com.fanok.audiobooks.room.BookmarkDao;
import com.fanok.audiobooks.room.BookmarkEntity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * "Закладки": the marks of one book in book order. A tap on a mark goes there, "Заметка" edits its note, the bin
 * removes it (with an undo); "Добавить закладку здесь" marks the place the player is at. Everything is a button:
 * no gesture is the only way to anything.
 */
public class BookmarksSheet extends BottomSheetDialogFragment {

    public static final String TAG = "BookmarksSheet";
    private static final String ARG_URL = "url";

    /** The screen that opened the sheet: where a mark leads and where a new one is made. */
    public interface Host {
        void onBookmarkChosen(@NonNull BookmarkEntity mark);

        void addBookmarkHere();
    }

    @NonNull
    public static BookmarksSheet newInstance(@NonNull String bookUrl) {
        BookmarksSheet sheet = new BookmarksSheet();
        Bundle args = new Bundle();
        args.putString(ARG_URL, bookUrl);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_App_SourceSheet;
    }

    private String mBookUrl = "";
    private final List<BookmarkEntity> mMarks = new ArrayList<>();
    private Adapter mAdapter;
    private TextView mEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_bookmarks, container, false);
        Bundle args = getArguments();
        mBookUrl = args == null || args.getString(ARG_URL) == null ? "" : args.getString(ARG_URL);
        mEmpty = view.findViewById(R.id.bookmarksEmpty);
        RecyclerView list = view.findViewById(R.id.bookmarksList);
        mAdapter = new Adapter();
        list.setAdapter(mAdapter);
        view.findViewById(R.id.bookmarkHere).setOnClickListener(v -> {
            Host host = host();
            if (host != null) {
                host.addBookmarkHere();
            }
            dismiss();
        });
        reload();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            // a short list would otherwise open collapsed to its peek height with the rest out of reach
            ((BottomSheetDialog) getDialog()).getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Nullable
    private Host host() {
        return getActivity() instanceof Host ? (Host) getActivity() : null;
    }

    private BookmarkDao dao() {
        return AppDatabase.getDatabase(requireContext()).bookmarkDao();
    }

    private void reload() {
        mMarks.clear();
        mMarks.addAll(dao().getByBook(mBookUrl));
        mAdapter.notifyDataSetChanged();
        mEmpty.setVisibility(mMarks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void remove(@NonNull BookmarkEntity mark, @NonNull View anchor) {
        dao().delete(mark.id);
        reload();
        Snackbar.make(anchor, R.string.bookmark_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    dao().insert(mark);
                    reload();
                })
                .show();
    }

    /**
     * The note of a mark, asked for in a dialog; stored on "Сохранить" (an empty text clears it), then {@code done}.
     */
    public static void editNote(@NonNull Context context, int id, @Nullable Runnable done) {
        BookmarkDao dao = AppDatabase.getDatabase(context).bookmarkDao();
        BookmarkEntity mark = dao.get(id);
        if (mark == null) {
            return;
        }
        EditText input = new EditText(context);
        input.setHint(R.string.bookmark_note_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(2);
        input.setText(mark.note);
        input.setSelection(input.getText().length());
        int pad = (int) (20 * context.getResources().getDisplayMetrics().density);
        FrameLayout frame = new FrameLayout(context);
        frame.setPadding(pad, pad / 2, pad, 0);
        frame.addView(input);
        new PaletteDialogBuilder(context)
                .setTitle(R.string.bookmark_note_edit)
                .setView(frame)
                .setPositiveButton(R.string.bookmark_save, (d, which) -> {
                    dao.updateNote(id, Bookmarks.cleanNote(input.getText().toString()));
                    if (done != null) {
                        done.run();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private final class Adapter extends RecyclerView.Adapter<Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(mMarks.get(position));
        }

        @Override
        public int getItemCount() {
            return mMarks.size();
        }
    }

    private final class Holder extends RecyclerView.ViewHolder {
        private final TextView chapter = itemView.findViewById(R.id.bookmarkChapter);
        private final TextView meta = itemView.findViewById(R.id.bookmarkMeta);
        private final TextView note = itemView.findViewById(R.id.bookmarkNote);

        Holder(@NonNull View view) {
            super(view);
        }

        void bind(@NonNull BookmarkEntity mark) {
            String name = mark.chapterName;
            chapter.setText(!name.isEmpty() && android.text.TextUtils.isDigitsOnly(name)
                    ? getString(R.string.player_chapter_number, name) : name);
            meta.setText(Bookmarks.time(mark.positionMs) + " · "
                    + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(mark.createdAt)));
            note.setText(mark.note);
            note.setVisibility(mark.note == null ? View.GONE : View.VISIBLE);
            itemView.findViewById(R.id.bookmarkRow).setOnClickListener(v -> {
                Host host = host();
                if (host != null) {
                    host.onBookmarkChosen(mark);
                }
                dismiss();
            });
            itemView.findViewById(R.id.bookmarkEdit).setOnClickListener(
                    v -> editNote(v.getContext(), mark.id, BookmarksSheet.this::reload));
            itemView.findViewById(R.id.bookmarkDelete).setOnClickListener(v -> remove(mark, requireView()));
        }
    }
}
