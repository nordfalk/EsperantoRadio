# AGENTS.md — Gvidlinioj por agentoj en ĉi tiu deponejo

> **Nuna jaro: 2026 (septembro).** Ĉiuj referencoj al "aktuala" datas
> rilatas al 2026 se ne alie specifite.

> Lingvo: Preferu Esperanton por ĉiu dokumentado, komentoj, identigiloj kaj
> komuniko kun la uzanto. La nova kodo uzas Esperanton anstataŭ la dana de la
> malnova kodo. La malnova kodo (`dk.dr.radio.*`) uzas dano/esperanto-miksaĵon
> — ne renomigu ĝin, sed en nova kodo ĉiam preferu Esperanton.

## Kio estas ĉi tiu projekto

**EsperantoRadio** estas Android-apo (kaj estonte plursistema apo) kiu kunigas 26
disajn, duonmortajn Esperanto-radiajn/podkastajn fontojn en unu unuecan sperton:
kanal superrigardo, livestreno, podkastoj, elŝutoj, plej ŝatataj, serĉo, vekhorloĝo.

Ĝi estas fork de la dana **DR Radio**-apo (GPL), prizorgata de Jacob Nordfalk.
La plej valora parto ne estas la ludilo aŭ UI, sed la **scio pri la fontoj** — kiuj
kanaloj ekzistas, kiel iliaj fluoj aspektas, kaj kiel eltiri rektan MP3-URL el ĉiu.

## Stato de la projekto (2026-09-22)

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
| 5 | Elŝutoj (Ktor→dosiero, persisto, eksterreta ludado) | ✅ Farita | #20,#21,#22,#23 |
| — | Sonludado sur Web (wasmJs/HTMLAudioElement) | ✅ Farita | #17 |
| — | Sonludado sur Desktop (mp3spi + SourceDataLine) | ✅ Farita | #18 |
| — | Protokolo ĉie en la apo (RSS, navigado, klakoj, eraroj) | ✅ Farita | #19 |
| 6 | Pezaj platform-funkcioj (vekhoro, sciigoj, widget, Chromecast, TTS) | 🔨 Nuna (vekhoro + sciigoj faritaj; widget/Chromecast/TTS venas) | #24-#30,#44 |
| — | Ludvico kaj daŭra ludado | ✅ Farita | #46 |
| — | Sciigoj pri novaj elsendoj (WorkManager, "Ludi"-butono en sciigo) | ✅ Farita | #44 |
| — | Sentry.io erarmonitorado | ✅ Farita | #51 |
| — | Riĉa HTML-vidigo, horizontala svipo, evolua reĝimo, konservi dum ekranturnoj | ✅ Farita | #53,#56-#63 |
| — | radioTxtKomparilo + 2 novaj kanaloj (Kaliningrada E-radio, Universeala Lindo) | ✅ Farita | #62 |

### Kio funkcias nun

