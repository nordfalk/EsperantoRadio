# 3. Parsado kaj fontoj (PLEJ GRAVA)

> Ĉi tiu dokumento priskribas la kernon de la projekto. La UI, ludilo, plej ŝatataj
> kaj serĉo estas ordinaraj. La parsado — kunigi ~15 malsamajn, duonmortajn fontojn
> en unu unuecan bildon — estas la malfacila kaj unika parto. Ĝi devas esti
> konservita en la nova apo.

## La centra problemo

Esperanto-radiot ne vivas en unu loko. La celo de la apo estas kunigi ĉirkaŭ 15
tre malsamajn fontojn en unu unuecan bildon de "kanaloj kun elsendoj". La fontoj
estas malordaj, duonmortaj, heterogenaj:

- Puraj podkast-fluoj kun `<enclosure>` (Kernpunkto, anchor.fm-kanaloj)
- WordPress-blogoj kun sono kiel pluraj `<audio>`-ludiloj ene de la enhavo-HTML (Varsovia Vento)
- Blogger-blogoj kun sono malantaŭ `<iframe>` al Google Drive / archive.org (Esperanta Retradio/Peranto)
- Atom-fluoj kun idiosinkrazia strukturo (Vinilkosmo/ipernity)
- Malnova **radio.txt**-dosiero uzata kiel komunuma "plej novaj elsendoj"-listo

## Datumiŝa hierarĥio (3 fontoj + antaŭeco)

```
(1) Kanal-konfiguro: esperantoradio_kanaloj_v9.json (defora + ene-enigita)
        ↓ por ĉiu kanal:
(3) elsendojRssUrl       kontraŭ    (2) radio.txt (komunuma listo, esperanto-radio.com/radio.txt)
    po-kanala RSS/Atom              rezervo / suplemento

ANTAŬECO: RSS venkas. radio.txt-elsendoj estas uzataj nur se la kanal
          jam ne havas elsendojn de RSS (eo_datumFonto="rss").
```

Se radio.txt mencias kanalon ne en la JSON-konfiguro, la kanal estas **kreita
dinamike**.

## Kanal-konfiguro (`esperantoradio_kanaloj_v9.json`)

**Formato-averto:** La dosiero finiĝas per `.json` sed estas **JSON kun komentoj**
(`// ...`) kaj povas enhavi duplikatajn ŝlosilojn. La nuna kodo uzas malnovan
indulgentan parsilon. Nova implementaĵo devas aŭ stripi `//`-komentojn antaŭ
parsado, aŭ uzi indulgentan JSON5/JSONC-parsilon.

### Supranivelaj kampoj

| Kampo | Signifo |
|---|---|
| `android` | Objekto: `kontakt_url`, `kontakt_modtagere`, `kontakt_titel`, `drift_statusmeddelelse` (montrata kiel rubando — ekz. ke Muzaiko ne funkcias). Ŝlosiloj kun prefikso `x0`/`x1`/`sds…` estas **malaktivigitaj** malnovaj mesaĝoj. |
| `intervals` | `{ "playlist": 30, "settings": 1800 }` (sekundoj) — kiom ofte refreŝigi. |
| `komenca_kanalo` | Slug de kanal montrata ĉe startigo (`"muzaiko"`). |
| `elsendojUrl` | URL al radio.txt (`https://esperanto-radio.com/radio.txt`). |
| `hejmpaĝo` | Retejo de la projekto (vikio). |
| `sugestoj_por_alarmoj` | Antaŭdifinitaj vekhorloĝo-sugestoj. |
| `kanaloj` | Listo de **aktivaj** kanaloj. |
| `FORPRENITAJ_KANALOJ` | Listo de **forigitaj/mortaj** kanaloj (nur historie; ignoru). |

### Po-kanalaj kampoj

