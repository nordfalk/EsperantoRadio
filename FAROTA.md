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
- **Konfirmo** (2026-10-27, Sentry): la sola evento de la lastaj 14 tagoj okazis sur dist **249** (la antaŭ-ripara konstruo); sur dist ≥ 250, inkluzive 3.1.0 (252), **nul** okazoj. KMP-1V solvita en Sentry.

## GRAVA

#### G42 — KMP-X: `ForgottenCoroutineScopeException: rememberCoroutineScope left the composition` (212 okazoj / 58 uzantoj ekde 2026-09-13)
- **Statuso**: [ ]
- **Fonto**: Sentry-analizo 2026-10-19 (issue [KMP-X](https://esperantoradio.sentry.io/issues/KMP-X); 32 okazoj / 10 uzantoj en la lasta semajno, lasta 10-16)
- **Loko**: `rememberCoroutineScope` en `App.kt:99`, `SerĉoEkrano.kt:34`, `PlejŝatatajEkrano.kt:37`, `HejmoEkrano.kt`, `ElŝutitajEkrano.kt:39`, `DiagnozoEkrano.kt:37`, `ElsendoEkrano.kt:73`, `KanalEkrano.kt:108`
- **Priskribo**: Korutino lanĉita el `rememberCoroutineScope` postvivas la komponaĵon — tipa kaŭzo: lanĉo en klik-lambdo, kies rezulto estas uzata post navigado foren.
- **Ĝisdatigo 2026-10-27** (Sentry, 14 tagoj): 23 eventoj / 4 uzantoj — 22× dist **248**, 1× 246; sur 3.1.0 (252; 6 uzantoj, 50 %-specimenado) nul. Restas malfermita: la pruvo sur la nova konstruo estas malforta (malmultaj uzantoj + specimenado), kaj la kodvojo probable ekzistas plu.
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
- **Ĝisdatigo 2026-10-27** (Sentry 3.1.0/252, 14 tagoj + ekstera kontrolo): `radioscienca` fiaskas ankaŭ en la nuna eldono — ~15 eventoj ĉe ~5 uzantoj, ofte kun **0 kaŝenitaj elsendoj** (uzanto vidas malplenan kanalon). La fluo respondas HTTP 200 de ekstere (kontrolita per curl, ankaŭ kun Ktor/okhttp/apo-UA — do ne UA-blokado, sed intermita servila problemo de la gastiganto). Same aperas: `polaretradio`, `radiovatikana`, `spiritismavocxo` (0 kaŝenitaj), `lemondediplomatique`, `lamalfamuloj`, `vinilkosmo`. La DNS-avertoj (`UnresolvedAddressException`, 52 eventoj) venas de unu aparato sen reto kaj estas pritraktitaj ĝuste per kaŝmemoro.

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

#### G47 — Ekludo el sciigo restartiĝas (de pozicio 0) dum ekranturno
- **Statuso**: [x] (diagnozita kaj riparita 2026-10-30, PR #149)
- **Fonto**: uzanto-raporto de la posedanto (2026-10-30 antaŭtagmeze kaj denove vespere, Samsung-telefono en Danio, Varsovia Vento: "La 199a elsendo 1a/2a parto"); konfirmita en Sentry (uzanto `5f08712e…`, release `3.1.1-beta29okt`): la 2026-10-30 "Ludado komencita de sciigo" aperis dufoje 1 s aparte (07:22:51/52, 07:38:19/07:39:25) kaj eĉ 5-foje ene de 9 s (19:15:44–53) — ĉiufoja rekreo de la Activity.
- **Kaŭzo**: `MainActivity` havas nek `configChanges` nek `launchMode` → ĉe ekranturno `onCreate` refariĝas kun la SAMA malnova intent kaj vokis `traktuIntenton(intent)` senkondiĉe → `traktuMalfermiElsendon` → `fiksiFonton(fonto, komencoPozicioMs=0)` + `ludi()` → `setMediaItem(aĵo, 0)` → restarto de la komenco; krome `AtendantaElsendoNavigacio.setu` re-navigis.
- **Riparo**: en `MainActivity.onCreate` la intent estas traktata nur ĉe freska kreo (`savedInstanceState == null`); `onNewIntent` restas por novaj intent. Regresa testo: `SciigoLudiTest.rekreoNeRestartigasLudonDeSciigo` (lanĉas la Activity kun MALFERMI_ELSENDON, `recreate()`, kontrolas ke la pozicio ne rekomenciĝas).
- **Sugesto**: post eldono observi en Sentry ke "Ludado komencita de sciigo" ne plu aperas plurfoje ene de sekundoj.

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
- **Statuso**: [x]
- **Loko**: `ElsendoDeponejoImpl.kt` (`ŝargiElsendojn` — du samtempaj vokoj, ekz. pull-to-refreŝ + aŭtomata ŝargo, faris duoblan elŝuton)
- **Riparo** (2026-10-29): in-flight dedup per kanalo en `ŝargiElsendojn` — `Mutex` + `Map<slug, Deferred>`; dua samtempa voko aliĝas al la daŭranta `Deferred` (lanĉita en sendependa skopo `Dispatchers.Default + SupervisorJob`, do ĝi finiĝas eĉ se la unua vokanto estas nuligita). La ero estas forigata kiam la ŝargo finiĝas (`invokeOnCompletion`) resp. kiam finiĝas la vokanto (`finally` kun `isCompleted`-kontrolo). Testo: `samtempajVokojPorSamaKanaloDedupatas` (du vokoj → 1 HTTP-peto).

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
- **Ĝisdatigo 2026-10-27**: ankoraŭ aktuala — 55 erar-eventoj sur dist 248 kaj 30 sur 246 en 14 tagoj; ĉiuj erar-eventoj de la periodo venas de dist ≤ 249, dum 3.1.0 (252) produktis neniujn (nur `logw`-avertojn). La problemo malkreskos nature dum la uzantoj ĝisdatiĝas. KMP-1 ("Sentry-testo") montriĝis intenca provo-eraro (dist 1, release 0.1) — ignorita en Sentry.

#### C48 — Post-ripara observo en Sentry
- **Statuso**: [x]
- **Riparo** (2026-10-27, observado de 3.1.0/252 dum 14 tagoj): KMP-1V **malaperis** sur ≥ 250 (sola evento sur dist 249, la antaŭ-ripara konstruo) kaj estas solvita en Sentry; KMP-1S: 1 evento nur sur dist 248 (laŭ atendado de G43); KMP-X (G42) daŭre malfermita, sed nur sur malnovaj konstruoj; la eraro-taso sur 3.1.0 mem estas **nul** — ĉiuj erar-eventoj de la periodo venas de dist ≤ 249. La 152 avertoj de 3.1.0 estas ĉiuj atendataj ret-fiaskoj (`logw`) — konduto laŭregola. Sentry-atentigoj por fatalaj kraŝoj ankoraŭ ne agorditaj; la ideo de averto pri "RSS-malsukcesa kun 0 kaŝenitaj elsendoj" transiras al G44 (la freŝaj datenoj tie).
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
  (c) konfirmo antaŭ forigo de elŝuto (forigo mem jam ekzistas — tuj, sen dialogo);
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

#### C54 — Kanal-RSS-fluo kopiebla kaj kunhavigebla al alia podkasta apo
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24)
- **Priskribo**: butono en la kanal-ekrano, kiu kopiigas la `podkastaRssUrl` de la kanalo
  (tondujo + partigo), por ke uzantoj abonu la kanalon en alia podkasta apo.
- **Sugesto**: Android: `ClipboardManager` + partigo-intento (atentu: ĝi ankaŭ utilos por
  C51(a) — kunhavigi ELSENDON; la partig-infrstrukturon kreu unufoje); la URL estas
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

#### C56 — Esploro: reta (WASM) versio kun datumoj de la servilo, ludado rekte el la fonto
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24); modelo: https://esperanto-radio.com/
- **Priskribo**: esplori ĉu la reta versio povus funkcii tiel, ke la fluo- kaj
  metadatumoj venas de la estonta arkiv-servilo (docs/nova/06, C43), sed la ludado mem
  estas reta HTML-komponento kiu ludas rekte de la fonto. Ŝlosila fakto: CORS blokas
  `fetch`/XHR (pro tio plej multaj RSS-fluoj kaj ankaŭ la Coil-bildoj ne ŝargeblas en
  la nuna reta versio), sed **ne** `<audio src>` — la rekt-ludado do funkcias sen
  prokurilo, kaj `WasmJsLudiloRegilo` jam uzas HTML-`<audio>`-elementon. Do:
  datumvojoj (RSS, bildoj) devas veni samdevene (el la servilo aŭ per `<img>`-etikedoj
  anstataŭ Coil-fetch); la sono restas rekt-flua el la fonto (nur anchor.fm-MP3 nun
  funkcias, ĉar ĝi jam estas CORS-permesa).
- **Sugesto**: prototipo post kiam la arkiv-servilo havas JSON-API-on (docs/nova/06):
  samdevena listo de kanaloj/elsendoj → la ekzistanta ŝarga-logiko, kun la bild-URL-oj
  servataj de la servilo aŭ rekte `<img>`-ebligataj. Kvanto-specimenado: mezuru ĉu la
  servila datumvojo povas esti subflue (sen CORS-kapoj) por ĉiuj 26 kanaloj.

#### C57 — "Proponu kanalon"-butono (retpoŝta)
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24)
- **Priskribo**: butono kiu simple kreas retpoŝtan mesaĝon por proponi novan kanalon
  (al la prizorganto; kun antaŭskribita temo/korpo: nomo, RSS-URL, priskribo).
