# Book Screen Actions Implementation Plan

> **For agentic workers:** Use executing-plans to implement this plan task-by-task; check off each step. No commits unless requested.

**Goal:** Simplify the book screen while keeping playback, favorites, and download state accurate.

**Architecture:** Keep existing book activity, description fragment, playback service, and download machinery; route controls to existing actions and derive visible state from their authoritative models. Keep the player associated with the active book and identify it explicitly.

**Tech Stack:** Android Java 8, Material Components, JUnit, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-12-book-actions-design.md`

## Global Constraints

- Preserve existing playback and saved data, localized strings, TV focus, and theme support.
- No emulator or device operations without explicit permission; one Gradle build at a time; no clean or commit.
- Retain all book menu actions in the overflow or expanded player; don't erase existing preferences.

## Review Focus

- Another book playing: description action starts the displayed book rather than toggling the other one.
- Paused or stopped playback: button state tracks service events and displayed book identity.
- Partially downloaded or actively downloading: never report fully downloaded; avoid destructive actions.
- Unsupported download or empty chapter list: no misleading enabled download action.
- Rotation, theme, narrow screens and TV: state restores, labels remain readable, and controls remain focusable.

### Task 1: Toolbar and player

**Files:** `app/src/main/java/com/fanok/audiobooks/activity/BookActivity.java`, `app/src/main/res/menu/book_activity_options_menu.xml`, book toolbar preferences, `app/src/main/res/layout/player.xml`, relevant dimension resources, and related tests.

- [ ] Add tests verifying all book menu actions stay accessible in overflow regardless of saved toolbar settings, and the player row has the intended larger height without changing its controls.
- [ ] Run focused tests and confirm they fail for the expected missing behavior.
- [ ] Remove action icons from the book toolbar, retain back/overflow, revise the obsolete settings entry without deleting stored preferences, and increase the collapsed player height; identify the active book in the collapsed row while preserving chapter and selection controls.
- [ ] Verify focused tests, screen-size layout constraints and TV focus behavior.

### Task 2: Book-specific playback and favorites

**Files:** `app/src/main/java/com/fanok/audiobooks/fragment/DescriptionBookFragment.java`, `app/src/main/java/com/fanok/audiobooks/activity/BookActivity.java`, playback presenter/service as necessary, strings and `app/src/test/java/`.

- [ ] Add focused tests for a different active book, the same playing book, the same paused book, no active book, and live favorite changes; verify they fail first.
- [ ] Connect the description button to the displayed book's identity and playback state; make its label and icon update on playback events, including pause and resume.
- [ ] Bind the favorite button to the existing favorite model with filled/accented icon and localized state text; keep toggle behavior and accessibility labels.
- [ ] Verify state changes and subscriptions across lifecycle recreation and focused tests.

### Task 3: Download state and verification

**Files:** `app/src/main/java/com/fanok/audiobooks/fragment/DescriptionBookFragment.java`, existing download state components, string resources and tests.

- [ ] Add focused tests for not downloaded, active progress, partial, complete, failed and unsupported downloads, including a change while the screen is open; verify failure first.
- [ ] Bind the download control to authoritative chapter/download status; show progress/partial/full labels, keep deletion a separate confirmed action, and retain existing download eligibility behavior.
- [ ] Run `gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` sequentially in one Gradle invocation; report actual results and any verification limitations. Do not run an emulator.