| Kampo | Signifo | Uzo |
|---|---|---|
| `kodo` | **slug** (unika id, ekz. `"muzaiko"`, `"peranto"`) | ŝlosilo ĉie; parto de elsend-id |
| `nomo` | Vidiga nomo (`"Pola Retradio"`) | UI |
| `emblemoUrl` | URL de logotipo | kanalemblemo (kaŝenita/malpligrandigita) |
| `elsendojRssUrl` | Po-kanala RSS/Atom-fluo | elsendofonto (3) |
| `rektaElsendaSonoUrl` | **Livestream**-URL (nur Muzaiko) | rekta ludado |
| `rektaElsendaPriskriboUrl` / `...JsonUrl` | "nun ludanta"-fonto (HTML/JSON) | rekta metadateno |
| `hejmpaĝoButono` | Retejo-ligo | "vizitu retejon"-butono |
| `retpoŝto` | Kontakt-retpoŝto | kontakto |
| `elsendojRssIgnoruTitolon` | bulea — fluotitolo senutila (ofte nur dato) | → derivu titolon el priskribo (regulo 6.5) |
| `montruTitolojn` | bulea — ĉu montri elsendotitolon en UI | UI |
| `uziWebViewPorElsendo` | bulea — montri elsendon en retpaĝa vido anstataŭ ludilo | UI/ludado |

### Aktivaj kanaloj (v9)

| Slug | Nomo | Fluotipo / specialaĵo |
|---|---|---|
| `muzaiko` | Muzaiko | **Livestream HLS** + RSS-arkivo; ignoruTitolon |
| `varsoviavento` | Varsovia Vento | WordPress, **pluraj `<audio>` po ero** (regulo 6.2) |
| `lamalfamuloj` | La Malfamuloj | anchor.fm norma-RSS (`<enclosure>`) |
| `3zzzenesperanto` | 3ZZZ en Esperanto | (fluo nune markita difektita) |
| `polaretradio` | Pola Retradio | norma-RSS; ignoruTitolon |
| `babibejo` | BabiBEJO | anchor.fm norma-RSS |
| `kernpunkto` | Kernpunkto | norma-RSS; **https→http- apartaĵo** (regulo 6.1) |
| `movada-vidpunkto` | Movada Vidpunkto | norma-RSS; retpaĝa vido |
| `radioscienca` | Radio Scienca | norma-RSS |
| `radiovatikana` | Radio Vatikana | norma-RSS |
| `spiritismavocxo` | Spiritisma Voĉo | spreaker norma-RSS |
| `vinilkosmo` | Vinilkosmo | **ipernity Atom** (regulo 6.4) |
| `bitmono` | Bitmono | anchor.fm norma-RSS |
| `sano` | Pri Sano | anchor.fm norma-RSS |
| `radiofrei` | Radio Frei | (fluo nune markita difektita) |
| `peranto` | Esperanta Retradio | **Blogger/iframe → Google Drive/archive.org** (regulo 6.3) |

### Mortaj kanaloj (ignoru)

`radioaktiva`, `amindaradioesperanto`, `radiohavanokubo`, `radioverda`,
`kaliningrada`, `laboren.org`, `vej` (Voĉoj el Japanio), `verdastacio`,
`krokoloko`.

## radio.txt-formato

De `http://esperanto-radio.com/radio.txt`. Tekstdosiero. **Eroj apartigitaj per
malplena linio.** Unua ero estas kapo (tempmarko, ekz. `A1488107244`).
Ĉiu sekva ero = **4 linioj**:

```
<kanalnomo>
<dato yyyy-MM-dd>
<audio-URL (mp3)>
<priskribo (unu linio)>
```

### Konkreta ekzemplo

```
A1488107244

Muzaiko
2017-02-26
http://muzaiko.info/public/podkasto/podkasto-2017-02-25.mp3
Bharat GHIMIRE kaj Mireille GROSJEAN parolas pri lingvolernado ...

Pola Retradio
2017-02-24
http://feedproxy.google.com/~r/retradio/~5/Hw9-8Z8Rjro/RetRadio_24.02.2017_-vendredo.mp3
La 627-a E_elsendo el la 24.02.2017 ĉe www.pola-retradio.org – ...
```

### Interpretaj reguloj

- **Kanal-kongruo nomo→slug:** forigu spacojn, minuskligu, anstataŭigu `ĉ`→`cx`.
  Ekz. "Pola Retradio"→`polaretradio`. Speciala: `movadavidpunkto` → `movada-vidpunkto`.
- "Esperanta Retradio" en radio.txt kongruas kun la kanal kun slug `peranto`
  (nomo kaj slug malsamas) — do kongruo okazas kaj laŭ nom-derivita slug kaj
  laŭ ekzistanta slug.
- **Nekonata kanalnomo** → kreu novan kanalon dinamike kun slug = nom-derivita,
  `eo_datumFonto="radio.txt"`.