- **Sugesto**: `PlatformLigilo.malfermuRetpoŝton` jam ekzistas kaj funkcias (uzata sur
  la kanal-ekrano por "Retpoŝto"); metu la butonon en la navigan tirkeston aŭ
  Kanalaron (aŭ estontan Pri-ekranon, C51(g)); la retpoŝto-celo determinu la prizorganton.

#### C58 — "White label"-ligilo por kanal-eldonantoj
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24)
- **Priskribo**: ebleco por kanal-eldonanto kreii specialan ligilon al la apo, kiu
  malfermas ĝin tiel, ke ĝuste tiu kanalo staras ĉe la frontpaĝo ("white label" —
  ekz. ManchaPod alvokas siajn aŭskultantojn al "sia" EsperantoRadio).
- **Sugesto**: Android App Links / intenco-ŝablono (ekz. `…/kanalo/<kodo>`) → nav3
  deep-link (hodiaŭ la apo ne havas deep links — C34) → enscenigu `Vojo.KanaloDetalo`
  kiel komenca ekrano (eble kun `Vojo.Hejmo` sube en la stako, por ke reen-gesto
  kondutu nature); por la reta versio (C56) sufiĉas URL-parametro. Aldonu ankaŭ al la
  Play-Stor-listo ligilojn (`assetlinks.json`-kontrolo) kaj memoru: la kanalo devas
  ekzisti en la agordo (A5).

