# FAROTA.md — Malfermitaj trovoj por posta riparado

> **Fonto**: tutplena kodrevizio de 2026-10-08, farita per 8 subagentoj (tavoloj: kerno, ludilo, UI/navigado, Android, testoj+build+CI, sekureco, historia `malnova/`, delta `b92f739..c92766a`).
> **Pliigo 2026-10-19**: K1, G42–G45 kaj C46–C48 aldoniĝis el la Sentry-analizo (organizo `esperantoradio`, projekto `kmp` — uzoj, landoj, eraroj kaj kanaloj en la semajno 10-12 → 10-19; per la MCP-servilo `mcp.sentry.dev`, vidu la skill `sentry-esplorado`).
> **Bazo**: `b92f739` + delto ĝis `c92766a` (la loka `master` estis ĝisdatigita per `git pull --ff-only` dum la revizio; liniaj referencoj estas al `c92766a`).
> **Kiel uzi**: la punktoj estas ordigitaj laŭ graveco (KRITIKA → GRAVA → KONSILO). Marku la status-keston `[x]`, kiam finite. Ne forigu punkton sen riparo — anstataŭe movu ĝin al "Jam farita" sube.
> **Rilato al la aliaj dosieroj** (ekde 2026-10-11): la DAŬRE VALIDA scio el la jam finitaj ([x]) trovindikoj staras en [`NOTOJ.md`](NOTOJ.md) (permanente notinde, laŭ temo); la historio de la riparoj (problemo→kaŭzo→testo) en [`ŜANĜOJ.md`](ŜANĜOJ.md) (enskribo 13). Ĉi tiu dosiero restas nur por la MALFERMAJ punktoj.

## KRITIKA

