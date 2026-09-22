# 4. Parsado kaj arkivo (la kerno)

> Ĉi tiu dokumento specifas kiel la nova apo reproduktas la sep parsregolojn de
> la malnova apo. La parsado estas la plej malfacila kaj plej valora parto.
> Por la plena fono legu `../malnova/03_parsado_kaj_fontoj.md`.

## Kerno-principo: dateno-movita, ne malmola

La malnova apo malmole kodas per-kanalajn branĉojn (`if slug == "varsoviavento"`).
La nova apo **ankaŭ** uzas malmolan `when (kanalo.slug)` por la parsbranĉo —
tio estis simpligita el la originala plano kiu havis `parsStrategio`-kampon.
La kampo ne estis aldonita al la `Kanalo`-modelo; la enkursigo restas slug-bazita.

Tamen, iuj aferoj restas dateno-movitaj:
- La **kernpunkto-https-korekto** estas endosita en la ĝenerala parsilo (ĉiam `https://`).
- La **salto-listo** de neeltireblaj gastigantoj (youtube/soundcloud/vimeo/...) estas konstanto en la parsilo.
- Kelkaj aferoj restas kodigitaj (saltoj de konataj malplenaj datoj en Peranto,
  `orkestro_sklavidojj`-korekto) ĉar ili estas tro specifaj. Documentu ilin.

## La parsilo (`shared/data/parser/RssParsilo.kt`)

```kotlin
class RssParsilo {
    fun parsuRss(
        fluoTeksto: String,
        kanalo: Kanalo,
        httpKliento: suspend (String) -> String = { "" }  // por archive.org-embed-skrapado
    ): List<Elsendo> {
        val doc = Ksoup.parseXml(fluoTeksto, "")
        val deArkivo = kanalo.podkastaRssUrl?.contains("podkasta_arkivo") == true
        return if (deArkivo) {
            parsuGxenerala(doc, kanalo)   // jam normigita
        } else when (kanalo.slug) {
            "varsoviavento" -> parsuVarsoviaVento(doc, kanalo)
            "peranto" -> parsuPeranto(doc, kanalo, httpKliento)
            "vinilkosmo" -> parsuVinilkosmo(doc, kanalo)
            else -> parsuGxenerala(doc, kanalo)
        }
    }
}
```

La enkursigo estas slug-bazita (`when (kanalo.slug)`), ne dateno-movita.
La `httpKliento`-parametro (defaŭlta `{ "" }` = no-op) estas necesa nur por la
Peranto-regulo (archive.org-embed-skrapado). La ĝenerala parsilo ne bezonas ĝin.

## La sep regoloj — kiel reprodukti

### Regulo 6.1 — Ĝenerala (`parsuGxenerala`)

Pura RSS/Atom-parsado. Po `<item>`/`<entry>`:
1. `id = entry.uri` aŭ `<kanalo.slug>:<dato>`.
2. Legu iTunes-modulon: `summary`, `duration`, `image`.
3. `priskribo` = `<description>` aŭ iTunes-`summary`; se `<content:encoded>` ĉeestas, uzu ĝin.
4. `fluo` = `<enclosure type="audio/*" url>`. Se neniu: parsu priskribo-HTML, prenu `<audio><source src>`. Se ankoraŭ neniu → **forĵetu**.
5. `dauro` el iTunes; `bildoUrl` el iTunes-bildo.
6. Se `fluo` komenciĝas per `http://` → `https://` (ĉiam https-korekto).
7. Se `kanalo.ignoruTitolon` → derivu titolon el priskribo (regulo 6.5).

### Regulo 6.2 — Varsovia Vento (`parsuVarsoviaVento`)

1. Purigu enhavo-HTML per `kanal.puriguModeloj` (regulo 6.6).
2. Por **ĉiu** `<audio>`-elemento: prenu `<source src>`, faru po unu elsendo.
3. `titolo = entry.titolo + " " + partnum + "a parto"`, `id = <slug>:<dato>:<partnum>`.

### Regulo 6.3 — Peranto (`parsuPeranto`)

