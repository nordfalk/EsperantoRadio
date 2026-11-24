# FAROTA.md — Malfermitaj trovoj por posta riparado

> **Kiel uzi** (ĝisdatigita 2026-11-20): ĉi tiu dosiero enhavas NUR la MALFERMAJN punktojn, ordigitaj laŭ graveco (KRITIKA → GRAVA → KONSILO). Kiam punkto estas solvita, FORIGU ĝin tute — ĝia priskribo kaj la historio de la riparo estas arkivitaj en `git log` de ĉi tiu dosiero, en la PR/CHANGELOG kaj en [`ŜANĜOJ.md`](ŜANĜOJ.md); la daŭre valida scio el finitaj punktoj staras en [`NOTOJ.md`](NOTOJ.md).
> **Rilato al la aliaj dosieroj** (ekde 2026-10-11): [`NOTOJ.md`](NOTOJ.md) = konstanta scio laŭ temo; [`ŜANĜOJ.md`](ŜANĜOJ.md) = historio de la riparoj (problemo→kaŭzo→testo); `CHANGELOG.md` = uzantvida resumo.

## Revizio 2026-11-20 (kontraŭ master + Sentry, 7–14 tagoj)

Ĉiuj punktoj sube kontrolitaj kontraŭ la nuna kodo; Sentry-datumoj de 2026-11-13 → 11-20.

