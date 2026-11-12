package com.fanok.audiobooks.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.fanok.audiobooks.R;
import com.fanok.audiobooks.activity.MainActivity;
import com.fanok.audiobooks.model.ListenStore;
import com.fanok.audiobooks.model.ListeningStats;
import com.fanok.audiobooks.room.AppDatabase;
import com.fanok.audiobooks.util.ListeningClock;
import com.fanok.audiobooks.util.RemainingTime;
import com.fanok.audiobooks.view.DayBarsView;
import com.google.android.material.navigation.NavigationView;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Map;
import java.util.TimeZone;

/**
 * "Статистика": the time really listened to today, in the week, the month and all, the 30 days as bars, the books
 * listened to the end and the days in a row. Read from the database, so it opens at once; the figures come from
 * model.ListeningStats.
 */
public class StatsFragment extends Fragment {

    public static final String TAG = "stats";

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    /** What one read returned. */
    private static final class Data {
        final Map<Integer, Long> days;
        final int today;
        final int finished;
        final int finishedMonth;

        Data(Map<Integer, Long> days, int today, int finished, int finishedMonth) {
            this.days = days;
            this.today = today;
            this.finished = finished;
            this.finishedMonth = finishedMonth;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.menu_stats);
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            NavigationView navigationView = activity.getNavigationView();
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_stats);
            } else {
                activity.highlightNav(R.id.layout_nav_stats);
            }
        }
        final android.content.Context app = requireContext().getApplicationContext();
        mDisposables.add(Single.fromCallable(() -> {
            long now = System.currentTimeMillis();
            int today = ListeningClock.dayOf(now, TimeZone.getDefault().getOffset(now));
            AppDatabase db = AppDatabase.getDatabase(app);
            return new Data(ListenStore.days(app), today, db.finishedBookDao().count(),
                    db.finishedBookDao().countSince(now - ListeningStats.CHART_DAYS * 24L * 3600_000L));
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::show, e -> Log.w(TAG, "cannot read the statistics", e)));
    }

    @Override
    public void onDestroyView() {
        mDisposables.clear();
        super.onDestroyView();
    }

    private static String time(long ms) {
        return RemainingTime.format(ms / 1000);
    }

    private void show(@NonNull Data data) {
        View view = getView();
        if (view == null) {
            return;
        }
        long todayMs = ListeningStats.sum(data.days, data.today, 1);
        long total = ListeningStats.total(data.days);
        ((TextView) view.findViewById(R.id.statsToday)).setText(time(todayMs));
        ((TextView) view.findViewById(R.id.statsWeek)).setText(time(ListeningStats.sum(data.days, data.today, 7)));
        ((TextView) view.findViewById(R.id.statsMonth)).setText(
                time(ListeningStats.sum(data.days, data.today, ListeningStats.CHART_DAYS)));
        ((TextView) view.findViewById(R.id.statsTotal)).setText(time(total));
        ((TextView) view.findViewById(R.id.statsFinished)).setText(String.valueOf(data.finished));
        ((TextView) view.findViewById(R.id.statsFinishedMonth)).setText(
                getString(R.string.stats_finished_month, data.finishedMonth));
        ((TextView) view.findViewById(R.id.statsStreak)).setText(
                getString(R.string.stats_days, ListeningStats.streak(data.days, data.today)));
        DayBarsView chart = view.findViewById(R.id.statsChart);
        chart.setValues(ListeningStats.lastDays(data.days, data.today, ListeningStats.CHART_DAYS));
        chart.setContentDescription(getString(R.string.stats_chart_desc, time(todayMs)));
        view.findViewById(R.id.statsEmpty).setVisibility(total == 0 && data.finished == 0 ? View.VISIBLE : View.GONE);
    }
}
