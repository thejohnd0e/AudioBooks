# Akniga description formatting Implementation Plan

> **For agentic workers:** Use the executing-plans skill to implement this plan. Steps use checkbox syntax for tracking.

**Goal:** Make Akniga's overview readable without losing useful description metadata.

**Architecture:** Extract structured text from Akniga's existing description elements before they are flattened. Keep the existing expandable/copyable text view and all other source adapters unchanged.

**Tech Stack:** Java 8, Jsoup, JUnit 4, Android Gradle.

**Spec:** Approved design in the conversation: separate description, collections, alternate names and date; omit duplicate other readings only when available in the readings tab; preserve copying and collapsing.

## Global Constraints

- Do not change other providers or device data; no emulator or ADB without an explicit request.
- Do not commit without an explicit request; run Gradle builds sequentially.

## Review Focus

- Existing HTML with paragraphs and `<br>`: retain meaningful paragraph breaks.
- Other readings absent or unavailable to the dedicated tab: do not silently discard information.
- Optional and empty sections: do not produce empty headings or excessive whitespace.
- Nested site markup and plain-text fallback: avoid duplicated text and retain fallback behavior.
- Existing saved book fixtures and collection pages: preserve their parsed content and metadata.

## Task 1: Parse readable Akniga overview

**Files:**
- Modify: `app/src/main/java/com/fanok/audiobooks/source/Akniga.java`
- Test: add an Akniga-focused test in `app/src/test/java/com/fanok/audiobooks/source/`, using fixtures in `app/src/test/resources/fixtures/akniga/` and synthetic HTML where needed.

**Interfaces:** Existing `Akniga.description(Document, String)` returns `DescriptionPOJO`; `getDescription()` supplies the existing expandable and copyable text.

- [ ] Trace the exact HTML selectors and the other-readings tab data path; confirm when readings are actually available before deciding whether to omit that section.
- [ ] Add focused tests asserting distinct headings and paragraph breaks, optional/empty sections, conditional readings retention, nested markup without duplication, and the existing fallback; include existing book and collection fixtures.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*AknigaDescriptionTest'` (use `gradlew.bat` on Windows); confirm the new formatting assertions fail before implementation.
- [ ] Update the Akniga parsing path to preserve meaningful structure, separate nonempty sections with blank lines, and suppress readings only when the tab can supply them. Preserve the existing plain-text fallback and avoid changes to the UI binding.
- [ ] Repeat the focused test, then run `./gradlew :app:testDebugUnitTest` and `./gradlew :app:lintDebug` sequentially; report any environment or test failures honestly. Do not run `clean`.