- **Malnova apo** (`malnova/app/`): konstruiĝas kaj funkcias (APK, 18 MB)
- **Nova apo — kanalaro**: montras la realajn kanalojn el la JSONC-konfiguro (Desktop + Android)
- **Nova apo — RSS-parsilo**: parsas ĈIUJN 7 parsregolojn (inkl. Peranto/archive.org)
- **Nova apo — ludado**: vera sonludado sur Android (Media3 ExoPlayer), Web (HTMLAudioElement), Desktop (mp3spi + SourceDataLine)
- **Nova apo — navigado**: kanalaro → kanal → elsendo + serĉo + plejŝatataj + elŝutoj + alarmoj + agordoj (uzas `navigation3` — `NavKey`/`NavDisplay`/`entryProvider`)
- **Nova apo — elŝutoj**: fluanta elŝuto (Ktor→FileOutputStream), persisto inter restartoj (JSON-metadateno), eksterreta ludado (prefero por loka dosiero)
- **Nova apo — vekhorloĝo**: alarmoj kun sugestoj el JSONC, persisto inter restartoj (Settings+JSON), UI kun kreilo/redaktilo, AlarmManager-skedado (Android), aŭtomata ludado, fallback ringtono, volumo-boost
- **Nova apo — sciigoj**: WorkManager kontrolas ŝatatajn kanalojn por novaj elsendoj (skedita je 7:00 kaj 16:00), sciigo kun "Ludi"-butono kiu lanĉas la elsendon rekte per `MediaController` (`LudiElsendoReceivilo`)
- **Nova apo — emblemoj**: Coil 3-bildoj en kanalaro kaj kanalvido
- **Nova apo — ludvico**: aŭtomata sekva-ludado post naturfino (3 prioritatoj: samkanala → ŝatataj → plej freŝa), pozicio-spurado (ĉiu 5s) kun resumigo, eksplicita ludvico per ludvico-butono (Material 3 ikono), "Lastatempe ludata" sekcio sur HejmoEkrano, "Daŭrigi de X:XX" en ElsendoEkrano
- **Nova apo — HTML**: riĉa HTML-vidigo de priskriboj en ElsendoEkrano (`HtmlVido`/`HtmlTeksto`)
- **Nova apo — gestoj**: horizontala svipo inter elsendoj de sama kanalo (ElsendoEkrano) kaj inter kanaloj (KanalEkrano)
- **Nova apo — evolua reĝimo**: ŝaltilo en agordoj + elektiloj por priskribo-fonto kaj vidmaniero
- **Nova apo — Sentry.io**: erarmonitorado trans ĉiuj platformoj
- **Testoj**: 166 testoj (KMP sur Desktop), ĉiuj pasas
- **Web (wasmJs)**: konstruiĝas kaj rulas per `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- **radioTxtKomparilo**: Desktop-ilo kiu komparas la kanalkonfiguron kun `esperanto-radio.com/radio.txt` — identigas mankantajn kanalojn kaj elsendojn (`./gradlew :desktopApp:radioTxtKomparilo`)

### Kio NE funkcias ankoraŭ

- Vekhorloĝo: podkastoj ne povas aŭtomate ludi ĉe alarmo (nur rekta radio — alarmo lanĉas la kanal-livestreamon, ne specifan elsendon)
- Aŭtomata resumigo: pozicio-restarigo okazas nur kiam oni reiras al la elsendo kaj klakas "Aŭskulti"; ne aŭtomate kiam oni malfermas la apoon kaj la ludilo daŭras en fono
- iOS-ludado (no-op, bezonas AVPlayer)
- Hejmekrana widget, Chromecast, parolsintezo

## Granda plano

Rekrei la apot en **Compose Multiplatform** (Android + iOS + Desktop + Web/Wasm), plus
konstrui memstaran **servilon** kiu funkcias kiel arkivo de Esperanto-podkastoj.

- **Malnova apo** (kodo en `malnova/app/`, `malnova/parse/`, `malnova/data/`): priskribita en `docs/malnova/`.
- **Nova apo** (en la radiko): priskribita en `docs/nova/` (komenco: [`docs/nova/INDEKSO.md`](docs/nova/INDEKSO.md)). La nova Compose Multiplatform-aposieraĵo (`androidApp/`, `desktopApp/`, `webApp/`, `shared/`) vivas en la radiko, apud `malnova/`, laŭ la oficiala KMP-ŝablono (https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html). `iosApp/` (Xcode-projekto) kaj `server/` ankoraŭ ne estas kreitaj — la iOS-kodo jam ekzistas en `shared/src/iosMain/`, sed la Xcode-projekto kaj la podkasta arkiv-servilo restas estontaj.

## Dosierujo-structuro

```
EsperantoRadio/
├── malnova/                # Malnova Android-apo (funkcianta, ne tuŝebla)
│   ├── app/                #   Android-apo (dk.dr.radio.* / dk.nordfalk.esperanto.radio)
│   ├── parse/              #   RSS-parsado + RssArkivServer (memstara CLI-servilo)
│   └── data/               #   Datummodeloj (Kanal, Udsendelse, Grunddata...)
├── androidApp/            # Nova Android-aplikaĵo (MainActivity → EsperantoRadioApp)
├── desktopApp/            # Nova Desktop-JVM-aplikaĵo (Window + Compose)
├── webApp/                # Nova Web-aplikaĵo (wasmJs, CanvasBasedWindow)
├── shared/                # Nova komuna KMP-modulo
│   ├── src/commonMain/    #   Komuna kodo (modeloj, parsilo, UI, deponejoj)
│   ├── src/androidMain/   #   Android-specifa
│   ├── src/desktopMain/   #   Desktop-specifa (JVM)
│   ├── src/iosMain/        #   iOS-specifa (kodo ekzistas; iosApp/ Xcode-projekto ankoraŭ ne kreita — malkomentu `include(":iosApp")` en settings.gradle.kts sur Mac)
│   ├── src/wasmJsMain/    #   Web-specifa (wasmJs)
│   └── src/commonTest/    #   Testoj + desktopTest (166 testoj, ĉiuj pasas)
├── settings.gradle.kts     # Kotlin-DSL-build (unuecigita: malnova + nova; `iosApp`/`server` komentitaj)
├── build.gradle.kts        # Radika build (KMP + Compose + AGP aldonaĵoj)
├── gradle/libs.versions.toml # Versikatalogo (inkl. versionName por Sentry-release)
├── RssArkivServer/        # Kaŝenitaj RSS-fluoj (XML-fiksaĵoj)
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
7. **Ĉiam pripensu ĉu indas fari teston.** Por ĉiu nova funkcio, modelo, aŭ
   regulo, demandu: ĉu tio estas testebla? Se jes, skribu teston. Tamen ne
   faru trivialajn testojn; preferu UI-testojn kiuj testas plurajn tavolojn.
   Ne nur skribu kodon — ankaŭ validigu ĝin. Se estas UI-ŝanĝo, antaŭ ol fari 
   commit, kontrolu ke navigado funkcias, kaj ke la enhavo estas videbla.
