# FAROTA.md — Malfermitaj trovoj por posta riparado

> **Fonto**: tutplena kodrevizio de 2026-10-08, farita per 8 subagentoj (tavoloj: kerno, ludilo, UI/navigado, Android, testoj+build+CI, sekureco, historia `malnova/`, delta `b92f739..c92766a`).
> **Bazo**: `b92f739` + delto ĝis `c92766a` (la loka `master` estis ĝisdatigita per `git pull --ff-only` dum la revizio; liniaj referencoj estas al `c92766a`).
> **Kiel uzi**: la punktoj estas ordigitaj laŭ graveco (KRITIKA → GRAVA → KONSILO). Marku la status-keston `[x]`, kiam finite. Ne forigu punkton sen riparo — anstataŭe movu ĝin al "Jam farita" sube.
> **Rilato al la aliaj dosieroj** (ekde 2026-10-11): la DAŬRE VALIDA scio el la jam finitaj ([x]) trovindikoj staras en [`NOTOJ.md`](NOTOJ.md) (permanente notinde, laŭ temo); la historio de la riparoj (problemo→kaŭzo→testo) en [`SXANGXOJ.md`](SXANGXOJ.md) (enskribo 13). Ĉi tiu dosiero restas nur por la MALFERMAJ punktoj.

## GRAVA

#### G7 — Dependencoj 1–2 jarojn malnovaj; CVE-oj nekontrolitaj
- **Statuso**: [ ]
- **Loko**: `gradle/libs.versions.toml` (media3 1.5.1, ktor 3.1.3, coil 3.0.4, sentry 8.41.0, AGP 8.9.1, work 2.10.1, activity-compose 1.9.3)
- **Priskribo**: Neniu CVE-kontrolo estis ebla (sen reto en la revizia medio). Precipe atentindaj: media3 (analizila surfaco nutrata de nefidindaj URL-oj, vidu G1/G2) kaj ktor.
- **Sugesto**: `./gradlew dependencyCheckAnalyze` + Dependabot en CI; prioritatigu media3 kaj ktor.

#### G16 — id-skemo `slug:dato:guid` malkongruas kun la dokumentita konvencio kaj rompas la servilan rondiron
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:122-123` (ĝenerala), `:283` (Peranto); dokumento: `docs/nova/04` ("Id-konvencioj"); malnova: `RomePodcastParser.parseAndre:191`
- **Priskribo**: La dokumento kaj la malnova kodo uzas la guid sen prefikso (`<slug>:<dato>`); la kodo uzas `<slug>:<dato>:<guid>`. Sekvoj: (a) rondiro kun la planita servilo: la servilo skribos `Elsendo.id` kiel `<guid>`, la apo re-parsas kaj denove prefiksas → la id kreskas ĉiun fojon kaj la inkrementa kunfando (`e.id !in konataj`, `docs/nova/06`) neniam trafas; (b) estonta migrado de uzantdatumo (elŝutoj, ludpozicioj, plejŝatataj estas ŝlositaj per `id`) rompiĝus; (c) la dokumenta tabelo estas malĝusta.
- **Sugesto**: uzu la guid senŝanĝa se ĝi jam komenciĝas per `<kanaloSlug>:` (aŭ ĉiam por `deArkivo`), alie `<kanaloSlug>:<guid>` (unu prefikso); kaj vicigu la dokumenton kaj la kodon.

#### G25 — Etikedo `v3.0.2` montras al commit, kiu ne estas sur master
- **Statuso**: [ ]
- **Loko**: git-etikedo `v3.0.2` → `16ae6d6` (gepatro `39dd2d8`); la master-a eldona komito estas `9e835b9` (squash-merge de la sama PR)
- **Priskribo**: `git merge-base --is-ancestor 16ae6d6 HEAD` → ne. La enhavo de la APK estas identa (nur dokumentaro malsamas), sed la dokumentita proceduro ("etikedo sur la eldona komito") kaj F-Droid (konstruas el la etikedo) celas la master-an komiton, kaj la etikedo estas neatingebla de `master`.
- **Sugesto**: rekrei la etiketon sur `9e835b9` kaj puŝi (`git tag -f v3.0.2 9e835b9 && git push -f origin v3.0.2`) — **atentu, tio estas re-verkado de publika etikedo (ekstera ŝanĝo, bezonas vian aprobon)**.

#### G31 — https-korekto nur por `kernpunkto`, kontraŭ la dokumento "ĉiam"
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:109-111` (`if (kanalo.slug == "kernpunkto" && fluo.startsWith("http://"))`); dokumento: `docs/nova/04`, regulo 6.1.6
- **Priskribo**: Aliaj kanaloj kun `http://`-enclosure restus http (Android blokus ilin per cleartext-protekto; ankaŭ miksita enhavo en la UI). La nuna konfiguro uzas `https://` ĉie (kontrolita supraĵe), do nuntempe ne aktiva — sed la dokumento kaj la kodo kontraŭdiras.
- **Sugesto**: korektu ĉiam (krom se iu kanalo eksplicite bezonas http — tiam per konfiguro), kaj aldonu ĝeneralan teston; aŭ korektu la dokumenton.