- **Esend-id (slug):** `<kanalslug> + ":" + <dato>` (ekz. `polaretradio:2017-02-24`).
- **Antaŭeco:** se kanal jam havas `eo_datumFonto=="rss"`, radio.txt-elsendoj
  estas ignoritaj por tiu kanal.

## Elsenda datummodelo (la kontrakto)

Sendepende de la fonto, parsado produktas unuecajn **elsend**-objektojn. Nova
implementaĵo devas plenigi la samajn kampojn:

| Kampo | Enhavo | Notoj |
|---|---|---|
| `slug` | **unika id** | vidu id-konvenciojn sube — uzata por dedup, plejŝatataj, daŭriga pozicio |
| `kanal` | posedanta kanal | |
| `titel` | titolo | eble derivita el priskribo (regulo 6.5) |
| `beskrivelse` | HTML/teksto | purigita de reklam-blokoj (regulo 6.6) |
| `billedeUrl` | elsend/iTunes-bildo | povas esti malplena |
| `startTid` / `startTidDato` | publikigdato | normigita al `yyyy-MM-dd` |
| `stream` | **audio-URL (mp3)** | la plej grava kampo; elsendoj sen `stream` estas **forĵetataj** |
| `duration` | daŭro en sekundoj | el iTunes-modulo se konata; aliokaze 0 |
| `link` | ligo al originala paĝo | |

### Id-konvencioj (slug) — gravas reprodukti

- Norma: `entry.uri` se ĉeestas, aliokaze `<kanalslug>:<dato>`.
- radio.txt: `<kanalslug>:<dato>`.
- Varsovia Vento (pluraj partoj po ero): `<slug>:<dato>:<partnum>`
  (ekz. `varsoviavento:2025-04-24:1`).
- Vinilkosmo: `vk:<publikig-sen-tempzonon>`.
- Rekta elsendo (Muzaiko): `<slug>_rekta`, kun `startTidDato="REKTA"`.

### Dat-formataj kaptiloj

- Kanona: `yyyy-MM-dd` (US-locale).
- Kelkaj RSS-`pubDate` estas `Thu, 01 Aug 2013 12:01:01 +02:00`. La
  **dupunkta tempzona sufikso** (`+02:00`) estas malakceptata de kelkaj dat-parsiloj
  → la nuna kodo normigas `:00$` → `00` antaŭ parsado. Nova kodo devas trakti
  RFC-822-datojn fortike (inkluzive `GMT`, `+0000`, kaj `+02:00`-variaĵoj).
- Atom-datoj estas ISO-8601 (`2018-03-21T17:00:56+00:00`); dato = teksto antaŭ `T`.

## La sep parsregoloj (kun konkretaj fluo-elfragmentoj)

La parsado **branĉiĝas laŭ kanal-slug**:

```
parsRss(fluoTeksto, kanal):
    deArkivo = kanal.elsendojRssUrl enhavas "podkasta_arkivo"   // servil-generata pura arkivo → ĉiam "ĝenerala"
    se slug == "varsoviavento" kaj ne deArkivo → parsVarsoviaVento   (regulo 6.2)
    alie se slug == "peranto" kaj ne deArkivo → parsePeranto         (regulo 6.3)
    alie → parseGenerel                                           (regulo 6.1)
```

### Regulo 6.1 — Ĝenerala parsado (`parseAndre`)

Majoritato de kanaloj. Uzata de Kernpunkto, anchor.fm-kanaloj (La Malfamuloj,
BabiBEJO, Bitmono, Pri Sano), Pola Retradio, Radio Vatikana, Radio Scienca,
Spiritisma Voĉo, Movada Vidpunkto, ktp.

Reguloj po `<item>`/ero:
1. `slug = entry.uri` aliokaze `<kanalslug>:<dato>`.
2. Legu **iTunes-podkast-modulon** por `summary`, `duration`, `image`.
3. `beskrivelse` = `<description>` aliokaze iTunes-`summary`; se `<content:encoded>`
   ekzistas, uzu ĝin.
4. **`stream`** = `<enclosure url=… type="audio/…">`. Se neniu enclosure: parsu la
   priskribo-HTML kaj prenu `<audio><source src=…>`. Se ankoraŭ neniu →
   **forĵetu la elsendon**.
