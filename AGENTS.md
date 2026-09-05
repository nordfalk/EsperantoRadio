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

## Stato de la projekto (2026-09-05)

La nova KMP-apo estas en konstruado. Jen la fazoj kaj ilia stato:

| Fazo | Priskribo | Stato | PR |
|---|---|---|---|
| 0 | KMP-strukturo (shared, androidApp, desktopApp, webApp) | ✅ Farita | #5 |
| 1a | Domajnmodeloj, JSONC-leganto, kanalaro-UI | ✅ Farita | #6 |
| 1b | RSS-parsilo (reguloj 6.1, 6.2, 6.4–6.7) + 13 golden-testoj | ✅ Farita | #7 |
| 1c | Kanalvido (elsendlisto) + elsendodetalo + Ktor + Coil 3 | ✅ Farita | #9 |
| 1d | Peranto-parsilo (archive.org + Google Drive, regulo 6.3) | ✅ Farita | #10 |
| 2 | Ludado (LudiloRegilo, Media3 ExoPlayer sur Android, mini-ludilbreto) | ✅ Farita | #11,#13 |
| 3 | Personigo (plejŝatataj, serĉo, agordoj) + navigado | ✅ Farita | #12,#13 |
| — | UI-testoj + Android assets-fix | ✅ Farita | #14 |
| 4 | Malfono & mediaintegriĝo (MediaSession, sciigoj) + persisto | ✅ Farita | #15,#16 |
| 5 | Elŝutoj | 🔨 Nuna | — |
| — | Sonludado sur Web (wasmJs/HTMLAudioElement) | ✅ Farita | #17 |
| — | Sonludado sur Desktop (mp3spi + SourceDataLine) | ✅ Farita | #18 |
| — | Protokolo ĉie en la apo (RSS, navigado, klakoj, eraroj) | ✅ Farita | #19 |
| 6 | Pezaj platform-funkcioj (vekhoro, widget, Chromecast, TTS) | Planita | — |

### Kio funkcias nun

- **Malnova apo** (`malnova/app/`): konstruiĝas kaj funkcias (APK, 18 MB)
- **Nova apo — kanalaro**: montras la realajn kanalojn el la JSONC-konfiguro (Desktop + Android)
- **Nova apo — RSS-parsilo**: parsas ĈIUJN 7 parsregolojn (inkl. Peranto/archive.org)
- **Nova apo — ludado**: vera sonludado sur Android (Media3 ExoPlayer), Web (HTMLAudioElement), Desktop (mp3spi + SourceDataLine)
- **Nova apo — navigado**: kanalaro → kanal → elsendo + serĉo + plejŝatataj + agordoj
- **Nova apo — emblemoj**: Coil 3-bildoj en kanalaro kaj kanalvido
- **Testoj**: 41 testoj (37 KMP sur Desktop + 4 Android sur emulatoro), ĉiuj pasas
- **Web (wasmJs)**: konstruiĝas kaj rulas per `./gradlew :webApp:wasmJsBrowserDevelopmentRun`

### Kio NE funkcias ankoraŭ

- Elŝutoj (eksterreta reĝimo) — fazo 5, nune komencata
- iOS-ludado (no-op, bezonas AVPlayer)
- Vekhorloĝo, hejmekrana widget, Chromecast, talesyntezo

## Granda plano

Rekrei la apot en **Compose Multiplatform** (Android + iOS + Desktop + Web/Wasm), plus
konstrui memstaran **servilon** kiu funkcias kiel arkivo de Esperanto-podkastoj.

- **Malnova apo** (kodo en `malnova/app/`, `malnova/parse/`, `malnova/data/`): priskribita en `docs/malnova/`.
- **Nova apo** (en la radiko): priskribita en `docs/nova/`. La nova Compose Multiplatform-aposieraĵo (`androidApp/`, `iosApp/`, `desktopApp/`, `webApp/`, `shared/`, `server/`) vivas en la radiko, apud `malnova/`, laŭ la oficiala KMP-ŝablono (https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html).

## Dosierujo-structuro