#### G41 — `RssArkivServer-filcache/` kontraŭdiro: la oraj fiksaĵoj ne estas reprodukteblaj el la repo
- **Statuso**: [ ]
- **Loko**: `.gitignore` (`/RssArkivServer-filcache/`) kontraŭ AGENTS.md ("NE versiigu") kaj `docs/nova/04:141` ("Kopiu la kaŝenitajn fluojn el `RssArkivServer-filcache/`")
- **Priskribo**: Sekve nur 4 fiksaĵoj estas en `shared/src/commonTest/resources/feeds/` (kernpunkto, peranto, varsoviavento, vinilkosmo), dum la ora test-tabelo priskribas pliajn fiksaĵojn — interalie anchor.fm "La Malfamuloj" (≥114 elsendoj, `docs/nova/04:153`), kiu **mankas** en la repo. La plena aro de golden fixtures ne estas reproduktebla el la repo.
- **Sugesto**: solvi la kontraŭdiron: aŭ versiigu la fiksaĵojn (sen sekretoj), aŭ korektu AGENTS.md/dokumenton ("ne reproduktebla el la repo") kaj kopii la mankantajn fiksaĵojn (precipe anchor.fm) al `resources/feeds/`.

## KONSILO

#### C5 — Subskrib-konfiguro: defaŭlta absoluta keystore-vojo en publika deponejo
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:47` (`storeFile = file(System.getenv("KEYSTORE_PATH") ?: "/home/j/android/A_signaturer/jacobnordfalk.keystore")`, pasvortoj defaultas al `""`)
- **Sugesto**: forigu la hardkoditan vojon; ĵetu eraron se `KEYSTORE_*` mankas ĉe release-konstruo (fail-fast). (La CI `eldonado.yml` faras tion ĝuste: GitHub-secrets → env.)

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
- **Loko**: `HejmoEkrano.kt:618-658` (tripunkta menuo 28.dp, ludo-butono 32.dp); `ElshutitajEkrano.kt:140-148` (`IconButton` 36.dp); `MalsupraNavigaBreto.kt:30-54` (ikonoj kun `contentDescription = null`); `ElsendoEkrano.kt:332-411` (buton-ikonoj kun `contentDescription = null`)
- **Sugesto**: `Modifier.minimumTouchTargetSize()` aŭ pligrandigo; aldonu `contentDescription` al la ikonoj.

#### C34 — iOS: `PlatformLigilo` estas TODO; neniuj deep links
- **Statuso**: [ ]
- **Loko**: `shared/src/iosMain/.../ui/PlatformLigilo.ios.kt` (`malfermuLigon`/`malfermuRetposhton` malplenaj); neniu `ACTION_VIEW` intent-filter
- **Sugesto**: konata limito (iOS venos); se iam bezonataj, aldonu deep links kaj traktu la URL-on en `MainActivity.traktuIntenton`.

#### C38 — Morta kodo
- **Statuso**: [ ]
- **Loko**: `PersonigoDeponejojImpl.kt:36,48` (`LastAuxskultitajDeponejo.pozicioj` neniam plenigita — `getPozicio` ĉiam redonas `null`); `NoOpElshutDeponejo.kt:18` (nova `MutableStateFlow` je ĉiu voko de `observiElshutStaton`); `Diagnozo.kt:32` (`DiagnozoTipo.RETO` neniam konstruita; `Severeco.INFO` neniam produktata — la filtrilo en `HejmoEkrano.kt:332` estas senefika); `ElsendoDeponejoImpl.kt:38-46` (`getElsendojn` kaŝo-nur, sed la interfaco sugestas alie — dokumentu aŭ renomu)
- **Sugesto**: forigu aŭ dokumentu; direktu la poziciojn al `LudatojDeponejo`.

#### C39 — `AppStato`: duobla ŝargo de la sama kanalo (neniu in-flight dedup)
- **Statuso**: [ ]
- **Loko**: `ElsendoDeponejoImpl.kt` (`sxargxiElsendojn` — du samtempaj vokoj, ekz. pull-to-refresh + aŭtomata ŝargo, faras duoblan elŝuton)
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
- **Priskribo**: (a) `ændrerSigIkke=true` = "kaŝita → neniam refreŝi" — tio igas `RssArkivServer-filcache/` la determinismajn golden-fixture-ojn; la nova plano diras nur "reuzo de Ktor-HttpCache + RssArkivServer-filcache" sen precizigi tiun semantikon. (b) La malnova `RomeFeedWriter` skribis `enclosure length` el la loka kaŝo kaj **per-jarajn arkivdosierojn** `feed-<slug>-<yyyy>.xml` — la nova plano tion ne mencias. (c) La plan-snippet vokas `parsilo.parsRss(fluo, kanal)` **sen** `httpKliento` — por Peranto tio signifas divenitajn, ofte malĝustajn, archive.org-URL-ojn en la arkivo — la servilo devas transdoni klienton kaj persistentan `ArchiveOrgDosiernomoKasho` (vidu ankaŭ G16 kaj G34). (d) La id-rondira problemo (G16) rekte tuŝas la inkrementan kunfandon de la plano.
- **Sugesto**: ĝisdatigu `docs/nova/06_servilo_arkivo.md` antaŭ ol konstrui la servilon.

#### C44 — Perditaj kontraŭ la malnova apo (konsideroj, ne nepre riparoj)
- **Statuso**: [ ]
- **Priskribo**: (a) redirect-host-guard kontraŭ kaptivaj portaloj (`Diverse.tjekOmdirigering`); (b) `serverCurrentTimeMillis` (kontraŭ aparata horloĝ-eraro); (c) la regulo ">95% → rekomencu de 0" de la malnova ludilo (`Afspiller.onPrepared`) — nun nur la `finita`-flago; (d) "elŝuti nur per WiFi" (`hentKunOverWifi`); (e) perioda refreŝo (malnova: kanalo ĉiujn 30 s, grunddata ĉiujn 30 min — nova: nur tiral-aktualigo kaj eniro al ekrano); (f) fora elŝuto de la kanalkonfiguro kaj radio.txt dum rultempo (nun nur bundled; atentu: la `muzaiko`-RSS en la agordo estas ekstere gastigata); (g) ksoup `parseXml` kaj krudaj ne-XML-aj entitoj (`&nbsp;` rekte en la fluo) — rekomendas golden-teston kun kruda `&nbsp;`.
- **Sugesto**: decidu intence, kiuj el ili estos reenkondukitaj; almenaŭ (c) kaj (g) estas malmultekostaj.

---


---

> **Pri la finitaj trovindikoj** (62): ilia daŭre valida scio staras en
> [`NOTOJ.md`](NOTOJ.md); la historio de la riparoj en [`SXANGXOJ.md`](SXANGXOJ.md)
> (enskribo 13) kaj en la PR-oj #89–#95. La etikedoj (K…, G…, C…) restas
> spureblaj per `git log` kaj per NOTOJ.md.
