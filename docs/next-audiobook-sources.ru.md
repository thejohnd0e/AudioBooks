# Каталоги аудиокниг для добавления в AudioBooks (ABooks++): полный отчёт и план интеграции

**Дата исследования и живой проверки:** 14 ноября 2026 г.  
**Репозиторий:** `https://github.com/thejohnd0e/AudioBooks` (`D:\MyProjects\AudioBooks`)  
**Текущее состояние (`Provider.java`):** подключено **29 каталогов** (13 базовых + 10 в версии 8.0 + 6 в версии 8.3):
> **Akniga** (`akniga.org`), **Книга в ухе** (`knigavuhe.org`), **ИзиБук** (`izib.uk`), **AudioBook-mp3** (`audiobook-mp3.com`), **База Книг** (`baza-knig.top`), **Книгоблуд** (`knigoblud.club`), **Audioboo** (`audioboo.org`), **Отрубин** (`otrub.in`), **Полекниг** (`poleknig.com`), **уКниг** (`uknig.com`), **Аудиокниги.fun** (`audioknigi.fun`), **Aknig.xyz** (`s2.aknig.xyz`), **Аудиокнига Life** (`audiokniga.life`), **Aknigi24** (`aknigi24.com`), **Lis10book** (`lis10book.com`), **Audiokniga.one** (`audiokniga.one`), **Библус** (`biblus.org`), **Audiokniga-online** (`audiokniga-online.ru`), **Slushat-knigi** (`slushat-knigi.com`), **Audioknigi.pro** (`audioknigi.pro`), **Bookish** (`bookish.site`), **Golosknig** (`golosknig.com`), **Голосом** (`golosom.org`), **ЯКнига** (`yakniga.org`), **BookInfo** (`bookinfo.net`), **Audio-kniga.biz** (`audio-kniga.biz`), **База Книг (info)** (`baza-knig.info`), **4Read (укр)** (`4read.org`), **Akniga.me** (`akniga.me`).

Известные доменные алиасы в `Url.HOST_ALIASES` (уже обрабатываются приложением и не являются отдельными источниками): `baza-knig.ink`, `izibuk.ru`, `pda.izibuk.ru`, `audioboo.ru`, `m.knigavuhe.org`, `aknig.xyz`, `m.otrub.in`, `mob.knigoblud.club`, `m.biblus.org`, `sweetbook.net`, `audioknigi.com.ua`, а также `knigorai.com` (редирект на `poleknig.com`).

---

## Часть 1. Обзор кандидатов по группам (сводка)

Все оставшиеся кандидаты за пределами 29 уже подключённых каталогов делятся на **4 группы** по технической готовности и ценности контента.

### 1.1. Проверенный резерв и новые рабочие каталоги (готовый стек HTML / DLE / Playerjs / XHR, подтверждённое аудио `206 audio/mpeg`)

Многие ограничения этих сайтов, отмеченные в октябрьском обзоре `docs/new-audiobook-sources.ru.md`, уже закрыты общими механизмами, добавленными в версиях 8.0–8.3 (`Dle`, `Players`, `refererHosts()`, `searchHitsNeedBook()`, `listsDuration()`, `cookies()`):

