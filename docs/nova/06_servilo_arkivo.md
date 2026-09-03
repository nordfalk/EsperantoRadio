# 6. La podkasta arkiv-servilo

> La nova projekto inkluzivas memstaran **servilon** kiu funkcias kiel arkivo
> de Esperanto-podkastoj. Ĝi estas la spirit-ido de la malnova `RssArkivServer`
> (vidu `../malnova/05_arkiva_servilo.md`), sed reverkita en Kotlin kaj pli
> kapabla: ne nur konstruu arkivajn fluojn, sed ankaŭ **servu** ilin per HTTP API.

## Kial servilo

La fontoj estas rompiĝemaj. La apo povas funkcii sen la servilo (rekte el la
font-fluoj), sed la servilo estas la **sekureca valvo**: ĝi normigas la
rompiĝemajn fontojn servilflanke kaj liveras purajn, stabilajn RSS-fluojn kiujn
la apo povas trakti per la simpla "ĝenerala" parsado.

## Du roloj

1. **Arkivo-konstruilo** (batch) — ruliĝas regulige, marŝas tra la fontoj,
   sekvas `rel="next"` paĝigon malantaŭen, kreas/kunfandas la kompletan historion.
2. **HTTP-servilo** (daemon) — servas la arkivajn fluojn kaj JSON-API al la apo.

## Modulo (`server/`)

```
server/
├── src/main/kotlin/dk/nordfalk/esperanto/server/
│   ├── ArkivKonstruilo.kt       # batch-konstruado (main())
│   ├── ArkivaServilo.kt        # Ktor-server (HTTP-API + statika dosier-servado)
│   ├── FluSkribilo.kt          # skribas RSS 2.0 / Atom (anstataŭ RomeFeedWriter)
│   ├── Stokado.kt             # persistaj elsendoj (JSON/SQLite)
│   └── ...
├── build.gradle.kts
└── data/                       # la konstruita arkivo (RSS-dosieroj, stato)
```

## Teknologio

- **Ktor Server** (pura Kotlin, KMP/JVM) — HTTP-servado, malpeza.
- **Kotlinx Serialization** — JSON-API-respondoj.
- Reuzu la komunajn parsregolojn el `shared/data/parser` (la **sama** parsilo
  kiel la apo — tio estas la avantaĝo de KMP).
- Stokado: unue JSON-dosieroj (simile al `RssArkivServer.ser`), poste SQLite
  se la datumo kreskas.

## Arkivo-konstruado (inkrementa)

```kotlin
suspend fun konstruuArkivon() {
    val kanaloj = leguKanalAgordon()
    for (kanal in kanaloj.filter { it.havasPodkastojn }) {
        try {
            val konataj = stokado.getElsendojn(kanal.slug)
            val plejMalnova = konataj.minByOrNull { it.dato }
            var paghoUrl = kanal.podkastaRssUrl
            val novaj = mutableListOf<Elsendo>()
            while (paghoUrl != null) {
                val fluo = httpKliento.get(paghoUrl).bodyAsText()
                val elsendoj = parsilo.parsRss(fluo, kanal)
                // Haltu ĉe jamkonata id (inkrementa)
                val neKonataj = elsendoj.takeWhile { e -> e.id !in konataj.map { it.id } }
                if (neKonataj.isEmpty()) break
                novaj.addAll(neKonataj)
                paghoUrl = leguNextLink(fluo)   // rel="next" (regulo 6.7)
            }
            val cxiuj = (konataj + novaj).distinctBy { it.id }.sortedByDescending { it.dato }
            stokado.skribu(cxiuj, kanal.slug)
            skribuFluDosierojn(cxiuj, kanal)   // monato/jaro/kunfandita
        } catch (e: Exception) {
            protokolo("Kanalo ${kanal.slug} fiaskis: ${e.message}")
            // daŭrigu — unu eraro ne haltigas la aliajn
        }
    }
}
```

## Eliraj flu-dosieroj (kiel la malnova `RomeFeedWriter`)

Po kanal, la servilo skribas:
- `feed-<slug>-<yyyy-MM>.xml` — monataj fluoj
- `feed-<slug>-<yyyy>-aktuala.xml` — jara aktuala fluo
- `feed-<slug>-aktuala.xml` — la plej novaj elsendoj
- `feed-<slug>.xml` — kompleta kunfandita fluo