#### C59 — Transsalti ripetiĝan enkondukon de kanalo (kun detekta-ilo)
- **Statuso**: [ ]
- **Fonto**: ideo de la posedanto (2026-10-24)
- **Priskribo**: kelkaj kanaloj ĉiam sonas ekzakte same dum la komenco — teda post
  kelkaj fojoj. (a) Enskribi en la kanalaj informoj kiom da sekundoj oni povas
  transsalti en la komenco. (b) Eble eĉ krei ilon kiu trovas tion: komparas la
  komencajn sekundojn de pluraj epizodoj kaj elkalkulas la ripetan longon.
- **Sugesto**: (a) nova kampo en `esperantoradio_kanaloj_v9.json` (ekz.
  `saltiKomenceSekundojn` — daten-movita, A5; kontroli la JSONC-kampanimadon kaj la
  parsajn regulojn P6.1+, ankaŭ la agord-kompateblecon kiam la kampo mankas);
  la ludilo jam subtenas komencan pozicion — `LudiloRegilo.fiksiFonton(fonto,
  komencoPozicioMs)` — do `LudvicoRegilo.ludiElsendon` transdonu
  `max(komencoPozicioMs, savitaPozicio)` kaj la pozicio-indikilon ne montru la
  transsaltitan parton (aŭ montru 0:xx de la komenco de la enhavo). (b) la detekta
  ilo komparu la komencajn ~60 s (RMS/ampleks-envelopa korelacio aŭ audio-fingerprint);
  natura loko: la estonta arkiv-servilo (docs/nova/06), kiu jam elŝutos la sonojn —
  ĝi povus proponi la valoron por ĉiu kanalo.

#### C60 — Serĉ-ordigo: unue kanaltitoloj, poste kanalpriskriboj, poste elsendtitoloj, poste elsendpriskriboj
- **Statuso**: [x]
- **Fonto**: ideo de la posedanto (2026-10-30)
- **Riparo** (2026-10-30, PR #150): nova `SerĉoRezulto(kanaloj, elsendoj)` — ambaŭ listoj jam ordigitaj laŭ graveco (titolo-trovoj unue, poste priskribo-trovoj; dedup per `partition`), redonata de `SerĉoDeponejo.serĉi`. `SerĉoDeponejoImpl` serĉas nun ankaŭ kanalojn per `KanaloDeponejo`; por elsendoj unu grupiga metodo `ElsendoDeponejo.serĉiElsendojnGrupigite` (Pair). `SerĉoEkrano` montras la sekciojn "Kanaloj" kaj "Elsendoj", klako sur kanalo navigan al `Vojo.KanaloDetalo`. Testoj: `SerĉoDeponejoImplTest` (ordigo + dedup + uskleco), `SerĉoEkranoTest.montrasGrupigitajnSekciojn`.

---


---

> **Pri la finitaj trovindikoj** (62): ilia daŭre valida scio staras en
> [`NOTOJ.md`](NOTOJ.md); la historio de la riparoj en [`ŜANĜOJ.md`](ŜANĜOJ.md)
> (enskribo 13) kaj en la PR-oj #89–#95. La etikedoj (K…, G…, C…) restas
> spureblaj per `git log` kaj per NOTOJ.md.
