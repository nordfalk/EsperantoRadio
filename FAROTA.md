# FAROTA.md — Malfermitaj trovoj por posta riparado

> **Fonto**: tutplena kodrevizio de 2026-10-08, farita per 8 subagentoj (tavoloj: kerno, ludilo, UI/navigado, Android, testoj+build+CI, sekureco, historia `malnova/`, delta `b92f739..c92766a`).
> **Bazo**: `b92f739` + delto ĝis `c92766a` (la loka `master` estis ĝisdatigita per `git pull --ff-only` dum la revizio; liniaj referencoj estas al `c92766a`).
> **Kiel uzi**: la punktoj estas ordigitaj laŭ graveco (KRITIKA → GRAVA → KONSILO). Marku la status-keston `[x]`, kiam finite. Ne forigu punkton sen riparo — anstataŭe movu ĝin al "Jam farita" sube.

## Prioritata ordo (resumo)

1. **Sekureca pako** (unu PR): URL-skema validado + `network_security_config` + `WebViewClient` + intent-nonce por `MainActivity` (G1–G4).
2. **K1** — HTTP-status-kontrolo (datumperdo; ~5 linioj + testo).
3. **K2 + C7** — `Application`-klaso anstataŭ la globala `lateinit appContext` (sciigoj vivaj en fono; Sentry ĉie inicializita).
4. **K3 + G26** — ExoPlayer `halti()`/`playerError`; `CancellationException` reĵetata en `LudvicoRegilo`.
5. **Rapidaj venkoj**: K4 (`.clickable`), K5 (URLDecoder + testo), G9 (IRo-ordigo: `sortedByDescending`), G17 (mankanta komo en la JSON-konfiguro).
6. **Elŝutoj** (G10–G12): triligo-kontrolo, purigo de partaj dosieroj, `ConcurrentHashMap`.
7. **G8 + G14** — `traktiFinon`-gardilo (atoma, ŝlosita sur la finanta fonto) + korekta testo.
8. **CI kaj kernaj testoj** (G24, G37, G38, G40): test-laborfluo por PR, purigo en `DiskKashoTest`, smoke-testo de la vera konfiguro, persisto-testoj.
9. **Dokumentaj konsekvencoj**: `puriguModeloj` (G15 aŭ dok), id-konvencio (G16), F-Droid-metadato (G23), etikedo `v3.0.2` (G25 — bezonas aprobon, ekstera ŝanĝo).

---

## KRITIKA

### K1 — Datumperdo: HTTP-statuso nenie kontrolata en `ElsendoDeponejoImpl`
- **Statuso**: [x]
- **Loko**: `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/data/repository/ElsendoDeponejoImpl.kt:67-75` (kliento: `AppStato.kt:94-95`, sen `expectSuccess`)
- **Priskribo**: `httpKliento.get(url).bodyAsText()` redonas la korpon de 404/500/HTML-fiaskpaĝo same kiel tiun de vera fluo. La fiaskpaĝo estas parsata (neniu `<item>`/`<entry>` → malplena listo), anstataŭigas la enmemoran kaŝon KAJ la diskkaŝon (`skribuKashon`, linio 69) — eĉ post restarto la apo montras nenion ĝis sukcesa re-elŝuto. Ofta kondiĉo: servera eraro, kaptiva portalo.
- **Sugesto**: kontrolu `response.status.isSuccess()` kaj ĵetu (la ekzistanta `catch` redonas la malnovan kaŝon); aŭ `expectSuccess = true` en la kliento. Aldone: ne anstataŭigu la kaŝon per malplena rezulto se la korpo ne enhavas `<item`/`<entry`/`<rss`/`<feed`; ne skribu la diskkaŝon antaŭ sukcesa status-kontrolo.
- **Takso**: ~5 linioj + unuo-testo (falsa Ktor-motoro, 404 → kaŝo konservita).

### K2 — Sciigoj pri novaj elsendoj ne funkcias kun fermita apo (WorkManager-procezo)
- **Statuso**: [x]
- **Loko**: `shared/src/androidMain/kotlin/dk/nordfalk/esperanto/data/repository/NovajElsendojKontroloWorker.kt:63-90` (`doWork`:63, `sciigPermesoDonita()`:76); la globalo `lateinit var appContext` (`data/config/`) estas agordata nur en `MainActivity.kt:49`, `AlarmoReceivilo.kt:45`, `BootReceivilo.kt:26,42`
- **Priskribo**: Kiam WorkManager lanĉas procezon por la perioda laboro (7:00/16:00, apo fermita — la normala kazo): sur API 33+ `UninitializedPropertyAccessException` → `catch` → `loge` → `Result.retry()` — **senfina reprovo-ciklo** (baterio-dreno); sur API 26–32 `kreuSettings()` reeniras al `NoOpSettings` → "neniu ŝatata kanalo" → `Result.success()` — **silenta nenio-faro**. La instrumento-testo ne kaptas tion (ĝi rulas kun viva `MainActivity`).
- **Sugesto**: enkonduku `Application`-klason (aŭ `androidx.startup`), kiu agordas la globalan kontekston kaj inicializas Sentry; en la worker uzu la propran `applicationContext` anstataŭ la globalo en `senduSciigon` (linio 152). Aldonu teston, kiu lanĉas la worker sen `MainActivity`.
- **Takso**: meza (ligita kun C7).

### K3 — Android: ludado povas rekomenciĝi per si mem post "Halti"
- **Statuso**: [x]
- **Loko**: `androidApp/src/androidMain/kotlin/dk/nordfalk/esperanto/android/ExoPlayerLudiloRegilo.kt:158-213` (`updateState`:158, `halti`:204-207); `LudvicoRegilo.kt:149` (`provuReprovi`), `:182` (catch), reprovo-gardilo ĉirkaŭ `:168-176`
- **Priskribo**: En Media3 1.5.1 `stop()` kaj `clearMediaItems()` **ne nuligas `playbackError`** (nur `prepare()`; kontrolita en la font-jar de la gradle-kaŝo). Post `halti()`: `onMediaItemTransition(null)` → `updateState()` prioritategas `playerError` super `playbackState` → stato `Eraro` anstataŭ `Haltita` kun `nunaFonto = null`. La observanto de `LudvicoRegilo` vidas transiron `Haltita → Eraro` kaj vokas `provuReprovi()`; la gardilo akceptas `nunaFonto == null` kiel "sama fonto" → **reprovo lanĉiĝas kaj la muziko rekomencas post ol la uzanto premis "Halti"**. Ĉe daŭraj eraroj (404): duobla `traktiFinon` (duobla `erarojSinsekvaj++`, duobla aŭtoludo).
- **Sugesto**: (a) en `updateState()`, se `c.mediaItemCount == 0` → ĉiam `Haltita`; (b) en `LudvicoRegilo` traktu `nunaFonto == null` en la reprovo-gardilo kiel "nuligu"; (c) direktu la butonon "Halti" tra `LudvicoRegilo.haltuLudadon()` (nuligu reprovon + `ludilo.halti()`), vidu G13. Testu sur la emulilo: ludu, igu erari, premu "Halti" dum la reprovo-atendo.
- **Takso**: meza; konfirmu sur aparato/emulilo.

### K4 — LudvicoEkrano: klako sur vico estas senefikiva
- **Statuso**: [x]
- **Loko**: `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/ui/LudvicoEkrano.kt:87-118` (`VicoEro`:87, `ListItem`:92; `onClick` ricevata:89, uzata nur de la forigo-butono:109)
- **Priskribo**: `VicoEro` ricevas `onClick` (pasata en la vokanto, linio 71) sed la `ListItem` havas nur `Modifier.fillMaxWidth()`, sen `clickable`. La uzanto ne povas klaki sur vico-eron por malfermi la elsendon — la tuta `onClick` kaj la `onElsendo`-navigado estas senefikaj.
- **Sugesto**: aldonu `modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)` al la `ListItem` (aŭ uzu `Surface(onClick = ...)`). Aldonu UI-teston.
- **Takso**: 1 linio + testo.

### K5 — Malĝusta percent-dekodado de Esperantaj supersignoj en alarm-sugestoj
- **Statuso**: [x]
- **Loko**: `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/data/config/KanalAgordoLeganto.kt:158-167` (`parsuSugestojnPorAlarmoj`; vokata nur en `AppStato.kt:116`; nenie testata)
- **Priskribo**: La anstataŭiga tabelo estas parte malĝusta: `%C4%A5` estas **ĥ** (ne ĵ), `%C5%9C` estas **Ŝ** (ne ŝ), `%C5%AC` estas **Ŭ** (ne Ŝ — kaj la linio aperas dufoje); mankas tute ĝ (%C4%9D), ĵ (%C4%B5), ŝ (%C5%9D), Ĥ (%C4%A4). Estonta etikedo kun ekz. "Ŭaŭde" aperus kiel "Ŝaŭde".
- **Sugesto**: anstataŭigu la manan `.replace`-ĉenon per ĝusta percent-dekodado (UTF-8; atentu, ke `+` → " " aplikiĝas nur kie intencite); aldonu teston kun `+`, `%0A` kaj ĉiuj supersignoj.
- **Takso**: ~10 linioj + testo.

---

## GRAVA

### Sekureco

#### G1 — Tutmonda klara teksto: `usesCleartextTraffic="true"`
- **Statuso**: [x]
- **Loko**: `androidApp/src/androidMain/AndroidManifest.xml:15`; 41 `http://`-URL-oj en la kanalkonfiguro (~22 gastigantoj)
- **Priskribo**: Retatacanto (MITM) sur iu ajn http-fonto povas anstataŭigi la tutan RSS-enhavon: titolojn, HTML-priskribojn (→ WebView, vidu G4), bild-URL-ojn kaj **enclosure-URL-ojn** (→ ExoPlayer, vidu G2). Krome ExoPlayer analizas la elŝutitan medion — historia CVE-surfaco.
- **Sugesto**: anstataŭigu per `res/xml/network_security_config.xml`: `cleartextTrafficPermitted="false"` tutmonde + `<domain-config>`-esceptoj nur por la konkretaj heredaĵaj gastigantoj (kaj plano por https-migrado aŭ la planata arkiv-servilo, `docs/nova/06_servilo_arkivo.md`).