Formato: RSS 2.0 kun iTunes-modulo (duration, image). Ĉiu elsendo havas
`<enclosure url="…mp3" type="audio/mpeg">`.

## HTTP-API

### Statikaj fluoj

```
GET /feed-<slug>.xml              # kompleta fluo
GET /feed-<slug>-aktuala.xml      # nur plej novaj
GET /feed-<slug>-<yyyy-MM>.xml    # monata arkivo
```

Kiam la apo vidas `podkasta_arkivo` en la URL, ĝi uzas la **ĝenerala** parsadon
(jam normigita) — ĉiuj per-kanalaj specialaĵoj estas preterpasataj.

### JSON-API

```
GET /api/kanaloj                       → List<KanalDto>
GET /api/kanaloj/<slug>                 → KanalDto
GET /api/kanaloj/<slug>/elsendojn       → List<ElsendoDto> (?pagho=, ?ekde=)
GET /api/kanaloj/<slug>/elsendojn/<id>  → ElsendoDto
GET /api/sercxi?q=<teksto>              → List<ElsendoDto>
GET /api/radio.txt                      → la kunfandita radio.txt
GET /api/stato                          → servil-stato, kanal-sano, lasta konstru-tempo
```

### JSON-API-respondo (ekzemplo)

```json
{
  "slug": "kernpunkto",
  "nomo": "Kernpunkto",
  "elsendojn": [
    {
      "id": "kernpunkto:2022-11-09",
      "titolo": "KP204 Pigmentoj",
      "dato": "2022-11-09",
      "dauro": 6916,
      "stream": "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
      "bildUrl": "https://kern.punkto.info/bildoj/kp204-pigmentoj.jpg",
      "priskribo": "Ekde nia infaneco ... la pigmentoj"
    }
  ],
  "pagho": {
    "sekva": "/api/kanaloj/kernpunkto/elsendojn?pagho=2"
  }
}
```

## Servil-sano kaj toleremo

La `/api/stato`-punkto raportas:
- kiuj kanaloj sukcese konstruiĝis, kiuj malsukcesis,
- lasta konstru-tempo,
- nombro da elsendoj po kanal.

La apo povas uzi tion por decidi ĉu fidi la arkiv-fluon aŭ fali reen al la
rekta font-fluo.

## Ruli la servilon

```bash
# Konstrui la arkivon (batch)
./gradlew :server:run --args="konstruu"

# Ruli la HTTP-servilon (daemon)
./gradlew :server:run --args="servu --port=8080"

# Aŭ pakita:
./gradlew :server:installDist
./server/build/install/server/bin/server servu --port=8080
```

## Celscenaroj

| Celo | Kiel |
|---|---|
| Apcjelo elŝutas kanalojn | `GET /api/kanaloj` |
| Apo bezonas la plej novajn elsendojn de Kernpunkto | `GET /feed-kernpunkto-aktuala.xml` aŭ `GET /api/kanaloj/kernpunkto/elsendojn` |
| Font-fluo de Varsovia Vento mortis | servilo jam havas la arkivitan fluon; apo uzas `podkasta_arkivo`-URL → ĝenerala parsado |
| Uzanto serĉas "pigmentoj" | `GET /api/sercxi?q=pigmentoj` |
| Prizorganto volas scii kiuj kanaloj fiaskis | `GET /api/stato` |

## Rilato al la malnova `RssArkivServer`

| Malnova | Nova |
|---|---|
| Java-jar (`rssarkivserver.jar`) | KMP-servilo (Ktor) |
| Nur konstruas dosierojn | Konstruas + servas per HTTP |
| Java-seriigo (`RssArkivServer.ser`) | JSON/SQLite-stokado |
| Rome-biblioteko por skribi fluojn | `FluSkribilo.kt` (pura Kotlin) |
| `FilCache` por kaŝeni fontfluojn | reuzo de Ktor-`HttpCache` + `RssArkivServer-filcache/` |
| Malmolaj parsbranĉoj | komunaj parsregoloj el `shared/data/parser` (unu kodo, du uzoj) |

La plej grava avantaĝo: **la servilo kaj la apo dividas la saman parsilon**.
Kiam la fonto-kono estas ĝisdatigita (ekz. nova iframe-regulo), ĝi estas
ĝisdatigita unuloke en `shared/` kaj kaj la apo kaj la servilo profitas.
