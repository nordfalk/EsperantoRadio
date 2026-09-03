# 5. La arkiva servilo (RssArkivServer)

## Kio ĝi estas

`RssArkivServer` estas **memstara Java-aplikaĵo** (ne parto de la Android-apo),
kiu konstruas kaj prizorgas kompletan historian RSS-arkivon el la diversaj
podkastaj fontoj. Ĝi rulas sur PC, ne sur telefono.

## Kiel ruli ĝin

```bash
./gradlew :parse:rssarkivserverJar
java -jar parse/build/libs/rssarkivserver.jar
```

## Kie ĝi vivas

```
parse/src/main/java/rssarkivserver/
├── RssArkivServer.java        # ĉefprogramo (main())
├── RomeFeedWriter.java        # skribas RSS 2.0 elsendojn per Rome
├── archiveorg/
│   └── ArchiveOrg.java         # Archive.org-wayback-elŝutilo (memstara ilo)
└── skrald/                     # rubujo / eksperimentoj
    ├── RomeSyndFeedTestParsning.kt
    └── RomePodcastHenter.kt
```

## La `main()`-fluo

1. Preparas `FilCache` en `RssArkivServer-filcache/`.
2. Legas grunddata-JSON de `app/src/main/res/raw/esperantoradio_kanaloj_v9.json`
   per `Grunddataparser.getGrunddataPåPC()`.
3. Reakiras datumojn de la antaŭa rulo (`RssArkivServer.ser`) por rehavi antaŭe
   parsitajn elsendojn.
4. Por ĉiu kanal kun `eo_elsendojRssUrl`:
   - Elŝutas la RSS-fluon per `FilCache.hentFil()`.
   - Parsas per `RomePodcastParser.parsRss()` (sekvante `rss_nextLink`-paĝigon
     por Peranto).
   - Forigas duplikatojn, ordigas laŭ dato.
   - Skribas arkivajn RSS-dosierojn per `RomeFeedWriter.write()` — unu po monato
     (`slug-YYYY-MM.xml`) plus `slug-YYYY-aktuala.xml` plus `slug-aktuala.xml`.
5. Seriigas staton en `RssArkivServer.ser` (por pli posta inkrementa rulo).

## Inkrementa konstruo

La servilo estas **inkrementa**: ĝi aldonas nur elsendojn pli novajn ol la
laste konata (halte kiam jamkonata id estas trovita), kaj sekvas `rel="next"`
paĝigon malantaŭen por elŝuti pli malnovajn elsendojn la unuan fojon.

## `RomeFeedWriter`

Konvertas `List<Udsendelse>` al RSS 2.0 XML per Rome-biblioteko. Kreas `SyndFeed`
kun titolo, ligo, priskribo; ĉiu `Udsendelse` iĝas `SyndEntryImpl` kun titolo,
ligo, publikigdato, priskribo, `enclosure` (audio/mpeg), kaj iTunes-duration-modulo.

## `ArchiveOrg.java`

Memstara utilo kiu elŝutas podkastojn de Archive.org per ekstera `waybackpack`-
komando. Kreas dosierujon `parse/data/s0_archive.org/<host>/`. **Ne integrita
en la ĉefservilan fluon** — estas aparta ilo.

## Rilato al la apo

```
apo  ──── dependas de ────►  parse  ──── dependas de ────►  data
 │                                                      │
 │  Backend.java (apo) vokas Grunddataparser.initGrunddata2()
 │  Backend.java vokas RomePodcastParser.parsRss()
 │  FilCache.java (parse) uzas Kanal/Udsendelse (data)
 │
 │  RssArkivServer.java (parse) estas MEMSTARA main()
 │  — ne estas parto de la Android-apo
 │  — konstruas RSS-arkivojn kiujn la apo povas (eble) uzi
```

La parse-modulo estas ambaŭ:
1. **Biblioteko** — la Android-apo rekte vokas `Grunddataparser`,
   `RomePodcastParser`, `FilCache`, `Diverse`, `UnescapeHtml`.
2. **Memstara programo** — `RssArkivServer.main()` rulas sur PC por konstrui
   arkivajn RSS-fluojn.

## La "sekureca valvo"

La apo nune elŝutas plejparte rekte de la fontfluoj. La arkiva servilo povas
liveri **puran, kunfanditan fluon** (URL-modelo `…/podkasta_arkivo/feed-<slug>.xml`)
— kaj kiam kanal-fluo enhavas `podkasta_arkivo`, ĉiuj po-kanalaj specialaĵoj
estas preterpasataj (la fluo jam estas normigita → "ĝenerala" parsado). Tio
estas la plej grava sekureca valvo: rompiĝemaj fontoj povas esti normigitaj
servilflanke kaj la apo evitas skraplogikon.

## `RssArkivServer-filcache/`

La dosierujo `RssArkivServer-filcache/` (gitignorita) estas kreita de
`FilCache.init(new File("RssArkivServer-filcache"))`. Ĝi kaŝenas la realajn
RSS-fluojn elŝutitajn dum arkiva konstruado — kaj funkcias kiel **golden fixtures**
por testado (vidu `03_parsado_kaj_fontoj.md`). La nomo de ĉiu dosiero estas la
URL kun ne-alfabetaj signoj anstataŭigitaj per `_` (vidu `FilCache.findLokaltFilnavn()`).
Ekzemple `https://www.podkasto.net/feed/` iĝas `www.podkasto.net_feed_`.