#### G2 — Neniu validado de URL-skemo el RSS antaŭ ol ludi/elŝuti/bildigi
- **Statuso**: [x]
- **Loko**: `androidApp/.../ExoPlayerLudiloRegilo.kt:127-129,182-188` (`MediaItem.setUri`); fluo-el-RSS: `RssParsilo.kt:100-107` (nur `kernpunkto` ricevas http→https); `KtorElshutDeponejo.kt:107`
- **Priskribo**: La defaŭlta `DefaultDataSource` de ExoPlayer **malfermas `file://`, `content://`, `asset://`** — malica aŭ rompita fonto (aŭ MITM per G1) povas igi la apon malfermi lokajn dosierojn / `content://`-URI-ojn por ludi (privateca enketo; la erar-mesaĝo kun la tuta URI poste iras al Sentry). Ktor rifuzas ne-http(s) (malpli riska), Coil denaske ŝovas tiajn URI-ojn.
- **Sugesto**: akceptu nur `https?://` ĉe la limo — plej bone en `RssParsilo` (unu kola punkto, regulo 5 konservita) aŭ antaŭ `setUri`/`get`; `file://` nur por interne konstruitaj vojoj (`Sonfonto.LokaElsendo`).

#### G3 — Eksportita `MainActivity` fidas intent-ekstraĵojn sen kontrolo de la sendinto
- **Statuso**: [x]
- **Loko**: `androidApp/.../MainActivity.kt:69-73,81-116,119-137` (agoj `dk.nordfalk.esperanto.MALFERMI_ELSENDON`, `dk.nordfalk.esperanto.ALARMO_EKIGAS`)
- **Priskribo**: `MainActivity` estas `exported="true"` (necesa, lanĉilo) kaj traktas intent-ekstraĵojn, **kiujn iu ajn apliko povas forĝi** (la agoj estas publikaj en la deponejo). Per tio alia apliko povas: (a) igi la apon ludi ajnan URI-on (inkluzive de `file://`/`content://` — kombinite kun G2), (b) montri atakant-kontrolitan titolo/priskribo/bildo en la apo (navigacio al `ElsendoDetalo` per `PendingElsendoNavigacio.setu`, linio 116 — phishing ene de la apo), (c) per `ALARMO_EKIGAS` altigi la sisteman median laŭtecon al 2/5 (linio 137) kaj startigi ludadon.
- **Sugesto**: la intencoj originatas de niaj propraj nemutablaj PendingIntents — aldonu **po-instalan hazardan nonce-ekstraĵon** (generita kaj konservita en Settings ĉe unua lanĉo) kaj verifiku ĝin en `traktuIntenton`; almenaŭ validu la skemon de `fluo` kaj traktu ĉiujn ekstraĵojn kiel nefidindajn.

#### G4 — WebView por RSS-HTML sen `WebViewClient`; la sanitizilo lasas `intent://`-ligilojn
- **Statuso**: [x]
- **Loko**: `shared/src/androidMain/.../ui/HtmlVido.android.kt:17-35`; sanitizilo: `RssParsilo.kt:425-441` (`puriguHtmlKunEtikedojn`); WebView-reĝimo defaŭlta por 4 kanaloj (`uziWebViewPorElsendo: true`)
- **Priskribo**: `javaScriptEnabled = false` kaj neniu `addJavascriptInterface` (bone), sed **neniu `WebViewClient`**: klako sur `intent://…#Intent;action=…;end` en la priskribo **lanĉas intencon** (defaŭlta WebView-konduto por ne-http-skemoj), kaj http(s)-ligiloj navigas ene de la WebView (uzanto "kaptita"). La sanitizilo forigas `script/style/iframe/object/embed/form/input/button/meta/link` kaj `on*`-atributojn, sed **konservas `<a href>` kun ajna skemo**, kaj la `javascript:`-kontrolo estas uskleco-sentema kaj ne traktas kaŝitan obfuskigon kiel `java\tscript:`.
- **Sugesto**: aldonu `WebViewClient` — en `shouldOverrideUrlLoading` malfermu http/https ekstere (per validita intenco) kaj redonu `true` por ĉio alia; plifortigu la sanitizilon (skemo-blanka listo `href`/`src`, usklec-insistema `javascript:`-kontrolo, forigo de stir-signoj); eksplicite `setAllowFileAccess(false)`.

#### G5 — Sentry misagordita por produktado; aŭskult-datumoj sen filtrado
- **Statuso**: [ ]
- **Loko**: `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/SentryAgordo.kt:16-25`; `MainActivity.kt:48`; `ProtokoloUtil.kt:21-33`
- **Priskribo**: `options.environment = "evoluo"` kaj `tracesSampleRate = 1.0` (100 %) estas koditaj por ĉiuj build-oj inkluzive de release; `initialiguSentry()` estas vokata en **ĉiu** `MainActivity.onCreate` (re-kreoj → averto pri duobla init); Sentry ne estas inicializita en procezoj lanĉitaj de WorkManager/riceviloj. Ĉiuj logoj (kun plenaj URL-oj kaj titoloj — aŭskult-historio, kelkaj gastigantoj uzas subskribitajn URL-ojn kun token-oj) iras al Sentry; neniu filtrado (`beforeSend`/`beforeSendLog`).
- **Sugesto**: `environment` laŭ build-tipo (aŭ lasu la Sentry-Gradle-pluginon); `tracesSampleRate` malpliigu en release; inicialigu unufoje (Application — ligita kun K2); `beforeSend`/`beforeSendLog` por forigi query-ĉenojn/URL-ojn; mencii Sentry en la privateca politiko.

#### G6 — `allowBackup` ne agordita (defaŭlte true)
- **Statuso**: [x]
- **Loko**: `androidApp/src/androidMain/AndroidManifest.xml`
- **Priskribo**: Aŭskult-historio, alarmoj (kun uzant-ŝatimitaj etikedoj) kaj ŝatataj estas sekurkopiataj al la nubo (Auto Backup).
- **Sugesto**: `allowBackup="false"` aŭ `dataExtractionRules` kun ekskludo; mencii en la privateca politiko.

#### G7 — Dependencoj 1–2 jarojn malnovaj; CVE-oj nekontrolitaj
- **Statuso**: [ ]
- **Loko**: `gradle/libs.versions.toml` (media3 1.5.1, ktor 3.1.3, coil 3.0.4, sentry 8.41.0, AGP 8.9.1, work 2.10.1, activity-compose 1.9.3)
- **Priskribo**: Neniu CVE-kontrolo estis ebla (sen reto en la revizia medio). Precipe atentindaj: media3 (analizila surfaco nutrata de nefidindaj URL-oj, vidu G1/G2) kaj ktor.
- **Sugesto**: `./gradlew dependencyCheckAnalyze` + Dependabot en CI; prioritatigu media3 kaj ktor.

### Ludilo