| № | Каталог | Объём | Движок и аудио | Особенности и что нужно учесть |
|---|---|---|---|---|
| 1 | **Izibuk.club** (`izibuk.club`) | ≈50,6 тыс. (≈4 216 стр. × 12) | Свой HTML (Yii-подобный) + внешний Playerjs JSON на `redirectto.cc` | **Новая находка проверки 14.11.2026.** Не зеркало `izib.uk`: свои ID (`/book-63319-...`), обложки на своём домене `/uploads/covers/...`, полноценные карточки поиска с обложкой и пагинацией, разделы `/genres`, `/authors`, `/performers`, `/top`. Полные многочасовые MP3 (проверены книги на 14 ч и 15,5 ч). Классы CSS хешированные (`_ccb9b7`) — нужны семантические селекторы по `a[href^=...]`. |
| 2 | **КнигиMP3** (`knigimp3.com`) | ≈105,5 тыс. (5 275 стр. × 20) | DLE + внешний Playerjs JSON на `redirectto.cc` | Есть `ТОП-100` (`/top.html`), жанры, поиск. Требует `Referer: https://knigimp3.com/` для плейлиста и MP3 (решается через `refererHosts()`), в карточках списков нет длительности (`listsDuration = false`). |
| 3 | **ВсеАудиокниги** (`vseaudioknigi.com`) | ≈74+ тыс. | DLE + внешний Playerjs JSON (`redirectto.cc`) | Есть разделы авторов, исполнителей, жанров и тематических подборок; аудио и плейлисты отдаются с `Referer: https://vseaudioknigi.com/`. |
| 4 | **Audioknigi.top** (`audioknigi.top`) | ≈33,2 тыс. (1 846 стр. × 18) | DLE + XHR `audio_signed_playlist` (`s5.vozknig.com`) | Богатые карточки в каталоге и поиске (обложка, автор, чтец, длительность, серии), комментарии. На главной нужно исключать две карусели рекомендаций `.card--big`. Подписанные ссылки обновляются через страницу книги. |
| 5 | **Audiokniga-one.com** (`audiokniga-one.com`) | ≈79,8 тыс. (5 326 стр. × 15) | DLE + внешний Playerjs (`.pl.txt` на `redirectto.cc`) | Не путать с уже подключённым `audiokniga.one`. Упор на Сталкер, постапокалипсис и фантастику; есть `Топ-100` (`/top-audioknig.html`) и `Топ месяца` (`/top-month.html`), но нет длительности в карточках и описании (`listsDuration = false`). |
| 6 | **Слушкин Онлайн** (`slushkin.online`) | ≈70+ тыс. | DLE + inline Playerjs (`bookis.shop`) | Длительность `HH:mm:ss`, автор и чтец присутствуют прямо в карточках каталога и поиска; прямые MP3 (`206 audio/mpeg`). Публичных серверных сортировок нет (только `Новые`). |
| 7 | **АудиокнигиВсе** (`audioknigivse.ru`) | ≈13,7 тыс. (1 372 стр. × 10) | DLE + прямые MP3 на `Archive.org` | В карточках каталога есть время и серии с номерами томов; на странице поиска есть обложка, но нет длительности. |
| 8 | **СлушкинВсем** (`slushkinvsem.ru`) | ≈12,4 тыс. (830 стр. × 15) | DLE + прямые MP3 на `Archive.org` | Работает без JS при передаче статической cookie `beget=begetok` (`cookies()` в адаптере). Сильно пересекается по записям с `audioknigivse.ru`. |
| 9 | **Book-Zvuk** (`book-zvuk.com`) | >15 тыс. (>1 000 стр. × 15) | HTML + внешний Playerjs JSON (`redirectto.cc`) | Есть `/top` и жанры (`/sections`). Поиск `/search?text=...` отдаёт текстовые ссылки без обложек и времени — теперь штатно закрывается через `searchHitsNeedBook()`, но сам сервер нестабилен (14.11.2026 давал таймауты чтения). |

---

### 1.2. Языковые витрины и публичные API

1. **Языковые разделы `AudioBook-mp3` (`/uk`, `/en`):**
   - В шаблоне `audiobook-mp3.com` (подтверждено по `app/src/test/resources/fixtures/abmp3/listing-index-p1.html`) присутствуют отдельные языковые разделы `https://audiobook-mp3.com/uk` (украинский) и `https://audiobook-mp3.com/en` (английский).
   - Поскольку в версии 8.3 уже выделен параметризуемый движок `source/Abmp3Engine.java` (используемый для `Abmp3`, `AudioKnigaBiz` и `BazaKnigInfo`), подключение украинской или английской витрины сводится к тонкой обёртке над `Abmp3Engine.Site`.