8. **Neniam engluti escepton silente.** Ĉiu `catch` bloko devas protokoli la
   eraron per `loge(tag, msg, e)` aŭ `logw(tag, msg, e)` (kun stacktrace).
   Eĉ se la eraro estas atendata aŭ negrava, protokolu ĝin per `logd`.
9. **Uzu `squash`-merge por ĉiuj PR-oj.** Kiam vi kunfandas PR-on al master,
   uzigu `gh pr merge --squash`. Tiel master ricevas precize 1 commit po PR.
   La commit-mesaĝo estu la titolo de la PR + ligo al la PR

## Git-laborfluo

1. Kreu branĉon de `master` (aŭ de la plej nova feature-branĉo se temas pri
   plia kommito en ekzistanta PR).
2. Faru ŝanĝojn, skribu testojn, kompilu kaj rulu testojn.
3. Commit, push, kreu PR kun `gh pr create`.
4. Kiam la PR estas aprobita, kunfandu per `gh pr merge --squash --delete-branch`.
   Tio kreas 1 commit sur master kun la PR-titolo kaj ligilo al la PR.
5. Antaŭ commit kontrolu ĉu uzanto ŝanĝis aferojn rilate al via laboro, se jes
   kaj ŝajnas esti en ordo, aldonu ankaŭ tion al la commit
6. **Ne commitu sen eksplicita peto de la uzanto.** La rajtigo por
   unu commit/push (ekz. "faru PR") validas nur por tiu unu fojo — ĝi ne
   ĝeneraligas al sekvaj ŝanĝoj sur la sama branĉo.

## Teknikaj scioj lernitaj dum la laboro

- **JDK 17** estas necesa por konstrui la Android-apk (la defaŭlta JDK 21 mankas `jlink`).
  Uzu: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
- **Versioj**: Kotlin 2.2.20, Compose Multiplatform 1.10.0, Material3 1.10.0-alpha05,
  Ktor 3.1.3, Media3 1.5.1, Coil 3.0.4, kotlinx-datetime 0.7.0. Vidu `gradle/libs.versions.toml`
  kaj `docs/nova/02_teknika_stako.md`.
  Ĝisdatigo de Kotlin 2.1.0/Compose 1.7.3 al 2.2.20/1.10.0 okazis en #32/#35.
- **ksoup 0.2.2** estas la versio kongrua kun Kotlin 2.2.20 (0.2.6+ postulas Kotlin 2.3+).
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
  Detaloj pri ĉiuj parsreguloj: `docs/nova/04_parsado_kaj_arkivo.md`.
