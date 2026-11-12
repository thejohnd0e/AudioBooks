package com.fanok.audiobooks.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.fanok.audiobooks.model.FavoritePeople;
import com.fanok.audiobooks.model.NewReleases;
import com.fanok.audiobooks.pojo.BookPOJO;
import com.fanok.audiobooks.room.AppDatabase;
import com.fanok.audiobooks.room.PersonBookDao;
import com.fanok.audiobooks.room.PersonBookEntity;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * New books of the favourite authors and readers: once a day, some time after the app starts and only on an unmetered
 * network, the first page of each person's listing on each active site is looked at, and the books not seen before
 * are kept (model.NewReleases, the table person_book) for the Continue screen. Gentle with the sites: at most two sites
 * at once, one request per person and site, a pause between the requests to one site, and a site that answers "too
 * many requests" or refuses is left alone until tomorrow. It never fails the app.
 */
public final class ReleaseCheck {

    /** The setting: on by default. */
    public static final String PREF_ENABLED = "pref_check_releases";
    static final String PREF_CHECKED_AT = "release_check_at";
    /** Persons already looked at once on a site ({@link NewReleases#marker}): the first look makes nothing news. */
    static final String PREF_BASELINED = "release_baselined";
    /** The check waits this long after the start: the user's own pages and the sources' check come first. */
    static final long START_DELAY_SECONDS = 90;
    /** Sites asked at once. */
    static final int PARALLEL = 2;
    /** Between two requests to one site. */
    static final long PAUSE_MS = 2_000;

    private ReleaseCheck() {
    }

    /** One listing to look at. */
    static final class Job {
        final int kind;
        final String key;
        final String url;

        Job(int kind, String key, String url) {
            this.kind = kind;
            this.key = key;
            this.url = url;
        }
    }

    /** Starts the check when it is on, a day has passed and the network is unmetered; never fails. */
    @SuppressLint("CheckResult") // process-wide and short: it ends by itself, nothing to dispose
    public static void checkIfDue(@NonNull Context context) {
        final Context app = context.getApplicationContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (!prefs.getBoolean(PREF_ENABLED, true)
                || !SourceHealth.due(prefs.getLong(PREF_CHECKED_AT, 0), System.currentTimeMillis())
                || !SourceHealth.unmetered(app)) {
            return;
        }
        Flowable.timer(START_DELAY_SECONDS, TimeUnit.SECONDS, Schedulers.io())
                .flatMap(tick -> SourceHealth.unmetered(app) && prefs.getBoolean(PREF_ENABLED, true)
                        ? run(app, prefs) : Flowable.<Integer>empty())
                .subscribe(done -> { }, e -> Log.w("ReleaseCheck", "The release check failed", e),
                        () -> { });
    }

    /** The sites' jobs: {@code people}' listing on each active site that has one; a person has one per site at most. */
    @NonNull
    static Map<Provider, List<Job>> plan(@NonNull List<FavoritePeople.Person> people,
            @NonNull List<Provider> active) {
        Map<Provider, List<Job>> jobs = new EnumMap<>(Provider.class);
        for (FavoritePeople.Person person : people) {
            if (person.key == null) {
                continue;
            }
            for (Provider provider : active) {
                String url = person.urlFor(provider);
                if (url != null) {
                    List<Job> list = jobs.get(provider);
                    if (list == null) {
                        list = new ArrayList<>();
                        jobs.put(provider, list);
                    }
                    list.add(new Job(person.kind, person.key, url));
                }
            }
        }
        return jobs;
    }

    private static Flowable<Integer> run(Context app, SharedPreferences prefs) {
        FavoritePeople favorites = FavoritePeople.get(app);
        List<FavoritePeople.Person> people = new ArrayList<>(favorites.all(FavoritePeople.AUTHOR));
        people.addAll(favorites.all(FavoritePeople.READER));
        forgetRemoved(app, people);
        Map<Provider, List<Job>> jobs = plan(people, Sources.active());
        return Flowable.fromIterable(jobs.entrySet())
                .flatMap(e -> Flowable.fromCallable(() -> {
                    lookAt(app, prefs, e.getKey(), e.getValue());
                    return 1;
                }).subscribeOn(Schedulers.io()), PARALLEL)
                .doOnComplete(() -> {
                    AppDatabase.getDatabase(app).personBookDao()
                            .expireNew(System.currentTimeMillis() - NewReleases.FRESH_MS);
                    prefs.edit().putLong(PREF_CHECKED_AT, System.currentTimeMillis()).apply();
                });
    }

    /** What was kept of persons no longer favourite goes: their books are no news to anyone. */
    private static void forgetRemoved(Context app, List<FavoritePeople.Person> people) {
        PersonBookDao dao = AppDatabase.getDatabase(app).personBookDao();
        for (int kind : new int[] {FavoritePeople.AUTHOR, FavoritePeople.READER}) {
            Set<String> keep = new HashSet<>();
            for (FavoritePeople.Person person : people) {
                if (person.kind == kind) {
                    keep.add(person.key);
                }
            }
            for (String key : dao.getPersonKeys(kind)) {
                if (!keep.contains(key)) {
                    dao.deletePerson(kind, key);
                }
            }
        }
    }

    /** One site's listings, one after another with a pause; stops at the first sign that the site wants peace. */
    private static void lookAt(Context app, SharedPreferences prefs, Provider provider, List<Job> jobs) {
        for (Job job : jobs) {
            if (!SourceHealth.unmetered(app)) {
                return; // the network changed meanwhile
            }
            ProviderPage<BookPOJO> page;
            try {
                page = provider.parser.loadListing(job.url, 1);
            } catch (IOException | RuntimeException e) {
                Log.w("ReleaseCheck", provider.prefValue + ": " + job.url, e);
                page = null;
            }
            if (page != null && (page.status == ProviderPage.Status.RATE_LIMITED
                    || page.status == ProviderPage.Status.BLOCKED || page.challenge)) {
                return;
            }
            if (page != null && !page.isFailure()) {
                keep(app, prefs, provider, job, page.items);
            }
            try {
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    static void keep(Context app, SharedPreferences prefs, Provider provider, Job job,
            List<BookPOJO> items) {
        PersonBookDao dao = AppDatabase.getDatabase(app).personBookDao();
        String marker = NewReleases.marker(job.kind, job.key, provider.prefValue);
        Set<String> looked = new HashSet<>(prefs.getStringSet(PREF_BASELINED, new HashSet<String>()));
        boolean baselined = looked.contains(marker);
        Set<String> known = new HashSet<>(dao.getKnownUrls(job.kind, job.key));
        long now = System.currentTimeMillis();
        for (BookPOJO book : NewReleases.unseen(known, items)) {
            PersonBookEntity row = new PersonBookEntity();
            row.kind = job.kind;
            row.personKey = job.key;
            row.urlBook = book.getUrl();
            row.name = book.getName();
            row.photo = book.getPhoto();
            row.author = book.getAutor();
            row.artist = book.getArtist();
            row.time = book.getTime();
            row.foundAt = now;
            row.isNew = baselined; // the first look only learns what is there
            dao.insert(row);
        }
        if (!baselined) {
            looked.add(marker);
            prefs.edit().putStringSet(PREF_BASELINED, looked).apply();
        }
    }
}