2. **LibriVox (`librivox.org`):**
   - Официальный открытый JSON API: `https://librivox.org/api/feed/audiobooks/?format=json&extended=1&coverart=1&limit=50&offset=0`.
   - Легко ложится на уже готовый транспорт `SiteHttp.getJson` / `JsonPage`.
   - Аудио хранится на `Archive.org` (прямые MP3 по главам). Содержит общественное достояние (русская и мировая классика). Ограничения: нет серверных сортировок по популярности/рейтингу и нет серверного параметра фильтрации по языку в базовом endpoint.

---

### 1.3. Сайты-зеркала и альтернативные оболочки (по модели `mirrorOf` в `Provider.java`)

В `Provider.java` уже встроена поддержка зеркал (`mirrorOf`, `original()`, `isMirrorOf()`, `mirrors()`). Если нужны резервные точки входа к существующим каталогам с других доменов:

1. **2knigi (`https://2knigi.net`) — зеркало `ЯКнига` (`yakniga.org`):**
   - Живая проверка 14.11.2026: главная `https://2knigi.net/` отвечает `HTTP 200`, GraphQL-эндпоинт `POST https://2knigi.net/graphql` отвечает `HTTP 200` (`{"data":{"__typename":"Query"}}`).
   - Может быть подключён либо как зеркало `Provider.YAKNIGA` (по параметризации хоста в `Yakniga.java`), либо добавлен в `Url.HOST_ALIASES` для открытия ссылок.
2. **Аудиополка (`https://audiopolka.club`) — альтернативная оболочка семьи `Книгоблуд` / `Голосом`:**
   - Объём ≈93,4 тыс. записей (`3 894` стр. × 24), карточки сразу содержат обложку, автора, чтеца, жанр, время и серию.
   - В отличие от `golosom.org` (семантические slug-адреса), использует числовые адреса `/voice/ID/`, `/series/ID/` и конструктор `KB.playerInit` с подписанными MP3 на `r7.audioknigi.xyz`.
3. **Sound-Books (`https://sound-books.net`) — зеркало `4Read` (`4read.org`):**
   - Уже указано в `four_read/info.json`, однако при прямой проверке 14.11.2026 соединение по TLS зависало по таймауту (`_ssl.c:983: The handshake operation timed out`).

---

### 1.4. Сайты, которые добавлять НЕ следует (отсеяны проверками)

По результатам проверок 29.10.2026 (`docs/new-audiobook-sources.ru.md`) и новой живой проверки **14.11.2026** исключены:

| Домен | Причина отсева (проверено фактически) |
|---|---|
| **`sluhayaudio.com`** | **Проверено 14.11.2026:** главная и карточка книги открываются (`200`), но `/genres`, `/authors`, `/top` и `/search?text=...` падают с **`HTTP 500 Internal Server Error`**, пагинация `/?page=2` возвращает первую страницу, а `Playerjs` в HTML указывает на `Xp4sTM90BVzr.frontroute.org/...pl.txt`, который отдаёт 2 байта `OK` вместо JSON (рабочий плейлист отдаётся только при ручной замене хоста на `9giiu0g54k8c.redirectto.cc`). |
| **`audio-knigki.com`** | **Проверено 14.11.2026:** на страницах книг нет онлайн-плеера; старая кнопка «Скачать книгу» и `<iframe>` закомментированы в HTML (`<!-- ... -->`), активна только партнёрская кнопка «Купить и скачать аудиокнигу» (ЛитРес). Каталог перестал обновляться в феврале 2025 г. |
| **`a-kniga.com`** | В плейлисте отдаёт 20-минутный ознакомительный фрагмент (1200 с, 19,2 МБ) вместо полной книги (заявлено 10 ч 58 мин). |
| **`slovushko.com`** | Неэкранированные кавычки внутри JSON `audio_signed_playlist` ломают `JSON.parse`; в карточках остатки киношаблона (`98 мин`, `WebDL`). |
| **`zvukbook.com`** | В карточках и описании «Продолжительность: Нет данных»; плеер лишь декодирует ссылки на `uknig.com/files`. |
| **`abook-club.ru`** | Торрент/файлообменный каталог с демозаписями, без прямого поглавного онлайн-плеера. |
| **`bibe.ru`, `staroeradio.ru`, `sheba.spb.ru`** | **Проверено 14.11.2026:** истёкшие TLS-сертификаты (`SSL: CERTIFICATE_VERIFY_FAILED: certificate has expired`). |
| **`sluhay.com`, `sluhayknigi.com`, `sluhay.com.ua`** | **Проверено 14.11.2026:** первые два отдают `HTTP 403 Forbidden` (Cloudflare), третий — таймаут TLS-рукопожатия. |
| **`audio-lib.club`, `knizhin.net`, `audiotales.info/.biz`, `audioknigi-onlain.com`** | Закрыты постоянным Cloudflare Turnstile / JS-challenge при прямых HTTP-запросах. |
| **`homeread.net`, `4etalka.ru`** | Текстовые читалки электронных книг без аудиоплееров. |
| **`patephone.com`, `author.today`** | Коммерческие сервисы с платной подпиской/покупкой и обязательной авторизацией. |