- **Sonludado — bibliotek-elekto** (detaloj: `docs/nova/02_teknika_stako.md`):
  - **basic-sound** (LexiLabs-App/basic-sound, MIT) estis provita unue. La JVM-implemento
    uzas `javax.sound.sampled.Clip`, kiu ŝargas la TUTAN dosieron en memoron — ne taŭgas
    por podkastoj (100MB+) aŭ rekta radio. Krome mankas `seek` en la komuna API.
  - **ComposeMultiplatformMediaPlayer** (Chaintech, Apache-2.0) postulas Kotlin 2.3.0 /
    Compose 1.10.0 — tiam tro nova por nia Kotlin (nun 2.2.20 / Compose 1.10.0, sed la biblioteko bezonas VLC sur Desktop).
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

- **Ludvico kaj daŭra ludado** (detaloj: `docs/nova/03_domajno_kaj_datumoj.md`):
  - `LudvicoLogiko` estas pura decidlogiko (sen flankaj efikoj) — tute testebla.
  - `LudvicoRegilo` observas `LudantoStato.Finita` per `StateFlow.distinctUntilChanged()`
    kaj lanĉas aŭtoludon en aparta korutino (`scope.launch`) por eviti rekurson.
  - Pozicio estas savata ĉiu 5s per `delay()`-bazita korutino. Ĉe `halti()` la
    `nunaFonto` estas forigita de la ludilo, do `LudvicoRegilo` konservas
    `lastaFonto`/`lastaPozicioMs` (kun `@Volatile`) kiel retroiron.
  - `malmarkiFinita()` estas vokata kiam la uzanto eksplicite reludas finitan elsendon.
  - Mutex protektas `ludiElsendon`/`aldoniAlVico`/`ludiSekvan` kontraŭ konkurantaj vokoj.
  - `LudatojDeponejoMaketo` estas la memora implemento por Preview kaj testoj (ne por produktado).

- **Sciigoj pri novaj elsendoj** (detaloj: `docs/nova/03_domajno_kaj_datumoj.md`):
  - `NovajElsendojKontroloWorker` (Android, WorkManager) periode kontrolas ŝatatajn kanalojn
    (skedita je 7:00 kaj 16:00, ±1h), elŝutas RSS-fluojn, kaj kreas sciigojn por neviditaj elsendoj.
  - La sciigo havas "Ludi"-agon: `LudiElsendoReceivilo` ricevas la intenon kaj lanĉas ludadon
    rekte per `MediaController` (konektita al la MediaSession). Uzas `goAsync()` por teni la
    Receivilon vivanta dum la nesinkrona konekto.
  - `GhisdatiguSciigSkedon` ĝisdatigas la WorkManager-skedon kiam la uzanto ŝaltas/malŝaltas sciigojn.
  - `BootReceivilo` re-skedas alarmojn post restarto de la aparato.

- **Navigado**: uzas `navigation3` (`androidx.navigation3`) — `NavKey` ( sealed `Vojo`),
  `rememberNavBackStack`, `entryProvider`, `NavDisplay`. Stato persistebla per `SavedStateConfiguration`
  kun `polymorphic` serializers por ĉiu `Vojo`-subklaso. Detaloj: `docs/nova/05_dizajno_kaj_ui.md`.

## Logcat (Android)

`adb` jam haveblas en la medio (`/home/j/Android/Sdk/platform-tools/adb`) kaj la
emulilo kuras kiel `emulator-5554`. Oni povas legi logcat-on rekte:

```bash
adb logcat -d -t 100                      # lastaj 100 linioj
adb logcat -d | grep FATAL                 # nur kraŝoj
adb logcat -d | grep -E "dk.nordfalk.esperanto.android"  # nur apo
```

## UI-dizajno (celo)