1. Saltu konatajn malplenajn datojn (konstanta aro: `2019-11-08`, `2019-09-29`).
2. Eltiru `bildUrl` el unua `<img>`; forigu `<img>`, `<iframe>`, `<div class="separator">`.
3. Unua `<iframe src>`:
   - `drive.google.com/file/d/<ID>/...` → `https://drive.google.com/u/1/uc?id=<ID>&export=download`
   - `archive.org/embed/<nomo>` → elŝutu embed-paĝon, skrapu MP3-URL (teksto ĝis `.mp3"`, tondas ĉe lasta `http`, malkodi entitojn). Apliku konatan korekton `orkestro_sklavidojj` → `orkestro_sklavidoj`.
   - Gastiganto en `kanal.iframeReguloj` kun ago `saltu` → saltu.
   - Neniu iframe → saltu.
4. `id = peranto:<dato>` (aŭ `entry.uri` se ĉeestas).

### Regulo 6.4 — Vinilkosmo (`parsuVinilkosmo`)

Pura Atom-parsado. `<link rel="enclosure" type="audio/mpeg">` → stream;
`<link type="image/jpeg">` → bildo; `<link type="text/html">` → ligo;
`<published>` → dato (teksto antaŭ `T`); `id = vk:<publikig-sen-tempzonon>`.
Purigu priskribon per `kanal.puriguModeloj` (forigu `<p class="who">…</p>`).

### Regulo 6.5 — Titol-derivado

Por `kanal.ignoruTitolon == true`: deriva titolo el priskribo — stripi HTML-etikedojn,
malkodi entitojn, anstataŭigi linisaltigojn per spacoj, fortondi, **tranĉi al 200 signoj**.

### Regulo 6.6 — HTML-purigado

Purig-modeloj estas en `kanal.puriguModeloj` (regex-listo). Aplikataj al
`priskribo` antaŭ montro/stokado. La malnovaj malmolaj modeloj (por Varsovia Vento,
Vinilkosmo) iĝas agordaj datumoj.

### Regulo 6.7 — Paĝigo

Sekvu `<atom:link rel="next" href=...>` ĝis ne plu paĝoj. Konservu la sekva-paĝan
URL (`rss_nextLink`). Uzata de la arkiva servilo por marŝi malantaŭen.

## radio.txt-parsilo

> **Ne implementita kiel aparta klaso.** La `RadioTxtParsilo` priskribita en la
> originala plano ne ekzistas en la kodo. La radio.txt-datumoj estas difinitaj en
> la kanalkonfiguro (`esperantoradio_kanaloj_v9.json`), ne parsataj dise.
>
> Ekzistas tamen `RadioTxtKomparilo` (en `desktopApp/`) kiu komparas la kanalkonfiguron
> kun la reala `esperanto-radio.com/radio.txt` por identigi mankantajn kanalojn
> kaj elsendojn. Rulu per `./gradlew :desktopApp:radioTxtKomparilo`.

La radio.txt-formato (por referenco):
```kotlin
// Eroj apartigitaj per malplena linio. Unua ero = kapo (ignorita).
// Ĉiu sekva ero = 4 linioj: kanalnomo / dato / mp3-url / priskribo.
// nomo→slug: forigu spacojn, minuskligu, ĉ→cx. Speciala: movadavidpunkto→movada-vidpunkto.
// "Esperanta Retradio" kongruas kun slug "peranto".
// Nekonata nomo → kreu novan kanalon dinamike (datumFonto="radio.txt").
// Antaŭeco: se kanal jam havas datumFonto="rss", ignoru radio.txt-elsendojn por ĝi.
```

## Id-konvencioj (devas esti precize reproduktitaj)

| Fonto | id-formato |
|---|---|
| Ĝenerala | `entry.uri` aŭ `<kanalSlug>:<yyyy-MM-dd>` |
| radio.txt | `<kanalSlug>:<yyyy-MM-dd>` |
| Varsovia Vento | `<kanalSlug>:<yyyy-MM-dd>:<partnum>` |
| Vinilkosmo | `vk:<publikig-sen-tempzonon>` |
| Rekta (Muzaiko) | `<kanalSlug>_rekta` kun `dato=REKTA` |

