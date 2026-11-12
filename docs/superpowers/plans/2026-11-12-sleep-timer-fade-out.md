# Sleep Timer Fade-Out Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When the sleep timer fires (countdown or clock mode), glide the volume to silence over ~15 seconds before the existing pause/stop, always, regardless of the "warn" (beeps) and "shake" options.

**Architecture:** A final-fade state machine in `MediaPlayerService`, mirroring the existing warning-fade style: a saved pre-fade volume, `handler`-scheduled ticks that call `player.setVolume(...)`, and restoration on every path that cancels it. The existing stop logic (`pause()` + wake window, or `stop()`) runs when the fade completes.

**Tech Stack:** Java, ExoPlayer (`player.setVolume/getVolume`), Android `Handler`, MVP service as-is; instrumented tests (`androidTest`) with the existing MockWebServer harness.

**Spec:** `docs/superpowers/specs/2026-11-12-sleep-timer-fade-out-design.md`

## Global Constraints

- Fade duration `SLEEP_FADE_OUT_MS = 15_000` ms; tick period `SLEEP_FADE_OUT_TICK_MS = 250` ms (spec: "~250–500 мс").
- Fade starts from the volume level current at fire time (with "warn" on that is the warning's 15% level — no jump back up to 100%).
- End-of-chapter mode: no fade, unchanged behavior.
- Every cancel path restores the volume before the next play: pause, play, stop, timer cancel, extend.
- No UI/string changes; beep sound untouched (separate task).
- All service mutations on the main thread (handler is the main-thread `handler` already used by the warning fade).

## Review Focus

- **Clock-mode timer firing while the book is paused**: no fade of a silent player — the existing immediate stop path must run (`wantPlaying`/`playWhenWhenReady` check at fire).
- **Timer re-armed (or extended) while a fade runs**: stale fade callbacks must not pause the book afterwards — cancel removes `handler` callbacks.
- **Chapter auto-advance mid-fade**: the fade continues and still ends in a pause; volume restored on the next manual play.
- **Book replaced mid-fade (`stop()`/`apply()`)**: volume restored so the new book is not near-silent.
- **Fade completes with "shake" on**: wake window opens after the fade-done pause exactly as it does today at fire time.

---

### Task 1: The final fade in the service

**Files:**
- Modify: `app/src/main/java/com/fanok/audiobooks/service/MediaPlayerService.java` (sleep timer section, ~lines 2564–2900, plus `pause()` ~line 830 and `play()` ~line 760)
- Test: `app/src/androidTest/java/com/fanok/audiobooks/service/MediaPlayerTestSleepFade.java` (new class, following `MediaPlayerServiceTest` helpers)

**Interfaces:**
- Consumes: existing `player.setVolume(float)`, `player.getVolume()`, `handler`, `stopSleepWarning()`, `openWakeWindow()`, `pause()`, `stop()`, `onSleepTimerFired(boolean)`, `releaseSleepTimer()`.
- Produces:
  - `private static final long SLEEP_FADE_OUT_MS = 15_000;`
  - `private static final long SLEEP_FADE_OUT_TICK_MS = 250;`
  - `private boolean fadingOut;` `private float volumeBeforeFadeOut = -1;` `private long fadeOutStart;` (uptime of the fade's start, for level math)
  - `private void startSleepFadeOut()` / `private boolean onSleepFadeOutTick()` (returns done) / `private void finishSleepFadeOut()` / `private void cancelSleepFadeOut()`
  - `@VisibleForTesting public void setSleepFadeOutMsForTest(long ms)` — swaps the fade length so tests run in ~1 s.

- [ ] **Step 1: Write the failing instrumented test**

New class `MediaPlayerTestSleepFade` modeled on `MediaPlayerServiceTest` (same `book(...)`, `startAndBind`, `onMain`, `await`, `player(service)` helpers — copy the harness setup from `MediaPlayerServiceTest`'s beginning, or extend that class if its harness is reusable):

```java
@Test
public void timerFiresIntoAFadeThenStops() throws Exception {
    MediaPlayerService service = startAndBind(book("fade", 20), true);
    await("playing", 8_000, () -> player(service).isPlaying());
    onMain(() -> { service.setSleepFadeOutMsForTest(1_000); return null; });
    float before = onMain(() -> player(service).getVolume());

    context.sendBroadcast(new Intent(SleepTimerPresenter.Broadcast_SleepTimer)
            .putExtra("start", true).putExtra("time", 2_000L).setPackage(context.getPackageName()));

    await("volume gliding down", 3_000, () -> player(service).getVolume() < before);
    assertTrue(onMain(() -> player(service).isPlaying())); // fade, not a stop
    await("stopped after the fade", 8_000, () -> !service.isRunning() || !service.isWantPlaying());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fanok.audiobooks.service.MediaPlayerTestSleepFade` (device connected)
Expected: FAIL — playback stops hard at fire, volume never drops.

- [ ] **Step 3: Implement the fade in `MediaPlayerService.java`**

In the sleep timer section, add the fields/constants above and:

```java
/** The timer ran out: the volume glides to silence, then the stop the timer always made. */
private void startSleepFadeOut() {
    if (player == null || !wantPlaying || !player.getPlayWhenReady()) {
        finishSleepFadeOut(); // nothing audible to fade: the stop as it always was
        return;
    }
    fadingOut = true;
    volumeBeforeFadeOut = player.getVolume();
    fadeOutStart = SystemClock.uptimeMillis();
    handler.postDelayed(this::runSleepFadeOutTick, SLEEP_FADE_OUT_TICK_MS);
}

private void runSleepFadeOutTick() {
    if (!fadingOut) return;
    long left = fadeOutLength() - (SystemClock.uptimeMillis() - fadeOutStart);
    if (left <= 0) { player.setVolume(0f); finishSleepFadeOut(); return; }
    player.setVolume(volumeBeforeFadeOut * (left / (float) fadeOutLength()));
    handler.postDelayed(this::runSleepFadeOutTick, SLEEP_FADE_OUT_TICK_MS);
}

/** The fade is over: volume back first so the next play is normal, then the timed stop as before. */
private void finishSleepFadeOut() {
    cancelSleepFadeOut();
    if (sleepShake && wantPlaying && hasAccelerometer()) {
        pause();
        openWakeWindow();
    } else {
        stop();
    }
}

/** Stops the fade and puts the volume back; -1 means nothing to undo. */
private void cancelSleepFadeOut() {
    handler.removeCallbacks(this::runSleepFadeOutTick);
    fadingOut = false;
    if (volumeBeforeFadeOut >= 0) {
        if (player != null) player.setVolume(volumeBeforeFadeOut);
        volumeBeforeFadeOut = -1;
    }
}
```

with `fadeOutLength()` reading a field initialized to `SLEEP_FADE_OUT_MS` that `setSleepFadeOutMsForTest(long)` overrides. Key integration points (exact behavior, one line each):

- `onSleepTimerFired(boolean endOfChapter)`: for the timed branch replace the `if (sleepShake…) pause()+openWakeWindow() else stop()` tail with `startSleepFadeOut()`; the `endOfChapter` branch keeps returning early as today. Capture the current level before `stopSleepWarning()` runs, and after it restores the warning volume set `player.setVolume(<captured>)` in the same main-thread turn, so the fade starts where the warning left off (no jump); then start the fade.
- `pause()` and `play()`: first statement `cancelSleepFadeOut();`.
- `stop()`: its `releaseSleepTimer()` call covers the cancel; add `cancelSleepFadeOut()` into `releaseSleepTimer()` before `stopSleepWarning()` so cancel/extend/book-change paths restore too.
- `extendSleepTimer(...)`: if `fadingOut`, `cancelSleepFadeOut()` then re-arm (`armSleepTimer(sleepMode == MODE_CLOCK ? SLEEP_CLOCK_EXTEND_MS : sleepDurationMs)`) so "продлить" revives playback at full volume.

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fanok/audiobooks/service/MediaPlayerService.java app/src/androidTest/java/com/fanok/audiobooks/service/MediaPlayerTestSleepFade.java
git commit -m "feat(sleep-timer): fade volume to silence over 15 s before the timed stop"
```

---

### Task 2: Fade interactions — cancel, restore, warn continuity

**Files:**
- Modify if needed: `app/src/main/java/com/fanok/audiobooks/service/MediaPlayerService.java`
- Test: `app/src/androidTest/java/com/fanok/audiobooks/service/MediaPlayerTestSleepFade.java` (add cases)

**Interfaces:**
- Consumes: Task 1's `setSleepFadeOutMsForTest`, `cancelSleepFadeOut` behavior.

- [ ] **Step 1: Write the failing tests (three cases in `MediaPlayerTestSleepFade`)**

- `pauseDuringFadeRestoresVolumeAndPausesNow`: arm 2 s timer with 1 s test fade; during the fade `sendAction(MediaPlayerService.ACTION_PAUSE)`; await paused within ~1 s; assert `player(service).getVolume()` back at the pre-fade level.
- `cancelBroadcastDuringFadeRestoresVolume`: during the fade send `Broadcast_SleepTimer` with `start=false`; await still playing; assert volume restored and playback continues (fade callbacks gone: still playing 2 s later).
- `timerFiresWhilePausedStopsWithoutAFade`: `startAndBind(book("paused", 20), false)`, await ready-and-paused; arm a clock-mode timer ~2 s ahead (`"at"` = now + 2000) with the test fade 1 s; await service stopped/paused — the guard in `startSleepFadeOut()` must take the immediate path, no crash, no stray ticks.
- `warnOnFadesFromTheWarningsLevelNotFromFull`: arm timer with `warn=true` (and `shake=false`), test fade 1 s; the warning fade would be skipped for a short countdown, so instead assert the general contract at fire time: right after the timer fires, volume is below the pre-timer level and monotonically non-increasing across two samples 300 ms apart (no restore-jump to 1.0 between warning and fade).

- [ ] **Step 2: Run tests to verify the gap**

Run: same `connectedDebugAndroidTest` command with the class filter.
Expected: at least the pause/cancel cases FAIL until Task 1's cancel wiring is confirmed complete; warn case may pass already.

- [ ] **Step 3: Fix what the tests catch**

Likely gaps: missing `cancelSleepFadeOut()` in `pause()`/`play()`/`releaseSleepTimer()`, or the volume-jump at the warning→fade handoff in `onSleepTimerFired`.

- [ ] **Step 4: Run the full sleep-timer test set**

Run: `.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fanok.audiobooks.service.MediaPlayerTestSleepFade,com.fanok.audiobooks.service.MediaPlayerServiceTest`
Expected: all PASS (existing `endOfChapterTimerFollowsTheCurrentChapter` must stay green — no fade in end-of-chapter mode).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fanok/audiobooks/service/MediaPlayerService.java app/src/androidTest/java/com/fanok/audiobooks/service/MediaPlayerTestSleepFade.java
git commit -m "fix(sleep-timer): restore volume on every path that cancels the fade-out"
```

---

### Task 3: Build the APK for the user

**Files:**
- Build output: `dist/` (per project convention).

- [ ] **Step 1: Assemble debug/release APK** per `docs/howto.md` / the android-apk-signing skill, install on the user's device, and have the user try a short sleep timer with options off.

- [ ] **Step 2: Commit any dist artifacts per repo convention.**