```
EsperantoRadio/
├── malnova/                # Malnova Android-apo (funkcianta, ne tuŝebla)
│   ├── app/                #   Android-apo (dk.dr.radio.* / dk.nordfalk.esperanto.radio)
│   ├── parse/              #   RSS-parsado + RssArkivServer (memstara CLI-servilo)
│   └── data/               #   Datummodeloj (Kanal, Udsendelse, Grunddata...)
├── androidApp/            # Nova Android-aplikaĵo (MainActivity → EsperantoRadioApp)
├── iosApp/                # Nova iOS-Xcode-projekto (malkomentu en settings.gradle.kts sur Mac)
├── desktopApp/            # Nova Desktop-JVM-aplikaĵo (Window + Compose)
├── webApp/                # Nova Web-aplikaĵo (wasmJs, CanvasBasedWindow)
├── shared/                # Nova komuna KMP-modulo
│   ├── src/commonMain/    #   Komuna kodo (modeloj, parsilo, UI, deponejoj)
│   ├── src/androidMain/   #   Android-specifa
│   ├── src/desktopMain/   #   Desktop-specifa (JVM)
│   ├── src/iosMain/       #   iOS-specifa
│   ├── src/wasmJsMain/    #   Web-specifa (wasmJs)
│   └── src/commonTest/    #   Testoj (27 testoj, ĉiuj pasas)
├── server/                # Nova podkasta arkiv-servilo (estonte)
├── settings.gradle.kts     # Kotlin-DSL-build (unuecigita: malnova + nova)
├── build.gradle.kts        # Radika build (KMP + Compose + AGP aldonaĵoj)
├── gradle/libs.versions.toml # Versikatalogo
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
   La testoj jam kopiis 3 fiksaĵojn al `shared/src/commonTest/resources/feeds/`.
4. **Unu fonto-eraro ne devas panei la apot.** Se unu kanal-fluo mortas, la aliaj
   devas daŭre funkcii. Toleremeco al putrantaj fontoj estas deziro.
5. **Konservu la kanalkonfiguron** (`esperantoradio_kanaloj_v9.json`). Ĝi estas
   daten-movita konfiguro, ne malmola kodo. Per-kanalaj apartaĵoj devas esti
   en agordo, ne en logiko.
6. **GPL-licenco.** Ĉiu derivaĵo devas resti GPL.

## Teknikaj scioj lernitaj dum la laboro

- **JDK 17** estas necesa por konstrui la Android-apk (la defaŭlta JDK 21 mankas `jlink`).
  Uzu: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
- **ksoup 0.2.2** estas la versio kongrua kun Kotlin 2.1.0 (0.2.6+ postulas Kotlin 2.3+).
  La API: `Ksoup.parseXml(teksto, "")` por XML, `Ksoup.parse(teksto)` por HTML.
  `selectFirst(...)` ekzistas (ne nur `select(...).firstOrNull()`).
- **Ktor 3 CIO-motoro** funkcias trans ĉiuj platformoj (JVM/Android/Native/WasmJs) sen
  `expect`/`actual`. Nur HTTP/1.x sed sufiĉas por JSON+RSS.
- **Neniu DI-framintervalo** — permana injektado en konstruktiloj, kiel la ekzistanta kodo.
- **Neniu datumbazo** — la kliento simple kaŝenas servil-respondojn kiel dosierojn.
- **Web**: nur `wasmJs` (ne `js` — la JS-celo havis Skia-bindings-eraron). Rulu per
  `./gradlew :webApp:wasmJsBrowserDevelopmentRun`.
- **JSONC-parsado**: la kanalkonfiguro havas `//`-komentojn kaj plurliniajn ĉenojn kun
  `\` ĉe lini-fino. La `KanalAgordoLeganto.striptiguKomentojn` traktas ambaŭ.
- **Varsovia Vento**: la `<audio>`-elementoj estas ene de CDATA en `<content:encoded>`.
  Uzu `getElementsByTag("content:encoded").firstOrNull()?.text()` (ne `html()`) por
  akiri la malkoditan HTML-enhavon, poste `Ksoup.parse(htmlEnhavo)` por trovi `<audio>`.
- **Sonludado — bibliotek-elekto**:
  - **basic-sound** (LexiLabs-App/basic-sound, MIT) estis provita unue. La JVM-implemento
    uzas `javax.sound.sampled.Clip`, kiu ŝargas la TUTAN dosieron en memoron — ne taŭgas
    por podkastoj (100MB+) aŭ rekta radio. Krome mankas `seek` en la komuna API.
  - **ComposeMultiplatformMediaPlayer** (Chaintech, Apache-2.0) postulas Kotlin 2.3.0 /
    Compose 1.10.0 — tro nova por nia Kotlin 2.1.0 / Compose 1.7.3. Ankaŭ bezonas VLC sur Desktop.
  - **JavaFX MediaPlayer** estis provita — `org.openjfx:javafx-media:17.0.13` kompilas
    kaj la naciaj bibliotekoj elŝutiĝas, sed ĉe rulado sur Linukso ĝi donas
    `ERROR_MEDIA_AUDIO_FORMAT_UNSUPPORTED` por MP3, malgraŭ ĉiuj GStreamer-kromprogramoj
    estantaj instalitaj. La OpenJFX-jaro de Maven ne ĝuste ligiĝas al la sistema GStreamer.
  - **mp3spi + SourceDataLine** estis elektita: `com.googlecode.soundlibs:mp3spi` registrigas
    MP3-malkodilon cxe `javax.sound.sampled.AudioSystem`. Pura Java — neniu nacia dependeco.
    Fluas MP3 super HTTP (malfermas `URL.openStream()` → `AudioSystem.getAudioInputStream()` →
    `SourceDataLine.write()` en fona korutino). Subtenas volumon (`FloatControl.Type.MASTER_GAIN`),
    pozicion (kalkulita el bajtoj luditaj). Seek ne implementita (malfacila por streaming MP3).
- **Platforma subteno — sonludado**:

  | Funkcio | Android | Desktop (JVM) | Web (wasmJs) | iOS |
  |---|:--:|:--:|:--:|:--:|
  | MP3-fluado | ExoPlayer | mp3spi + SourceDataLine | HTMLAudioElement | no-op |
  | HLS | ExoPlayer | ne | retumilo | no-op |
  | Seek | ExoPlayer | ne (streaming) | HTMLAudioElement | no-op |
  | Volumo | ExoPlayer | FloatControl | HTMLAudioElement | no-op |
  | Pozicio-sekvado | ExoPlayer | bajtoj/kadraj | eventlistener | no-op |

## Konstru-komandoj

```bash
# Malnova apo
./gradlew :app:assembleDebug         # konstruas la malnovan Android-apk (bezonas JDK 17)
./gradlew :parse:rssarkivserverJar   # konstruas RssArkivServer-jaron
java -jar malnova/parse/build/libs/rssarkivserver.jar   # rulas la arkivan servilon