---

## Часть 2. Детальные результаты живой проверки лучших кандидатов (14.11.2026)

### 2.1. `Izibuk.club` (`https://izibuk.club`) — главный новый кандидат

В октябрьском исследовании сайт был оставлен без оценки («Тождество не доказано; не добавлен в десятку без проверки аудио и независимости каталога»). Полная проверка 14.11.2026 показала, что это **самостоятельный рабочий каталог на ~50,6 тыс. книг с полным аудио**.

- **Отличие от `izib.uk`:** несмотря на название и хешированные CSS-классы (`_ccb9b7`, `_680f12`, `_802db0`), движок и база другие:
  - маршруты книг: `/book-<id>-<slug>` (например, `/book-63319-delo-o-ljubvi`, `/book-63315-strakh-zvjozd`);
  - маршруты разделов: `/genres` (`200`), `/authors` (`200`), `/performers` (`200`), `/top` (`200`);
  - ссылки сущностей: `/genre-5-detektivy-trillery`, `/author-11565-agata-kristi`, `/performer-9877-aleksandr-bolsheshalskijj`;
  - пагинация: параметр `?page=N` (проверены `/?page=2`, `/?page=99`, `/?page=100`, `/?page=1000`, `/?page=3000`, `/?page=4000` — по 12 книг на странице; ограничение в 99 страниц, присущее `Abmp3Engine`, **отсутствует**; конец каталога около стр. 4 216, после чего отдаётся последняя страница из 7 книг, начиная с `/book-11447-lyubaviny-vasiliy-shukshin`);
  - обложки лежат на самом сайте (`/uploads/covers/63319/63319.jpg`), а не на внешнем CDN.
- **Поиск:** `GET /search?q=<запрос>` и `/search?q=<запрос>&page=2` отдают полноценные карточки по 12 штук (с обложкой, жанром, названием, автором, чтецом и краткой аннотацией). `searchHitsNeedBook()` не нужен.
- **Карточка и страница книги:**
  - В карточках списков и поиска есть обложка, жанр, автор, чтец и описание, но **нет длительности** (`listsDuration() = false`).
  - На странице книги есть `<b>Время:</b> 15:31:45`, `<b>Автор:</b> <span itemprop="author">...`, чтец, жанр, описание и скрипт `new Playerjs({id: "player", title: "...", file: "https://9giiu0g54k8c.redirectto.cc/s02/images/audiobooks/.../.pl.txt"})`.