5. `duration` = iTunes-daŭro en sekundoj (ms/1000), aliokaze 0. `billedeUrl` = iTunes-bildo.

**Kernpunkto-apartaĵo (devas konservi):** La fluo-`<enclosure>` estas `https://…`,
sed la nuna RSS-biblioteko reskribas al `http://…`, kaj la ludilo ne povas sekvi
`http→https`-redirekton reen. Do: **se kanal==kernpunkto kaj stream komenciĝas per
`http://`, devigu ĝin reen al `https://`.** (Nova kodo povas eviti la reskrib-eraron
mem, sed devas certigi ke Kernpunkto finas per `https`.)

**Ekzemplo — Kernpunkto** (`https://kern.punkto.info/feed/mp3/`):
```xml
<item>
  <title>KP204 Pigmentoj</title>
  <link>https://kern.punkto.info/2022/11/09/kp204-pigmentoj/</link>
  <pubDate>Wed, 09 Nov 2022 20:59:06 +0000</pubDate>
  <description><![CDATA[Ekde nia infaneco ... la pigmentoj]]></description>
  <enclosure url="https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3"
             length="97068694" type="audio/mpeg"/>
  <itunes:duration>01:55:16</itunes:duration>
  <itunes:image href="https://kern.punkto.info/bildoj/kp204-pigmentoj.jpg"/>
</item>
```
→ Elsendo: titolo `KP204 Pigmentoj`, stream `https://…/kp204-pigmentoj.mp3`,
daŭro `6916 s`, bildo fiksita, dato `2022-11-09`.

### Regulo 6.2 — Varsovia Vento: pluraj sonpartoj po ero (`parsVarsoviaVento`)

WordPress-fluo (`https://www.podkasto.net/feed/`). Ĉiu blogero (`<entry>`/`<item>`)
enhavas **HTML kun pluraj `<audio>`-ludiloj** — elsendo ofte estas dividita en
parto 1, 2, 3…

Reguloj:
1. Purigu enhavo-HTML per fiksaj regex-modeloj (regulo 6.6) — forigu Facebook-
   reklamon, elŝut-ligojn, "subtenu nin" ktp.
2. Por **ĉiu** `<audio>`-elemento: prenu `<source src>`. Faru **po unu elsendo por
   parto**.
3. Titolo = `entry.titolo + " " + (partnum) + "a parto"`; id = `varsoviavento:<dato>:<partnum>`.

**Ekzemplo** — unu ero (VVE185, 24-04-2025) produktas tri partojn:
```html
<audio class="wp-audio-shortcode" id="audio-11293-1" ...>
  <source type="audio/mpeg" src="https://www.podkasto.net/wp-content/uploads/2025/04/250424VVE185P1.mp3"/>
</audio>
<audio ... id="audio-11293-2" ...>
  <source type="audio/mpeg" src="https://www.podkasto.net/wp-content/uploads/2025/04/250424VVE185P2.mp3"/>
</audio>
<audio ... id="audio-11293-3" ...>
  <source type="audio/mpeg" src="https://www.podkasto.net/wp-content/uploads/2025/04/250424VVE185P3.mp3"/>
</audio>
```
→ 3 elsendoj: `…:2025-04-24:1/2/3`, titoloj "… 1a parto / 2a parto / 3a parto",
fluoj P1/P2/P3.mp3.

### Regulo 6.3 — Esperanta Retradio / Peranto: iframe-detektiva laboro (`parsePeranto`)

La **plej rompiĝema** fonto. Blogger/Blogspot-Atom-fluo
(`https://esperantaretradio.blogspot.com/feeds/posts/default`). Sono **ne**
estas en enclosure, sed kaŝita malantaŭ `<iframe>` en la enhavo-HTML, kaj la
iframe montras al diversaj gastigantoj. (En la fluo, HTML estas eskapita, ekz.
`src=&quot;https://archive.org/embed/…&quot;` — unue malkodu/HTML-parsu.)

Reguloj po ero:
1. Saltu konatajn malplenajn datojn (malmolaj: `2019-11-08`, `2019-09-29` — neniu sono en HTML).
2. Eltiru `billedeUrl` el la unua `<img>`; poste forigu `<img>`, `<iframe>`,
   `<div class="separator">` el priskribo.
3. Trovu unuan `<iframe src>` kaj determinu la fonton:

