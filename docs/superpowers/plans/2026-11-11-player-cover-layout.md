# Player Cover Layout Implementation Plan

**Goal:** Show the full book cover prominently above a bottom-aligned, scrollable chapter list.

**Architecture:** Adapt the existing player layout to share available height between the cover and chapter list. Limit the list to 16 rows and reserve room for the cover on short screens. Preserve the existing player controls and alternate TV/tablet layouts unless deliberately adapted.

**Tech Stack:** Android XML, Java, JUnit, Gradle.

**Spec:** Conversation: cover without cropping; up to 16 visible chapters, fewer when space is limited; controls remain at the bottom.

## Global Constraints

- Keep work on the existing player-cover branch; do not commit or push without a request.
- Preserve application data and debug signing; no emulator or device operations without permission.
- Preserve existing cover loading and parental-control behavior.

## Review Focus

- Fewer than 16 chapters: list ends at the controls and cover uses remaining space.
- More than 16 chapters: list remains scrollable and no more than 16 rows are shown.
- Small screens and enlarged fonts: controls and some chapters remain accessible; cover retains space.
- Missing or failed cover: avoid reserving unusable blank space.
- Alternate layouts and book-cover aspect ratios: no crashes or cropping.

### Task 1: Adapt and test player layout

**Files:** `app/src/main/res/layout/player.xml`, `app/src/main/java/com/fanok/audiobooks/activity/BookActivity.java`, `app/src/test/java/com/fanok/audiobooks/PlayerCoverLayoutTest.java`; add a small sizing helper if needed.

- [ ] Inspect chapter row dimensions, layout variants, and existing cover behavior.
- [ ] Write tests for the sizing policy (0, fewer than 16, more than 16, constrained height) and aspect-preserving layout; run and confirm failure.
- [ ] Implement adaptive sizing and an uncropped cover, retaining the chapter list's scrolling and bottom controls.
- [ ] Run unit tests and assemble a debug APK compatible with the user's existing debug application ID; verify APK identity and avoid changing committed signing configuration.
- [ ] Report results and limitations; request visual verification without claiming device testing.
