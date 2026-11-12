package com.fanok.audiobooks.model;

import android.content.Context;
import androidx.annotation.NonNull;
import com.fanok.audiobooks.policy.ParentalControl;
import com.fanok.audiobooks.pojo.AudioListPOJO;
import com.fanok.audiobooks.pojo.BookPOJO;
import com.fanok.audiobooks.presenter.BookPresenter;
import com.fanok.audiobooks.room.AppDatabase;
import com.fanok.audiobooks.room.HistoryEntity;
import java.util.ArrayList;
import java.util.List;

/** Reads the books to continue from the history, the stored places and the stored chapters. Off the main thread. */
public final class ContinueModel {

    private ContinueModel() {
    }

    /** At most {@code limit} books, the most recently opened first; books the parental control hides are left out. */
    @NonNull
    public static List<ContinueListening.Item> load(@NonNull Context context, int limit) {
        Context app = context.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(app);
        ParentalControl.Store parental = ParentalControl.store(app);
        AudioDBModel position = new AudioDBModel(app);
        AudioListDBModel chapters = new AudioListDBModel(app);
        float speed = BookPresenter.getSpeed();
        List<ContinueListening.Item> items = new ArrayList<>();
        try {
            List<HistoryEntity> history = db.historyDao().getAll(); // oldest first
            for (int i = history.size() - 1; i >= 0 && items.size() < limit; i--) {
                BookPOJO book = history.get(i).toPojo();
                String url = book.getUrl();
                if (url == null || url.isEmpty() || !ParentalControl.isAllowed(parental, book)) {
                    continue;
                }
                String chapter = position.getName(url);
                int seconds = position.getTime(url);
                List<AudioListPOJO> list = chapters.get(url);
                int percent = list.isEmpty() ? -1 : ListeningProgress.percent(list, chapter, seconds);
                if (!ContinueListening.qualifies(chapter, percent)) {
                    continue;
                }
                long left = list.isEmpty() ? -1 : ContinueListening.leftSeconds(list, chapter, seconds, speed);
                items.add(new ContinueListening.Item(book, percent, chapter, left));
            }
        } finally {
            position.closeDB();
            chapters.closeDB();
        }
        return items;
    }
}