| iframe-src komenciĝas/enhavas | Ago |
|---|---|
| (neniu iframe) | saltu (neniu sono) |
| `https://drive.google.com/file/d/<ID>/…` | **rekonstruu elŝut-URL:** `https://drive.google.com/u/1/uc?id=<ID>&export=download` |
| `https://archive.org/embed/<nomo>` | **elŝutu embed-paĝon, skrapu MP3-URL el ĝi** (vidu sube) |
| `yourlisten.com` / `audioboom.com` / `vimeo.com` / `ipernity.com` / `youtube.com/` / `w.soundcloud.com/player/` / `vocaroo.com` | **saltu** — MP3 ne eltirebla |

**archive.org-skrapado (konkrete):** iframe montras ekz. al
`https://archive.org/embed/natria_bikarbonato1`. La embed-HTML enhavas la veran
dosier-URL. La eltiro prenas tekston ĝis `.mp3"`, tondas ĉe lasta `http`,
malkodas HTML-entitojn. Rezulto:
```
iframe:   https://archive.org/embed/natria_bikarbonato1
→ stream: https://archive.org/download/natria_bikarbonato1/natria_bikarbonato1.mp3
```
Estas almenaŭ unu malmola fluo-korekto: `…/embed/orkestro_sklavidojj` →
`…/embed/orkestro_sklavidoj`.

En la kaŝenita fluoversio, **ĉiuj** son-iframes estas archive.org-embed-oj (ekz.
`seksismo_sabotas`, `timis-telefonon`, `rentumas_nekalkuleba`, `kato_kapablas`,
`eviti_mikroplastajhojn`). La Google Drive-branĉo ekzistas por pli malnovaj eroj.

### Regulo 6.4 — Vinilkosmo: ipernity-Atom (`parsiElsendojnDeRssVinilkosmo`)

Atom-fluo (`http://api.ipernity.com/feed/doc?user_id=vinilkosmo&only=audio`).
Po `<entry>`:
- `<link rel="enclosure" type="audio/mpeg" href=…>` → **stream**
- `<link type="image/jpeg" href=…>` → bildo; `<link type="text/html" href=…>` → ligo
- `<published>` → dato (teksto antaŭ `T`); id = `vk:<publikig-sen-tempzonon>`
- Purigu priskribon: forigu `<p class="who">…</p>`, fortondu gvidan `<p>`/`</div>`.

**Ekzemplo:**
```xml
<published>2018-03-21T17:00:56+00:00</published>
<link rel="enclosure" type="audio/mpeg" href="https://cdn.ipernity.com/200/59/46/46405946.42a4e885.mp3"/>
```
→ id `vk:2018-03-21T17:00:56`, dato `2018-03-21`, stream
`https://cdn.ipernity.com/…/46405946.42a4e885.mp3`.

### Regulo 6.5 — Titol-derivado (`elsendojRssIgnoruTitolon`)

Kelkaj fluoj havas senutulan titolon (ofte nur la dato, jam montrata aliloke):
Muzaiko, Pola Retradio, 3ZZZ, Peranto. Por tiuj kanaloj
(`elsendojRssIgnoruTitolon=true`): **derivu titolon el priskribo** — stripi
HTML-etikedojn, malkodi entitojn, anstataŭigi linisaltigojn per spacoj, fortondi,
kaj **tranĉi al maksimume 200 signoj**.

### Regulo 6.6 — HTML-purigado (fiksaj "purigu"-modeloj)

Enhavo-HTML enhavas reklamon/promon forigi antaŭ montro. Por **Varsovia Vento**
forigu (regex): sekcioj kun "Ĉe Facebook/Fejsbuko ni kreis …", "Paŝo post paŝo
moderniĝas nia retejo …", "Elŝutu podkaston …", "tempo-daŭro … elŝutu",
"Download audio file …", "Subtenu nin …", "Por scii novaĵojn vizitu subpaĝon …",
kaj la `<audio>…</audio>`-blokoj (sono jam eltirita). Por **Vinilkosmo** forigu
`<p class="who">…</p>`. Nova implementaĵo devas meti tiujn purig-modelojn
**po-kanale en dateno/agordo**, ne malmole en logiko, por facila prizorgado.

### Regulo 6.7 — Paĝigo (`rel="next"`)