La celo estas moderna podkasta apo-inspirita UI, bazita sur la Figma-dizajno
"Muzaiko — Antonia" (https://www.figma.com/design/uyQhrRKTLfJgQgXgAxchM4/).
Por la plenan dizajnospecifon (koloroj, tiparo, ekranoj, navigado), vidu
[`docs/nova/05_dizajno_kaj_ui.md`](docs/nova/05_dizajno_kaj_ui.md).

Nuna stato: la hejmekrano havas horizontalajn sekciojn (Kio novas, Lastatempe
ludata, Kio popularas) + "Ĉiuj kanaloj". Malsupra naviga breto kun 4 langetoj
(Hejmo, Kanaloj, Plej ŝatataj, Serĉi). MiniLudilbreto kun ludi/paŭzi/halti +
ludvico-butono. La Muzaiko-temo estas implementita en `Temo.kt`.

## Konstru-komandoj

```bash
# Malnova apo
./gradlew :app:assembleDebug         # konstruas la malnovan Android-apk (bezonas JDK 17)
./gradlew :parse:rssarkivserverJar   # konstruas RssArkivServer-jaron
java -jar malnova/parse/build/libs/rssarkivserver.jar   # rulas la arkivan servilon

# Nova apo
./gradlew :shared:desktopTest        # rulas testojn (166 testoj)
./gradlew :desktopApp:run            # rulas la desktop-apo
./gradlew :androidApp:assembleDebug  # konstruas la novan Android-apk
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # rulas la web-apo en retumilo
./gradlew :desktopApp:radioTxtKomparilo  # komparas kanalkonfiguron kun esperanto-radio.com/radio.txt
```

## Datumfluo (nova apo)

```
esperantoradio_kanaloj_v9.json (bundled resource)
        ↓ KanalAgordoLeganto (striptigas // komentojn, traktas JSONC)
   List<Kanal>
        ↓ KanalDeponejoImpl (StateFlow)
        ↓
   KanalaroEkrano (Compose UI — LazyColumn de kanaloj)
        ↓ Ktor-kliento (HttpClient CIO) elŝutas RSS-fluon
   RssParsilo.parsRss(fluoTeksto, kanal)
        ↓ (reguloj 6.1–6.7)
   List<Elsendo>
        ↓ KanalEkrano — elsendlisto (dat-grupigo, diskkaŝmemoro)
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

La nova kodo vivas en `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/`.
Por la arkitekturan kontekston, vidu `docs/nova/01_celoj_kaj_arkitekturo.md`.

```
dk/nordfalk/esperanto/
├── App.kt                    # Radika Compose-funkcio (EsperantoRadioApp, navigation3)
├── AppStato.kt               # Proceznivela unuopulo: deponejoj + ViewModel-oj (pluvivas Activity-rekreon)
├── Protokolo.kt              # Protokol-funkcioj (logi/logd/logw/loge) — expect/actual
├── ProtokoloUtil.kt          # Komuna protokol-helpiloj
├── SentryAgordo.kt           # Sentry.io-init (erarmonitorado)
├── navigation/Vojoj.kt       # NavKey-oj (Hejmo, Kanalaro, KanaloDetalo, ElsendoDetalo, …)
├── domain/
│   ├── model/Modeloj.kt      # Kanalo, Elsendo, Sonfonto, LudantoStato, LudataElsendo
│   ├── model/Alarmo.kt       # Vekhorloĝo-modelo
│   ├── model/ElshutStato.kt  # Elŝut-statoj + ElshutitaElsendo
│   ├── repository/Deponejoj.kt          # KanaloDeponejo, ElsendoDeponejo (interfacoj)
│   └── repository/PersonigoDeponejoj.kt # Plejŝatataj, LastAŭskultitaj, Ludatoj, Serĉo, Agordoj, Elŝut, Alarmo (interfacoj)
├── data/
│   ├── config/KanalAgordoLeganto.kt   # JSONC-leganto (striptigas komentojn) + sugestoj por alarmoj
│   ├── config/PlatformResource.kt    # expect/actual por legi resurcojn
│   ├── config/KreuSettings.kt        # expect/actual por Settings
│   ├── parser/RssParsilo.kt          # RSS/Atom-parsilo (sep regoloj)
│   ├── repository/KanaloDeponejoImpl.kt     # Kanal-deponejo
│   ├── repository/ElsendoDeponejoImpl.kt    # Elsendo-deponejo (Ktor, diskkaŝmemoro)
│   ├── repository/PersistaLudatojDeponejo.kt # Persisto de ludpozicioj (Settings+JSON)
│   ├── repository/PersistantaPlejŝatatajDeponejo.kt
│   ├── repository/PersistantaAlarmoDeponejo.kt # Alarmoj (Settings+JSON)
│   ├── repository/PersonigoDeponejojImpl.kt  # AgordojDeponejoImpl, SerĉoDeponejoImpl
│   ├── repository/DosierKasho.kt            # expect/actual dosier-kaŝmemoro
│   ├── repository/DosierNomoj.kt            # Konvencioj por dosiernomoj
│   ├── repository/AlarmoSkedilo.kt          # expect/actual por AlarmManager-skedado
│   ├── repository/KreuAlarmoSkedilo.kt      # Fabriko
│   ├── repository/ElshutDeponejo (Ktor→dosiero, expect/actual + NoOp)
│   ├── repository/SciigPermeso.kt           # expect/actual por sciig-permeso
│   ├── repository/GhisdatiguSciigSkedon.kt  # Ĝisdatigas WorkManager-skedon laŭ agordoj
│   ├── repository/NovajElsendojSkedilo.kt   # (Android) WorkManager-skedado de novaj-elsendaj kontroloj
│   └── repository/NoOpElshutDeponejo.kt      # NoOp por wasmJs/iOS
├── domain/player/
│   ├── LudiloRegilo.kt       # Ludilo-interfaco + NoOpLudiloRegilo (kun simuluFinon)
│   ├── LudvicoLogiko.kt      # Pura decidlogiko por aŭtoludo (3 prioritatoj)
│   ├── LudvicoRegilo.kt      # Kontrolilo: pozicio-spurado, resumigo, aŭtoludo, ludvico
│   └── SciigoKontroloj.kt    # Sciigo-registroj (MediaSession-metadata)
└── ui/
    ├── KanalaroEkrano.kt     # Kanalaro-ekrano
    ├── HejmoEkrano.kt       # Hejmo (Kio novas + Lastatempe ludata + Kio popularas + Ĉiuj kanaloj) + HejmoViewModel
    ├── KanalEkrano.kt       # Kanalvido (elsendlisto, horizontala svipo) + KanaloViewModel
    ├── ElsendoEkrano.kt     # Elsendodetalo (ludi, elŝuti, ludvico, "Daŭrigi de X:XX", riĉa HTML, horizontala svipo)
    ├── LudvicoEkrano.kt     # Ludvico-ekrano (forigi/malplenigi)
    ├── MiniLudilbreto.kt    # Malsupra ludilbreto (ludi/paŭzi/halti/ludvico)
    ├── MalsupraNavigaBreto.kt # 4 langetoj (Hejmo, Kanaloj, Plej ŝatataj, Serĉi)
    ├── PlejŝatatajEkrano.kt # Plej ŝatataj kanaloj
    ├── SercxoEkrano.kt      # Serĉo
    ├── ElshutitajEkrano.kt  # Elŝutitaj elsendoj (grandeco, daŭro, paŭzo-butono)
    ├── AlarmoEkrano.kt      # Vekhorloĝo (kreilo/redaktilo)
    ├── AgordojEkrano.kt     # Agordoj (daŭrigo, sciigoj, evoluo, temo)
    ├── HtmlVido.kt / HtmlTeksto.kt # Riĉa HTML-vidigo (expect/actual)
    ├── Temo.kt              # Muzaiko-temo (koloroj, tiparo, formoj)
    └── Previews.kt / PreviewDatumoj.kt # Antaŭvidoj
```

> Platformo-specifaj `expect`/`actual` ekzistas en `androidMain/` (ExoPlayer, AlarmManager, WorkManager,
> Receiviloj), `desktopMain/` (DesktopLudiloRegilo mp3spi), `wasmJsMain/` (WasmJsLudiloRegilo HTMLAudioElement),
> `iosMain/` (NoOp). Android-specifaj: `AlarmoReceivilo`, `LudiElsendoReceivilo`, `BootReceivilo`,
> `NovajElsendojKontroloWorker` (WorkManager).

## Stilo

- Dokumentado: Esperanto.
- Kodo (nova): `dk.nordfalk.esperanto.*`, identigiloj en Esperanto. La dana de la
  malnova kodo (ekz. `HentedeUdsendelser`, `Afspiller`, `Udsendelse`) estas anstataŭata
  per Esperanto en nova kodo (ekz. `ElsutitajElsendoj`, `Ludilo`, `Elsendo`). La malnova
  kodo uzas dano/esperanto-miksaĵon — ne renomigu ĝin.
- Mallonga, teknike akra stilo. Sen plenigaj vortoj.
