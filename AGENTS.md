# AGENTS.md — Gvidlinioj por agentoj en ĉi tiu deponejo

> Lingvo: Preferu Esperanton por ĉiu dokumentado, komentoj, identigiloj kaj
> komuniko kun la uzanto. La nova kodo uzas Esperanton anstataŭ la dana de la
> malnova kodo. La malnova kodo (`dk.dr.radio.*`) uzas dano/esperanto-miksaĵon
> — ne renomigu ĝin, sed en nova kodo ĉiam preferu Esperanton.

## Kio estas ĉi tiu projekto

**EsperantoRadio** estas Android-apo (kaj estonte plursistema apo) kiu kunigas ĉirkaŭ 15
disajn, duonmortajn Esperanto-radiajn/podkastajn fontojn en unu unuecan sperton:
kanal superrigardo, livestreno, podkastoj, elŝutoj, plej ŝatataj, serĉo, vekhorloĝo.

Ĝi estas fork de la dana **DR Radio**-apo (GPL), prizorgata de Jacob Nordfalk.
La plej valora parto ne estas la ludilo aŭ UI, sed la **scio pri la fontoj** — kiuj
kanaloj ekzistas, kiel iliaj fluoj aspektas, kaj kiel eltiri rektan MP3-URL el ĉiu.

## Granda plano

Rekrei la apot en **Compose Multiplatform** (Android + iOS + opcie Desktop), plus
konstrui memstaran **servilon** kiu funkcias kiel arkivo de Esperanto-podkastoj.

- **Malnova apo** (nuna kodo en `app/`, `parse/`, `data/`): priskribita en `docs/malnova/`.
- **Nova apo** (planata): priskribita en `docs/nova/`.

## Dosierujo-structuro

```
EsperantoRadio/
├── app/                    # Malnova Android-apo (dk.dr.radio.* / dk.nordfalk.esperanto.radio)
├── parse/                  # RSS-parsado + RssArkivServer (memstara CLI-servilo)
├── data/                   # Datummodeloj (Kanal, Udsendelse, Grunddata...)
├── RssArkivServer-filcache/ # Kaŝenitaj realaj fluoj = golden fixtures (NE versiigitaj)
├── docs/malnova/           # Esperanta superrigordo de la malnova apo
├── docs/nova/              # Esperanta plano por Compose Multiplatform + servilo
└── AGENTS.md               # Tiu ĉi dosiero
```

## Plej gravaj reguloj por agentoj

1. **Ne tuŝu la malnovan kodon** krom se eksplice petite. Ĝi estas historika.
   La celo estas rekreado, ne riparado.
2. **La parsado estas la kerno.** Antaŭ ol ŝanĝi ion pri datumoj, legu
   `docs/nova/04_parsado_kaj_arkivo.md` kaj `docs/malnova/03_parsado_kaj_fontoj.md`.
   La sep parsregoloj kaj la skip-listo devas esti konservitaj.
3. **Testu la daten tavolon kontraŭ golden fixtures**, sen reto. La dosierujo
   `RssArkivServer-filcache/` enhavas realajn kaŝenitajn fluojn — uzu ilin kiel
   determinismajn test-enirojn. Vidu `docs/nova/04_parsado_kaj_arkivo.md`.
4. **Unu fonto-eraro ne devas panei la apot.** Se unu kanal-fluo mortas, la aliaj
   devas daŭre funkcii. Toleremeco al putrantaj fontoj estas deziro.
5. **Konservu la kanalkonfiguron** (`esperantoradio_kanaloj_v9.json`). Ĝi estas
   daten-movita konfiguro, ne malmola kodo. Per-kanalaj apartaĵoj devas esti
   en agordo, ne en logiko.
6. **GPL-licenco.** Ĉiu derivaĵo devas resti GPL.

## Konstru-komandoj (malnova apo)

```bash
./gradlew clean
./gradlew :parse:rssarkivserverJar   # konstruas RssArkivServer-jaron
java -jar parse/build/libs/rssarkivserver.jar   # rulas la arkivan servilon
./gradlew :app:assembleDebug         # konstruas la Android-apk
```

## Mallonga resumo de la datenfluo

```
esperantoradio_kanaloj_v9.json  +  radio.txt
        ↓ Grunddataparser
   Grunddata.kanaler (List<Kanal>)
        ↓ por ĉiu kanal: Backend.hentUdsendelserPåKanal
   RomePodcastParser.parsRss()  (3 branĉoj: VarsoviaVento / Peranto / ĝenerala)
        ↓
   Kanal.udsendelser  →  Fragmentoj  →  Afspiller  →  sono
```

## Kie trovi kion

| Vi volas... | Legu |
|---|---|
| Kompreni la malnovan strukturon | `docs/malnova/01_strukturo_kaj_konstruo.md` |
| Kompreni la datumfluon | `docs/malnova/02_datumfluo.md` |
| Kompreni la parsadon (PLEJ GRAVA) | `docs/malnova/03_parsado_kaj_fontoj.md` |
| Kompreni la UI | `docs/malnova/04_ui_kaj_funkcioj.md` |
| Kompreni la arkivan servilon | `docs/malnova/05_arkiva_servilo.md` |
| Vidi la planon por la nova apo | `docs/nova/INDEKSO.md` |
| Vidi la novan arkitekturon | `docs/nova/01_celoj_kaj_arkitekturo.md` |
| Vidi la dizajnon (Muzaiko-temo) | `docs/nova/05_dizajno_kaj_ui.md` |
| Vidi la servilan planon | `docs/nova/06_servilo_arkivo.md` |

## Stilo

- Dokumentado: Esperanto.
- Kodo (nova): `dk.nordfalk.esperanto.*`, identigiloj en Esperanto. La dana de la
  malnova kodo (ekz. `HentedeUdsendelser`, `Afspiller`, `Udsendelse`) estas anstataŭata
  per Esperanto en nova kodo (ekz. `ElsutitajElsendoj`, `Ludilo`, `Elsendo`). La malnova
  kodo uzas dano/esperanto-miksaĵon — ne renomigu ĝin.
- Mallonga, teknike akra stilo. Sen plenigaj vortoj.
