---
name: kanalkonfiguro
description: Ŝargu ĉi tiun skill-on kiam vi redaktas la kanal-konfiguron (esperantoradio_kanaloj_v9.json / JSONC), aldonas aŭ forigas kanalon, ŝanĝas puriguModeloj-regexojn, aŭ prilaboras RSS-parsadon kaj podkastajn fluojn. Ankaŭ por radioTxtKomparilo kaj serĉado de novaj fluoj.
---

# Kanalkonfiguro (JSONC) kaj parsado

> Antaŭ ĉio legu `docs/nova/04_parsado_kaj_arkivo.md` kaj
> `docs/malnova/03_parsado_kaj_fontoj.md` — la sep parsreguloj kaj la skip-listo
> devas esti konservitaj. La konfiguro estas daten-movita (regulo A5):
> per-kanalaj apartaĵoj en agordo, ne en logiko.

## La du kopioj kaj la du aroj

- La konfiguro ekzistas **dufoje kaj devas resti identa**:
  - `shared/src/commonMain/resources/esperantoradio_kanaloj_v9.json`
  - `androidApp/src/androidMain/assets/esperantoradio_kanaloj_v9.json`
- Sur wasmJs/iOS ĝi estas enigita kiel Kotlin-ĉeno dum la konstruo
  (`generuEnigitanKanalkonfiguron` en `shared/build.gradle.kts`) — la fonto restas
  la JSONC-dosiero.
- La JSONC havas **du arojn**: `kanaloj` (aktivaj) kaj `FORPRENITAJ_KANALOJ`
  (referencataj, de la kodo NE legataj). Novaj kanaloj devas iri en `kanaloj`;
  la dosiero FINIĜAS per la forprenitaj — "aldonu ĉe la fino" surteriĝas en la
  malĝusta aro (2026-10-08: la 7 novaj podkastoj unue erare tie).

## JSONC-sintakso-minacoj

- `//`-komentoj kaj plurliniaj ĉenoj kun `\` ĉe lini-fino — traktataj de
  `KanalAgordoLeganto.striptiguKomentojn`.
- **Regex-`"`-signoj devas esti refutitaj** (`\"`): regex kiel
  `<p class="who">.+?</p>` rekte en `puriguModeloj` rompas la tutan konfiguron
  (`JsonDecodingException`). Uzu `<p class=\"who\">(?:(?!</p>).)*?</p>`.
- **Forigo de ŝlosilo per skripto lasas vostan komon**, se ĝi estis la lasta ero —
  `KanalAgordoFumTesto` kaptas per `JsonDecodingException: Trailing comma`.

## Purigaj regex-modeloj

`<p>.+?FRazo.+?</p>` + DOTALL estas **danĝeraj**: la `.+?` komenciĝas ĉe la unua
`<p>` en la tuta teksto kaj manĝas ĉiun alineon ĝis la celita frazo (forviŝis la
enkondukon de Varsovia Vento). Uzu la temperitan formon `(?:(?!</p>).)` anstataŭ
`.+?` por ke la modelo ne povu transiri alineon.

## Kontrolu post ĉiu programa redakto

```bash
./gradlew :shared:desktopTest        # inkl. KanalAgordoFumTesto kaj KanalAgordoLegantoTest
./gradlew :desktopApp:radioTxtKomparilo   # komparas kontraŭ esperanto-radio.com/radio.txt
```

## Serĉi novajn podkastajn fluojn

- **iTunes Search-API** eldonas la kanonikan RSS-URL-on:
  `https://itunes.apple.com/lookup?id=<appleId>` → JSON kun `feedUrl`,
  `artworkUrl600`, `releaseDate`, `trackCount`;
  `https://itunes.apple.com/search?term=Esperanto&entity=podcast&limit=25`
  listigas ĉiujn ĉe Apple indeksitajn Esperanto-podkastojn (tiel kolektiĝis la
  fluoj de la 7 novaj kanaloj, 2026-10-08).

## Specialaj parsaj kazoj (el la sep reguloj)

- **Varsovia Vento**: la `<audio>`-elementoj estas ene de CDATA en
  `<content:encoded>` — uzu `getElementsByTag("content:encoded").firstOrNull()?.text()`
  (ne `html()`), poste `Ksoup.parse(htmlEnhavo)` por trovi `<audio>`.
- Aliajn regulojn (P6.1–P6.7, Peranto/archive.org) vidu en `docs/nova/04`.