# Nova apo
./gradlew :shared:desktopTest        # rulas testojn (27 testoj)
./gradlew :desktopApp:run            # rulas la desktop-apo
./gradlew :androidApp:assembleDebug  # konstruas la novan Android-apk
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # rulas la web-apo en retumilo
```

## Datumfluo (nova apo)

```
esperantoradio_kanaloj_v9.json (bundled resource)
        ↓ KanalAgordoLeganto (striptigas // komentojn, traktas JSONC)
   List<Kanal>
        ↓ KanalDeponejoImpl (StateFlow)
        ↓
   KanalaroEkrano (Compose UI — LazyColumn de kanaloj)
        ↓ (estonte: Ktor-kliento elŝutas RSS-fluon)
   RssParsilo.parsRss(fluoTeksto, kanal)
        ↓ (reguloj 6.1–6.7)
   List<Elsendo>
        ↓ (estonte: KanalEkrano — elsendlisto per dat-grupigo)
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
| Vidi la teknikan stakon | `docs/nova/02_teknika_stako.md` |
| Vidi la domajnmodelojn | `docs/nova/03_domajno_kaj_datumoj.md` |
| Vidi la parsad-specifaĵon | `docs/nova/04_parsado_kaj_arkivo.md` |
| Vidi la dizajnon (Muzaiko-temo) | `docs/nova/05_dizajno_kaj_ui.md` |
| Vidi la servilan planon | `docs/nova/06_servilo_arkivo.md` |

## La nova kodo — strukturo

La nova kodo vivas en `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/`:

```
dk/nordfalk/esperanto/
├── App.kt                    # Radika Compose-funkcio (EsperantoRadioApp)
├── domain/
│   ├── model/Modeloj.kt      # Kanal, Elsendo, Sonfonto, LudantoStato
│   └── repository/Deponejoj.kt # KanalDeponejo, ElsendoDeponejo (interfacoj)
├── data/
│   ├── config/KanalAgordoLeganto.kt  # JSONC-leganto (striptigas komentojn)
│   ├── config/PlatformResource.kt   # expect/actual por legi resurcojn
│   ├── parser/RssParsilo.kt         # RSS/Atom-parsilo (sep regoloj)
│   └── repository/KanalDeponejoImpl.kt # Deponej-implementaĵo
└── ui/
    └── KanalaroEkrano.kt     # Kanalaro-ekrano (Compose UI)
```

## Stilo

- Dokumentado: Esperanto.
- Kodo (nova): `dk.nordfalk.esperanto.*`, identigiloj en Esperanto. La dana de la
  malnova kodo (ekz. `HentedeUdsendelser`, `Afspiller`, `Udsendelse`) estas anstataŭata
  per Esperanto en nova kodo (ekz. `ElsutitajElsendoj`, `Ludilo`, `Elsendo`). La malnova
  kodo uzas dano/esperanto-miksaĵon — ne renomigu ĝin.
- Mallonga, teknike akra stilo. Sen plenigaj vortoj.