- **Проверка полноты аудио (`Range: bytes=0-1023`):**
  1. Короткая книга `/book-63319-delo-o-ljubvi` (заявлено `46:43`) → плейлист `88334.pl.txt` (`200` с `Referer: https://izibuk.club/`) → 1 трек `delo-o-ljubvi.mp3` → `206 Partial Content`, `Content-Range: bytes 0-1023/44852589` (44,8 МБ ≈ 46,7 мин при 128 кбит/с, сигнатура `ID3`).
  2. Длинная книга одним файлом `/book-63316-invazija-sobiraja-oskolki` (заявлено `13:56:56`) → плейлист `88331.pl.txt` (`200`) → 1 трек `invazija-sobiraja-oskolki.mp3` → `206 Partial Content`, `Content-Range: bytes 0-1023/803454764` (**803,4 МБ** — полная 14-часовая запись, не фрагмент).
  3. Длинная многоглавная книга `/book-63315-strakh-zvjozd` (заявлено `15:31:45`) → плейлист `88330.pl.txt` (`200`) → **11 глав** (`strakh-zvjozd-01.mp3` … `11.mp3`), первая глава `206 Partial Content`, `Content-Range: bytes 0-1023/71248364` (71,2 МБ, сигнатура `ID3`).
- **Как интегрировать:** отдельный адаптер `source/IzibukClub.java` с `Players.loadPlayerjs`, `referer() = "https://izibuk.club/"`, `refererHosts() = Collections.singleton("redirectto.cc")`, `listsDuration() = false`, сортировками `Sort.NEW` и `Sort.TOP_100` (`/top`). Селекторы строить строго по `a[href^=/book-]`, `a[href^=/author-]`, `a[href^=/performer-]`, `a[href^=/genre-]`, `ul.pagination li.next a` (по образцу `Izibuk.java` / `Otrub.java`, без привязки к хеш-классам `_ccb9b7`).

---

### 2.2. `КнигиMP3` (`https://knigimp3.com`) и `ВсеАудиокниги` (`https://vseaudioknigi.com`)

Оба сайта проверены 14.11.2026 (`HTTP 200`, 55 КБ и 39 КБ HTML) и работают по одной архитектурной схеме, близкой к уже подключённому `SlushatKnigi.java`:

- **Движок:** DataLife Engine (DLE), пагинация `/page/N/`, стандартный поиск `/?do=search&subaction=search&story=<запрос>`.
- **Объём:** у `knigimp3.com` — ≈105,5 тыс. записей (`5 275` страниц по 20 карточек), есть `ТОП-100` (`/top.html`); у `vseaudioknigi.com` — >74 тыс. записей, есть указатели авторов, исполнителей и тематические подборки.
- **Аудио:** `Playerjs` со ссылкой на внешний JSON-плейлист на `9giiu0g54k8c.redirectto.cc`. Без `Referer` плейлист отвечает `403`, с `Referer` сайта — `200 JSON` со списком глав `{title, file}`, MP3 отдаёт `206 audio/mpeg` (`ID3`).
- **Как интегрировать:** адаптеры `source/Knigimp3.java` и `source/Vseaudioknigi.java` по образцу `SlushatKnigi.java`, с `referer()`, `refererHosts() = Collections.singleton("redirectto.cc")` и `Players.loadPlayerjs`. Для `knigimp3.com` выставить `listsDuration() = false` (в карточках списков нет времени).

---

### 2.3. `Audioknigi.top` (`https://audioknigi.top`)

- **Статус 14.11.2026:** `HTTP 200` (49,9 КБ), ≈33 215 записей (`1 846` страниц по 18 карточек `.short-nm`).
- **Сильные стороны:** карточки каталога и поиска сразу содержат обложку, автора, чтеца, длительность `HH:mm:ss` и серию. На странице книги есть аннотация, рейтинг и комментарии.
- **Аудио:** элемент плеера содержит `data-playlist-endpoint="/engine/ajax/controller.php?mod=audio_signed_playlist&id=<newsId>"`. Запрос через `SiteHttp.body(SiteHttp.xhr(url, bookUrl))` возвращает JSON `{ok: true, playlist: [...], source: "slovushko"}` с подписанными MP3 на `s5.vozknig.com` (`206 audio/mpeg`, `ID3`).
- **Нюанс парсинга:** на главной странице перед основной лентой `.short-nm` расположены две карусели рекомендаций `.card--big` — парсер `listing()` должен выбирать только `.short-nm` внутри `#dle-content`, чтобы не дублировать карточки карусели на каждой странице.

