# AudioBooks

**English** · [Русский](readme.ru.md)

[![Build](https://github.com/thejohnd0e/AudioBooks/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/thejohnd0e/AudioBooks/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/thejohnd0e/AudioBooks?sort=semver&label=release)](https://github.com/thejohnd0e/AudioBooks/releases/latest)
[![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](#requirements)

**Make room for books in your everyday life.** AudioBooks is a free Android app for discovering and listening to audiobooks from 29 online catalogs. Enjoy a story on your commute, download a book before a trip, or fall asleep listening with a sleep timer.

[Download](https://github.com/thejohnd0e/AudioBooks/releases/latest) · [Report an issue](https://github.com/thejohnd0e/AudioBooks/issues)

## Screenshots

<table>
  <tr>
    <th>Book catalog</th>
    <th>Book details & player</th>
    <th>Genres</th>
  </tr>
  <tr>
    <td><a href="docs/assets/screenshots/books_list.png"><img src="docs/assets/screenshots/books_list.png" width="220" alt="Audiobook catalog with covers, titles, authors, narrators, and durations"></a></td>
    <td><a href="docs/assets/screenshots/player.png"><img src="docs/assets/screenshots/player.png" width="220" alt="Book description, direct download button, and enlarged playback bar"></a></td>
    <td><a href="docs/assets/screenshots/categories.png"><img src="docs/assets/screenshots/categories.png" width="220" alt="Genre list with book counts and favorite stars"></a></td>
  </tr>
  <tr>
    <th>Main menu</th>
    <th>Source management</th>
    <th>Settings</th>
  </tr>
  <tr>
    <td valign="top"><a href="docs/assets/screenshots/main_menu.png"><img src="docs/assets/screenshots/main_menu.png" width="220" alt="Navigation drawer to books, genres, authors, narrators, sources, favorites, history, and downloads"></a></td>
    <td valign="top"><a href="docs/assets/screenshots/available_sources.png"><img src="docs/assets/screenshots/available_sources.png" width="220" alt="Source list with response times, health status, and proxy toggle"></a></td>
    <td valign="top"><a href="docs/assets/screenshots/settings.png"><img src="docs/assets/screenshots/settings.png" width="220" alt="Settings grouped into appearance, playback, sources, downloads, favorites, backup, and general"></a></td>
  </tr>
</table>

<details>
<summary>Contents</summary>

- [Screenshots](#screenshots)
- [What you can do](#what-you-can-do)
- [Book sources](#book-sources)
- [Requirements](#requirements)
- [Credits](#credits)

</details>

## What you can do

- **Explore 29 audiobook catalogs.** Browse by genre, author, or narrator; search across up to 13 active sources simultaneously; explore series and recommendations where available.
- **Cloud synchronization & backup.** Seamlessly sync your library between devices via **Google Drive** or **Yandex Disk** — syncs favorites, history, exact playback positions (chapter and second), favorite authors and narrators, and app settings. Offline local JSON backup and restore is also available.
- **Listen online or offline.** Stream directly or download full audiobooks in one tap (or save specific chapters). Dedicated «Downloaded» section and offline filter to listen with zero internet connection.
- **Modern, polished interface.** Clean Material cards with duration and source badges directly on covers, toggle between 2-column grid and detailed list view, and minimal visual noise.
- **Reworked book details & player.** Large cover with soft rounded corners and shadow, one-tap direct download button, prominent listen button, collapsible synopsis, and series cards.
- **Comfortable playback bar.** Bottom player bar with enlarged touch targets (44×44dp and 48×48dp Play/Pause button with accent circle), top chapter progress indicator, playback speed control, and skip backward (10s) / forward (30s).
- **Custom color palettes & themes.** Light, Dark, and Black themes, three calming natural palettes (**Muted Sage**, **Muted Umber**, **Muted Steel**), plus a full color wheel picker for completely custom background, surface, and accent colors.
- **Personal library & favorites.** Save favorite books, authors, and narrators; filter favorites by downloaded books; pick up where you stopped with smart listening history and progress indicators.
- **Sleep timer & equalizer.** Wind down comfortably with a sleep timer that smoothly pauses playback, and fine-tune sound with the built-in equalizer.
- **Reach blocked catalogs.** Built-in proxy for catalog pages, audio streaming, downloads, and book covers when sources are restricted in your country, complete with live latency and reachability status per source.
- **Parental controls.** Protect settings and restrict 18+ or adult-themed books behind a secure PIN code.
- **Big screens & Android TV.** Fully adapted for Android smartphones, tablets (portrait and landscape layouts), and Android TV.

## Book sources

The app connects to **29 audiobook catalogs**:

> **Akniga**, **Книга в ухе**, **ИзиБук**, **AudioBook-mp3**, **База Книг**, **Книгоблуд**, **Audioboo**, **Отрубин**, **Полекниг**, **уКниг**, **Аудиокниги.fun**, **Aknig.xyz**, **Аудиокнига Life**, **Aknigi24**, **Lis10book**, **Audiokniga.one**, **Библус**, **Audiokniga-online**, **Slushat-knigi**, **Audioknigi.pro**, **Bookish**, **Golosknig**, **Голосом**, **ЯКнига**, **BookInfo**, **Audio-kniga.biz**, **База Книг (info)**, **4Read (укр)**, and **Akniga.me**.

- Switch the browsing catalog in **Drawer → Sources** (`Источники`) or **Settings → Sources & network** (`Настройки → Источники и сеть`).
- Enable up to 13 sources for simultaneous search in **Search sources** (`Источники поиска`).
- Check catalog response times and server health in the **Sources** screen.

## Requirements

| | |
|---|---|
| **Device** | Android phone, tablet, or TV running **Android 7.0 or later** (API 24+) |
| **Network** | Internet connection for browsing, streaming, and downloading books |
| **Storage** | Sufficient free storage for offline audiobooks |
| **App language** | Russian |
| **Package name** | `link.intertop.audiobooks` (debug: `link.intertop.audiobooks.debug`) |

## Credits

**Original app developer:** fanok.inc ([fanok1](https://github.com/fanok1/AudioBooks)). Maintained and expanded by [thejohnd0e](https://github.com/thejohnd0e).