| Punkto | Verdikto 2026-11-20 |
|---|---|
| G42 | **SOLVITA 2026-11-20**: la vera risko estis `scope.launch` en `onDispose` (App.kt) — nun nova scope; ceteraj uzoj jam uzas `LaunchedEffect`/nuligeblajn taskojn. Punkto forigita. |
| G44 | ** Ankoraŭ aktuala, nova kaŭzo**: anchor.fm nun redonas 403 `geofence:blocked` (Spotify-geobloko); krome substack/vaticannews ConnectTimeout, ivoox 503, "Connection refused" (3zzzenesperanto, peranto). |
| C5 | Aktuala (`build.gradle.kts:64` hardkodita keystore-vojo). |
| C9 | Parte: `jcenter()` forigita, sed la jcenter-URL ankoraŭ registrita kiel maven-repo; `verification-metadata.xml` mankas; dependabot.yml ekzistas. |
| C15, C16, C21, C25, C30 | **SOLVITAJ 2026-11-20** (forigo de malnovaj aŭskultantoj en wasmJs; fermo de `rawStream` + KDoc de `saltiAl` sur Desktop; forigo de `LOCKED_BOOT_COMPLETED`; asertoj en `EkranfotoTesto`; KDoc en `LudvicoLogiko`) — punktoj forigitaj el ĉi tiu dosiero; verdaj: `:shared:desktopTest`, `:shared:compileKotlinWasmJs`, `:androidApp:assembleDebug`. |
| C22, C40 | **SOLVITAJ 2026-11-20** (`lifecycleScope` anstataŭ propra ne-nuligebla scope en `MainActivity`; `-Xmx3g` en `gradle.properties`) — punktoj forigitaj; verdaj: `:androidApp:assembleDebug`, `:shared:desktopTest`. |
| C26, C27, C32 | **SOLVITAJ 2026-11-20** (horloĝ-injekto en `HejmoViewModel`/`HejmoEkrano` + fiksa dato en la testo; pura `estasPasemaHttpStatuson` en commonMain + unuo-testo; tuŝ-celoj pligrandigitaj al 48 dp) — punktoj forigitaj; verda: `:shared:desktopTest`. |
| C34 | Forigita 2026-11-20: la TODOj jam estas en la iOS-kodo (`PlatformLigilo.kt` + deep-link-noto); deep links restas parto de C58. |
| C29 | **SOLVITA** — `resolutionStrategy.force` forigita; punkto forigita el ĉi tiu dosiero. |
| C40 | **SOLVITA 2026-11-20** (`-Xmx3g`); punkto forigita. |
| C41 | **SOLVITA 2026-11-20** (AGENTS.md mencias nun la diagnozan ekranon; `docs/nova/02/03` korektitaj pri `puriguModeloj`); punkto forigita. |
| C43 | **SOLVITA 2026-11-20** (FilCache-semantiko, per-jaraj arkivdosieroj kaj `httpKliento`/`ArchiveOrgDosiernomoKaŝo` dokumentitaj en `docs/nova/06`); punkto forigita. |
| C44 | **SOLVITA 2026-11-20** parte: (c) regulo „>95 % → de 0“ reenkondukita (`LudvicoLogiko.komencaPozicio` + testo), (g) golden-testo kun kruda `&nbsp;`; la ceteraj konsideroj (a,b,d,e,f) intence ne reenkondukitaj. Punkto forigita. |
| C54 | **SOLVITA 2026-11-20**: butono „Kopii RSS-URL“ en KanalEkrano + `kopiuTekston` (expect/actual por android/desktop/wasmJs/ios). Punkto forigita. |
| C50, C51, C52, C55, C58 | Ankoraŭ aktualaj (C51(f),(h) kaj C52(a) parte; C51(c) solvita). C56 SOLVITA 2026-11-24: retversio gastigata samdevene je javabog.dk/apo/, nur-arkiva datumvojo, sono rekte el la fonto (PR/opo #210). |
| C46 | **SOLVITA 2026-11-20**: lud-eraroj nun raportiĝas per 2-arg `logw` (videblaj en Sentry Logs); reprovo jam ekzistis (`ReprovoLogiko`). Punkto forigita. |
| C47 | Forigita 2026-11-20 laŭ decido de la posedanto (observa punkto, historio en git log). |
| C49 | Nuntempe ne aktiva: neniu `RespondoTroGrandaException` en 30 tagoj. Plu observu malaltpriore. |
| C53 | Parte farita: HUMILOPINIE aldonita; restas kontroli "Una Vaga Idea"/"Cuéntame, Hermosura". |
| C59 | Parte farita: `transsaltuKomenceSekundojn` ekzistas kun ĉiuj valoroj; restas (b) plena aŭtomatiĝo (`enkonduk-detekto`). |
| C61 | **SOLVITA 2026-11-24** (`aktualaVersio` vivas kaj pelas la ĝisdatigo-indikilon — tipo `Platformo`, versiokarto, Play-butono; `funkciaAnonco` forigita el la konfiguro, PR #204) — punkto forigita el ĉi tiu dosiero. |

## GRAVA

#### G44 — KMP-N k.a.: daŭraj RSS-malsukcesoj de specifaj kanaloj (1227 okazoj / 57 uzantoj ekde 2026-09-13)
- **Statuso**: [ ]
- **Fonto**: Sentry-analizo 2026-10-19 (issue [KMP-N](https://esperantoradio.sentry.io/issues/KMP-N) kaj parencaj; en la lasta semajno: spiritismavocxo, peranto, bitmono, radiovatikana, sano — ofte "uzas 0 kaŝenitajn elsendojn"; pli frue ankaŭ lamalfamuloj, babibejo, universeala-lindo, varsoviavento, kaliningrada, radioscienca, kernpunkto, vinilkosmo, movada-vidpunkto, polaretradio)
- **Loko**: `esperantoradio_konfiguro.json` (fluo-URL-oj), `ElsendoDeponejoImpl.kt` (ŝargo), `NovajElsendojKontroloWorker.kt:119-121`
- **Priskribo**: Parto estas pasemaj ret-problemoj (timeout/DNS ĉe spreaker, anchor.fm, blogspot — KMP-12/14/15/M/Q), sed la ripetiĝo ĉe la samaj kanaloj sugestas mortajn aŭ ŝanĝitajn fluojn. `radiovatikana` aperas ankaŭ en lud-eraroj (`UnknownHostException` por `media.vaticannews.va`) — eble la tuta gastigo estas morta.
- **Sugesto**: kontroli ĉiun suspektatan fluon (curl: ĉu HTTP 200 + RSS?), ĝisdatigi la URL-on aŭ marki la kanalon neaktiva en la agordo (skill `kanalkonfiguro`); konsideri pli longan timeout/reprovon por malrapidaj gastigoj.
- **Ĝisdatigo 2026-10-27** (Sentry 3.1.0/252, 14 tagoj + ekstera kontrolo): `radioscienca` fiaskas ankaŭ en la nuna eldono — ~15 eventoj ĉe ~5 uzantoj, ofte kun **0 kaŝenitaj elsendoj** (uzanto vidas malplenan kanalon). La fluo respondas HTTP 200 de ekstere (kontrolita per curl, ankaŭ kun Ktor/okhttp/apo-UA — do ne UA-blokado, sed intermita servila problemo de la gastiganto). Same aperas: `polaretradio`, `radiovatikana`, `spiritismavocxo` (0 kaŝenitaj), `lemondediplomatique`, `lamalfamuloj`, `vinilkosmo`. La DNS-avertoj (`UnresolvedAddressException`, 52 eventoj) venas de unu aparato sen reto kaj estas pritraktitaj ĝuste per kaŝmemoro.

## KONSILO

#### C5 — Subskrib-konfiguro: defaŭlta absoluta keystore-vojo en publika deponejo
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:64` (`storeFile = file(System.getenv("KEYSTORE_PATH") ?: "/home/j/android/A_signaturer/jacobnordfalk.keystore")`, pasvortoj defaultas al `""`)
- **Sugesto**: forigu la hardkoditan vojon; ĵetu eraron se `KEYSTORE_*` mankas ĉe eldon-konstruo (fail-fast). (La CI `eldonado.yml` faras tion ĝuste: GitHub-secrets → env.)

#### C9 — Gradle-higieno: dependency verification, Dependabot, jcenter
- **Statuso**: [ ]
- **Loko**: radika `build.gradle.kts:25-27` (la jcenter-URL ankoraŭ registrita kiel `maven { url = ... }` — nur por malnovaj dependoj de `malnova/`); neniu `gradle/verification-metadata.xml`; `.github/dependabot.yml` ekzistas
- **Sugesto**: aldonu Gradle-dependency-verification; enŝaltu OWASP `dependencyCheck` en CI; limigu la jcenter-repon al la malnova projekto aŭ forigu ĝin iam.

#### C49 — Kontrolu ĉu `RespondoTroGrandaException` trafas realan fluon (limo 1 MB, G45)
- **Statuso**: [ ]
- **Fonto**: G45-riparo (2026-10-20): `KorpaLegilo.kt` limas la RSS-korpo-legadon al
  `MAKSIMUMA_FLUA_GRANDECO_BAJTOJ = 1 MB` (la plej granda konata legitima fluo ~310 KB).
- **Priskribo**: Se iam reala kanal-fluo transpasos 1 MB (ekz. podkasto kun centoj da
  longaj priskriboj), la kanalo silente ne ŝargiĝos — la escepto estas nur `logw`-averto,
  ne eraro. Tio estas intence (memor-protekto), sed necesas observado.
- **Sugesto**: periode (ekz. ĉiusemajne, per la skill-o `sentry-esplorado`) serĉi en
  Sentry la escepto-nomon `RespondoTroGrandaException` aŭ la protokolon "transpasas la
  limon": se ĝi aperas por reala fluo (ne por misdirektita MP3), levu
  `MAKSIMUMA_FLUA_GRANDECO_BAJTOJ` aŭ serĉu la kaŭzon en la koncerna kanalo.

#### C50 — Eldonaj kanaloj ekster Google Play: F-Droid, Aptoide
- **Statuso**: [ ]
- **Fonto**: README.md "Farota (notoj)" (2026-10-24, movita ĉi tien; prepara laboro en PR #66)
- **Priskribo**: la apo eldoniĝas nur en Google Play; F-Droid kaj Aptoide estis planitaj
  sed neniam realigitaj.
- **Progreso (2026-10-29)**: la teknika preparo estas farita — la paŝoj de la
  posedanto staras en `docs/nova/07_eldonado.md` (sekcioj 2 kaj 3):
  - nesubskribita release-konstruo ebligita (release subskribas nur kiam la
    keystore ekzistas; pruvita en pura worktree — F-Droid kondiĉo);
  - `fdroid/metadata/dk.nordfalk.esperanto.radio.yml` estas pretigita laŭ la
    nuna fdroiddata-formato kaj **kontraŭvalidigita kontraŭ la oficiala
    `schemas/metadata.json`** (kategorioj `Radio`/`Podcast`; `AutoUpdateMode:
    Version` — la malnova `v%v`-ŝablono ne plu rajtas; lokalizitaj kampoj
    malpermesataj en la yml — la en-US-tekstojn F-Droid legas aŭtomate el
    nia Play-listing `androidApp/src/main/play/` (Triple-T-strukturo));
    3.1.1/253, etikedo `v3.1.1`, aŭtomata ĝisdatigo per etikedoj
    (la unua fork-CI-provo de la posedanto malkaŝis la skemo-erarojn, 2026-10-31);
  - ĉiuj ses forko-CI-taskoj reproduktitaj VERDAJ loke per la samaj iloj
    (fdroidserver + check-jsonschema + tools/-skriptoj, 2026-10-31): la yml
    estas nun en la kanona rewritemeta-formo (neniu komento), NENIU teksto
    en fdroiddata (resumo/priskribo/ŝanĝoprotokolo venas el nia Play-listing,
    Triple-T + `release-notes/en-US/default.txt`), `commit` = plena haketaĵo,
    `UpdateCheckName: Ignore` (la nom-boto prenus la nomon de la malnova
    apo — KMP-manifesto ne troveblas), `scanignore: build.gradle.kts`
    (la skanilo rifuzas la jcenter-deklaron necesan por la malnova apo);
    `fdroid checkupdates --auto` finas per „up to date" kaj nenia diff;
  - fdroid-internaĵoj esploritaj kaj dokumentitaj: `gradlew-fdroid` trovas la
    radikan `gradle/wrapper` ankaŭ el `subdir: androidApp`; la `gradle:`-kampo
    signifas gustumojn (`yes` = `assembleRelease`), ne taskojn;
  - Sentry-raportado iĝis **dumoda** (PR #148): konstruoj sen nia subskribo
    (tiel F-Droid konstruas) = opt-in kun unufoja konsento-demando; konstruoj
    kun nia subskribo (Play/Aptoide) = defaŭlte ŝaltita, malŝaltebla en
    Agordoj → Privateco (kun fina protokolo ĉe malŝalto kaj protokolo ĉe
    reŝalto) — la F-Droid-kontraŭeco `Tracking` ne aplikeblas al la
    F-Droid-konstruo.
- **Stato (2026-10-31)**:
  - **fdroiddata-MR !55129 estas SUBMETITA kaj atendas revizion**:
    https://gitlab.com/fdroid/fdroiddata/-/merge_requests/55129 —
    „New app: EsperantoRadio", 27/28 ŝablonaj kestoj (nur Reproducible
    Builds malmarkita kun kialo), 4 ekranfotoj, ŝanĝoj-tabulo = nur la
    yml, ĉiuj pipelinoj verdaj INKLUZIVE fdroid build (la APK eĉ
    konstruiĝis kaj F-Droid-subskribiĝis sur ilia servilo). La
    prizorgantoj etikedos (New App/review-requested — ekstera kontribuinto
    ne povas mem) kaj revizios, tigoj–semajnoj; **respondu rapide al
    komentoj** (tio decidas la rapidecon).
  - **Play**: 3.1.1 (versionCode 254) kuŝas en la interna trako kun la
    „Nova en 3.1.1"-notoj — restas nur la **promocio al produktado**
    (https://play.google.com/console/u/0/developers/8294421063218887510/app/4973090730523647228/releases/overview).
    Rimarko: F-Droid konstruas la etikedon v3.1.1 kiel **253** (253
    bruliĝis en Play pro la interna testo 3.1.1-beta29okt el la
    nekunfandita PR #144; ekde v3.1.2 la kodoj re kongruos).
  - **Sentry-telemetrio** (C48/C49/G44): el F-Droid-uzantoj nur konsentintoj
    plu raportos; la malpliiĝo estas atendata, ne incidento.
- **Stato (2026-11-22)**: la revizianto demandis en la MR-fadeno
  „Why no RB? Provide reason else put Binaries and AllowedAPKSigningKeys in
  yaml and release signed apk in github releases". Ni elektis **plenajn RB**:
  - `BuildConfig.SEN_NIA_SUBSKRIBO` forigita — la telemetria defaŭlto decidiĝas
    RUTEMPE per la subskriba atestilo de la APK
    (`estasSubskribitaPerNiaAtestilo()`, SHA-256-komparo);
  - `dependenciesInfo { includeInApk = false; includeInBundle = false }`;
  - kontrolite: du sendependaj konstruoj de la sama fontstato -> bit-identa
    nesubskribita APK (haketo `9e6acc63…`);
  - RESTAS: venonta eldono kun kongrua versionCode (ekde v3.1.2 re unu
    numero), subskribita APK en GitHub-releases, ĝisdatigita yml
    (+ `AllowedAPKSigningKeys`/`Binaries`), respondo en la MR-fadeno.
    Detaloj: docs/nova/07_eldonado.md -> „Reprodukteblaj konstruoj (RB)".
- **Restas poste**: (1) post mergo de !55129 — 1–3 tagoj ĝis la paĝo
  https://f-droid.org/en/packages/dk.nordfalk.esperanto.radio/ ekvivos, tiam
  „Get it on F-Droid"-emblemo al la README (GVIDILO § 6); (2) Aptoide
  Connect-konto + posedo-pruvo + alŝuto de la subskribita APK; (3) PR #144
  (la beta-branĉo) revizii/fermi. Vidu ankaŭ
  https://github.com/nordfalk/EsperantoRadio/pull/66

#### C51 — Funkcioj de la malnova apo ankoraŭ ne portitaj
- **Statuso**: [ ]
- **Fonto**: README.md "Farota (notoj)" (2026-10-24, movitaj ĉi tien; la referencaj
  ŝlosiloj el `malnova/app/src/main/res/values/eo_strings.xml`). Komplemento al C44.
- **Priskribo**:
  (a) konigi/kunhavigi elsendon (neniu partigo-intento en la nova apo —
  `PlatformLigilo` malfermas nur ligilojn/retpoŝtojn);
  (b) elektebla loko de elŝutitaj elsendoj / ekstera memoro, kaj averto pri libera
  spaco (`Placering_af_hentede_udsendelser`, `Det_lykkedes_ikke_at_hente_...`);
  (c) ~~konfirmo antaŭ forigo de elŝuto~~ — SOLVITA 2026-11-20 (dialogo en ElŝutitajEkrano + testo);
  (d) "Ĉu ĉesi ludi? / Daŭrigi fone" ĉe fermo de la apo;
  (e) nombro da novaj elsendoj ĉe Plej ŝatataj, kaj nombroj (elŝutoj) + sekva
  alarmtempo en la naviga tirkesto — la malnova tirkesto-skizo montris ilin;
  (f) "Nun estas ludata" por la rekta elsendo — `rektaElsendaPriskriboUrl`
  (muzaiko.info/data.json) estas legata sed ne montrata;
  (g) Pri / Kontakto-ekrano;
  (h) averto ke la vekhorloĝo ne povas garantii kontraŭ teknikaj problemoj.
  Jam faritaj (forigitaj el la listo): aŭtoludo ĉe malfermo (ekzistas), elŝut-forigo
  (ekzistas, sen konfirmo — vidu (c)).
- **Sugesto**: decidu intence kiuj indas; (c), (f) kaj (h) estas malmultekostaj;
  (e) postulas nombradon de novaj elsendoj (kun C47/C48 observado).

#### C52 — Ideoj kaj plibonigoj (konsideroj, ne deciditaj)
- **Statuso**: [ ]
- **Fonto**: README.md "Farota (notoj)" (2026-10-24, movitaj ĉi tien)
- **Priskribo**:
  (a) animacioj pli ĉieaj — nun nur la mini-ludilbreto havas; mankas ekz. ĉe
  ŝargado-indikoj, listo-ŝanĝoj (la nunaj ekran-transiroj venis en PR #116);
  (b) post aŭtomata ludado de kelkaj podkastaj elsendoj, aŭtomate transiri al la
  rektaj elsendoj (ideo);
  (c) transskribo de elsendoj kun tempokodoj, montrataj dum ludado — por lerni
  Esperanton (ideo; nature kunligi kun la estonta arkiv-servilo,
  `docs/nova/06_servilo_arkivo.md`).
- **Sugesto**: (c) povus esti la unua praktika uzanto de la arkiv-servilo.

#### C53 — Nova kanalo: HUMILOPINIE (ManchaPod) — kaj kontrolo de la ceteraj ManchaPod-fluoj
- **Statuso**: [ ]
  ripeta "Enigu min en la debato"-boilerplate kaj ora testo sur fiksaĵo; RESTAS: kontroli
  "Una Vaga Idea" kaj "Cuéntame, Hermosura" pri Esperanto-epizodoj)
- **Fonto**: ideo de la posedanto (2026-10-24); pod.ligiloj el https://allmylinks.com/manchapod
- **Priskribo**:
  - **"Ne Parolu pri Esperanto"** (spreaker-show **5202159**,
    `https://www.spreaker.com/show/5202159/episodes/feed`) **jam estas en la agordo**
    (kodo `neparolupriesperanto`) — kontrolita 2026-10-24.
  - **"HUMILOPINIE"** (spreaker-show **6684993**) mankas:
    `https://www.spreaker.com/show/6684993/episodes/feed`
    (pod.ligilo: `https://pod.link/aHR0cHM6Ly93d3cuc3ByZWFrZXIuY29tL3Nob3cvNjY4NDk5My9lcGlzb2Rlcy9mZWVk`).
  - La allmylinks-paĝo listigas pliajn ManchaPod-elsendojn ("Una Vaga Idea",
    "Cuéntame, Hermosura" — parte nacilingvaj): kontroli ĉu ili havas sufiĉe da
    Esperanto-epizodoj antaŭ ol aldoni.
  - Spreaker-antaŭvido ekzistas en la agordo (almenaŭ 2 kanaloj); G44 notas
    pasemajn spreaker-tempolimojn (KMP-12/14/15).
- **Sugesto**: sekvu la skill-on `kanalkonfiguro` (JSONC-kampoj, `puriguModeloj`-minacoj,
  parsreguloj P6.1+, golden-testo laŭ A3). Datumoj: emblemo kaj retpoŝto
  (`manchapod@gmail.com`) el la fluo/paĝo; `hejmpaĝoButono`
  `https://www.spreaker.com/podcast/humilopinie-manchapod--6684993`. Post aldono:
  `radioTxtKomparilo` kaj fluo-kontrollo (G44-stilo: ĉu HTTP 200 + RSS?).

  jam en la kanal-objekto (agordo, A5) — neniu nova datumvojo necesas.

#### C55 — Eldoni la komputilan (desktop) version
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24); AGENTS.md "Stato" jam listigas desktop-eldonom kiel estontan
- **Priskribo**: `desktopApp` ekzistas kaj konstruiĝas (Gradle 9/AGP 9, PR #117), sed
  estas nenie distribuita. Aparta tasko disde C50 (F-Droid/Aptoide = Android).
- **Sugesto**: decidi eldonformon (Linukso: AppImage/deb/flatpak; Vindozo: porta
  .zip/.exe; makinto?) kaj kanalon (GitHub-Releases estas la plej simpla komenco);
  unue traktu la konatajn limojn (C16: `saltiAl` estas ŝajnigo sur desktop-mp3spi;
  HLS ne funkcias — docs/nova/02) kaj provu sur sen-JDK-aparatoj (jlink/`jpackage`).

#### C58 — "White label"-ligilo por kanal-eldonantoj
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24)
- **Priskribo**: ebleco por kanal-eldonanto kreii specialan ligilon al la apo, kiu
  malfermas ĝin tiel, ke ĝuste tiu kanalo staras ĉe la frontpaĝo ("white label" —
  ekz. ManchaPod alvokas siajn aŭskultantojn al "sia" EsperantoRadio).
- **Sugesto**: Android App Links / intenco-ŝablono (ekz. `…/kanalo/<kodo>`) → nav3
  deep-link (hodiaŭ la apo ne havas deep links — C34) → enscenigu `Vojo.KanaloDetalo`
  kiel komenca ekrano (eble kun `Vojo.Hejmo` sube en la stako, por ke reen-gesto
  kondutu nature); por la retversio (https://javabog.dk/esperanto-radio/apo/, ekde la solvo de C56) sufiĉas URL-parametro. Aldonu ankaŭ al la
  Play-Stor-listo ligilojn (`assetlinks.json`-kontrolo) kaj memoru: la kanalo devas
  ekzisti en la agordo (A5).

#### C59 — Transsalti ripetiĝan enkondukon de kanalo (kun detekta-ilo)
- **Statuso**: [ ]
  — listo de { "sekundoj": X, "ĝis": "YYYY-MM-DD" } (frakcieblaj valoroj; fina dato
  inkluziva; la lasta ero sen „ĝis" validas ankaŭ por estontaj elsendoj; unuopa
  valoro = unu ero sen „ĝis"; malplena listo = ne transsalti), efikas nur se la
  uzanto jam aŭskultis ion de la sama kanalo dum la pasinta semajno
  (TRANSSALTA_SEMAJNO_MS = 7 tagoj — 2026-10-30 decido de la posedanto, antaŭe
  monato); komenco = max(savitaPozicio, transsalto).
  Aldone (PR #144): snackbar „Transsaltis la enkondukon" kun klariga dialogo;
  agordoj „Transsalti enkondukon" (ĉefa ŝaltilo) kaj „Sciigo pri transsaltita
  enkondukon" („Bone" en la dialogo malŝaltas ĝin).
  RESTAS: (b) plena aŭtomatiĕo (la skripto jam ekzistas kiel la skill-o
  `enkonduk-detekto` kun `detekto.py`). Plena detekto farita 2026-10-29 (21 kanaloj
  analizitaj, 5 plej freŝaj elsendoj po kanalo; 6 preterlasitaj: muzaiko rekta-kanalo,
  radioscienca/spiritismavocxo/vinilkosmo/peranto/kaliningrada mortaj fluoj — G44):
  kernpunkto = 0 laŭ decido de la posedanto (la komunaĵo 8-25 s estas antaŭata de
  varianta antaŝanonco, kiun oni ne transsaltu),
  **agorditaj valoroj**:
  babibejo 50.0 (0-50 s identaj en ĉiuj 10 paroj), fremdulo 11.0, iro 21.0,
  lemondediplomatique 11.0 (ĉiuj kun 10/10 koheraj paroj), universeala-lindo 39.0
  kaj polaretradio 22.0 (nur 2 elsendoj haveblaj — malforta pruvo, rekontrolota).
  **3zzzenesperanto** (2026-10-30, epok-konscia detekto de 95 elsendoj 2025-01…2026-10;
  la unua 5-plej-freŝaj-rulo malsukcesis ĉar ĝi transiris epok-limon):
  2025 = 27.0; ekde 2026-01-05 = 26.0; ekde 2026-07-20 = 37.0 (antaŭrolo ~10-14 s +
  nova enkonduko); sen-enkondukaj unuopaj elsendoj = 0.0 (2025-01-13, 2025-06-09,
  2026-05-04…25, 2026-08-31, 2026-10-12 — perdo konfirmita, revenis je 10-19;
  decidoj de la posedanto: plena formul-a komenco, kaj 37 s por la antaŭrola epoko).
  **Reviziite 2026-11-24** (30 plej novaj elsendoj po kanalo, fenestro 70 s — la
  norma detekto.py vidas nur 30 s kaj kapablus nek konfirmi nek kontraŭpruvi
  valorojn >30): 3zzz 37→21 ekde 2026-05-26 (la 37 tranĉis ĝis ~11 s en la enhavo
  de la septembraj elsendoj — iliaj paroj finas ĉe 22–26 s); 2026-05-04…25 ne estas
  „sen enkonduko“ sed kvar epizodoj kun inter si egala 62-s enkonduko (62.0).
  babibejo ricevis epokojn (2021-05-30 = 0; ĝis 2022-06-15 = 38; 2022-10-17 = 6;
  ekde 2022-10-25 = 50). fremdulo: la epizodoj ĝis 2021-04-20 ne havas la
  enkondukon (0 ĝis tiam, 11 poste). iro: 21 konfirmita por 26/30 plej novaj;
  sen-enkondukaj = 2026-01-08, 2026-05-06 kaj la lasta 2026-06-18 (kanalo dormas;
  fina valoro 0). Konfirmitaj sen ŝanĝo: polaretradio 22 (nur 3 elŝuteblaj sonoj),
  universeala-lindo 39, lemondediplomatique 11 (tutflue konstanta).
  **Rifuzitaj**: movada-vidpunkto (dispersaj 19-35 s mini-segmentoj — nerekta
  enkonduko), radiovatikana (bimodala 4/15 s laŭ epizodo — ne laŭ dato),
  tuluzo (dispersaj 10-22 s), sano (posta epoko nur 7-8 s kaj kanalo dormas),
  lamalfamuloj/rakontatempo/bitmono (malnova epoko kun nur 6-8-s komunaĵo — ne
  indas), radiofmkubo (tute dispersaj 4-66 s), kernpunkto (komunaĵo estas — sed
  ĝi validas nur post varia antaŭanonco, restas la decido de 2026-10-29 ĝis 0),
  la ceteraj sen iu ajn komuna segmento.
- **Fonto**: ideo de la posedanto (2026-10-24)
- **Priskribo**: kelkaj kanaloj ĉiam sonas ekzakte same dum la komenco — teda post
  kelkaj fojoj. (a) Enskribi en la kanalaj informoj kiom da sekundoj oni povas
  transsalti en la komenco. (b) Eble eĉ krei ilon kiu trovas tion: komparas la
  komencajn sekundojn de pluraj epizodoj kaj elkalkulas la ripetan longon.
- **Sugesto**: (a) nova kampo en `esperantoradio_konfiguro.json` (ekz.
  `saltiKomenceSekundojn` — daten-movita, A5; kontroli la JSONC-kampanimadon kaj la
  parsajn regulojn P6.1+, ankaŭ la agord-kompateblecon kiam la kampo mankas);
  la ludilo jam subtenas komencan pozicion — `LudiloRegilo.fiksiFonton(fonto,
  komencoPozicioMs)` — do `LudvicoRegilo.ludiElsendon` transdonu
  `max(komencoPozicioMs, savitaPozicio)` kaj la pozicio-indikilon ne montru la
  transsaltitan parton (aŭ montru 0:xx de la komenco de la enhavo). (b) la detekta
  ilo komparu la komencajn ~60 s (RMS/ampleks-envelopa korelacio aŭ audio-fingerprint);
  natura loko: la estonta arkiv-servilo (docs/nova/06), kiu jam elŝutos la sonojn —
  ĝi povus proponi la valoron por ĉiu kanalo.

#### C60 — Regula (ĉiuhora/ĉiutaga) rulado de la servilaj taskoj — neniu cron ekzistas
- **Statuso**: [ ]
- **Fonto**: esplorado 2026-11-24 (C56): `crontab` je ambaŭ maŝinoj kaj `systemctl list-timers` estis kontrolitaj — NENIU temp-planita rulado ekzistas; ĉio ĝis nun rulis permane. La establon faros la posedanto mem.
- **Priskribo**: nenio aŭtomate refreŝigas la arkivon. La perioda rulado devas kovri almenaŭ:
  - **`:server:arkivKonstruilo`** — RSS-fluoj kaj la publikigota kanalkonfiguro; de PR #210 ĝi ankaŭ elŝutas novajn emblemojn/bildetojn al `publika/bildoj/`. Ekz. ĉiuhore aŭ ĉiutage.
  - **AI-tekstoj** — la STT-pipelino (`server/data/privata/skriptoj/stt_chiujn.py` → `privata/transkriptoj/`) kaj la LLM-prilaboro al pretaj rezultoj (`privata/rezumoj/`). Kompute peza: ekz. ĉiunokte aŭ semajne, kun la limigoj `N_POR_KANALO` kaj `MAKS_DAURO_MIN`.
  - **`rsync`-publikigo** — decidu, ĉu ĝi iĝu parto de la aŭtomato aŭ restu permana. Aktuala regulo: „neniam publikigu al la servilo sen eksplicita peto de la uzanto" — aŭtomata publikigo do bezonas apartan, eksplicitan aprobon de la posedanto (kun `--chmod=D755,F644` kaj `-L` por la ligiloj; vidu la skill-on `servilo-arkivo`).
- **Sugesto**: unu `crontab`-linio sur la konstanta maŝino — tiu kun la kanona datum-arbo (`/home/j/Code/EsperantoRadio/server/data`) — ekz. `5 * * * *` (ĉiuhora) aŭ `17 4 * * *` (ĉiutage), kun protokolo al `tmp/`. Post-vidigo: `stato.json` (`lastaRulo`) kaj https://javabog.dk/esperanto-radio/kanaloj/ estas freŝaj; unu mortinta kanal-fluo ne haltigu la aliajn (A4).

---

> **Pri la finitaj trovindikoj**: ekde 2026-11-20 ili ne plu listiĝas ĉi tie — vidu
> `git log` de FAROTA.md (la etikedoj K…, G…, C… restas spureblaj), la PR-ojn,
> `CHANGELOG.md`, [`ŜANĜOJ.md`](ŜANĜOJ.md) kaj [`NOTOJ.md`](NOTOJ.md).
