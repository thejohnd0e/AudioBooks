package com.fanok.audiobooks.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fanok.audiobooks.R;
import com.fanok.audiobooks.download.DownloadStatus;
import com.fanok.audiobooks.service.Checkpoint;
import java.util.Collections;
import org.junit.Test;

public class BookActionStateTest {
    @Test public void listenButtonShowsListenForFreshOrOtherPlayingBook() {
        DescriptionBookFragment.ListenAction fresh =
                DescriptionBookFragment.listenAction("http://b", null, true, null);
        assertEquals(R.string.action_listen, fresh.textRes);
        assertEquals(R.drawable.ic_play, fresh.iconRes);
        assertEquals(DescriptionBookFragment.ACTION_START_THIS, fresh.action);

        DescriptionBookFragment.ListenAction other =
                DescriptionBookFragment.listenAction("http://b", "http://a", true, null);
        assertEquals(R.string.action_listen, other.textRes);
        assertEquals(DescriptionBookFragment.ACTION_START_THIS, other.action);
    }

    @Test public void listenButtonShowsPauseWhileThisBookPlays() {
        DescriptionBookFragment.ListenAction playing =
                DescriptionBookFragment.listenAction("http://b", "http://b", true, new Checkpoint("http://b", 0, 0));
        assertEquals(R.string.pause, playing.textRes);
        assertEquals(R.drawable.ic_pause, playing.iconRes);
        assertEquals(DescriptionBookFragment.ACTION_TOGGLE_ACTIVE, playing.action);
    }

    @Test public void listenButtonShowsResumeWhenThisBookIsPausedWithProgress() {
        //the page loads its book into the player paused: a never-listened book still says «Слушать»
        DescriptionBookFragment.ListenAction pausedFresh =
                DescriptionBookFragment.listenAction("http://b", "http://b", false, null);
        assertEquals(R.string.action_listen, pausedFresh.textRes);
        assertEquals(R.drawable.ic_play, pausedFresh.iconRes);
        assertEquals(DescriptionBookFragment.ACTION_TOGGLE_ACTIVE, pausedFresh.action);

        DescriptionBookFragment.ListenAction pausedActive =
                DescriptionBookFragment.listenAction("http://b", "http://b", false,
                        new Checkpoint("http://b", 0, 30_000));
        assertEquals(R.string.action_resume, pausedActive.textRes);
        assertEquals(R.drawable.ic_play, pausedActive.iconRes);
        assertEquals(DescriptionBookFragment.ACTION_TOGGLE_ACTIVE, pausedActive.action);

        DescriptionBookFragment.ListenAction withCheckpoint =
                DescriptionBookFragment.listenAction("http://b", "http://a", true, new Checkpoint("http://b", 1, 0));
        assertEquals(R.string.action_resume, withCheckpoint.textRes);
        assertEquals(DescriptionBookFragment.ACTION_START_THIS, withCheckpoint.action);

        DescriptionBookFragment.ListenAction liveProgress =
                DescriptionBookFragment.listenAction("http://b", null, false, null, 0, 15_000);
        assertEquals(R.string.action_resume, liveProgress.textRes);
        assertEquals(DescriptionBookFragment.ACTION_START_THIS, liveProgress.action);
    }

    @Test public void favoriteButtonReflectsBookFavoriteState() {
        DescriptionBookFragment.FavoriteAction off = DescriptionBookFragment.favoriteAction(false);
        assertEquals(R.string.action_to_favorites, off.textRes);
        assertEquals(R.drawable.ic_favorite_add, off.iconRes);
        assertEquals(R.attr.colorAccentPrimary, off.tintAttr);

        DescriptionBookFragment.FavoriteAction on = DescriptionBookFragment.favoriteAction(true);
        assertEquals(R.string.action_in_favorites, on.textRes);
        assertEquals(R.drawable.ic_star_toggle_on, on.iconRes);
        assertEquals(R.attr.favoriteColor, on.tintAttr);
    }

    @Test public void downloadButtonReflectsNoneRunningPartialCompleteAndFailed() {
        assertFalse(DescriptionBookFragment.downloadAction(false, null, "12/40").visible);

        DescriptionBookFragment.DownloadAction none = DescriptionBookFragment.downloadAction(true, null, null);
        assertTrue(none.visible);
        assertEquals(R.string.action_download, none.textRes);
        assertEquals(R.drawable.ic_download, none.iconRes);
        assertFalse(none.highlighted);
        assertFalse(none.opensDownloads);

        DownloadStatus running = DownloadStatus.compute(Collections.singletonList(
                new DownloadStatus.Part("1", DownloadStatus.STATE_DOWNLOADING, 50, 100, 50f, 0, null)),
                0, 2, 2, new DownloadStatus.Live(false, false, 10, 0));
        DescriptionBookFragment.DownloadAction active = DescriptionBookFragment.downloadAction(true, running, "скачивается 1/2");
        assertEquals("скачивается 1/2", active.text);
        assertEquals(R.drawable.ic_progress_download, active.iconRes);
        assertTrue(active.highlighted);
        assertTrue(active.opensDownloads);

        DownloadStatus partialStatus = DownloadStatus.compute(Collections.emptyList(), 1, 1, 2, DownloadStatus.Live.NONE);
        DescriptionBookFragment.DownloadAction partial = DescriptionBookFragment.downloadAction(true, partialStatus, "1/2 главы");
        assertEquals("1/2 главы", partial.text);
        assertEquals(R.drawable.ic_download, partial.iconRes);
        assertTrue(partial.highlighted);
        assertFalse(partial.opensDownloads);

        DownloadStatus fullStatus = DownloadStatus.compute(Collections.emptyList(), 2, 2, 2, DownloadStatus.Live.NONE);
        DescriptionBookFragment.DownloadAction full = DescriptionBookFragment.downloadAction(true, fullStatus, "Скачано полностью");
        assertEquals(R.string.action_downloaded, full.textRes);
        assertEquals(R.drawable.ic_check_circle_outline, full.iconRes);
        assertTrue(full.highlighted);
        assertFalse(full.opensDownloads);

        DownloadStatus failedStatus = DownloadStatus.compute(Collections.singletonList(
                new DownloadStatus.Part("1", DownloadStatus.STATE_FAILED, 0, 100, 0f, 0, "err")),
                0, 2, 2, DownloadStatus.Live.NONE);
        DescriptionBookFragment.DownloadAction failed = DescriptionBookFragment.downloadAction(true, failedStatus, "Ошибка 0/2");
        assertEquals("Ошибка 0/2", failed.text);
        assertEquals(R.drawable.ic_download, failed.iconRes);
        assertFalse(failed.opensDownloads);
    }
}