#### G8 — `traktiFinon`: dup-traktada gardilo neatoma kaj ŝlosita sur la malĝusta fonto
- **Statuso**: [x]
- **Loko**: `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/domain/player/LudvicoRegilo.kt:206-215`; servo-voko: `androidApp/.../EsperantoLudadoServo.kt:62-83` (`traktiFinonPublika` vokata:74)
- **Priskribo**: La gardilo (`lastaTraktitaFontoId`, linioj 211/215) komparas la ID de la fonto **legata nun** (`ludilo.stato.value.nunaFonto`), ne la fonton kiu efektive finiĝis. La servo vokatatas sur `Dispatchers.Main`, la observanto sur `Dispatchers.Default`. Se la dua voko alvenas **post** kiam la aŭtoludo jam avancis al e2, la gardilo vidas `e2 != e1`, preterlasas, kaj poste markas **e2 kiel finita (neniam ludita!)** kaj haltigas la ludadon — ĝuste post la aŭtomata daŭrigo, la celo de la tuta ŝanĝo (PR #79). Krome la check-then-set sur `@Volatile` kampo ne estas atoma (du fadenoj povas ambaŭ trapasi → duobla `ludiSekvan` → la sekva elsendo restartiĝas de 0). La parametro `stato` de `traktiFinon` estas entute neuzata.
- **Sugesto**: la gardilo devas ŝlosiĝi sur la *finanta* fonto, kiun la voko portas (`LudantoInformo.nunaFonto` ĉe `Finita`), ne sur la nunan — la servo povas malkomodi ĝin el `player.currentMediaItem.mediaMetadata.extras` (`SonfontoKodilo`, sama procezo). Poste dedupigu sur (finanta fonto) kaj faru la kontrolon atoma (`AtomicReference.compareAndSet` aŭ `synchronized`). Simpla "ĉu la nuna stato ankoraŭ estas Finita/Eraro" **ne sufiĉas** — la servo-voko ofte alvenas antaŭ ol la app-flanka stato flipiĝas.
- **Takso**: meza; vidu ankaŭ G14 (la testo).

#### G9 — IRo-kanalo: neniu ordigas la elsendoliston — inversa ordo kaj malĝusta aŭtoluda direkto
- **Statuso**: [x]
- **Loko**: `ElsendoDeponejoImpl.kt:38-78` (neniu `sortedByDescending`); `KanalEkrano.kt:209` (`groupBy` konservas fluo-ordon); `LudvicoLogiko.kt:60-75` (KDoc supozas "plej-freŝe-unue")
- **Priskribo**: Verifikita per reto: la IRo-fluo (`fluo.muzaiko.info`, slug `iro`) listigas plej malnovajn unue (57 eroj). Sekvoj: (a) la kanalo montras elsendojn plej-malnovajn supre (ankaŭ la horizontala svipo iras malĝustadirekten); (b) aŭtoludo post fino de IRo-elsendo saltas al **pli nova** elsendo (malĝusta direkto kaj saltos ne-aŭskultitajn pli malnovajn). `HejmoEkrano` kaj `AlarmoElekto` ordigas lokde, do nur la kanala vido kaj prioritato 1 estas trafitaj.
- **Sugesto**: ordigu en la deponejo, unu loko: `parsilo.parsuRss(...).sortedByDescending { it.dato }` en `sxargxiElsendojn` kaj `leguKashitajnElsendojn`. La ID-oj estas kalkulitaj dum la parsado (sendependaj de la ordo), do tio estas sekura; la oraj parser-testaj estas netuŝitaj.
- **Takso**: 2 linioj + testo (fluo en inversa ordo → ordigita en la deponejo).

#### G10 — Elŝutoj: triligita elŝuto estas markata "Preta"
- **Statuso**: [x]
- **Loko**: `shared/src/androidMain/.../data/repository/KtorElshutDeponejo.kt:118-141`
- **Priskribo**: `channel.readAvailable(buffer)` redonas `<= 0` ĉe frua EOF; la ciklo rompiĝas kaj linio 131 metas `ElshutStato.Preta` — sen kontroli `elshutitaj == totalajBitokoj`. Se la servilo fermas la konekton frue (aŭ `contentLength` malpravas), la dosiero estas nemankebla kaj ankoraŭ persistita kiel kompleta (JSON-metadatenoj) — do ĝi neniam estos re-elŝutata.
- **Sugesto**: post la ciklo, se `totalajBitokoj > 0 && elshutitaj != totalajBitokoj` → `Eraro("Triligita: X/Y bajtoj")` kaj forigo de la parta dosiero.

#### G11 — Elŝutoj: partaj dosieroj neniam purigitaj; nesekuraj mapoj
- **Statuso**: [x]
- **Loko**: `KtorElshutDeponejo.kt:44-45,86,98,137-141,157-165`; `rekargxiElshutojn`:60-80 (forigas nur orfajn JSON-ojn, neniam orfajn MP3-ojn)
- **Priskribo**: (a) Ĉe eraro (linio 137) aŭ `haltigi` (Paŭzita) la parta MP3 restas sur disko — partaj dosieroj akumuliĝas por eterne. (b) `_statoj` kaj `joboj` estas simplaj `mutableMapOf` (LinkedHashMap), aliritaj samtempe el `Dispatchers.IO`-korutinoj (plurfadena!) kaj el la UI-fadeno — rasa kondiĉo (HashMap-korupto eblas).
- **Sugesto**: forigi la partan dosieron en `catch`/`haltigi`; purigi orfajn MP3-ojn ĉe reŝargo; uzi `ConcurrentHashMap` (aŭ `synchronized`).

#### G12 — Elŝutoj: `getExternalFilesDir()` povas redoni `null`
- **Statuso**: [x]
- **Loko**: `shared/src/androidMain/.../data/repository/KreuElshutDeponejo.kt:5`
- **Priskribo**: Se ekstera stokado ne estas muntita, `getExternalFilesDir` redonas `null` kaj `File(null, ...)` fariĝas **relativa vojo** ("EsperantoRadio" en la laboratoria dosierujo de la procezo) — elŝutoj iras al neatendita loko aŭ malsukcesas neklarmaniere.
- **Sugesto**: `getExternalFilesDir(...) ?: appContext.filesDir` (kun subdosierujo).

#### G13 — UI vokas `ludilo.halti()`/`ludi()` rekte, preterpasante `LudvicoRegilo`
- **Statuso**: [ ]
- **Loko**: `MiniLudilbreto.kt:191` (halti); `ElsendoEkrano.kt:171,330` (paŭzi/daŭrigi); `MainActivity.kt:82-99` (sciigo "Malfermi" → rekta `fiksiFonton`+`ludi`); `App.kt:170,180` (rekta kanalo)
- **Priskribo**: Sekvoj: neni `nuliguReprovon`, neni `malmarkiFinita` (finita elsendo restas finita en la deponejo), neni rekomencigo de `erarojSinsekvaj`. La fonto-kontrolo en la reprovo-tasko parte ŝirmas, sed K3 montras, ke tio ne sufiĉas.
- **Sugesto**: aldonu `LudvicoRegilo.haltuLudadon()` kaj uzu `ludiElsendon` ĉie; por rekta kanalo akceptendas rekta `fiksiFonton`, sed nuligu reprovon.

#### G14 — La testo `servoVoko_duoblaTraktadoDeSamaFonto_estasIgnorata` ne testas la gardilon
- **Statuso**: [x]
- **Loko**: `shared/src/commonTest/kotlin/dk/nordfalk/esperanto/domain/player/LudvicoRegiloTest.kt:393-416`
- **Priskribo**: Post la unua voko la aŭtoludo (Unconfined-scope, `kreuRegilon`:59) jam startigis e2; la dua voko vidas **alian** fonton (e2), la gardilo ne blokas, e2 estas markita kiel finita kaj la ludado haltas — kaj la aserto (`stato == Ludas || stato == Haltita`) pasas en ambaŭ okazoj. La testo nenion distingas, kaj ĝia nomo estas falsa: la reala dup-trakta vojo (ambaŭ vokoj vidas la saman finantan fonton) estas netestata; la testo eĉ sankcias la malbonan rezulton (e2 markita finita). La testo pasus eĉ sen la gardilo.
- **Sugesto**: restructuru la teston kun la "finanta fonto" (vidu G8) aŭ per ludilo kiu ne ŝanĝas la staton ĉe `ludi()`; asertu `!ludatoj.estasFinita(e2.id)` kaj ke la ludado daŭras post la dua voko.

#### G15 — Regulo 6.6 (`puriguModeloj`) dokumentita sed ne implementita
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:142-176` (`parsuVarsoviaVento`), `:356-380` (`parsuVinilkosmo`); dokumento: `docs/nova/04_parsado_kaj_arkivo.md:89-93`
- **Priskribo**: `puriguModeloj` nenie ekzistas (nek en la `Kanalo`-modelo, nek en la JSONC, nek aplikata en la parsilo). La malnova kodo aplikis 9 regexojn al Varsovia Vento (Facebook-reklamo, "Elŝutu podkaston", "Subtenu nin", `<audio …></audio>`) kaj forigis `<p class="who">` ĉe Vinilkosmo. Sekvoj en la nova apo: Varsovia Vento (`uziWebViewPorElsendo=true`) montras **enkonstruitajn son-ludilojn** en la priskribo; Vinilkosmo-priskriboj komenciĝas per `<p class="who">VINILKOSMO … has posted:</p>`; la plata `priskribo` enhavas la tutan reklam-tekstegaĵon.
- **Sugesto**: efektivigu regulo 6.6 kiel dokumentite — aldonu `puriguModeloj: List<String>` (regex) al `KanaloDto`/`Kanalo` kun la malnovaj modeloj por `varsoviavento` kaj `vinilkosmo` en la JSONC (ambaŭ kopioj), kaj apliku ilin en la parsilo; aŭ almenaŭ forigu `audio`-elementojn el `priskriboHtml` por Varsovia Vento kaj la `who`-alineon por Vinilkosmo; aŭ korektu la dokumenton.

#### G16 — id-skemo `slug:dato:guid` malkongruas kun la dokumentita konvencio kaj rompas la servilan rondiron
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:122-123` (ĝenerala), `:283` (Peranto); dokumento: `docs/nova/04` ("Id-konvencioj"); malnova: `RomePodcastParser.parseAndre:191`
- **Priskribo**: La dokumento kaj la malnova kodo uzas la guid sen prefikso (`<slug>:<dato>`); la kodo uzas `<slug>:<dato>:<guid>`. Sekvoj: (a) rondiro kun la planita servilo: la servilo skribos `Elsendo.id` kiel `<guid>`, la apo re-parsas kaj denove prefiksas → la id kreskas ĉiun fojon kaj la inkrementa kunfando (`e.id !in konataj`, `docs/nova/06`) neniam trafas; (b) estonta migrado de uzantdatumo (elŝutoj, ludpozicioj, plejŝatataj estas ŝlositaj per `id`) rompiĝus; (c) la dokumenta tabelo estas malĝusta.
- **Sugesto**: uzu la guid senŝanĝa se ĝi jam komenciĝas per `<kanaloSlug>:` (aŭ ĉiam por `deArkivo`), alie `<kanaloSlug>:<guid>` (unu prefikso); kaj vicigu la dokumenton kaj la kodon.

#### G17 — Mankanta komo en `FORPRENITAJ_KANALOJ` (latenta parser-risko)
- **Statuso**: [x]
- **Loko**: `esperantoradio_kanaloj_v9.json:331-332` (inter `radioverda` kaj `vej`; **ambaŭ kopioj**: `shared/src/commonMain/resources/` kaj `androidApp/src/androidMain/assets/`)
- **Priskribo**: `}` rekte sekvas `{` sen komo; ĉiuj aliaj eroj uzas `,{`. La `isLenient`-a leganto toleradas ĝin (kontrolita per la reala kompilita leganto: 27 kanaloj), sed la dosiero estas intencita kiel valida JSONC kaj estonta parser-ŝanĝo povus malakcepti ĝin.
- **Sugesto**: aldonu la komon (unu signumo, ambaŭ kopioj).

### Pliaj GRAVA trovoj (ludilo, platformoj)

#### G18 — Desktop: daŭrigo post paŭzo nuligas la pozicion
- **Statuso**: [ ]
- **Loko**: `shared/src/desktopMain/.../domain/player/DesktopLudiloRegilo.kt:169-186` (`komenciLudadon`, `totalBytesLuditaj = 0L`:180), `:222` (`ludi`)
- **Priskribo**: `komenciLudadon()` faras `ludaJob?.cancel()` kaj novan korutinon kun `totalBytesLuditaj = 0L`; `ludi()` vokas ĝin ankaŭ post paŭzo → post paŭzo+daŭrigo la pozicio rekomencas je 0 dum la fluo daŭras meze. Sekvoj: la UI montras 0:00, kaj la 5-sekunda savanto de `LudvicoRegilo` **superskribas la ĝustan konservitan pozicion per la malĝusta malgranda valoro** — "Daŭrigi de X:XX" koruptiĝas.
- **Sugesto**: ne nuligu `totalBytesLuditaj` ĉe daŭrigo (konservu `bytesBeforePause`); aŭ ne kanselu la paŭzitan taskon, nur malmarku `pauxzita`.

#### G19 — Desktop: eraro ĉe `fiksiFonton` portas `nunaFonto = null`
- **Statuso**: [ ]
- **Loko**: `DesktopLudiloRegilo.kt:159-165` (catch), `:76` (`halti()` ene de `fiksiFonton`)
- **Priskribo**: La `catch` faras `_stato.value.copy(stato = Eraro(...))`, sed antaŭe `halti()` jam metis `nunaFonto = null`. La stato `Eraro` do ne portas la fonton → `LudvicoRegilo.provuReprovi`/`traktiFinon` legas `lastaFonto` (la *antaŭa* elsendo!) → sur Desktop: 404 ne markas la elsendon erara, aŭto-ludo saltas al "plej freŝa neludata" (prioritato 2/3) anstataŭ la sekva samkanala, kaj povas eĉ reprovu la *malĝustan* (antaŭan) elsendon.
- **Sugesto**: en la `catch` elsendu `LudantoInformo(stato = Eraro(...), nunaFonto = fonto, pozicioMs = komencoPozicioMs, ...)` anstataŭ `.copy` de la haltita stato.

#### G20 — wasmJs: pozicio neniam ĝisdatatas dum ludado
- **Statuso**: [ ]
- **Loko**: `shared/src/wasmJsMain/.../domain/player/WasmJsLudiloRegilo.kt:40-67`
- **Priskribo**: Nur la eventoj `playing`, `pause`, `ended`, `error`; **mankas `timeupdate`**. `pozicioMs` restas je la valoro de la `playing`-evento → la progreso-linio/serĉbreto frostiĝas, kaj la 5-sekunda savanto de `LudvicoRegilo` konservas malnovan pozicion → rekomenco ("Daŭrigi de X:XX") estas malĝusta sur la reto.
- **Sugesto**: aldonu `timeupdate`-aŭskultanton (aŭ 1s `setInterval`), kiu metas `_stato.value.copy(pozicioMs = …)`.

#### G21 — wasmJs: `audio.play()`-promeso ne estas traktata
- **Statuso**: [ ]
- **Loko**: `WasmJsLudiloRegilo.kt:69`
- **Priskribo**: `audio?.play()` redonas `Promise`; malakcepto (aŭtomata ekigo blokita de la retumilo, CORS) **ne** ekigas la `error`-eventon → la stato restas `Konektas` eterne, sen reprovu nek aŭtoludo.
- **Sugesto**: traktu la promeson (`.then {}.catch { … }` per `asDynamic()`), kaj en `catch` emitu `Eraro(reprovebla = true)`.

#### G22 — `LudiElsendoReceivilo` likas `MediaController`-on
- **Statuso**: [ ]
- **Loko**: `shared/src/androidMain/.../data/repository/LudiElsendoReceivilo.kt:70-77`
- **Priskribo**: `future.get()?.apply { setMediaItem; prepare; play }` — la regilo **neniam estas `release()`-ata**. Ĉiu "Ludi"-klako en sciigo likas unu `MediaController` + Binder-konekton al la servo (procesa vivdaŭro).
- **Sugesto**: konservu la referencon kaj vokinu `controller.release()` post la komandoj (ili estas en la sama fadeno, do liveriĝas antaŭ la liberigo).

#### G23 — F-Droid-metadato malaktuala
- **Statuso**: [x]
- **Loko**: `fdroid/metadata/dk.nordfalk.esperanto.radio.yml:44-51` (`CurrentVersion: 3.0.0`, `CurrentVersionCode: 244`, `Builds … versionCode: 244`)
- **Priskribo**: La kodo estas `apoversio = 3.0.2` / `versionCode = 246` (`gradle/libs.versions.toml:6`, `androidApp/build.gradle.kts:59`). Ĉe la venonta F-Droid-konstruo la metadato kontraŭdiros la kodon.
- **Sugesto**: ĝisdatigu al 3.0.2/246 (aŭ la konvena) kaj aldonu la kontrolon al la eldon-procezo (AGENTS.md "Eldono de nova versio").

#### G24 — CI: neniu laborfluo rulas testojn aŭ konstruon por PR-oj
- **Statuso**: [x]
- **Loko**: `.github/workflows/` (nur `changelog-kontrolo.yaml` kaj `eldonado.yml`)
- **Priskribo**: 229 testoj kaj 4 platformoj ekzistas, sed PR povas enkonduki rompitajn testojn aŭ nekompileblan kodon sen ia rimarko — la eldonado (etikedo `v*`) konstruas, sed tio estas tro malfrue.
- **Sugesto**: aldonu `testoj.yml` (`on: pull_request`) kiu rulas `./gradlew :shared:desktopTest` kaj kompil-kontrolojn (`:desktopApp:compileKotlinDesktop`, `:androidApp:assembleDebug`, eventuale wasmJs-kompilo); pli longe: androidTest per emulilo.

#### G25 — Etikedo `v3.0.2` montras al commit, kiu ne estas sur master
- **Statuso**: [ ]
- **Loko**: git-etikedo `v3.0.2` → `16ae6d6` (gepatro `39dd2d8`); la master-a eldona komito estas `9e835b9` (squash-merge de la sama PR)
- **Priskribo**: `git merge-base --is-ancestor 16ae6d6 HEAD` → ne. La enhavo de la APK estas identa (nur dokumentaro malsamas), sed la dokumentita proceduro ("etikedo sur la eldona komito") kaj F-Droid (konstruas el la etikedo) celas la master-an komiton, kaj la etikedo estas neatingebla de `master`.
- **Sugesto**: rekrei la etiketon sur `9e835b9` kaj puŝi (`git tag -f v3.0.2 9e835b9 && git push -f origin v3.0.2`) — **atentu, tio estas re-verkado de publika etikedo (ekstera ŝanĝo, bezonas vian aprobon)**.

#### G26 — `CancellationException` englutiata (struktura konkurenteco)
- **Statuso**: [x]
- **Loko**: `LudvicoRegilo.kt:182` (reprovo-tasko), `:260` (`ludiSekvan`); `ElsendoDeponejoImpl.kt:112` (`leguKashitajnElsendojn`); `LudvicoRegilo.kt:169-173` (`provuReprovi` kaptas nur `Exception` — wasmJs `kotlin.Error` eskapas)
- **Priskribo**: Sur JVM `CancellationException` estas `RuntimeException`; engluti ĝin rompas la korutinan kontrakton: la tasko plenumas plu en nuligita kunteksto, protokoliĝas kiel falsa eraro, kaj povas mem plani novan reprovon post `nuliguReprovon()`. La projekto konas la ĝustan ŝablonon (`ElsendoDeponejoImpl.kt:77-79`: `catch (CancellationException) { throw e }` + `catch (Throwable)`).
- **Sugesto**: aldonu `catch (e: CancellationException) { throw e }` antaŭ `catch (e: Exception)` (kaj prefere `Throwable` + reĵeto de `CancellationException` por wasmJs-konformeco) en la cititaj lokoj.

#### G27 — `erarojSinsekvaj` ne rekomenciĝas ĉe sukcesa ludado
- **Statuso**: [x]
- **Loko**: `LudvicoRegilo.kt:93` (deklaro), `:227-236` (pliigo), `:338` (nuligo nur en `ludiElsendon`)
- **Priskribo**: Aŭtoludo uzas `ludiElsendonInterna` — do post 10 *akumulitaj* (ne sinsekvaj) eraroj dum longa seanco (ĉiu sekvita de sukcesa ludado) la regilo haltas ("eterna buklo"-protekto) kaj la uzanto devas premi ludon permane.
- **Sugesto**: nuligu `erarojSinsekvaj` ankaŭ en la branĉo `Ludas` (sukcesa (re)komenco = ne plu sinsekvaj eraroj).

#### G28 — Alarmo sen ekzakta-alarma permeso povas esti tute silenta
- **Statuso**: [ ]
- **Loko**: `shared/src/androidMain/.../data/repository/AlarmoReceivilo.kt:34-69` (`startActivity`:62, WakeLock liberigita:67-68); `AlarmoSkedilo.kt:38-48` (`setWindow` sen `SCHEDULE_EXACT_ALARM`)
- **Priskribo**: `context.startActivity(launchIntent)` el fona `BroadcastReceiver` povas esti blokita de la limigoj pri lanĉo de agado el fono (Android 10+, precipe ĉe vekiĝo kun elŝaltita ekrano). La `catch` nur protokolas — kaj la fallback-ringtono ekzistas **nur** en `MainActivity`, kiu tiam neniam lanĉiĝas. Do: alarmo ekigas, sed nenio sonas. Krome la WakeLock (10-minuta tempolimo) estas liberigita en `finally` tuj post `startActivity` — ĝi ne kovras la nesinkronan "trovi elsendon kaj ludi"-fazon.
- **Sugesto**: (a) en la `catch` de la lanĉo, ludu la fallback-ringtonon rekte en la ricevilo; (b) pli bone, uzu full-screen-intent-sciigon kun `setAlarmClock`-info (aŭ `setTurnScreenOn(true)` / `setShowWhenLocked`), kiel rekomendas la dokumentaro pri alarm-apoj; transdonu la WakeLock al `MainActivity` kaj liberigu ĝin kiam la ludado komenciĝas (aŭ post timeout). Testu sur reala aparato kun elŝaltita ekrano.

### Pliaj GRAVA trovoj (parsado, kerno)

#### G29 — Vinilkosmo: titolo derivata el la enhavo anstataŭ `<title>`
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:372` (`titolo = puriguHtml(priskriboKruda).take(200)`)
- **Priskribo**: La fiksaĵo kaj ambaŭ malnovaj vojoj uzis la `<title>` de la ero. La derivita titolo komenciĝas per `VINILKOSMO "Esperanto-Muzik-Prod." has posted: Esperanto: …` — klare malbona; la agordo ne havas `elsendojRssIgnoruTitolon` por `vinilkosmo`; la titolo ankaŭ ne estas `trim`-ita (male al `derivuTitolon`, `RssParsilo.kt:395-398`).
- **Sugesto**: `titolo = ero.selectFirst("title")?.text()?.trim() ?: ""`, kaj derivu nur se `kanalo.ignoruTitolon`.

#### G30 — Ĝenerala `<audio>`-fallback (regulo 6.1.4) ne funkcias por CDATA/eskaptita enhavo
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:105` (`ero.selectFirst("audio source")` sur la XML-dokumento); atendata: `Ksoup.parse(htmlEnhavo)` (kiel en `parsuVarsoviaVento`, linioj 153-157); ankaŭ: `<enclosure>` prenata sen kontrolo de `type="audio/*"` (linio 100)
- **Priskribo**: Enhavo de `content:encoded`/`description` estas teksta nodo, ne elementoj — do la fallback trovas `<audio>` nur ĉe `type="xhtml"`-inline-enhavo. Por iu ajn ĝenerala fluo kiu metas la sonon nur en `<audio>` ene de la priskribo, la ero estas **forĵetita** (linio 106 `return null`). Neniu aktiva kanalo dependas de tio nun, sed la regulo estas dokumentita kaj la estonta servilo/normigitaj fluoj baziĝos sur la ĝenerala parsilo.
- **Sugesto**: `Ksoup.parse(priskriboKruda).selectFirst("audio source")?.attr("src")`; akceptu nur `enclosure[type^=audio]` (`?: enclosure` defaŭlte); aldonu teston (neniu nuna testo kovras la fallback).

#### G31 — https-korekto nur por `kernpunkto`, kontraŭ la dokumento "ĉiam"
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:109-111` (`if (kanalo.slug == "kernpunkto" && fluo.startsWith("http://"))`); dokumento: `docs/nova/04`, regulo 6.1.6
- **Priskribo**: Aliaj kanaloj kun `http://`-enclosure restus http (Android blokus ilin per cleartext-protekto; ankaŭ miksita enhavo en la UI). La nuna konfiguro uzas `https://` ĉie (kontrolita supraĵe), do nuntempe ne aktiva — sed la dokumento kaj la kodo kontraŭdiras.
- **Sugesto**: korektu ĉiam (krom se iu kanalo eksplicite bezonas http — tiam per konfiguro), kaj aldonu ĝeneralan teston; aŭ korektu la dokumenton.

#### G32 — Ĝenerala skip-listo mankas en la nova parsilo (regulo 2)
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:82-129` (`parsuEroGxenerala`); malnova: `malnova/parse/.../RomePodcastParser.kt:140-164`; dokumento: `docs/nova/04:152`
- **Priskribo**: La malnova kodo saltis erojn kies sola son-gastiganto estas youtube/soundcloud/vimeo/audioboom/yourlisten/vocaroo. La nova ĝenerala parsilo **akceptas** ekzemple `<enclosure url="https://www.youtube.com/watch?v=…">` kiel validan fluon — tia elsendo aperus en la listo kaj fiaskus ĉe ludado. La skip-regulo ekzistas nur en la Peranto-regulo (`RssParsilo.kt:240-248`), malmulte kodita. AGENTS.md regulo 2: "la skip-listo devas esti konservita".
- **Sugesto**: komuna funkcio `estasSaltitaGastiganto(url: String)` en `RssParsilo` (aŭ agorde, se oni volas regulo 5), aplikata en `parsuEroGxenerala`, plus testoj (youtube/soundcloud/vimeo-enclosure → forĵetita).

#### G33 — archive.org-metadaten-petoj: senlima paraleleco (regulo 6.3, paŝo 2)
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:270` (`identigiloj.map { id -> async { … } }.awaitAll()`)
- **Priskribo**: La unua parsado de la Peranto-fluo (centoj da arkivaĵoj) povas lanĉi centojn da paralelaj petoj al archive.org → risko de 429/timumejo. La degrado estas bone traktata (5-s limtempo, fallback al la divenita nomo, neniam kaŝita fallback), sed multaj elsendoj provizore havos malĝustan (404-an) URL.
- **Sugesto**: limigu la paralelecon (ekz. `Semaphore(8)` aŭ `chunked`).

#### G34 — Diskkaŝa re-parsado sen `httpKliento` → Peranto ricevas divenitajn URL-ojn
- **Statuso**: [ ]
- **Loko**: `ElsendoDeponejoImpl.kt:107` (`parsilo.parsuRss(respondo, kanalo)` sen la kliento; kontraste kun linio 71)
- **Priskribo**: Ĉe malvarma startigo el disko, `trovuArchiveOrgMp3Dosiernomon` (`RssParsilo.kt:236-272`) nur konsulas la persistentan `ArchiveOrgDosiernomoKasho`; se tiu estas malplena (nova instalo kun konservita kaŝo, aŭ viŝita Settings), la fluoj estas la **divenitaj** — malĝustaj ĝuste por la kazoj kiel `malapero-benda-3` (la celo de commit 8c5ed4c) — ĝis la unua sukcesa refreŝo.
- **Sugesto**: transdoni la klienton (kun la sama 5s-limigo) kaj la kaŝon ankaŭ en `leguKashitajnElsendojn`.

#### G35 — `kaŝmemoro`/`fluoj` en `ElsendoDeponejoImpl` estas simplaj `mutableMapOf` sen Mutex
- **Statuso**: [ ]
- **Loko**: `ElsendoDeponejoImpl.kt:31-32`
- **Priskribo**: Ĉiuj metodoj estas `suspend` kaj nenio devigas unu-fadenan aliron; `getElsendo`/`sercxiElsendojn` iteracias la mapon dum alia korutino povas skribi (`put` en `sxargxiElsendojn`/`leguKashitajnElsendojn`). Nuntempe ĉio ŝajnas esti vokata el la ĉefa fadeno (ViewModel-oj), do la risko estas latenta — sed la klaso estas `open` kaj estonta vokanto el alia fadeno (ekz. WorkManager, vidu K2) povus kaŭzi `ConcurrentModificationException` aŭ perdon de skriboj.
- **Sugesto**: Mutex (kiel en `ArchiveOrgDosiernomoKasho`) aŭ dokumentita unu-fadena devigo.

#### G36 — `PersistaLudatojDeponejo`: read-modify-write sen Mutex + sinkrona diska skribo ĉiujn 5 s
- **Statuso**: [ ]
- **Loko**: `shared/src/commonMain/.../data/repository/PersistaLudatojDeponejo.kt:38,45-60`
- **Priskribo**: `registriPozicion` estas vokata ĉiujn 5 s dum ludado (el `LudvicoRegilo`) kaj povas interplektiĝi kun `markiFinita`/`malmarkiFinita` el alia korutino: ambaŭ legas `_ludatoj.value`, ambaŭ skribas — la lasta skribanto venkas kaj la `finita`-flago povas perdiĝi. Krome `skribu()` serializas la tutan mapon kaj skribas al Settings ĉe ĉiu pozici-ĝisdatigo — sur Desktop tio estas sinkrona diska I/O ĉiujn 5 s.
- **Sugesto**: Mutex ĉirkaŭ la read-modify-write; prokrasto de la persisto (ekz. maks. unufoje per 15-30 s kaj ĉe halto/paŭzo); limigo de la map-grandeco (LRU).

### Pliaj GRAVA trovoj (testoj, UI)

#### G37 — `DiskKashoTest` poluas la veran uzantan kaŝujon
- **Statuso**: [x]
- **Loko**: `shared/src/commonTest/.../data/repository/DiskKashoTest.kt`; `desktopMain/.../DosierKasho.kt:11` (`dosierKashoBazo` "interne ŝanĝebla por testoj" — neniu testo ŝanĝas ĝin)
- **Priskribo**: La testoj vokas `skribuKashon(slug, …)` rekte kaj la `finally`-purigo estas `skribuKashon(nomo, "")` — tio **malplenigas la dosieron, ne forigas ĝin**. Pruvita: en `~/.esperantoradio/cache/` troviĝas 5 malplenaj dosieroj (`test_diskkasho_*.rss`).
- **Sugesto**: en `DiskKashoTest`, starigi `dosierKashoBazo` al provizora dosierujo (JUnit `@TempDir`) kaj efektive forigi la dosierojn en `@AfterTest`.

#### G38 — La vera kanalkonfiguro (27 kanaloj) nenie estas testata
- **Statuso**: [x]
- **Loko**: `KanalAgordoLegantoTest` (5 testoj, nur sinteza JSONC); `KanalDeponejoImpl` (legas la veran dosieron) — neniu testo
- **Priskribo**: Se oni redaktas la JSONC kaj enmetas eraron — ekz. blok-komenton `/* */` (la striptigilo subtenas nur `//`, vidu C8) — la apo kraŝus ĉe ekfunkciigo kaj neniu testo kaptus. La konfiguro estas la kerno de la apo (AGENTS.md regulo 5).
- **Sugesto**: "smoke test" en `commonTest`: legi la veran dosieron el la resurcoj, aserti ke ĝi parsiĝas, havas 27 kanalojn, ĉiuj havas kodon kaj nomon, almenaŭ unu havas RSS-URL, ktp.

#### G39 — `RssParsiloTest` regulo 6.2 (Varsovia Vento) testita nur supraĵe
- **Statuso**: [ ]
- **Loko**: `shared/src/commonTest/.../data/parser/RssParsiloTest.kt:76-99` (`parsasVarsoviaVenton`)
- **Priskribo**: La kerna aserto estas `if (plurpartaj.isNotEmpty()) { … }` — se la fiksaĵo iam perdus plurpartajn erojn, la testo pasus sen kontroli ion ajn pri ili. Pruvita: la fiksaĵo havas 10 erojn → **31 elsendojn** (9 eroj × 3 partoj + "La 182a elsendo" × 4 partoj), kun konataj fluoj (`250424VVE185P1/P2/P3.mp3`). La dokumento (`docs/nova/04:150`) atendas precize tion.
- **Sugesto**: `assertEquals(31, elsendoj.size)` + konkretaj asertoj por "La 185a elsendo": tri id-oj `varsoviavento:2025-04-24:1/2/3`, titoloj "La 185a elsendo 1a parto" ktp., fluoj finiĝantaj per `250424VVE185P1/P2/P3.mp3`.

#### G40 — `PersistaLudatojDeponejo` kaj `PersistantaPlejŝatatajDeponejo` estas netestitaj
- **Statuso**: [x]
- **Loko**: `shared/src/commonMain/.../data/repository/PersistaLudatojDeponejo.kt` (111 linioj), `PersistantaPlejŝatatajDeponejo.kt`
- **Priskribo**: `PersistaLudatojDeponejo` konservas la ludpoziciojn ("Daŭrigi de X:XX" trans restartoj) — kerna funkcio — sed neniu testo kontrolas: pozicio konservita kaj re-legita (kiel post restarto), korupta JSON → malplena (la `runCatching` en `legu()`), `malmarkiFinita`, `registriPozicion` konservas `finita`/`erara`. (Modelon donas `PersistantaAlarmoDeponejoTest` en `desktopTest` — 3 testoj kun vera persisto.)
- **Sugesto**: aldoni `desktopTest` laŭ la modelo de `PersistantaAlarmoDeponejoTest` (ambaŭ deponejojn).

#### G41 — `RssArkivServer-filcache/` kontraŭdiro: la oraj fiksaĵoj ne estas reprodukteblaj el la repo
- **Statuso**: [ ]
- **Loko**: `.gitignore` (`/RssArkivServer-filcache/`) kontraŭ AGENTS.md ("NE versiigu") kaj `docs/nova/04:141` ("Kopiu la kaŝenitajn fluojn el `RssArkivServer-filcache/`")
- **Priskribo**: Sekve nur 4 fiksaĵoj estas en `shared/src/commonTest/resources/feeds/` (kernpunkto, peranto, varsoviavento, vinilkosmo), dum la ora test-tabelo priskribas pliajn fiksaĵojn — interalie anchor.fm "La Malfamuloj" (≥114 elsendoj, `docs/nova/04:153`), kiu **mankas** en la repo. La plena aro de golden fixtures ne estas reproduktebla el la repo.
- **Sugesto**: solvi la kontraŭdiron: aŭ versiigu la fiksaĵojn (sen sekretoj), aŭ korektu AGENTS.md/dokumenton ("ne reproduktebla el la repo") kaj kopii la mankantajn fiksaĵojn (precipe anchor.fm) al `resources/feeds/`.

#### G42 — Serĉo: sen debounce, sen escept-traktado, konkurantaj korutinoj
- **Statuso**: [ ]
- **Loko**: `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/ui/SercxoEkrano.kt:29-40`
- **Priskribo**: Ĉiu klavpremo lanĉas novan `scope.launch { rezultoj.value = sercxoDeponejo.sercxi(it) }`. La korutinoj konkuras — la rezulto povas veni en malĝusta ordo (malrapida serĉo por "abc" povas fini post rapida por "abcd"). Se `sercxi` ĵetas escepton, la korutino kraŝos kaj la aplikaĵo povas kraŝi (`rememberCoroutineScope` sen `CoroutineExceptionHandler`).
- **Sugesto**: uzu `LaunchedEffect(teksto)` kun `delay(300)` (debounce) kaj `try/catch` kun `loge` kaj malplena rezulto ĉe eraro.

#### G43 — `AlarmoRedaktilo` kaj serĉo: stato perdiĝas ĉe ekranturno
- **Statuso**: [ ]
- **Loko**: `AlarmoEkrano.kt:235-238` (`remember` sen `rememberSaveable`); `SercxoEkrano.kt:23-25` (`teksto`, `rezultoj`, `sxargxas`)
- **Priskribo**: Se la uzanto turnas la ekranon dum redactado/kreado de alarmo, la tajpita horo, minuto, elektita kanalo kaj ripeto perdiĝas; same la serĉteksto kaj rezultoj.
- **Sugesto**: `rememberSaveable` (kun `Saver` se necese) aŭ almenaŭ `remember(ekzistanta?.id)`.

---

## KONSILO

### Sekureco / Sentry / build

#### C1 — Sentry: duobla kapturo de la sama fiasko; atendataj eraroj iĝas erarnivelaj eventoj
- **Statuso**: [ ]
- **Loko**: `ProtokoloUtil.kt:41-46`; `ElsendoDeponejoImpl.kt:83-89`; kap-komento `ProtokoloUtil.kt:7` diras "ĉiu log*-voko aldonas Sentry-breadcrumb", sed `logd` kaj la 3-argumentaj `logw`/`loge` ne faras tion
- **Sugesto**: por atendataj eraroj uzu `logw(tag, msg, e)` (loka protokolo sen Sentry-kapturo) kaj evitu la duoblan kaptilon; vicigu la komenton.

#### C2 — `socketTimeoutMillis` mankas en la Ktor-kliento
- **Statuso**: [ ]
- **Loko**: `AppStato.kt:94-95` (`requestTimeoutMillis = 30_000`, `connectTimeoutMillis = 10_000`)
- **Sugesto**: aldonu `socketTimeoutMillis = 30_000` (blokita/malrapida korpo povas teni la peton viva pli longe ol la dezirata limo).

#### C3 — `DosierNomoj.sanigiDosiernomon`: neniu long-limigo, nul-bajtoj, Windows-rezervitaj nomoj
- **Statuso**: [ ]
- **Loko**: `shared/src/commonMain/.../data/repository/DosierNomoj.kt:8`
- **Priskribo**: Path-traversal estas bone malebligita (`:/\|?*"<>|` → `-`, do `../` iĝas `..-`), sed: neniu long-limigo (RSS-id-oj ofte estas plenaj URL-oj → >255 bajtoj → elŝuto malsukcesas), neniu traktado de nul-bajtoj, Windows-rezervitaj nomoj (`CON`, `PRN`) kaj finaj punktoj (Desktop sur Windows).
- **Sugesto**: limigu la longon (hash + mallonga prezervaĵo), kaj defendpe: `File(hejjo, nomo).canonicalFile.parentFile == hejjo.canonicalFile`.

#### C4 — `Alarmo` sen validigo de horo/minuto/ripeto; korupta JSON anstataŭigata
- **Statuso**: [ ]
- **Loko**: `PersistantaAlarmoDeponejo.kt:63`; `domain/model/Alarmo.kt:54` (`Alarmo.sekvaEkigo` konstruas `LocalTime(horo, minuto)`)
- **Priskribo**: Ĉe ekstervalora horo (ekz. 99 el malbona konfiguro) `LocalTime` ĵetas `IllegalArgumentException` kaj povas kraŝi la planadon. Se la persistita JSON estas korupta, `legu()` revenas al la sugestoj kaj la sekva `persistu()` anstataŭigas la koruptan valoron — la uzantaj alarmoj estas definitive perditaj.
- **Sugesto**: validigu/koercu en `Alarmo.init` aŭ en `parsuSugestojnPorAlarmoj`; konservu la krudan valoron kiel rezervon anstataŭ anstataŭigi ĝin.

#### C5 — Subskrib-konfiguro: defaŭlta absoluta keystore-vojo en publika deponejo
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:47` (`storeFile = file(System.getenv("KEYSTORE_PATH") ?: "/home/j/android/A_signaturer/jacobnordfalk.keystore")`, pasvortoj defaultas al `""`)
- **Sugesto**: forigu la hardkoditan vojon; ĵetu eraron se `KEYSTORE_*` mankas ĉe release-konstruo (fail-fast). (La CI `eldonado.yml` faras tion ĝuste: GitHub-secrets → env.)

#### C6 — R8/ProGuard: nenio aktiva (neniu keep-regula risko hodiaŭ)
- **Statuso**: [ ]
- **Loko**: `androidApp/build.gradle.kts:55` (`isMinifyEnabled = false`; neniu `proguard-rules.pro`; neniu `consumerProguardFiles` en `shared/build.gradle.kts`; APK ~21 MB)
- **Sugesto**: kiam vi ŝaltos R8: konsumaj reguloj por Media3 session/exoplayer kaj Sentry (memkonfiguriĝas), atentu `SonfontoKodilo`/MediaMetadata-klasojn; re-testu la sciigojn kaj la fonon.

#### C7 — Enkonduku `Application`-klason (unufoja inicializado)
- **Statuso**: [ ]
- **Loko**: `MainActivity.kt:48-49` (Sentry + `appContext` ĉe ĉiu onCreate); neniu `Application`-klaso en la nova apo
- **Sugesto**: `Application`-klaso kiu agordas la globalan kontekston, inicializas Sentry unufoje (kaj ĝuste laŭ build-tipo), kaj pretigas la kanaldeponejon — tio solvas K2 kaj parton de G5.

#### C8 — `striptiguKomentojn` ne subtenas `/* */`; `coerceInputValues` kaŝas agord-erarojn
- **Statuso**: [ ]
- **Loko**: `KanalAgordoLeganto.kt:37-72` (nur `//` kaj `\`-daŭrigoj), `:22` (`coerceInputValues = true`)
- **Sugesto**: dokumentu la limigon (aŭ subtenu blokkomentojn); pripensu forigi `coerceInputValues` por novaj kampoj.

#### C9 — Gradle-higieno: dependency verification, Dependabot, jcenter
- **Statuso**: [ ]
- **Loko**: radika `build.gradle.kts` (`jcenter()` en `allprojects` — nur por malnovaj dependoj de `malnova/`); neniu `gradle/verification-metadata.xml`; neniu `dependabot.yml`
- **Sugesto**: aldonu Gradle-dependency-verification; enŝaltu Dependabot kaj OWASP `dependencyCheck` en CI; limigu `jcenter()` al la malnova projekto aŭ forigu ĝin iam.

### Ludilo

#### C10 — `ExoPlayerLudiloRegilo`: MediaController neniam liberigita, neniu `onDisconnected`
- **Statuso**: [ ]
- **Loko**: `androidApp/.../ExoPlayerLudiloRegilo.kt:78-121`
- **Priskribo**: Se la sistemo mortigas la servon aŭ la konekto falas, `controller` restas stale kaj `ludi()/pauxzigi()/saltiAl()` (poŝtataj al `controller?.`) silente nenion faras ĝis procezo-restarto. Se `buildAsync()` malsukcesas, `konektita.completeExceptionally` faras, ke ĉiu posta `fiksiFonton` ĵetas — `ludiElsendonInterna` nur logas, la stato restas `Haltita`: silenta morto.
- **Sugesto**: traktu `onDisconnected` (emitu `Eraro(reprovebla = true)` kaj rekonstruu la regilon); ĉirkaŭu `konektita.await()` per `withTimeout` kaj emitu `Eraro` ĉe malsukceso.

#### C11 — `SonfontoKodilo` enhavas la tutan `Elsendo` en `MediaMetadata.extras`
- **Statuso**: [ ]
- **Loko**: `shared/src/commonMain/.../domain/player/SonfontoKodilo.kt:23`; uzata en `ExoPlayerLudiloRegilo.kt:113-135` kaj `LudiElsendoReceivilo.kt:56-58`
- **Priskribo**: La JSON trairas Binder (regilo→servo) kaj loĝas en la sesio; riĉaj HTML-priskriboj (ekz. Varsovia Vento kun enkorpigitaj `<audio>`) povas esti dekoj da KB — risko de `TransactionTooLargeException` kaj malnecesa ŝarĝo.
- **Sugesto**: serializu nur `id, kanaloSlug, titolo, fluo, dato, bildoUrl` kaj rekonstruu la `Elsendo`-on per `ElsendoDeponejo.getElsendo(id)`.

#### C12 — `halti()` dum `fiksiFonton` atendas → ludado komenciĝas post halto
- **Statuso**: [ ]
- **Loko**: `ExoPlayerLudiloRegilo.kt:182-201` kontraŭ `:204-213`
- **Priskribo**: `fiksiFonton` atendas `konektita.await()`/`withContext(Main)`; se la uzanto premas "Halti" dum tio, `setMediaItem`+`prepare` ruliĝas *post* `clearMediaItems` → muziko komenciĝas. Fenestro estas mallarĝa (unua konektado).
- **Sugesto**: generacia token / `Mutex` komuna inter `fiksiFonton` kaj `halti`.

#### C13 — Aŭtoludo povas superpaŝi uzantan agon
- **Statuso**: [ ]
- **Loko**: `LudvicoRegilo.kt:260,372` (`traktiFinon` lanĉas `ludiSekvan` en aparta korutino; antaŭ ol ludi, nenio rekontrolas, ke la ludilo ankoraŭ ludas la ĵus finitan elsendon)
- **Sugesto**: komence de `ludiSekvanInterna`, komparu `ludilo.stato.value.nunaFonto` kun `nunaElsendo`; se ili malsamas, rezignu.

#### C14 — `AlarmoElekto`: la rezerva elekto ignoras `erara`
- **Statuso**: [ ]
- **Loko**: `shared/src/commonMain/.../domain/player/AlarmoElekto.kt:17-18` (`?: ordigitaj.firstOrNull()` povas elekti la plej novan *eraran* elsendon; `MainActivity` tiam mem korektas al la ringtono post 10 s)
- **Sugesto**: dua falilo "plej nova ne-erara", kaj nur tiam la ringtono.

#### C15 — wasmJs: malnovaj evento-aŭskultantoj de anstataŭigita elemento
- **Statuso**: [ ]
- **Loko**: `WasmJsLudiloRegilo.kt:33-38`
- **Priskribo**: `fiksiFonton` paŭzigas la malnovan elementon kaj kreas novan; la `pause`-evento de la malnova alvenas nesinkrone kaj ĝia aŭskultanto (`_stato.value.copy(stato = Haltita)`) povas superskribi la ĵus metitan `Konektas`.
- **Sugesto**: forigu la aŭskultantojn (`removeEventListener`) aŭ uzu generacian kontrolon en ĉiu lambda.

#### C16 — Desktop: `rawStream` likas ĉe eraro; `saltiAl` estas ŝajnigo
- **Statuso**: [ ]
- **Loko**: `DesktopLudiloRegilo.kt:90-93` (se `AudioSystem.getAudioInputStream(rawStream)` ĵetas, `rawStream` neniam fermiĝas); `:273-275` (`saltiAl` nur metas la montratan pozicion — la UI gardas per `subtenasSaltadon = false`, do nuntempe bone)
- **Sugesto**: fermu `rawStream` en `catch`; klarigu en la KDoc de `saltiAl`, ke ĝi estas ŝajnigo.

#### C17 — Regulo 8: silentaj `runCatching` / netraktita korupta JSON en riceviloj
- **Statuso**: [ ]
- **Loko**: `LudiElsendoReceivilo.kt:54` (`runCatching { setArtworkUri(...) }` sen `onFailure`/logado); `BootReceivilo.kt:55-60` (`leguPersistitajnAlarmojn()` ne envolvas la JSON-malkodon — korupta valoreto ĵetas, kaj neniuj alarmoj re-skediĝas post reŝargo)
- **Sugesto**: aldonu `onFailure { logw(...) }` / `try-catch` kun protokolo.

### Android

#### C18 — `AlarmoSkedilo`: rasa fenestro inter `canScheduleExactAlarms()` kaj `setAlarmClock()`
- **Statuso**: [ ]
- **Loko**: `shared/src/androidMain/.../data/repository/AlarmoSkedilo.kt:38-48`; UI-voko sen try/catch: `AlarmoEkrano.kt:78,90,141`
- **Priskribo**: Sur Android 14 la permeso `SCHEDULE_EXACT_ALARM` estas forprenebla iam ajn; se tio okazas inter la kontrolo kaj la voketo, `setAlarmClock` ĵetas `SecurityException`; en la UI-vojo (`scope.launch { … }` sen try/catch) la nekaptita escepto en korutino kraŝigas la aplikon.
- **Sugesto**: `runCatching { setAlarmClock(...) }.onFailure { setWindow(...) }` — tio ankaŭ estas la intencita fallback.

#### C19 — Sciigo: fragilaj detaloj en `NovajElsendojKontroloWorker`
- **Statuso**: [ ]
- **Loko**: `NovajElsendojKontroloWorker.kt:152-176` (`Class.forName("dk.nordfalk.esperanto.android.MainActivity")` — hardkodita ĉeno en shared; `nm.notify(elsendo.id.hashCode(), …)` — kolizioj anstataŭigas sciigojn / "Ludi" povus ludi la alian elsendon; `android.R.drawable.ic_media_play` kiel malgranda ikono aspektas nebone)
- **Sugesto**: platform-indirekto por la klaso, stabila unika sciig-ID (kreskanta kontraŭo en Settings, aŭ UUID), propra vektora ikono.

#### C20 — `SciigPermeso.malfermuSciigAgordojn()` sen try/catch
- **Statuso**: [ ]
- **Loko**: `shared/src/androidMain/.../data/repository/SciigPermeso.kt:21-28`
- **Sugesto**: `runCatching { … }.onFailure { logw(...) }` (male al `malfermuEkzaktajnAlarmAgordojn()`, kiu havas).

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

### Testoj / CI

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

### UI / alirebleco

#### C31 — `LazyRow`/`items` sen ŝlosiloj; bildoj sen placeholder/error
- **Statuso**: [ ]
- **Loko**: `HejmoEkrano.kt:400-477` (`items(aktivajKanaloj)` ktp. sen `key`); `AsyncImage` sen `placeholder`/`error` en HejmoEkrano, KanalEkrano, MiniLudilbreto, KanalaroEkrano
- **Sugesto**: aldonu `key = { it.id }` (elsendoj) / `key = { it.kanalo.slug }` (kanaloj); aldonu placeholder/eraro-bildojn (Coil).

#### C32 — Tuŝ-celoj sub 48 dp; `contentDescription = null`
- **Statuso**: [ ]
- **Loko**: `HejmoEkrano.kt:618-658` (tripunkta menuo 28.dp, ludo-butono 32.dp); `ElshutitajEkrano.kt:140-148` (`IconButton` 36.dp); `MalsupraNavigaBreto.kt:30-54` (ikonoj kun `contentDescription = null`); `ElsendoEkrano.kt:332-411` (buton-ikonoj kun `contentDescription = null`)
- **Sugesto**: `Modifier.minimumTouchTargetSize()` aŭ pligrandigo; aldonu `contentDescription` al la ikonoj.

#### C33 — `AlarmoEkrano`: `LazyColumn` ene de `Column` kun `verticalScroll`; horo/minuto tajpataj sen cifereca klavaro
- **Statuso**: [ ]
- **Loko**: `AlarmoEkrano.kt:250-270` (nesting de ruliĝeblaj ujoj), `:247-258` (`OutlinedTextField` sen `keyboardOptions`)
- **Sugesto**: uzu `Column` kun `kanaloj.forEach { … }` (la kanallisto estas malgranda, 27 kanaloj) anstataŭ `LazyColumn`; `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)` aŭ Material-3 `TimePicker`.

#### C34 — iOS: `PlatformLigilo` estas TODO; neniuj deep links
- **Statuso**: [ ]
- **Loko**: `shared/src/iosMain/.../ui/PlatformLigilo.ios.kt` (`malfermuLigon`/`malfermuRetposhton` malplenaj); neniu `ACTION_VIEW` intent-filter
- **Sugesto**: konata limito (iOS venos); se iam bezonataj, aldonu deep links kaj traktu la URL-on en `MainActivity.traktuIntenton`.

### Kerno / parsado (malgrandaj)

#### C35 — `normigiDaton` forĵetas iujn validokusitajn datojn
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:384-400`
- **Priskribo**: Simpla `yyyy-MM-dd` (sen "T") ne estas rekonita (la regex malsukcesas → `return null` → **la tuta ero estas forĵetita**, linio 92); same por 4-letera "Sept". La malnova `Date.parse` akceptis ambaŭ.
- **Sugesto**: aldonu `if (datStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return datStr` kaj `(\\w{3,4})` + `take(3)` + testoj.

#### C36 — `retpaghoUrl` por Atom-eroj trafas malplenan ĉenon
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:116-117` (`selectFirst("link")?.text() ?: …attr("href")` — por Atom `<link href=…/>` havas malplenan tekston, kaj `""` ne estas `null` → `retpaghoUrl = ""`)
- **Sugesto**: `.text().ifEmpty { null } ?: …`

#### C37 — `leguDauron` ne pritraktas frakciajn sekundojn
- **Statuso**: [ ]
- **Loko**: `RssParsilo.kt:402-412` (`00:51:17.47` → `toLongOrNull("17.47")` malsukcesas → `null`)
- **Sugesto**: `substringBefore('.')` por la lasta komponento.

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

### Dokumentaro (konsekvencoj)

#### C41 — AGENTS.md kaj docs malfruas post la kodo
- **Statuso**: [ ]
- **Loko**: AGENTS.md "Kio funkcias nun" kaj la dosieruja arbo ne mencias la diagnozan ekranon, `DiagnozoRegilo`/`DiagnozoEkrano`/`Diagnozo.kt`, nek la fonan aŭtomatan daŭrigon; `docs/nova/03_domajno_kaj_datumoj.md` kaj `docs/nova/05_dizajno_kaj_ui.md` ricevis nur tajpo-riparojn; `docs/nova/04` priskribas neekzistantajn `puriguModeloj`/`iframeReguloj` kaj malprecizan id-konvencion (vidu G15, G16)
- **Sugesto**: 2-3 linioj en "Kio funkcias nun" + 3 linioj en la arbo; mallonga alineo en `docs/nova/03` pri `DiagnozoRegilo` (expect/actual); vicigu `docs/nova/04` kaj la kodon (G15/G16).

#### C42 — `DiagnozoTest` komenta troigo
- **Statuso**: [ ]
- **Loko**: `shared/src/commonTest/.../domain/DiagnozoTest.kt:19` (diras "La Android-implemento estas testata per instrumentoj sur la aparato" — sed neniun instrumentitan teston aldoniĝis)
- **Sugesto**: aŭ aldonu instrumentitan teston aŭ korektu la komenton.

### Estonta servilo (de la revizio de `malnova/` — por la plano `docs/nova/06_servilo_arkivo.md`)

#### C43 — Semantiko de `FilCache` kaj scio de `RssArkivServer` por la estonta servilo
- **Statuso**: [ ]
- **Priskribo**: (a) `ændrerSigIkke=true` = "kaŝita → neniam refreŝi" — tio igas `RssArkivServer-filcache/` la determinismajn golden-fixture-ojn; la nova plano diras nur "reuzo de Ktor-HttpCache + RssArkivServer-filcache" sen precizigi tiun semantikon. (b) La malnova `RomeFeedWriter` skribis `enclosure length` el la loka kaŝo kaj **per-jarajn arkivdosierojn** `feed-<slug>-<yyyy>.xml` — la nova plano tion ne mencias. (c) La plan-snippet vokas `parsilo.parsRss(fluo, kanal)` **sen** `httpKliento` — por Peranto tio signifas divenitajn, ofte malĝustajn, archive.org-URL-ojn en la arkivo — la servilo devas transdoni klienton kaj persistentan `ArchiveOrgDosiernomoKasho` (vidu ankaŭ G16 kaj G34). (d) La id-rondira problemo (G16) rekte tuŝas la inkrementan kunfandon de la plano.
- **Sugesto**: ĝisdatigu `docs/nova/06_servilo_arkivo.md` antaŭ ol konstrui la servilon.

#### C44 — Perditaj kontraŭ la malnova apo (konsideroj, ne nepre riparoj)
- **Statuso**: [ ]
- **Priskribo**: (a) redirect-host-guard kontraŭ kaptivaj portaloj (`Diverse.tjekOmdirigering`); (b) `serverCurrentTimeMillis` (kontraŭ aparata horloĝ-eraro); (c) la regulo ">95% → rekomencu de 0" de la malnova ludilo (`Afspiller.onPrepared`) — nun nur la `finita`-flago; (d) "elŝuti nur per WiFi" (`hentKunOverWifi`); (e) perioda refreŝo (malnova: kanalo ĉiujn 30 s, grunddata ĉiujn 30 min — nova: nur tiral-aktualigo kaj eniro al ekrano); (f) fora elŝuto de la kanalkonfiguro kaj radio.txt dum rultempo (nun nur bundled; atentu: la `muzaiko`-RSS en la agordo estas ekstere gastigata); (g) ksoup `parseXml` kaj krudaj ne-XML-aj entitoj (`&nbsp;` rekte en la fluo) — rekomendas golden-teston kun kruda `&nbsp;`.
- **Sugesto**: decidu intence, kiuj el ili estos reenkondukitaj; almenaŭ (c) kaj (g) estas malmultekostaj.

---

## Jam farita (ne en la listo supre)

- [x] **Ripara batch-o 3** (branĉo `riparoj/kodrevizio`, 250/250 testoj, neniuj novaj dosieroj en `~/.esperantoradio/cache/`):
  - G24 — nova CI-laborfluo `.github/workflows/testoj-kaj-konstruo.yaml`: `:shared:desktopTest` + Desktop/Android/wasmJs-kompilado por ĉiu PR kaj push al master (ankaŭ la wasmJs-kompiladon, kiu mankis kiam PR #79 enmetis `Dispatchers.IO`)
  - G37 — `DiskKashoTest` movita al `desktopTest` kaj direktas `dosierKashoBazo` al provizora dosierujo (antaŭe: verkis en la veran `~/.esperantoradio/cache/` kaj "purigis" per malplenigo)
  - G38 — nova `KanalAgordoFumTesto`: la VERA `esperantoradio_kanaloj_v9.json` parsiĝas, ≥20 kanaloj, unikaj kodoj, kodo+nomo, ≥15 kun RSS
  - G40 — novaj `PersistaLudatojDeponejoTest` (pozicio/finita/erara/malmarko trans restarto, korupta JSON → freŝa komenco) kaj `PersistantaPlejŝatatajDeponejoTest` (baskulo trans restarto) — laŭ la modelo de `PersistantaAlarmoDeponejoTest`
  - G23 — F-Droid-metadato ĝisdatigita al 3.0.2/246 (konforma al la kodo kaj la etikedo `v3.0.2`)
- [x] **Ripara batch-o 2** (branĉo `riparoj/kodrevizio`, 241/241 testoj, APK + wasmJs konstruitaj):
  - G8 — `traktiFinon` ricevas la **finantan fonton** de la vokanto (stato-emiso kaj `EsperantoLudadoServo` ambaŭ); nova atoma dedup-fenestro (Mutex + `ArrayDeque`, maks 4) anstataŭ la ne-atomCheck-then-set sur unu kampo
  - G10 — triligita elŝuto (frua EOF kontraŭ `contentLength`) ĵetas → `Eraro`, ne `Preta`
  - G11 — partaj MP3-oj forigitaj ĉe eraro/paŭzo + orfaj partaj dosieroj purigitaj ĉe reŝargo; `_statoj`/`joboj` → `ConcurrentHashMap`
  - G12 — `getExternalFilesDir()`-nulaĵo → retrofalo al `filesDir`
  - G14 — la testo `servoVoko_duoblaTraktadoDeSamaFonto_estasIgnorata` nun testas la veran dedup-vojon (malfrua duobla voko → e2 NE estas markita finita, neniu dua aŭtoludo); nova testo `servoVoko_senFontoNeFarasNenion`
- [x] **Ripara batch-o 2026-10-09** (branĉo `riparoj/kodrevizio`, 240/240 testoj pasas, APK
  konstruita, wasmJs+Desktop kompilas, fum-testo sur emulator-5554 sukcesa):
  - K1 — HTTP-status-kontrolo (`expectSuccess` + `kontroluFluecon`) en `ElsendoDeponejoImpl` + Worker; testoj (404 kaj 200-fiaskpaĝo ne anstataŭigas la kaŝon)
  - K2 + C7 — `Application`-klaso (`EsperantoRadioAplikajho`): `appContext` + Sentry unufoje en ĉiu procezo
  - K3 — ExoPlayer: `mediaItemCount == 0` → ĉiam `Haltita` (rosta `playerError`); reprovo-gardilo `nunaFonto == null` → nuligu; `traktiFinon` sen fonto → neniu aŭtoludo; nova `LudvicoRegilo.haltuLudadon()` uzata de la Halti-butono
  - K4 — `LudvicoEkrano`: vico-klakebligo (`.clickable`)
  - K5 — nova `malkoduUrlKoditajxon` (ĝusta UTF-8-percent-malkodado) anstataŭ la erara supersigna tabelo; testoj
  - G1 — `network_security_config.xml`: klara teksto nur por la 18 heredaĵaj gastigantoj el la konfiguro
  - G2 — `saniguRetUrlon`: nur http/https por fluo/bildo/retpagho en ĉiuj parser-voj; testoj
  - G3 — `IntentSxlosilo` (po-instala nonce): `AlarmoReceivilo` kaj Worker almetas ĝin, `MainActivity` kontrolas ĝin
  - G4 — `WebViewClient` (ligiloj malfermiĝas ekstere, `intent://` blokata) + `allowFileAccess(false)` + sanitizilo kun skemo-blanka listo; testoj
  - G6 — `allowBackup="false"`
  - G9 — `ordigitajPlejFreshajUnue` en la deponejo (IRo-montrordo + aŭtoluddirekto); testo
  - G17 — mankanta komo en `FORPRENITAJ_KANALOJ` (ambaŭ kopioj)
  - G26 — `CancellationException` reĵetata + `Throwable`-kapto en `LudvicoRegilo` kaj `ElsendoDeponejoImpl.leguKashitajnElsendojn`
  - G27 — `erarojSinsekvaj` nuliĝas ĉe naturfino (ne ĉe Ludas — tio rompus la 10-eraro-halton)
  - G31 — solvita per dok-ĝisdatigo (docs/nova/04: https-korekto nur por `kernpunkto`)
  - **Nova trovo, tuj riparita**: `Dispatchers.IO` en `AppStato` (PR #79) rompis la **wasmJs-kompiladon** — `Dispatchers.IO` ne ekzistas sur wasmJs; neniuj CI-laborfluo kompilas wasmJs, do ĝi pasis nekaptita (→ G24 estas nun urĝa). Licio aldonita al AGENTS.md.
- [x] `secrets.properties` (ŝlosilaj pasvortoj) al `.gitignore` — commit `e74c02a` (la dosiero restas loke, neversionigita, nun ignorata). Tio estis KRITIKA trovo de la sekureca revizio.
- [x] CHANGELOG: duobla `## [Neeldonita]` kunfandita; `## [3.0.2] - 2026-10-08` kun kompar-ligo — commit `9e835b9`.
- [x] Testnombroj forigitaj el AGENTS.md (la nombro estas nun 229 kaj kreskas) — commit `32e6b17`.
- [x] SXANGXOJ kompletigita por la novaj PR-oj (interalie PR #77) — en la delto ĝis `c92766a`.
- [x] Diagnoza ekrano + aŭtomata daŭrigo en la fono (PR #79) kaj 7 novaj kanaloj + IRo + Esp. Magazino Tuluzo (PR #83, #86) — en la delto; kontrolitaj de la delta-revizoro (229/229 testoj, APK konstruita, 11/11 instrumentitaj testoj sur emulator-5554, ambaŭ JSONC-kopioj byte-identikaj, 9 novaj fluoj vivaj).

## Kontrolo kaj re-rulado

```bash
./gradlew :shared:desktopTest          # 229 unuopecaj testoj (ĉiuj pasu)
./gradlew :androidApp:assembleDebug    # Android-APK
./gradlew :androidApp:connectedDebugAndroidTest   # instrumentitaj testoj (emulator-5554)
git diff b92f739..HEAD --stat          # la delto, kiun kovris la delta-revizoro
```

La plena detala raporto de la revizio (kun ĉiuj fontoj de la trovoj) estas konservita en la seanca skribujo: `kodrevizio-2026-10-08.md`.

*Dosiero kreita laŭ peto de la uzanto post la tutplena kodrevizio de 2026-10-08. Ne komitita (laŭ AGENTS.md: ne commitu antaŭ aprobo).*