Fluoj liveras nur la plej novajn N elsendojn. Por elŝuti la arkivon, sekvi
`<atom:link rel="next" href=…>` ĝis ne plu paĝoj. Ekzemplo (Kernpunkto):
`<atom:link rel="next" href="https://kern.punkto.info/feed/mp3/?paged=2" />`.
La sekva-paĝa URL estas konservata (`rss_nextLink`) kaj uzata de la arkiva
servilo (regulo 8) por marŝi malantaŭen en historio.

## Neeltireblaj / mortaj fontoj

**Son-gastigantoj neeltireblaj** (elsendoj de tiuj estas intence saltataj):
YouTube, SoundCloud, Vimeo, ipernity (en Peranto-branĉo), Audioboom, Yourlisten,
Vocaroo. Tio estas intenca deziro — la apo nur montras elsendojn kie rekta MP3
estas trovebla.

**Mortaj kanaloj** (en `FORPRENITAJ_KANALOJ` aŭ markitaj): Radio Verda, Radio
Havano Kubo, 3ZZZ (fluo forigita), Aminda Radio, La Bona Renkontiĝo,
Kaliningrada, Voĉoj el Japanio, Verda Stacio, Krokoloko, Radio Aktiva.
**Ignoru en la nova apo.**

**Livestream:** Muzaiko (`https://fluo.muzaiko.info/hls/muzaiko/live.m3u8`)
estas malstabila ekde 2022 laŭ la statusmesaĝo. Kontrolu frue; rekta ludado devas
malsukcesi elegante kaj retropaŝii al arkivaj elsendoj.

## Golden fixtures — kaŝenitaj fluoj por eksterretaj testoj

La dosierujo `RssArkivServer-filcache/` enhavas **realajn, kaŝenitajn fluojn**
uzeblajn rekte kiel eksterretaj test-eniroj (determinismaj, sen reto).

| Kanal / fonto | Fiksdosiero (sub `RssArkivServer-filcache/`) | Montras |
|---|---|---|
| Peranto | `esperantaretradio.blogspot.com/feeds_posts_default` (25 eroj) | iframe → archive.org/Google Drive |
| Peranto arkivpaĝoj | `www.blogger.com/feeds_..._start-index_*` | malantaŭen paĝigo |
| archive.org embeds | `archive.org/embed_*` (multaj) | MP3-skrapado el embed-paĝo |
| Varsovia Vento | `www.podkasto.net/feed_` (10 eroj) | pluraj `<audio>` po ero |
| Kernpunkto | `kern.punkto.info/feed_mp3_` (10 eroj) | enclosure + https-apartaĵo + next-link |
| Vinilkosmo | `api.ipernity.com/feed_doc_user_id_vinilkosmo_only_audio` (5 eroj) | ipernity Atom |
| La Malfamuloj | `anchor.fm/s_1e1d5f38_podcast_rss` (114 eroj) | anchor.fm enclosure |
| BabiBEJO / Bitmono / Pri Sano | `anchor.fm/s_54291ba8…`, `s_830574e4…`, `s_2a4fde34…` | anchor.fm |
| Pola Retradio | `pola-retradio.org/feed_` | norma-RSS |
| Movada Vidpunkto | `movada-vid.punkto.info/feed_mp3_` | norma-RSS |
| Radio Vatikana | `www.vaticannews.va/eo_podcast_esperanto-programo.podcast.xml` | norma-RSS |

Rekomendo: kopiu tiujn en la novan projekton kiel testrimedojn
(`testResources/feeds/…`) kaj frostigu ilin kiel golden-enirojn.

## Parser-kontrakto (kontrollisto)

Nova parsilo estas "ĝusta" kiam por ĉiu fiksaĵo supre ĝi produktas:
1. saman **kanton** de elsendoj (± konataj saltoj),
2. samajn **id**-ojn (slug-konvencioj),
3. validan, rektan **MP3-`stream`** por ĉiu elsendo (kaj forĵetas elsendojn sen),
4. ĝustan **daton** (`yyyy-MM-dd`),
5. **purigitan** priskribon (purigu-modeloj, regulo 6.6),
6. ĝustan **titolon** (inkluzive derivadon por `ignoruTitolon`, kaj "Na parto" por Varsovia Vento),
7. la ĝustajn **saltojn** por nesubtenataj gastigantoj.

Detalajn atendatajn valorojn vidu en `../nova/04_parsado_kaj_arkivo.md` (golden-testoj).