## Dat-normigo

- RFC-822 (`Thu, 01 Aug 2013 12:01:01 +02:00`): uzu fortikan dat-parsilon (ekz.
  `kotlinx-datetime` traktas `+02:00`). La malnova kodo normigas `:00$`→`00`
  pro malforta parsilo — ne necesa se la nova parsilo estas fortika.
- Atom ISO-8601 (`2018-03-21T17:00:56+00:00`): dato = teksto antaŭ `T`.
- Kanona elirejo: `yyyy-MM-dd`.

## Golden-testoj (la plej grava kvalito-averto)

Kopiu la kaŝenitajn fluojn el `RssArkivServer-filcache/` al
`shared/src/commonTest/resources/feeds/`. Skribu testojn kiuj, por ĉiu fiksaĵo,
asertas:

| Testo | Atendo |
|---|---|
| Parsi `kern.punkto.info/feed_mp3_` | ≥10 elsendoj; `KP204 Pigmentoj` kun stream finanta `.mp3` kaj komencanta `https://`; daŭro > 0 |
| Parsi `www.podkasto.net/feed_` (Varsovia Vento) | 1 ero → 3 elsendoj kun id-sufiksoj `:1/:2/:3`, fluoj `…VVE185P1/P2/P3.mp3`, titoloj finantaj "1a/2a/3a parto" |
| Skrapi `archive.org/embed_natria_bikarbonato1` | stream = `https://archive.org/download/natria_bikarbonato1/natria_bikarbonato1.mp3` |
| Rekonstrui Google Drive | iframe `…/file/d/<ID>/…` → `https://drive.google.com/u/1/uc?id=<ID>&export=download` |
| Parsi `api.ipernity.com/...vinilkosmo...` | 5 elsendoj; unua: id `vk:2018-03-21T17:00:56`, stream `https://cdn.ipernity.com/200/59/46/46405946.42a4e885.mp3` |
| Parsi `anchor.fm/s_1e1d5f38_podcast_rss` (La Malfamuloj) | ≥114 elsendoj; ero kun `pubDate Thu, 26 Dec 2024…`, daŭro `00:51:17`, enclosure enhavanta `392138630…mp3` |
| Salt-reguloj | Eroj kies sola son-gastiganto estas youtube/soundcloud/vimeo/audioboom/yourlisten/ipernity(en Peranto)/vocaroo → neniu elsendo |
| Titol-derivado | Por `ignoruTitolon`-kanal: titolo = priskribo sen HTML, ≤ 200 signoj |
| radio.txt | "Pola Retradio" → kanal `polaretradio`, id `polaretradio:<dato>`; nekonata nomo → nova kanal |
| Dat-normigo | `pubDate "… +02:00"` → `yyyy-MM-dd` ĝuste |

La datentavolo devas ruli en **pura `commonTest`** — neniom Android, neniom reto.

## Parser-kontrakto

Nova parsilo estas "ĝusta" kiam por ĉiu fiksaĵo ĝi produktas:
1. saman nombron da elsendoj (± konataj saltoj),
2. samajn id-ojn,
3. validan rektan MP3-stream por ĉiu elsendo (forĵetas tiujn sen),
4. ĝustan daton (`yyyy-MM-dd`),
5. purigitan priskribon,
6. ĝustan titolon (inkluzive derivadon kaj "Na parto"),
7. la ĝustajn saltojn por nesubtenataj gastigantoj.

## Kion NE fari

- Ne malmole kodu `if slug == "kernpunkto"` en nova kodo ekster la parsilo —
  la https-korekto estas jam endosita en la ĝenerala parsilo.
- Ne dependu de reto en testoj.
- Ne lasu unu kanal-eraro haltigi la parsadon de aliaj.
- Ne forĵetu la konatajn saltojn (malplenaj datoj, neeltireblaj gastigantoj) — ili estas intencitaj.