#### K1 — KMP-1V: RemoteServiceException — `startForegroundService()` sen `startForeground()` en `EsperantoLudadoServo` (kraŝo sur dist 249)
- **Statuso**: [x]
- **Riparo** (2026-10-19, branĉo `1019-ludadoservo-defenda-startforeground`): defendam `startForeground()` en `EsperantoLudadoServo.onStartCommand` — post 4 s, se la ludilo restas malplena (restarto post procezo-morto, ekstera starto; esploro de la media3-1.11.1-fonto montris, ke `MediaController` nur BINDAS, do nur la restarta/ekstera vojo povas lasi la kontrakton malfermita), la servo mem plenumas la kontrakton per provizora `IMPORTANCE_MIN`-sciigo kaj tiam `stopForeground()` + `stopSelf()`. Ludilo kun media-eroj restas sub Media3 (kontrakto plenumita je ekbufrado). La instrumentita testo ne estas inkluzivita (la kraŝo estas tempo-bazita sistema konduto, ne reprenebla per emulilo; la empiria kontrolilo estas la observado de KMP-1V en Sentry post la venonta eldono — vidu C48).
- **Fonto**: Sentry-analizo 2026-10-19 (issue [KMP-1V](https://esperantoradio.sentry.io/issues/KMP-1V), unua fojo 2026-10-18 16:47 UTC; nivelo fatal, nepritraktita; Android 11, Redmi Note 9 Pro, IT; `FOREGROUND_SERVICE_MEDIA_PLAYBACK: not_granted`)
- **Loko**: `androidApp/.../EsperantoLudadoServo.kt` (Media3 `MediaSessionService`); la start-vojoj per `SessionToken`/`MediaController`: `ExoPlayerLudiloRegilo.kt:77`, `LudiElsendoReceivilo.kt:78` (sciigo "ludi elsendon"), `AlarmoReceivilo.kt:161,313` (alarmo)
- **Priskribo**: La servo startiĝas per `startForegroundService()` el fona vojo (sciigo/alarmo/aŭtoludo). Media3 vokas `startForeground()` nur kiam seanco kun ludanta legilo iĝas aktiva; se la fonto fiaskas (ĝuste la `ExoPlaybackException`-ersetego, KMP-H) aŭ la konekto malfruas, neniu `startForeground()` okazas kaj Android mortigas la apoton per `RemoteServiceException`. Unu uzanto, unu okazigo — sed sur la aktuala produktada build (249) kaj en la vojo, kiun la reverkata alarma PR (revertita `10e76e6`) tuŝos.
- **Sugesto**: (a) en `EsperantoLudadoServo.onStartCommand`: se la servo startiĝis per `startForegroundService` kaj neniu seanco aktiviĝis ene de la limtempo, voku `startForeground()` per provizora sciigo — aŭ `stopSelf()` se neniu luda intenco restas; (b) en la regiloj (`ExoPlayerLudiloRegilo`, `LudiElsendoReceivilo`) traktu font-erarojn kaj ĉesu la kontrakton grace; (c) testu la sciigan kaj alarman vojon sur Android 11–16 (emulilo + vera aparato), precipe la alarman PR antaŭ ol merĝi; (d) post eldono observu Sentry: KMP-1V devas malaperi.
- **Testo**: instrumentita testo: starti la servon el fono (sciigo-klako) kun rompita fonto-URL → neniu kraŝo, la servo haltiĝas mem.

## GRAVA

#### G42 — KMP-X: `ForgottenCoroutineScopeException: rememberCoroutineScope left the composition` (212 okazoj / 58 uzantoj ekde 2026-09-13)
- **Statuso**: [ ]
- **Fonto**: Sentry-analizo 2026-10-19 (issue [KMP-X](https://esperantoradio.sentry.io/issues/KMP-X); 32 okazoj / 10 uzantoj en la lasta semajno, lasta 10-16)
- **Loko**: `rememberCoroutineScope` en `App.kt:99`, `SerĉoEkrano.kt:34`, `PlejŝatatajEkrano.kt:37`, `HejmoEkrano.kt`, `ElŝutitajEkrano.kt:39`, `DiagnozoEkrano.kt:37`, `ElsendoEkrano.kt:73`, `KanalEkrano.kt:108`
- **Priskribo**: Korutino lanĉita el `rememberCoroutineScope` postvivas la komponaĵon — tipa kaŭzo: lanĉo en klik-lambdo, kies rezulto estas uzata post navigado foren.
- **Sugesto**: inspekti ĉiun uzon; laboro de la ekrano → `LaunchedEffect` aŭ nuligo en `DisposableEffect`; laboro transvivanta navigadon → `viewModelScope` (vidu la komenton en `HejmoEkrano.kt:224`); neniam teni la skopecon en longdaŭra objekto.

#### G43 — KMP-1S: `IllegalArgumentException: La respondo ne estas RSS/Atom-fluo` el kaŝita fluo (substatuso: escalating)
- **Statuso**: [x]
- **Solvo** (2026-10-19): jam plene riparita antaŭ la Sentry-analizo — `leguKaŝitajnElsendojn`
  kaptas la ĵeton de `kontroluFluecon`, protokolas per `logw`, FORIGAS la koruptitan
  kaŝon kaj redonas null (PR #89/#109); `kontroluFluecon` vokiĝas ankaŭ ANTAŬ
  `skribuKaŝon` (fiaskpaĝo kun HTTP 200 ne estas kaŝata). La Sentry-okazoj (dist 249,
  lasta 10-14) kongruas kun la 249-eldondato kaj ĉesis post ĝi. Testoj:
  `malpuraKaŝoEstasForigata`, `http404NeAnstataŭigasKaŝon`,
  `fiaskpaĝoKunHttp200NeAnstataŭigasKaŝon` (ElsendoDeponejoImplTest).
- **Fonto**: Sentry-analizo 2026-10-19 (issue [KMP-1S](https://esperantoradio.sentry.io/issues/KMP-1S); 15 okazoj / 4 uzantoj, 10-13 → 10-14; staktraco: `HejmoEkrano → HejmoViewModel.ŝargi → ElsendoDeponejoImpl.leguKaŝitajnElsendojn`)
- **Loko**: `ElsendoDeponejoImpl.kt:204` (`kontroluFluecon`), vokita ĉe `:149` ( legado de la kaŝo) kaj `:109` (antaŭ kaŝado)
- **Priskribo**: Kaŝita respondo (verŝajne HTML-erarpaĝo aŭ koruptita dosiero) estas re-validata kiel RSS kaj ĵetas — unu korupta kaŝero rompas la ŝaradon de la koncerna kanalo por la uzanto.
- **Sugesto**: (a) dum legado de la kaŝo `kontroluFluecon` ne ĵetu — `logw` + traktu kiel malplenan kaj forigu la koruptitan kaŝeron; (b) validu la respondo (content-type / unuaj signoj `<`) **antaŭ** ol skribi en la kaŝon; (c) ampleksu la teston `ElsendoDeponejoImplTest.kt:95` per "korupta kaŝo ne rompas la legadon".

#### G44 — KMP-N k.a.: daŭraj RSS-malsukcesoj de specifaj kanaloj (1227 okazoj / 57 uzantoj ekde 2026-09-13)
- **Statuso**: [ ]
- **Fonto**: Sentry-analizo 2026-10-19 (issue [KMP-N](https://esperantoradio.sentry.io/issues/KMP-N) kaj parencaj; en la lasta semajno: spiritismavocxo, peranto, bitmono, radiovatikana, sano — ofte "uzas 0 kaŝenitajn elsendojn"; pli frue ankaŭ lamalfamuloj, babibejo, universeala-lindo, varsoviavento, kaliningrada, radioscienca, kernpunkto, vinilkosmo, movada-vidpunkto, polaretradio)
- **Loko**: `esperantoradio_kanaloj_v9.json` (fluo-URL-oj), `ElsendoDeponejoImpl.kt` (ŝargo), `NovajElsendojKontroloWorker.kt:119-121`
- **Priskribo**: Parto estas pasemaj ret-problemoj (timeout/DNS ĉe spreaker, anchor.fm, blogspot — KMP-12/14/15/M/Q), sed la ripetiĝo ĉe la samaj kanaloj sugestas mortajn aŭ ŝanĝitajn fluojn. `radiovatikana` aperas ankaŭ en lud-eraroj (`UnknownHostException` por `media.vaticannews.va`) — eble la tuta gastigo estas morta.
- **Sugesto**: kontroli ĉiun suspektatan fluon (curl: ĉu HTTP 200 + RSS?), ĝisdatigi la URL-on aŭ marki la kanalon neaktiva en la agordo (skill `kanalkonfiguro`); konsideri pli longan timeout/reprovon por malrapidaj gastigoj.

#### G45 — KMP-1H/1R: `OutOfMemoryError` — tuta respondo legata en la memoron (`readByteArray`)
- **Statuso**: [x]
- **Riparo** (2026-10-20, branĉo `1019-g43-g45-kasxero-sanigado`): nova komuna helpilo
  `KorpaLegilo.kt` — `HttpResponse.leguKorponLimigite()` (maks. 1 MB,
  `MAKSIMUMA_FLUA_GRANDECO_BAJTOJ`): (1) ContentLength-antaŭkontrolo ĵetas tuj sen
  legi; (2) `bodyAsChannel()` + peceta legado devigas la limon ankaŭ sen/kun mensoganta
  Content-Length. Anstataŭigis `bodyAsText()` en `ElsendoDeponejoImpl.ŝargiElsendojn`
  kaj `NovajElsendojKontroloWorker`. La kaŝo-resta konduto kovrita per
  `troGrandaRespondoNeAnstataŭigasKaŝon`; la helpilo mem per
  `leguKorponLimigiteSekvasLaLimon` kaj `leguKorponLimigiteFlueTranslimasSenContentLength`.
  La `arhivaPeto` (archive.org-metadatenoj, ~1-2 KB) restas kun `bodyAsText`.
- **Fonto**: Sentry-analizo 2026-10-19 (issues [KMP-1H](https://esperantoradio.sentry.io/issues/KMP-1H), [KMP-1R](https://esperantoradio.sentry.io/issues/KMP-1R); 10-08 → 10-13; provoledo de 60 MB; malaltklasaj aparatoj, 2–3 uzantoj)
- **Loko**: `ElsendoDeponejoImpl.kt:62,108` (`bodyAsText()` — ktor interne `SavedCallKt.save → kotlinx.io readByteArray`), `NovajElsendojKontroloWorker.kt:119-121`
- **Priskribo**: La tuta korpo de respondo estas enmemorigata; ekstreme granda aŭ fuŝita respondo (misdirektita elŝuto anstataŭ fluo) mortigas la apoton sur malfortaj aparatoj.
- **Sugesto**: limigi la legotan grandecon (maks. ~2–5 MB por fluoj — `bodyAsChannel` + limita legado anstataŭ `bodyAsText`), kaj kaŝi nur respondojn, kiuj aspektas kiel RSS (ligu kun G43(b)).

#### G46 — `logw` estas nesimetria: 2-arg sendas averton al Sentry (Logs), 3-arg sendas NENION — memevidenta konfuzo-fonto
- **Statuso**: [x]
- **Riparo** (2026-10-19, branĉo `1019-protokolo-logw-al-sentry`): 3-arg `logw` sendas la escepto-resumon kaj la staktracon kiel `Sentry.logger.warn` (du linioj) + KDoc; AGENTS.md kaj `Protokolo.kt`-KDoc korektitaj. Minimala diferenca ŝanĝo, sen nova test-infrastrukturo (laŭ deziro de la uzanto).
- **Fonto**: Sentry-analizo 2026-10-19 (post-raporta diskuto; la ŝajna "eraro-kolapso" post 10-14 parte kaŭzita de tio)
- **Loko**: `ProtokoloUtil.kt:54-58` (`logw(tag, msg)` → `Sentry.logger.warn`; `logw(tag, msg, e)` → nur `platformLogw`, neniu Sentry-voko; `platformLogw` = nur `android.util.Log.w`); dokumentoj: AGENTS.md ("logw(tag, msg, e) — ĝi restas LOKA" — vera nur por la 3-arg formo, sed tro ĝeneraligebla), la KDoc super `loge(tag,msg,e)` ("Eraroj estas ankaŭ kaptitaj de Sentry…") staras rekte super `logw(tag,msg,e)` sen propra KDoc, kaj `Protokolo.kt` (commonMain) asertas "kun Sentry-breadcrumb-oj" — sed `platformLog*` aldonas neniun breadcrumb.
- **Priskribo**: Du samnomaj troŝarĝoj kondutas malsame pri malproksima protokolado, nevideble ĉe la vok-lokoj (22 vokoj 2-arg, 44 vokoj 3-arg — la silenta formo estas la pli ofta). Sekvoj: (a) la avertoj de la 3-arg formo (RSS-malsukcesa, lud-eraroj, Worker-eraroj) estas tute nevideblaj en Sentry ekde 247/249 — oni kredis, ke la eraro-taso "kolapsis", dum ĝi nur fariĝis nevidebla; (b) AGENTS.md kaj la kodo kontraŭdiras (por la 2-arg formo: `Sentry.logger.warn` ja sendas); (c) "sendata al Sentry" estas plursenca — Sentry Logs kontraŭ Sentry Errors (avertoj neniam aperas kiel issue), do laŭ la kontrolata UI oni konkludas aŭ "sendata" aŭ "ne sendata".
- **Sugesto**: (1) 3-arg `logw` sendu `Sentry.logger.warn("[$tag] $msg: ${e::class.simpleName}: ${e.message}")` — unuforma regulo: "logw = averto en Sentry Logs, ĉiam; neniam error-evento; neniam eraro-kvoto"; (2) ĉio, krom logd iru al Sentry (kun la escepto ke la uzanto povas malŝalti ke logi iras al Sentry en la agordoj); (3) KDoc pri ĉiu troŝargo kun la celo-matriks (logcat / Sentry Logs nivelo / Sentry errors / kvoto), inkluzive de induko "ne sendata" aŭ "sendata al Sentry"; (4) korektu AGENTS.md kaj la Protokolo.kt-KDoc; (5) kontrakt-testo: malgranda interna Sentry-"sink" (anstataŭigebla per test-fako), kiu asertas en commonTest: logw 2-arg → warn, logw 3-arg → warn (post-riparo), loge 3-arg → captureException, logd → nenio malproksima.

## KONSILO

#### C5 — Subskrib-konfiguro: defaŭlta absoluta keystore-vojo en publika deponejo
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:47` (`storeFile = file(System.getenv("KEYSTORE_PATH") ?: "/home/j/android/A_signaturer/jacobnordfalk.keystore")`, pasvortoj defaultas al `""`)
- **Sugesto**: forigu la hardkoditan vojon; ĵetu eraron se `KEYSTORE_*` mankas ĉe eldon-konstruo (fail-fast). (La CI `eldonado.yml` faras tion ĝuste: GitHub-secrets → env.)

#### C6 — R8/ProGuard: nenio aktiva (neniu keep-regula risko hodiaŭ)
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:55` (`isMinifyEnabled = false`; neniu `proguard-rules.pro`; neniu `consumerProguardFiles` en `shared/build.gradle.kts`; APK ~21 MB)
- **Sugesto**: kiam vi ŝaltos R8: konsumaj reguloj por Media3 session/exoplayer kaj Sentry (memkonfiguriĝas), atentu `SonfontoKodilo`/MediaMetadata-klasojn; re-testu la sciigojn kaj la fonon.


#### C8 — `striptiguKomentojn` ne subtenas `/* */`; `coerceInputValues` kaŝas agord-erarojn
- **Statuso**: [ ]
- **Loko**: `KanalAgordoLeganto.kt:37-72` (nur `//` kaj `\`-daŭrigoj), `:22` (`coerceInputValues = true`)
- **Sugesto**: dokumentu la limigon (aŭ subtenu blokkomentojn); pripensu forigi `coerceInputValues` por novaj kampoj.

#### C9 — Gradle-higieno: dependency verification, Dependabot, jcenter
- **Statuso**: [ ]
- **Loko**: radika `build.gradle.kts` (`jcenter()` en `allprojects` — nur por malnovaj dependoj de `malnova/`); neniu `gradle/verification-metadata.xml`; neniu `dependabot.yml`
- **Sugesto**: aldonu Gradle-dependency-verification; enŝaltu Dependabot kaj OWASP `dependencyCheck` en CI; limigu `jcenter()` al la malnova projekto aŭ forigu ĝin iam.

#### C11 — `SonfontoKodilo` enhavas la tutan `Elsendo` en `MediaMetadata.extras`
- **Statuso**: [ ]
- **Loko**: `shared/src/commonMain/.../domain/player/SonfontoKodilo.kt:23`; uzata en `ExoPlayerLudiloRegilo.kt:113-135` kaj `LudiElsendoReceivilo.kt:56-58`
- **Priskribo**: La JSON trairas Binder (regilo→servo) kaj loĝas en la sesio; riĉaj HTML-priskriboj (ekz. Varsovia Vento kun enkorpigitaj `<audio>`) povas esti dekoj da KB — risko de `TransactionTooLargeException` kaj malnecesa ŝarĝo.
- **Sugesto**: serializu nur `id, kanaloSlug, titolo, fluo, dato, bildoUrl` kaj rekonstruu la `Elsendo`-on per `ElsendoDeponejo.getElsendo(id)`.

#### C15 — wasmJs: malnovaj evento-aŭskultantoj de anstataŭigita elemento
- **Statuso**: [ ]
- **Loko**: `WasmJsLudiloRegilo.kt:33-38`
- **Priskribo**: `fiksiFonton` paŭzigas la malnovan elementon kaj kreas novan; la `pause`-evento de la malnova alvenas nesinkrone kaj ĝia aŭskultanto (`_stato.value.copy(stato = Haltita)`) povas superskribi la ĵus metitan `Konektas`.
- **Sugesto**: forigu la aŭskultantojn (`removeEventListener`) aŭ uzu generacian kontrolon en ĉiu lambda.

#### C16 — Desktop: `rawStream` likas ĉe eraro; `saltiAl` estas ŝajnigo
- **Statuso**: [ ]
- **Loko**: `DesktopLudiloRegilo.kt:90-93` (se `AudioSystem.getAudioInputStream(rawStream)` ĵetas, `rawStream` neniam fermiĝas); `:273-275` (`saltiAl` nur metas la montratan pozicion — la UI gardas per `subtenasSaltadon = false`, do nuntempe bone)
- **Sugesto**: fermu `rawStream` en `catch`; klarigu en la KDoc de `saltiAl`, ke ĝi estas ŝajnigo.

#### C21 — `BootReceivilo`: `LOCKED_BOOT_COMPLETED` estas morta kodo
- **Statuso**: [ ]
- **Loko**: `BootReceivilo.kt:33-35`
- **Sugesto**: forigi la agon (la apo ne estas `directBootAware`) aŭ uzi `createDeviceProtectedStorageContext`.

#### C22 — `MainActivity.scope` neniam nuligeblas kaj postvivas la Activity
- **Statuso**: [ ]
- **Loko**: `androidApp/.../MainActivity.kt:43`
- **Priskribo**: La alarm-korutino (`delay(10_000)` + reto) retenas la detruitan Activity ĝis ~10+ sekundojn post `onDestroy`. Por la alarmo tio estas intenca (la servo transprenas), sed konsilindas apliki `lifecycleScope` por mallongaj UI-korutinoj kaj teni la alarman laboron aparte (kun komento).

#### C23 — `ui-test-junit4:1.7.3` hokita kaj multe pli malnova ol Compose 1.10
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:83` (`androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.3")`)
- **Sugesto**: vicigu al la konvena Jetpack Compose-versio (aŭ BOM / `compose.uiTestJUnit4` de CMP).

#### C24 — `defaultToAppBundles` komento kontraŭdiras la CI
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:76` (komento "Nur eldoni AAB (App Bundle), ne APK" — sed `eldonado.yml` uzas `publishReleaseApk`, ĉar la app ne estas en Play App Signing)
- **Sugesto**: korektu la komenton aŭ forigu `defaultToAppBundles.set(true)`.

#### C25 — `EkranfotoTesto`: 11 "testoj" sen asertoj
- **Statuso**: [ ]
- **Loko**: `shared/src/desktopTest/.../EkranfotoTesto.kt`
- **Sugesto**: akceptebla kiel fumtestoj; konsideru almenaŭ minimuman kontrolon (dosier-grandeco > sojlo, aŭ ke la radiko havas infan-nodojn).

#### C26 — `HejmoEkranoTest` uzas realtempan daton
- **Statuso**: [ ]
- **Loko**: `shared/src/commonTest/.../ui/HejmoEkranoTest.kt:40` (`Clock.System.todayIn(TimeZone.UTC)`)
- **Sugesto**: puraj testoj kun fiksa dato estus pli determinismaj (la limoj estas precize testitaj en `HejmoLogikoTest` kun fiksa dato).

#### C27 — Eraro-klasifiko: kerna decido al commonMain
- **Statuso**: [ ]
- **Loko**: `androidApp/.../EraroKlasifiko.kt:14-37` (bone testita, sed nur kiel instrumentita testo, kiu ne ruliĝas en CI)
- **Sugesto**: eligu la puran decidon (ekz. `klasifikuHttpStatuson(status: Int)`) al commonMain + unuo-testo; aŭ aldonu androidTest al CI per emulilo.

#### C28 — Neuzataj katalog-eroj
- **Statuso**: [ ]
- **Loko**: `gradle/libs.versions.toml:45,62,78` (`ktor-client-cache`, `multiplatform-settings-android`, `turbine` — neniu referenco en la build-dosieroj)
- **Sugesto**: forigu (aŭ uzu).

#### C29 — `resolutionStrategy.force("androidx.savedstate:savedstate-ktx:1.3.1")` — observi
- **Statuso**: [ ]
- **Loko**: `shared/build.gradle.kts:165-168` (workaround bone dokumentita: navigation3 1.1.1 postulas savedstate 1.4.0, sed savedstate-ktx 1.4.0 ne ekzistas)
- **Sugesto**: observu ĉe estontaj navigation3/CMP-ĝisdatigoj (kaj forigu kune kun la klib-ABI-limigo).

#### C30 — `LudvicoLogiko`: dokumenti kaŝitan dependecon
- **Statuso**: [ ]
- **Loko**: `LudvicoLogiko.kt` (`trovSekvanSamkanalan` saltas nur finitajn; eraraj estas saltataj ĉar `markiErara` metas `finita = true, erara = true` — `PersonigoDeponejojImpl.kt:89-100`)
- **Sugesto**: mencii en la KDoc de `trovSekvanSamkanalan` ke eraraj estas markitaj kiel finittaj de la deponejo.

#### C32 — Tuŝ-celoj sub 48 dp; `contentDescription = null`
- **Statuso**: [ ]
- **Loko**: `HejmoEkrano.kt:618-658` (tripunkta menuo 28.dp, ludo-butono 32.dp); `ElŝutitajEkrano.kt:140-148` (`IconButton` 36.dp); `App.kt` (Menuo-butono, `IconButton` sen `minimumTouchTargetSize`); `ElsendoEkrano.kt:332-411` (buton-ikonoj kun `contentDescription = null`)
- **Sugesto**: `Modifier.minimumTouchTargetSize()` aŭ pligrandigo; aldonu `contentDescription` al la ikonoj.

#### C34 — iOS: `PlatformLigilo` estas TODO; neniuj deep links
- **Statuso**: [ ]
- **Loko**: `shared/src/iosMain/.../ui/PlatformLigilo.ios.kt` (`malfermuLigon`/`malfermuRetpoŝton` malplenaj); neniu `ACTION_VIEW` intent-filter
- **Sugesto**: konata limito (iOS venos); se iam bezonataj, aldonu deep links kaj traktu la URL-on en `MainActivity.traktuIntenton`.

#### C38 — Morta kodo
- **Statuso**: [ ]
- **Loko**: `PersonigoDeponejojImpl.kt:36,48` (`LastAŭskultitajDeponejo.pozicioj` neniam plenigita — `getPozicio` ĉiam redonas `null`); `NoOpElŝutDeponejo.kt:18` (nova `MutableStateFlow` je ĉiu voko de `observiElŝutStaton`); `Diagnozo.kt:32` (`DiagnozoTipo.RETO` neniam konstruita; `Severeco.INFO` neniam produktata — la filtrilo en `HejmoEkrano.kt:332` estas senefika); `ElsendoDeponejoImpl.kt:38-46` (`getElsendojn` kaŝo-nur, sed la interfaco sugestas alie — dokumentu aŭ renomu)
- **Sugesto**: forigu aŭ dokumentu; direktu la poziciojn al `LudatojDeponejo`.

#### C39 — `AppStato`: duobla ŝargo de la sama kanalo (neniu in-flight dedup)
- **Statuso**: [ ]
- **Loko**: `ElsendoDeponejoImpl.kt` (`ŝargiElsendojn` — du samtempaj vokoj, ekz. pull-to-refreŝ + aŭtomata ŝargo, faras duoblan elŝuton)
- **Sugesto**: Mutex/Deferred per kanalo.

#### C40 — `gradle.properties`: `org.gradle.jvmargs=-Xmx1536m` malalta por KMP
- **Statuso**: [ ]
- **Loko**: `gradle.properties:9` (la wasmJs-konstruo bezonas `-Pkotlin.daemon.jvmargs=-Xmx4g`, dokumentita en AGENTS.md)
- **Sugesto**: pliigu la defaŭltan valoron.

#### C41 — AGENTS.md kaj docs malfruas post la kodo
- **Statuso**: [ ]
- **Loko**: AGENTS.md "Kio funkcias nun" kaj la dosieruja arbo ne mencias la diagnozan ekranon, `DiagnozoRegilo`/`DiagnozoEkrano`/`Diagnozo.kt`, nek la fonan aŭtomatan daŭrigon; `docs/nova/03_domajno_kaj_datumoj.md` kaj `docs/nova/05_dizajno_kaj_ui.md` ricevis nur tajpo-riparojn; `docs/nova/04` priskribas neekzistantajn `puriguModeloj`/`iframeReguloj` kaj malprecizan id-konvencion (vidu G15, G16)
- **Sugesto**: 2-3 linioj en "Kio funkcias nun" + 3 linioj en la arbo; mallonga alineo en `docs/nova/03` pri `DiagnozoRegilo` (expect/actual); vicigu `docs/nova/04` kaj la kodon (G15/G16).

#### C43 — Semantiko de `FilCache` kaj scio de `RssArkivServer` por la estonta servilo
- **Statuso**: [ ]
- **Priskribo**: (a) `ændrerSigIkke=true` = "kaŝita → neniam refreŝi" — tio igas `RssArkivServer-filcache/` la determinismajn golden-fixture-ojn; la nova plano diras nur "reuzo de Ktor-HttpCache + RssArkivServer-filcache" sen precizigi tiun semantikon. (b) La malnova `RomeFeedWriter` skribis `enclosure length` el la loka kaŝo kaj **per-jarajn arkivdosierojn** `feed-<slug>-<yyyy>.xml` — la nova plano tion ne mencias. (c) La plan-snippet vokas `parsilo.parsRss(fluo, kanal)` **sen** `httpKliento` — por Peranto tio signifas divenitajn, ofte malĝustajn, archive.org-URL-ojn en la arkivo — la servilo devas transdoni klienton kaj persistentan `ArchiveOrgDosiernomoKaŝo` (vidu ankaŭ G16 kaj G34). (d) La id-rondira problemo (G16) rekte tuŝas la inkrementan kunfandon de la plano.
- **Sugesto**: ĝisdatigu `docs/nova/06_servilo_arkivo.md` antaŭ ol konstrui la servilon.

#### C44 — Perditaj kontraŭ la malnova apo (konsideroj, ne nepre riparoj)
- **Statuso**: [ ]
- **Priskribo**: (a) redirect-host-guard kontraŭ kaptivaj portaloj (`Diverse.tjekOmdirigering`); (b) `serverCurrentTimeMillis` (kontraŭ aparata horloĝ-eraro); (c) la regulo ">95% → rekomencu de 0" de la malnova ludilo (`Afspiller.onPrepared`) — nun nur la `finita`-flago; (d) "elŝuti nur per WiFi" (`hentKunOverWifi`); (e) perioda refreŝo (malnova: kanalo ĉiujn 30 s, grunddata ĉiujn 30 min — nova: nur tiral-aktualigo kaj eniro al ekrano); (f) fora elŝuto de la kanalkonfiguro kaj radio.txt dum rultempo (nun nur bundled; atentu: la `muzaiko`-RSS en la agordo estas ekstere gastigata); (g) ksoup `parseXml` kaj krudaj ne-XML-aj entitoj (`&nbsp;` rekte en la fluo) — rekomendas golden-teston kun kruda `&nbsp;`.
- **Sugesto**: decidu intence, kiuj el ili estos reenkondukitaj; almenaŭ (c) kaj (g) estas malmultekostaj.

#### C46 — KMP-H: `ExoPlaybackException: Source error` (544 okazoj / 30 uzantoj ekde 2026-09-13; malpliiĝas)
- **Statuso**: [ ]
- **Fonto**: Sentry-analizo 2026-10-19 (issue [KMP-H](https://esperantoradio.sentry.io/issues/KMP-H); 42 okazoj / 5 uzantoj en la lasta semajno, lasta evento sur dist 248, 10-14)
- **Priskribo**: Plejparte ret-kulpaj kaŭzoj (`UnknownHostException`, `HttpDataSource$InvalidResponseCodeException`, timeout) — atendataj fiaskoj, sed ili nutras ankaŭ K1 (fiasko de la fonto prokrastas `startForeground()`). **Ĉar** ekde 249 (PR #109) `onPlayerErrorChanged` uzas `logw(tag, msg, e)` (3-arg = nur loka, la escepto ne plu atingas Sentry), la malpliiĝo de KMP-H post 10-14 estas grandparte **redukto de videbleco, ne pliboniĝo de stabileco** — sur 249-aparatoj lud-eraroj plu okazas, sed estas nevideblaj en Sentry.
- **Sugesto**: unu aŭtomata reprovo ĉe fon-eraro en `LudvicoRegilo`; pli klara uzantmesaĝo; kontroli mortajn fluo-URL-ojn (ligu kun G44 — `media.vaticannews.va`). Se oni volas plu vidi lud-erarojn en Sentry, raportu ilin per 2-arg `logw` (Sentry-averto, videbla en Logs) aŭ kiel specimenata `loge` — intenca decido pro la kvoto.

#### C47 — Malnovaj buildoj (dist ≤ 248) daŭre raportas erarojn, kiujn novaj buildoj ne plu sendas
- **Statuso**: [ ]
- **Fonto**: Sentry-analizo 2026-10-19 (KMP-G "Aŭtoludo: …" 758 okazoj / 85 uzantoj — lasta evento sur dist 246/3.0.2; KMP-N "RSS-malsukcesa …" 1227 / 57 — lasta sur dist 245/3.0.1; ambaŭ vivas nur sur aparatoj kun dist ≤ 246)
- **Priskribo**: **Ne solvita, nur malpliigite videbla**: "Aŭtoludo:"-mesaĝoj estis forigitaj (PR #107, en v3.0.3 ekde dist 247 — pro kvoto-plenigo per `captureMessage`); "RSS-malsukcesa" kaj lud-eraroj (KMP-H) estis malaltigitaj al `logw(tag, msg, e)` (PR #93 kaj #109), kio estas **nur loka** — la subaj problemoj (aŭtoludo fiaskas, fluoj neatingeblaj, fontoj mortaj) daŭre okazas, sed ekde 249 ili estas nevideblaj en Sentry krom kiel avertaj protokoloj (2-arg `logw` → `Sentry.logger.warn`). Ankaŭ la escepto-bazitaj retaj issues (KMP-12/14/15/M/Q — timeout/connect/DNS) venas nun nur de malnovaj buildoj: en la aktuala kodo ambaŭ ŝargo-vojoj (deponejo kaj `NovajElsendojKontroloWorker:140`) kaptas per 3-arg `logw` (loka). La eraro-taso kolapsis post 10-14 (225/tago → 1–3/tago) grandparte pro tio plus la 50 %-a specimenado (PR #109) — tio ne provas pliboniĝon de la stabileco.
- **Sugesto**: versio-bazita `beforeSend`-forĵeto por eldonoj ≤ 3.0.2 (ligita al FAROTA C45 — la ĝisdatebla centra listo; AGENTS.md avertas ne malmoligi la liston en la apo); alternative: en Sentry ignori la eldonojn ≤ 3.0.2 aŭ marki la koncernajn problemojn "ne plu riparebla". Se la videbleco de aŭtoludo/RSS/ludado gravas, raportu ilin intence kiel avertojn (vidu C46).

#### C48 — Post-ripara observo en Sentry
- **Statuso**: [ ]
- **Fonto**: Sentry-analizo 2026-10-19 (bazlinio: 182 malfermoj / 336 ludadoj en 7 tagoj; eraro-taso 1–3/tago post 10-14, krom la nova KMP-1V)
- **Sugesto**: post la sekva eldono kontroli: KMP-1V malaperis, la eraro-taso ne kreskas, KMP-X/KMP-1S fermiĝas; konsideri Sentry-atentigojn (alerts) por novaj fatalaj kraŝoj kaj por "RSS-malsukcesa kun 0 kaŝenitaj elsendoj" kiel frua averto pri mortaj kanaloj.

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

---


---

> **Pri la finitaj trovindikoj** (62): ilia daŭre valida scio staras en
> [`NOTOJ.md`](NOTOJ.md); la historio de la riparoj en [`ŜANĜOJ.md`](ŜANĜOJ.md)
> (enskribo 13) kaj en la PR-oj #89–#95. La etikedoj (K…, G…, C…) restas
> spureblaj per `git log` kaj per NOTOJ.md.