---

### 2.4. `Слушкин Онлайн` (`https://slushkin.online`) и `АудиокнигиВсе` (`https://audioknigivse.ru`)

- **`slushkin.online` (проверен 14.11.2026, `HTTP 200`):** DLE-каталог (~70+ тыс. публикаций), где длительность `HH:mm:ss`, автор и чтец есть прямо в карточках списков и поиска; плеер — inline `Playerjs` с массивом прямых MP3 на `bookis.shop` (`206 audio/mpeg`, `ID3`). Очень простой адаптер (`Players.inlinePlayerjs`), минус — отсутствие серверных сортировок кроме `Новые`.
- **`audioknigivse.ru` (проверен 14.11.2026, `HTTP 200`):** DLE-каталог (~13,7 тыс. записей), прямые ссылки на MP3 `Archive.org` прямо в HTML книги (`dle_audio_begin`), есть серии с номерами томов. В поиске нет длительности в карточке.

---

## Часть 3. Рекомендуемый порядок и план интеграции (по волнам)

Поскольку контракт `SourceParser` в `AudioBooks` уже полностью изолирует каждый источник (один класс в `app/src/main/java/com/fanok/audiobooks/source/<Site>.java` + одна строка в `Provider.java` + deep-link хост в `AndroidManifest.xml` + карточка `app/src/main/assets/sources/<row>/` + fixture-тест), общие классы приложения менять **не требуется**.

### Волна 1 — максимальная отдача при минимальном коде (4 источника)

1. **`IzibukClub` (`izibuk.club`, `sourceId = 29`):**
   - **Почему первым:** новый полноценный каталог на ~50,6 тыс. книг без лимита в 99 страниц, с живыми указателями `/genres`, `/authors`, `/performers`, подборкой `/top`, обложками на собственном домене и проверенными многочасовыми MP3.
   - **Точки реализации:** `source/IzibukClub.java` (`Players.loadPlayerjs`, `refererHosts = {"redirectto.cc"}`, `listsDuration = false`, устойчивые селекторы по `href^=`), `Provider.IZIBUK_CLUB`, `IzibukClubTest.java` (включая проверку устойчивости селекторов к смене хеш-классов по образцу `IzibukSelectorsTest`).
2. **`Knigimp3` (`knigimp3.com`, `sourceId = 30`):**
   - **Почему вторым:** очень крупный DLE-каталог (~105,5 тыс. записей) с `ТОП-100`, полностью ложащийся на шаблон `SlushatKnigi.java`.
3. **`Vseaudioknigi` (`vseaudioknigi.com`, `sourceId = 31`):**
   - **Почему третьим:** ~74+ тыс. книг, тот же стек DLE + `Playerjs` (`redirectto.cc`), но с собственными разделами авторов, исполнителей и подборок.
4. **`AudioknigiTop` (`audioknigi.top`, `sourceId = 32`):**
   - **Почему четвёртым:** ~33,2 тыс. книг с полными метаданными (включая длительность и серии прямо в поиске) и простым XHR-плейлистом (`SiteHttp.xhr`).

### Волна 2 — расширение нишевых, языковых и DLE-каталогов (4 источника)

5. **`SlushkinOnline` (`slushkin.online`, `sourceId = 33`)** — DLE + inline Playerjs (`bookis.shop`), время в карточках.
6. **`AudioknigaOneCom` (`audiokniga-one.com`, `sourceId = 34`)** — ~79,8 тыс. книг (Сталкер, постапокалипсис, фэнтези), `Топ-100` и `Топ месяца`, `listsDuration = false`.
7. **`Audioknigivse` (`audioknigivse.ru`, `sourceId = 35`)** — ~13,7 тыс. книг, прямые MP3 с `Archive.org`, серии с номерами.
8. **Украинская/английская витрины `Abmp3Engine` (`audiobook-mp3.com/uk`, `/en`)** или **зеркало `2knigi.net`** (для `Yakniga`).

### Волна 3 — резерв с дополнительными условиями

9. **`Book-Zvuk` (`book-zvuk.com`)** — подключать только при стабилизации времени ответа сервера (с повышенным `timeoutMs()` и `searchHitsNeedBook() = true`).
10. **`LibriVox` (`librivox.org`)** — если потребуется каталог классики на английском/русском в общественном достоянии через официальный JSON API.

---

## Часть 4. Чек-лист приёмки каждого нового источника (по `docs/howto.md`)

Для каждого добавляемого сайта из Волны 1–2 выполняются:
1. **Fixtures (`app/src/test/resources/fixtures/<site>/`):** страница 1 и страница 2 каталога, последняя страница, поиск с результатами и пустой поиск, жанры, страница книги, JSON плейлиста.
2. **Адаптер (`source/<Site>.java`) и `BookPageFixtures.BOOK_PAGES`:** чистые статические методы разбора + обязательные методы `SourceParser`.
3. **Регистрация:** строка в `Provider.java` с новым `sourceId` и `folder`, `<data android:host="..." />` в `AndroidManifest.xml`, жанры в `app/src/test/resources/genres/names.tsv`, карточка `app/src/main/assets/sources/<row>/` (`info.json`, `icon.webp`, `cover.webp`).
4. **Автотесты и живой дамп:**
   ```powershell
   .\gradlew.bat :app:testDebugUnitTest --tests "com.fanok.audiobooks.source.<Site>Test" --tests "com.fanok.audiobooks.source.SourceContractTest" --tests "com.fanok.audiobooks.search.SourceInfoTest"
   ```
   и проверка `LiveDumpTest` + `Range: bytes=0-1023` на реальном аудиофайле.

---

## Часть 5. Замер пересечения с подключёнными каталогами (14.11.2026) — решение: не подключать

Объём каталога не показывает, сколько книг он добавит. Поэтому до написания адаптеров проверено, есть ли книги кандидатов уже в приложении.

**Метод.** С каждого кандидата взята выборка: новинки с первой страницы и по 5 книг с 5 случайных страниц из глубины каталога. Каждая книга искалась во всех 29 подключённых каталогах их же адаптерами (`searchUrl` → `fetch` → `searchResults`). Книга считалась найденной при совпадении названия и фамилии автора.

| Кандидат | Книг в выборке | Нет ни в одном из 29 | Где есть чаще всего |
|---|---|---|---|
| Izibuk.club | 37 | 2 (5%), новинки — все есть | уКниг 29, Полекниг 26, Книга в ухе 26, Akniga 24 |
| Слушкин Онлайн | 33 | 0 (0%) | Книга в ухе 31, Lis10book 27, Полекниг 27 |
| АудиокнигиВсе | 35 | 1 (3%) | Slushat-knigi 29, Полекниг 28, уКниг 27 |

Не ответили в замере и не учитывались: AudioBook-mp3, Audioboo, База Книг (info), 4Read. С ними совпадений было бы больше, то есть доля уникальных книг выше занижена быть не может.

**Общие аудиохосты.** Кандидаты отдают звук с тех же серверов, что уже подключённые каталоги: `redirectto.cc` — AudioBook-mp3, Audio-kniga.biz, База Книг (info), Slushat-knigi (кандидаты Izibuk.club, КнигиMP3, ВсеАудиокниги, Audiokniga-one.com, Book-Zvuk); `s5.vozknig.com` — Audioknigi.pro (кандидат Audioknigi.top); `audioknigi.xyz` — Книгоблуд и Голосом (кандидат Аудиополка). Такие сайты — витрины той же базы: дают дубли в поиске, замедляют общий поиск и ломаются вместе с уже подключёнными.

**Решение.** Каталоги из частей 1–3 не подключать: порог, при котором новый источник окупает поддержку, — 20–30% книг, которых нет ни в одном подключённом каталоге. Новому кандидату — сначала такой же замер. `2knigi.net` держать в запасе как зеркало ЯКниги на случай блокировки основного домена.
