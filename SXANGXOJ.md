# Ŝanĝoj — 2026-10-24

## Stabilaj nomoj por la regul-aroj (A/L/P) — fino de "regulo 4"-ambiguecoj (PR: https://github.com/nordfalk/EsperantoRadio/pull/133)

Bare numeritaj regul-referencoj ("regulo 4", "regulo 8", "regulo 6.3") estis ambiguj:
la AGENTS-reguloj (1–12), la git-laborfluaj reguloj (1–8) kaj la sep parsreguloj
(6.1–6.7) interkovriĝis nomere — "regulo 8" povis signifi la protokolan regulon A8
aŭ la paĝigan parsregon (kaduka referenco en `docs/malnova/03` vere celis tiun).
Decido de la posedanto: ĉiuj regul-aroj ricevas unikajn, stabilajn nomojn kun
prefiksoj, kiuj ne kolizias kun la FAROTA-literoj (C/G/K/X):

- **A1–A12** — "Plej gravaj reguloj por agentoj" (AGENTS.md)
- **L1–L8** — la git-laborfluaj reguloj (AGENTS.md)
- **P6.1–P6.7** (+ subpunktoj P6.1.4, P6.1.6) — la sep parsreguloj (docs/malnova/03, docs/nova/04); la historiaj numeroj 6.x restas, nur la P-prefikso diferencigas ilin de la aliaj aroj

22 dosieroj ĝisdatigitaj: la difinoj (AGENTS.md-listoj, dok-kapoj) kaj ĉiuj
referencoj (kodo-komentoj, NOTOJ/SXANGXOJ, .vibe/skills, ambaŭ JSONC-kopietoj
de la kanalkonfiguro). Pura kontrolo: uskleco-sentema serĉo
`git grep -i -E 'regulo(j|n|jn)? ?\(?[0-9]'` trovas nun nur la intencajn citaĵojn
en ĉi tiu noto mem. La kaduka "(regulo 8)" en docs/malnova/03 (pri paĝigo, ene de
la P6.7-sekcio) ĝustigita al P6.7. (Fina revizio post la unua balao: la JSONC-komentoj
havis majusklan "Regulo 6.6" — la komenca `grep 'regulo [0-9]'` estis uskleco-sentema
kaj maltrafis ilin.)

## Hejmo: klakebla "Kanaloj", tro-rula tirkesto kiu sekvas la fingron, ĉiam videbla miniludilbreto (PR: https://github.com/nordfalk/EsperantoRadio/pull/131)

### 1. Klakebla sekcio-titolo "Kanaloj"

`SekcioTitolo` ricevis laŭvolan `onClick`; la titolo "Kanaloj" montras sag-piktogramon
kaj navigas per `switchTab(Vojo.Kanalaro)` — la stako rekomenciĝas je [Hejmo, Kanalaro],
reen revenas al Hejmo.

### 2. Tro-rula tirkesto-malfermo kiu SEKVAS la FINGRON

Unua provo estis sojl-bazita (akumulita 40dp-tiro → `tirkestStato.open()`); la uzanto
ĝuste rifuzis: la tirkesto devas sekvi la fingron. Sed `DrawerState.anchoredDraggableState`
estas `internal` en material3 1.13.0-alpha01 — la publika API ne povas movi la tirkeston
dum gesto (nur open/close/snapTo). Pro tio **forkigo de `NavigationDrawer.kt`**
(+ `Scrim.kt` + `internal/FloatProducer.kt`) en la apartan pakaĵon
`dk.nordfalk.compose.material3` — kun la originaj nomoj (`DrawerState`,
`rememberDrawerState`, `ModalNavigationDrawer`, `ModalDrawerSheet`) kaj kiel eble
plej laŭvorta kopiaĵo, laŭ decido de la posedanto: bibliotek-kopioj ricevas la originajn
nomojn en propra pakaĵo por minimumigi la diferencon. Nura API-diferenco:
`anchoredDraggableState` estas `public`. Ĉiuj ceteraj devioj estas markitaj per
`FORK-DIFF`-komentoj: MotionScheme-keytokenoj kaj internaj `Strings` (→ tween(256) +
fiksitaj esperantaj tekstoj), la prognoza-reena maŝino (→ nav3-`NavigationBackHandler`
en `ModalDrawerSheet`, sama loko), `fastMap`/`fastForEach` (→ `map`/`forEach`).

`ui/TirkestaTiro.kt` — `NestedScrollConnection` sur la 4 horizontalaj vicoj de
Hejmo (`Modifier.tirkestoJeTroRulo`): kiam la vico ne povas ruli pli reen kaj la uzanto
tiras dekstren, la deltoj iras rekte en `dispatchRawDelta` (la tirkesto sekvas la
fingron ambaŭdirekte; la delto estas konsumita, do neniu elasta tro-rul-efiko).
`onPostFling` solvas per `settle(available.x)` — la rapido-konscia (deprecita, sed
funkcia ĉar la forkigita `DrawerState` konstruas la `AnchoredDraggableState` kun la
originaj sojloj: pozicia 50%, rapida 400 dp/s) — do tute sama decido-logiko kiel la
vera tirkesto-gesto. NaN-gardoj: `offset`/`anchors.positionOf` antaŭ ĉia uzo.

Kaptindaĵo: `onPostFling` de la konekto efektive vokiĝas ankaŭ kiam la vico mem konsumis
nenion — la `scrollable` de LazyRow ĉiam eniras la flug-fazon post gesto. La
UI-testoj pruvvas la tutan ĉenon: `longaDekstraTroTiroGlitasLaTirkestonMalferme` kaj
`mallongaDekstraTroTiroRevenasFermita` (svipo tra la reala rul-mekanismo sur la
"Kanaloj"-vico; la tirkesta folio en la testoj bezonas realan larĝon — 240dp, alie
la ankroj degeneras al 0 kaj neniu tiro movas ion).

### 3. Miniludilbreto ĉiam videbla kiam nenio ludas

Antaŭe la breto tute malaperis kiam nenio ludis (escepte detalo-ekranojn, kie
ĝi "sekvis la navigadon" — PR #87). Nun:

- sur `KanaloDetalo`/`ElsendoDetalo` ĝi montras la nun VIDATAN kanalon/elsendon —
  sekvante SVIPOJN inter paĝoj (novaj `onVidataKanalo`/`onVidataElsendo`-revenvokoj
  el la `HorizontalPager`-oj per `LaunchedEffect(settledPage, …)`; `App.kt` resetas
  la staton per `LaunchedEffect(vojo)` je ĉiu eniro);
- sur ĉiuj aliaj ĉefekranoj ĝi montras la laste ludatan elsendon (elekto de la
  posedanto, "kiel en Spotify"): `lasteLudataElsendo` kaptata el `ludantoStato.nunaFonto`,
  post restarto bazita sur `HejmoViewModel.lastatempeLudataj`;
- klako sur la breto navigas al la sekvan enhavon (`sekvantaVojo`).

**Riparo post uzanta observo — la sekvanta enhavo fariĝis gluiĝema**: la unua
versio reagis al la NAVIGADO — sur detaloj la vidata celo, reen sur la ĉefekrano
subite la laste ludata elsendo, do la breto "saltis" al alia elsendo je ĉiu
reen-iro. Post diskuto de du alternativoj — (a) "la laste montrata enhavo
venkas" kaj (b) "vidado venkas super ludado"; ili diferĝas nur kiam oni ludas
elsendon sen rigardi ĝian detalon — la posedanto elektis (a): la enhavo estas
GLUIĜEMA — nur **vidado** de detala celo (eniro/svipo) kaj **ludado**
ĝisdatigas ĝin. Nova `ui/SekvantaEnhavoRegilo.kt` (pure, rekte testebla):
`vidisElsendon` / `vidisKanalon` / `ludisFonton` (la ludata elsendo/kanalo
iĝas la sekvanta — rekta kanalo iĝas kanala sekvanto; haltigo ŝanĝas nenion),
`elsendo()` kun la restarta semo el la persistaj ludatoj. App.kt nur konektas
la eventojn; la klako-navigado evitas duoblan puŝon se oni jam estas sur tiu
ekrano.

Kontrolrezulto: `:shared:desktopTest` (ĉiuj testoj verdaj, inkluzive la 2 novajn
gestajn UI-testojn), `:androidApp:assembleDebug`, `:webApp:compileKotlinWasmJs` verdaj.

# Ŝanĝoj — 2026-10-20

## FAROTA G43/G45 — limigita korpo-legado (KMP-1H/1R), G43-verigo

**G43 (KMP-1S, "escalating") — eraro en la FAROTA-listo, ne en la kodo**: la sugestitaj
riparoj (a), (b) kaj (c) jam ekzistis sur master ekde PR #89/#109 — `leguKashitajnElsendojn`
kaptas la ĵeton de `kontroluFluecon`, protokolas per `logw`, forigas la koruptitan kaŝon
kaj redonas null; `kontroluFluecon` vokiĝas antaŭ `skribuKashon`; la testoj
(`malpuraKashoEstasForigata` k.a.) estis jam tie. La Sentry-okazoj (dist 249, lasta
10-14) kongruas kun la 249-eldonperiodo kaj ĉesis post ĝi. FAROTA-ero markita [x].

**G45 (KMP-1H/1R, OOM)**: `bodyAsText()` enmemorigas la TUTAN korpon antaŭ kontroli —
misdirektita respondo (60 MB MP3 anstataŭ fluo) mortigis la apoton per OutOfMemoryError
sur malaltklasaj aparatoj.

Riparo: nova commonMain-helpilo `KorpaLegilo.kt`:

- `MAKSIMUMA_FLUA_GRANDECO_BAJTOJ = 1 MB` (la plej granda konata legitima fluo ~310 KB;
  laŭ decido de la uzanto — pli strikta ol la origine proponitaj 2–5 MB; observo-tasko
  en FAROTA C49);
- `HttpResponse.leguKorponLimigite(limo)`: ContentLength-antaŭkontrolo (ĵetas tuj, nul
  bajtoj legataj) + peceta legado per `bodyAsChannel()`/`readAvailable` (devigas la
  limon ankaŭ ĉe chunked/mensoganta Content-Length);
- `RespondoTroGrandaException` — atendata fiasko, kaptata de la ekzistantaj
  `logw`-catch-blokoj (nenia Sentry-eraro-kvoto).

Anstataŭigoj: `ElsendoDeponejoImpl.sxargxiElsendojn` (commonMain) kaj
`NovajElsendojKontroloWorker` (androidMain — ĝi elŝutas fluojn sendepende en fono).
`arhivaPeto` (archive.org-metadatenoj, ~1-2 KB) restas ĉe `bodyAsText`.

Kaptindaĵo: `readAvailable` estas ETENDO en `io.ktor.utils.io` (import devas esti
aldonata en commonMain) — ne membro de `ByteReadChannel`.

Testoj: `troGrandaRespondoNeAnstatauxigasKashon` (la kaŝo restas netuŝita),
`leguKorponLimigiteSekvasLaLimon` (ĝusta legado + ContentLength-antaŭkontrolo),
`leguKorponLimigiteFlueTranslimasSenContentLength` (chunked-reta translimo).
Kontrolrezulto: `:shared:desktopTest` 311/311, `:androidApp:assembleDebug`,
`:webApp:compileKotlinWasmJs` verdaj.

# Ŝanĝoj — 2026-10-19

## FAROTA G25 — etikedo v3.0.2 rekreita sur la master-a eldona komito

La etikedo `v3.0.2` montris al `16ae6d6` (ne sur master); la eldona komito sur
master estas `9e835b9` (squash-merge de la sama PR). Riparite per
`git tag -f v3.0.2 9e835b9 && git push -f origin v3.0.2` (kun uzanto-aprobo —
re-verkado de publika etikedo). Kontrolita: `git merge-base --is-ancestor v3.0.2 HEAD` → jes.

## FAROTA G16/G31/G41/G7 — id-konvencio, anchor.fm-fiksaĵo, FAROTA-higieno

**G16 (id-skemo)**: la ĝenerala kaj Peranto-parsiloj konstruis la id-on kiel
`<slug>:<dato>:<guid>` / `<slug>:<dato>:<indekso>`, dum la dokumento kaj la
malnova `RomePodcastParser.parseAndre` uzas la guid senŝanĝe. Tio rompus la
rondiron kun la planita arkiva servilo (re-parsado re-prefiksikus → la id
kreskus ĉiun fojon; la inkrementa kunfando `e.id !in konataj` neniam trafus).

Riparo: nova helpilo `RssParsilo.konstruuId(kanalo, guid, dato, deArkivo)`:

- guid senŝanĝa se ĝi jam komencas per `<slug>:` (kaj ĉiam por `deArkivo` —
  la servilo skribas la jam finan id-on kiel guid);
- alie precize unu prefikso `<slug>:<guid>`;
- sen guid: `<slug>:<dato>` (kiel la malnova parseAndre).

La ĝenerala parsilo legas nun ankaŭ Atom-`<id>` (ekvivalento de `entry.uri`).
`PerantoEro.indekso` foriĝis (mortinta kampo). Testoj: `kernpunktoIdFormato`
(streĉita), `idGvidasSenPrefikso…`, `idAldonasPrecizeUnuPrefikson…`,
`deArkivoUzasGuidonSenAjnaPrefikso`, `idEstasStabilaTransRondiro`,
`konstruuIdSekvasLaKonvencion`. `docs/nova/04` "Id-konvencioj" vicigita kun la
kodo. FLANKA EFIKO: la id-ŝanĝo restartigas uzantdatumojn ŝlositajn per id
(ludpozicioj, elŝutoj, plej ŝatataj) por la koncernaj kanaloj — unufoje.

**G31 (https-korekto)**: ne plu aktuala — `docs/nova/04` (regulo P6.1.6) jam
priskribas la kernpunkto-nuran korekton kiel intenca historia konduto, kongrue
kun la malnova kodo (`RomePodcastParser.parseAndre:206`). Forigita el FAROTA.

**G41 (filcache-kontraŭdiro)**: la mankanta anchor.fm-fiksaĵo "La Malfamuloj"
(121 eroj, https://anchor.fm/s/1e1d5f38/podcast/rss) nun estas versiigita kiel
`shared/src/commonTest/resources/feeds/lamalfamuloj_feed.xml` kun ora testo
(≥114 elsendoj; 2024-12-26 → daŭro 3077 s, fluo enhavanta `392138630…mp3`).
`docs/nova/04` nun diras ke la fiksaĵoj estas versiigitaj en la deponejo kaj
la testoj rulas sen `RssArkivServer-filcache/`. AGENTS.md ĝisdatigita (5 fiksaĵoj).

**G7 (dependencoj/CVE)**: ne plu aktuala — Dependabot ekzistas
(`.github/dependabot.yml`, gradle + actions, monate) kaj ĉiuj dependencoj estis
ĝisdatigitaj per PR #117/#118 (Gradle 9.8.1, AGP 9.4.1, CMP 1.12.2, media3
1.11.1, ktor 3.6.0, coil 3.6.3). Forigita el FAROTA.

Kontrolrezulto: `:shared:desktopTest` 308/308 verdas, `:androidApp:assembleDebug` verda.

# Ŝanĝoj — 2026-10-17

## Vekhorloĝo: tempoelekto kaj kanalelekto per dialogoj (PR #114)

La redaktilo de alarmoj uzis du nombrajn tekst-kampojn (horo, minuto) kaj longan
radio-butonan kanalliston. Nun ĝi montras grandan klakeblan tempon
(`displayMedium`) kaj klakeblan kanalnomon; klako malfermas dialogon:

- `TempoElektiloDialogo` — Material3 `TimePicker` (ciferblido 00–23, kiel la
  TimePickerDialog de la malnova apo) kun ŝaltilo al `TimeInput` por klavara
  enigo (utila sur Desktop). La stato kreasĝiĝas nur ene de la dialogo, do
  "Nuligi" forĵetas la elekton; nur "Bone" transigas horon/minuton.
- `KanalElektiloDialogo` — `AlertDialog` kun la kanallisto; elekto tuj fermas.

Riparo dum la laboro: la redaktilo ne ricevis la Scaffold-`padding`-on, do la
etikedo "Tempo" kaŝiĝis malantaŭ la TopAppBar. `AlarmoRedaktilo` nun ricevas
`scaffoldPadding: PaddingValues`. Kontrolinte ĉiujn 11 `Scaffold`-uzojn: neniu
alia ekrano havas la problemon.

### Forigitaj etikedoj — la teksto nun venas de la agordo

La etikedoj de la alarmo-sugestoj el la JSONC ("Muzaiko semajnfine", "Pola
Retradio vendrede kaj semajnfine" kc.) ne kongruis kun la enhavo (ekz.
polaretradio havis ripeton 0x44 = merkredo+dimanĉo, dum la teksto diris
"vendrede kaj semajnfine"). Ŝanĝoj:

- La `=etikedo`-partoj de la 6 sugestoj malpleniĝis en ambaŭ JSONC-kopioj
  (la parsilo `parsuSugestojnPorAlarmoj` restas malkonstruita kun la malnova
  formato; malplena etikedo → `null`).
- `AlarmoEro` ne plu montras `alarmo.etikedo` — la sola fonto de etikedoj estis
  la sugestoj (la redaktilo ne havas etikedan kampon). Persistintaj etikedoj
  restas en la datumoj sed ne videblas.
- `Alarmo.ripetoTeksto` generas homecran tekston el la ripet-masko:
  `Ĉiutage`, `Labortage` (0x1f), `Semajnfine` (0x60), unuopaj tagoj
  (`Ĵaŭde`, `Dimanĉe` …), kaj plenaj tag-nomoj kun "kaj" por aliaj kombinoj
  (`Merkredo kaj Dimanĉo`, `Lundo, Mardo kaj Sabato`); "Cxiutage" iĝis
  "Ĉiutage". La alarmo-listo montras nun: tempo → kanalnomo → ripetoteksto —
  ĉio derivata de la agordo. La ripet-ĉipoj en la redaktilo ricevis
  3-literajn nomojn (Lun, Mar, Mer, Ĵaŭ, Ven, Sab, Dim — ĉiuj 7 eniras en
  unu linion), en `FlowRow` kiel sekureco por mallarĝaj ekranoj kaj
  grandaj tiparoj.
- Nova testaro: `ripetoTekstoGeneratasElKonfiguro` (AlarmoTest);
  `ekranfoto_alarmo_redaktilo` kaptas redaktilon + ambaŭ dialogojn.

### Lecioj pri testado (ankaŭ en AGENTS.md)

- `AlertDialog` estas aparta kompona radiko → `onRoot()` fiaskas per "found 2
  nodes"; la ekranfoto-helpilo kaptas la lastan radikon per
  `onAllNodes(SemanticsMatcher("estasRadiko") { it.isRoot })` — `isRoot` estas
  eco, ne metodo.
- `onNodeWithText` serĉas ĉiujn radikojn: "Nuligi" ekzistas kaj en dialogo kaj
  en la ekrano malantaŭ ĝi — fermu per teksto unika en la dialogo.

## Post-revizio de PR #113: koda mapo ĝisdatigita + kompil-kaptindaĵoj en AGENTS.md

Zorga revizio de PR #113 (mallongigo de AGENTS.md) konstatis, ke la "Deponeja
koda mapo" translokigita al NOTOJ.md estis fuŝita de la realo: 13 novaj
dosieroj mankis (ApajNovajxojDeponejo, Diagnozo*, ReprovoLogiko, HttpMotoro,
MemorAlarmoDeponejo, k.a.), `KanaloDeponejoImpl.kt` estis erara nomo (reale
`KanalDeponejoImpl.kt`), `NovajElsendojSkedilo.kt` estis listigita en
commonMain kvankam ĝi vivas en androidMain, kaj `ElshutDeponejo` ne estis
dosiero (reale `KreuElshutDeponejo.kt`). La mapo nun kongruas kun la arbo
(63 commonMain-dosieroj, aŭtomate komparitaj per find+comm).

Krome la kvin kompilad-kaptindaĵojn de commonMain (Dispatchers.IO, `@Volatile`,
coroutines-swing, `HttpMotoro`, klib-ABI) oni remetis al AGENTS.md kiel
kompaktan sekcion — ili mordas jam dum la skribado de kodo, antaŭ ol la agento
ŝargas la konstru-skill-on. Korektitaj ankaŭ: literum-eraro "Ekzemplej" kaj
malpreciza pado de `release-notes-arkivo/` en la skill-oj.

Leciono: translokigitan dokumentaron ĉiam kontrolu kontraŭ la realo, ne
kopiu ĝin vorte.

# Ŝanĝoj — 2026-10-13

## GPP-eldona noto: trako-bazita, ne versio-bazita (malkovrita dum la 249-eldono)

La 249-a eldono aperis en Play kun la malnovaj 3.0.1-notoj. Kaŭzo (konfirmita
el la bajtkodo de GPP 3.12.2, `PublishArtifactWorkerBase.findReleaseNotes`):
GPP serĉas la noto-dosieron en `release-notes/en-US/` laŭ la **trako-nomo** —
`internal.txt` (aŭ `default.txt` kiel retroio) — NENIAM laŭ versionName. La
versio-dosieroj `3.0.1.txt`/`3.0.2.txt`/`3.0.3.txt` do estis ignoritaj kaj ĉiuj
eldonoj de 3.0.2 ĝis 249 eldoniĝis sen novaj notoj (la trako plu montris la
notojn iam fiksigitajn en la 3.0.1-epoko). Riparo: `internal.txt` kreita kun la
3.0.3-teksto; la jam eldonitan 249-on ne eblas re-alŝuti (API rifuzas
versionCode-reuzon), do ĝiajn notojn oni korektas permane en Play Console aŭ
per la venonta 250-a eldono. AGENTS.md-instruo ĝisdatigita. **La notoj de 249
estis korektitaj permane en Play Console la saman tagon.**

## Sentry-kvoto: eraroj protektataj (PR #109)

La averto de Sentry (80 % de la monata eraroj-kvoto — 4 004/5 000) devigis
agadon. La erar-eventoj (`loge(tag,msg,e)` → captureException; NE la protokoloj)
venis precipe de: malnovaj versioj (loge-RSS-fiaskoj, „Aŭtoludo“-
captureMessage), ExoPlayer-ludad-fiaskoj (KMP-H), la Compose-skopa escepto
(KMP-X/W) kaj la malpura-kaŝa eraro (KMP-1S). Nun:

- `options.sampleRate = 0.5` en produktado — duona specimenado de erar-eventoj
  (Sentry grupigas ilin al problemoj, do la problemoj plu formiĝas).
- Forĵeto de eraroj de malnovaj versioj (beforeSend kun `MALNOVAJ_VERSIOJ`)
  estis provizita kaj tuj retirita: malmoligita versio-listo en la apo ne
  povas scii kiu versio iĝos "malnova" post venontaj eldonoj — tio bezonas
  ĝisdateblan centran liston (FAROTA C45). La eraroj de ≤ 3.0.2 ĉesos
  nature per la ĝisdatigo de tiuj uzantoj.
- KMP-1S: `leguKashitajnElsendojn` forigas malpuran kaŝon (fiaskpaĝo) kaj
  raportas per logw — antaŭe la eraro ripetiĝis je ĉiu apomfermo.
- KMP-X/W (rememberCoroutineScope): la ŝargadoj de HejmoViewModel,
  KanaloViewModel kaj KanalaroViewModel rulas en propraj ViewModel-skopoj
  (`SupervisorJob() + Dispatchers.Main`, kiel la oficiala viewModelScope) —
  ili transvivas navigadon anstataŭ esti nuligitaj kaj ĵeti.
- loge→logw por atendeblaj fiaskoj (ne erar-eventoj): ExoPlayer „Ludanta
  eraro“ (KMP-H), LudvicoRegilo (ludi/reprovi/rekta), DesktopLudilo,
  Sercxo, NovajElsendojKontroloWorker, elŝuto-malsukcesoj.
- La agordo „detala protokolado“ estas nun defaŭlte ŜALTITA — la protokola
  kvoto montriĝis ne-problema (635 protokoloj/24h de 23 uzantoj en 3.0.3);
  malŝaltebla en Agordoj → Privateco.
- Nova `foriguKashon` (expect/actual ×4) en DosierKasho.

Testoj: `malpuraKashoEstasForigata` (ElsendoDeponejoImplTest) +
`defauxlteSxaltitaKajFlagoSinkronigita` (AgordojDetalaProtokoladoTest).

## Kvoto-protekto 249: unua taksado (2026-10-14, 26h post eldono)

- **Eraroj laŭ build-numero (`dist`)**: dist 248 = 47 eventoj (4 uzantoj),
  dist 246/3.0.2 = 20 (2 uzantoj), **dist 249 = NUL** en 24h — dum almenaŭ
  5 aktivaj 249-uzantoj (ilin malkaŝas la "detala protokolado"-defaŭlto:
  "parsado kompleta"-mesaĝoj povas veni nur de 249). Kun sampleRate 0.5 eĉ
  eventualaj 249-eraroj duoniĝus — sed la logw-ŝanĝoj (ludad-/ret-fiaskoj)
  kaj la KMP-1S/KMP-X-riparoj forigis la fontojn tute.
- **Taga eraro-kvanto**: ~138/tage (10-07…10-12) → 80 (eldonotago 10-13) →
  29 en 16h (10-14) — ~75 % malkresko; la restaĵo venas de malnovaj versioj
  (ĉinaj uzantoj kun reto-blokado) kaj formortos per ĝisdatigoj.
- **Kvoto**: ~110 erar-eventoj konsumitaj ekde la averto (4 004/5 000) —
  je tiu ritmo la monata kvoto komforte sufiĉos.
- **Ne-atendita sondaĵo**: "[LudadoServo] startForeground() malsukcesis
  (fono)" estas la plej laŭta restanta protokolo (119/24h, 4 uzantoj, logw →
  protokolo-kvoto) — kandidato por dedupigo en estonta versio.
- **Datumaro-limo**: `dist` ekzistas nur en errors; la logs-datumaro ne
  subtenas ĝin (API-eraro ĉe grupigo) — protokoloj ne distingeblas per
  build-numero, nur per release.

## Debug-konstruoj kaj emuliloj ne raportas al Sentry (PR #109)

La fatalaj OOM-kraŝoj en la produktadaj Sentry-datumoj (KMP-1R, KMP-1H) venis
de la propra emulila testado de la programisto (release-konstruo flanke
instalita sur `sdk_gphone*`, `isSideLoaded: true`). Kaj la debug-konstruoj
poluis la mediojn "evoluo"/"produktado". Nun:

- `EsperantoRadioAplikajho` inicialigas Sentry-n NUR por ne-debug konstruoj
  sur realaj aparatoj: `FLAG_DEBUGGABLE` aŭ emulila modelo (`sdk_gphone*`,
  `goldfish`, `ranchu`, `generic`, `Emulator`, Genymotion) → tute neniu
  inicialigo; ĉiuj Sentry-vokoj restas no-op (la KMP-SDK estas sendanĝera
  vokata sen init — la desktop-testoj delonge tion pruvas).
- Desktop-rulado same ne inicialigas (`desktopApp/Main.kt` — ne ekzistas
  release-konstruo, do ĉiu rulado estas evolu-rulado).
- Web (wasmJs) restas senŝanĝa: la SDK tie estas no-op laŭ `SentryAgordo.kt`.
- Por testi Sentry-n: release-konstruo sur reala aparato.

Kontrolitaj: AndroidManifest havas neniun Sentry-aŭtoinit-meta-datumon
(ni regas la tutan init-on per `initialiguSentry()`), kaj neniuj
instrumentitaj testoj dependas de Sentry.

# Ŝanĝoj — 2026-10-12

## Rezerva alarm-sono nun haltigebla kaj mem-haltiĝanta (sekvaĵo de la revizio de b834483)

Trovo en la revizio de la rapida permeso-korekto `b834483`: la rezerva
(fallback) alarm-sono — ludata de `AlarmoReceivilo.luduFallbackRingtonon`
kiam la kanalo/fluo ne disponeblas — havis **nenian haltigan vojon**.
`Ringtone.play()` ripetiĝas ĝis `stop()`, sed nenie estis konservita tenilo
kaj nenio iam vokis `stop()`. La sciigo diris "tuŝu por malŝalti", sed la
tuŝo nur malfermis la UI (kaj `setAutoCancel` forigis la sciigon) — la sono
daŭre sonis senfine.

Kritika detalo: observi la ludanto-staton NE sufiĉas — en la pura-fallback-
kazo la ExoPlayer neniam startas, do `halti()` metas `LudantoInformo(Haltita)`
EGALAN al la nuna valoro kaj la StateFlow nenion elsendas. Krome en tiu
kazo estas neniu mini-ludilbreto (nunaFonto==null) kaj neniu media sciigo —
do la tuŝo de la alarm-sciigo estas la sola halta afordanco. Tial la halto
estas hokita rekte en la agojn, ne per observado.

- `AlarmoReceivilo` (companion): `@Volatile private var fallbackRingtono`
  (tenilo), `@Volatile var ringtonoMaksDauroMs = 5 * 60 * 1000L` (test-ŝanĝebla),
  `haltuFallbackRingon()` (idempotenta: stop + null + logi/logw) kaj
  `fallbackRingtonoAktiva` (por testoj).
- `luduFallbackRingtonon`: unue `haltuFallbackRingon()` (neniam duobla sono),
  post `play()` konservas la tenilon kaj lanĉas fajron-kaj-forgesu tempon:
  post `ringtonoMaksDauroMs` (referenca komparo `===`, ke malnova tempumilo
  ne haltigu novan sonon) ĝi haltigas la sonon KAJ nuligas la daŭran sciigon
  (sen tio ĝi restus — la problemo riparita en la sama revizio).
- `expect fun haltuAlarmRingon()` en `LudiloRegilo.kt` (commonMain);
  Android-`actual` vokas `AlarmoReceivilo.haltuFallbackRingon()`, la aliaj
  platformoj (Desktop/Web/iOS) estas senefikaj no-op.
- Hokoj: `ExoPlayerLudiloRegilo.halti()` (la rekta vojo de la uzanto),
  `ExoPlayerLudiloRegilo.updateState()` kiam la kalkulita stato estas `Haltita`
  (kaptas ankaŭ paŭzo/halto el la media sciigo — Media3 direktas CMD_STOP al
  la player, preter `halti()`; sekura ĉar sen sono ĝi estas senefika), kaj
  `MainActivity.traktuAlarmIntent` — sed nur kiam la intento portas
  `de_sciigo=true`, kiun nur la sciigo mem metas (vidu sube; en la
  pura-fallback-kazo la tuŝo de la sciigo estas la sola halta afordanco;
  `setAutoCancel` forigas la sciigon).
- Testoj (instrumentitaj, ret-ne-dependaj — uzas NEekzistantan kanalon, do la
  ricevilo tuj ludas la fallback-ringtonon sen RSS-elŝuto): `fallbackRingtonoHaltigxigxasPerHalti`
  (halti() haltigas la sonon) kaj `fallbackRingtonoMemHaltigxigxasKajNuligxasSciigon`
  (`ringtonoMaksDauroMs = 2000` → sono haltas kaj sciigo nuligas).
- **Flakemo kaptita dum la testado** (unua provo de `fallbackRingtonoMemHaltigxigxasKajNuligxasSciigon`
  malsukcesis sur la Pixel 10): dum instrumentitaj testoj la testata procezo estas
  konsiderata "foreground", do la best-effort `startActivity` de la ricevilo ĈIAM
  sukcesas kaj MainActivity traktas la alarm-intenton — senkondiĉa hoko tie povus
  haltigi la ringtonon tuj post ĝia ekigo (la sono vivis nur ~100 ms kaj la atendu
  maltrafis ĝin). Ankaŭ `moveToState(CREATED)` ne helpis (la procezo restas
  "foreground" pro la aktiva instrumentado). **Solvo**: la sciigo metas
  `de_sciigo=true` en sian intenton kaj `traktuAlarmIntent` haltigas la ringtonon
  nur tiam — la distingon portas la intenco mem, ne la ĉirkaŭaĵo. Tio estas ankaŭ
  pli ĝusta produkta konduto: la best-effort lanĉo de la ricevilo ne estas uzanta
  ago kaj ne haltigu la sonon.
- Krome struktura riparo en CHANGELOG.md: la sekcio `## [Neeldonita]` (kreita
  en 2491035) staris meze de la 3.0.3-sekcio (antaŭ `## [3.0.2]`) — laŭ Keep a
  Changelog ĝi devas esti la unua versio-sekcio; nun ĝi estas supre antaŭ
  `## [3.0.3]`, kun la nova eniro.

### Revizio de PR #111: la konekt-time `updateState()` mortigis la fallback-ringtonon (2026-10-14)

Revizio de la PR trovis regreson, kiun la novaj testoj ne kaptis: en **freŝa
procezo** (la vera alarm-scenaro — la apliko ne estas malfermita) la sinsekvo estas
`akiru()` (ludilo kreita, konekto komenciĝas) → `luduFallbackRingtonon()` (sono
startas) → MediaController konektiĝas → `updateState()` kalkulas `Haltita` (malplena
media listo) → `haltuAlarmRingon()` → la ĵus startinta sono estas **mortigita**.
Nenio suspendas inter `akiru()` kaj `luduFallbackRingtonon` (`inicialigu` kaj
`leguBundledKanalkonfiguron` estas nesuspendaj), do la sono ĉiam startas antaŭ la
konekto — la alarmo restus silenta. Konfirmita sur emulator-5554 (API 26):
`Haltigis fallback-ringtonon` en la sama millisecondo kiel `MediaController konektita
al servo`, 4,5 s post ekigo.

La testoj en `AlarmoReceiviloTest` ne vidas tion, ĉar en la testa procezo la ludilo
jam ekzistas kaj estas konektita (`ActivityScenarioRule` lanĉas `MainActivity`).

**Korekto**: `haltuAlarmRingon()` en `updateState()` nur ĉe **transicio** al
`Haltita` (`_stato.value.stato !is Haltita`) — la unua `updateState()` post konekto
lasas la sonon viva; halto el la media sciigo (`Ludas → Haltita`) ankoraŭ haltigas
ĝin; `halti()`, la sciigo-tuŝo kaj la mem-halto estas senŝanĝaj.

**Verifikita sur emulator-5554**: nova regres-testo `FresxProcezaFallbackTest` (sen
`ActivityScenarioRule` — la ludilo ankoraŭ ne ekzistas, kiel en freŝa procezo): sen la
gardilo ĝi fiaskas determinisme, kun la gardilo ĝi pasas. `fallbackRingtonoHaltigxigxasPerHalti`,
`fallbackRingtonoMemHaltigxigxasKajNuligxasSciigon` kaj
`alarmoSciigoNuligxigxasKiamLudadoHaltigxas` pasas same. (`alarmoReceiviloMemKomencasLudadon`
malsukcesis en la revizia medio pro manko de reto — la RSS-fetch falis post 10 s
konekt-timeout; la testo estas ret-dependa.)

## Apaj novaĵoj en la nova apo

La malnova apo montris mesaĝon el la `android`-sekcio de la kanalkonfiguro
kie la redaktisto de la apo povas alini sciigojn (ekz. versio-anoncojn aŭ
ke Muzaiko ne funkcias) kaj ĝi montriĝis kiel dialogo — unufoje por ĉiu
nova enhavo (haŝaĵo de la konfirmita mesaĝo persistiĝis en
SharedPreferences). La nova apo parsis la kampon sed neniam uzis ĝin.
La funkcio nun portiĝas kiel **apaj novaĵoj** (dialogo-titolo: "Novaĵoj");
la malnova dana ŝlosilo tute malaperis (ankaŭ el la JSONC-konfiguro).

Noma historio de tiu ĉi labortago: la malnova dana ŝlosilo →
`servoStatusMesagxo` → "Kio novas" (kunfrotis kun la samnoma
hejmo-sekcio pri novaj elsendoj) → fina nomo laŭ deziro de la uzanto:
JSON-ŝlosilo `apaj_novaĵoj`, Kotlin-identigilo `apajNovajxoj`.

- JSONC (ambaŭ kopioj, denove identaj): la uzanto redaktis la
  android-sekcion — arĥivaj malaktivaj ŝlosiloj forigitaj, nova aktiva
  mesaĝo sub `apaj_novaĵoj`: versio-3.0.3-anonco (3.0.2 jam eldoniĝis
  la 8-an de oktobro) en la stilo de la Play-release-notes, komence
  "27 radiokanaloj kaj podkastoj" kaj poste la plej gravaj
  Neeldonita-eroj (vekhoro ĝustatempe, kolapsebla ludilbreto, puraj
  priskriboj, la novaĵo-dialogo mem).
- `KanalAgordo.AndroidSekcio`: nova kampo `apajNovajxoj`
  (`@SerialName("apaj_novaĵoj")` — ŝlosilo kun supersignoj kiel la
  ekzistantaj `retpoŝto`/`hejmpaĝoButono`).
- Nova `ApajNovajxojDeponejo` (Settings, ŝlosilo `apaj_novajxoj_hesh`):
  pura decidlogiko (`deciduMontratan`) — malplena mesaĝo neniam
  montriĝas; ne-malplena montriĝas nur se ĝia `hashCode` diferencas de
  la konservita (same kiel la malnova apo; `"".hashCode() == 0`
  koincidas kun la komenca valoro por novaj instalaĵoj). `konfirmu()`
  persistas la haŝaĵon kaj fermas.
- `App.kt`: `AlertDialog` titolita "Novaĵoj" kun `HtmlVido` (la uzanto
  elektis `uzuWebView = false` → AnnotatedString-vidigo); nur "Bone"
  fermas (kiel en la malnova apo, ne nuligebla per reen/ekstera klako).
- Fum-testo sur emulilo: dialogo aperas → "Bone" fermas → restarto ne
  reaperigas. Unuaj testoj: `ApajNovajxojTest` (decidlogiko + persisto +
  restarto-kongruo) + agordo-mapado en `KanalAgordoLegantoTest` kaj
  `KanalAgordoFumTesto`.
- **Incidento dum la JSONC-redakto**: forigo de la eksa statusmesaĝo-ŝlosilo
  per skripto postlasis vosan kometon (`,`) post `apaj_novaĵoj` — la tuta
  kanalkonfiguro fiaskis parsi kaj `KanalAgordoFumTesto` kaptis tion per
  `JsonDecodingException: Trailing comma`. Leciono: post ĉia programema
  redakto de la kanalkonfiguro rulu `./gradlew :shared:desktopTest`
  antaŭ ol fidi la rezulton.

## Sentry-kvoto: protokolado severe limigita (PR #107)

La Sentry-kvoto estis minacata de la multaj `logi`-vokoj (antaŭe ĉiu `logi`
iris al `Sentry.logger`). Nun:

- `logi` sendas al Sentry nur se la agordo **"Ŝaltu detalan protokoladon"**
  (Agordoj → Privateco) estas ŝaltita — la flago `protokoloDetala` en
  `ProtokoloUtil.kt`, sinkronigita de `AgordojDeponejoImpl` (persisto en
  `SettingsKeys.DETALA_PROTOKOLADO`).
- Nova `logGrava` (loka + `Sentry.logger.info` ĉiam) por la gravaj eventoj:
  - malfermo de la apo (`AppStato.inicialigu`),
  - ekludo de elsendo/rekta kanalo kun la kanalo (`LudvicoRegilo.ludiElsendonInterna`/`ludiRektan`),
  - ekludo el sciigo (`LudiElsendoReceivilo`),
  - vekiĝo de alarmo (`AlarmoReceivilo`).
- `logw`/`loge` restas ĉiam sendataj (avertoj/eraroj estas maloftaj).
- `Sentry.captureMessage("Aŭtoludo: …")` en `LudvicoRegilo` FORIGITA — ĉiu
  aŭtomata sekva kreis eventon; la ekludo estas protokolita per `logGrava`.
- `options.logs.enabled` nun ĉiam `true` (antaŭe nur en evoluo) — tiel la
  gravaj eventoj kaj la ŝaltita detala protokolado funkcias ankaŭ en produktado;
  la kvanto estas limigita per la kodo.

Testoj: `AgordojDetalaProtokoladoTest` (defaŭlto, persisto, sinkronigo de la
flago) + `AgordojEkranoTest.montrasSxaltilonPorDetalaProtokolado`.

## Eldono 3.0.3: vekhorloĝaj permesoj forigitaj + revizio de la rapida korekto

### La problemo
Google Play rifuzis la alŝuton de la APK (eraro 403): "You must let us know
whether your app uses any full-screen intent permissions" — la deklaro de
`USE_FULL_SCREEN_INTENT` en la manifest postulas la respondon en la Play-Konsolo.
Post esploro de alternativoj (deklari en la konsolo, aŭtomata vekiĝo sen FSI ktp.)
la uzanto akceptis la kompromison: **forigi la tri permesojn** `SCHEDULE_EXACT_ALARM`,
`USE_EXACT_ALARM` kaj `USE_FULL_SCREEN_INTENT` (komito `b834483`, versionCode
247 → 248). Sekvoj sur Android 12+: alarmoj povas ekigi ĝis 10 minutojn malfrue
kaj la ekrano ne vekiĝas aŭtomate; la sono ĉiam ludas. Sur Android ≤11 ĉio
funkcias same kiel antaŭe. Poste la uzanto promociis la eldonon al Production
(status: completed, 100 %).

### La Play-listigo
La nova priskribo + 8 ekranfotoj estis publikigitaj, sed la enhavo ĉe Play restis
la malnova — `publishReleaseListing` konsideris sin `UP-TO-DATE` kaj efektive ne
alŝutis. Devige re-rulita per `--rerun-tasks`; poste kontrolita per la Play
Developer API (listigo: nova priskribo 3913 signoj; bild-kolektoj: 8
`phoneScreenshots` + `featureGraphic` + `icon`). **Leciono**: sukceso/UP-TO-DATE de
GPP ne pruvas, ke la enhavo estas ĉe Play — kontrolu per la API (atento: la
listing-GET ne montras bildojn; ili estas en apartaj kolektoj
`listings/<lng>/phoneScreenshots` ktp.).

### Revizio de la rapida korekto (sekvaĵoj)
La uzanto petis revizion de `b834483` ("ĉu estas io farinda post tiu tre rapida
korekto?"). Trovitaj kaj riparitaj:

- **Daŭra sciigo restus por ĉiam**: la rapida korekto aldonis `setOngoing(true)`
  al la alarm-sciigo, sed nenio nuligis ĝin kaj daŭraj sciigoj ne estas forviŝeblaj
  per svingo — post halto de la alarmo la sciigo restus en la ombrumo por ĉiam.
  Riparita: fiksa sciigo-ID (`SCIIGO_ID`), `AlarmoReceivilo.nuliguSciigon()`, kaj
  gardo en `onReceive` kiu nuligas la sciigon kiam la ludado (iam ludinta/konektinta)
  iĝas `Haltita` aŭ `Finita`. (Eraro ne nuligas — tiam la fallback-ringtono daŭre
  sonas.) Testo: `AlarmoReceiviloTest.alarmoSciigoNuligxigxasKiamLudadoHaltigxas`.
- **Sentry-spamo**: `logw` sur la atendata Android-12+ `setWindow`-vojo (ĉiufoje
  kiam alarmo skediĝas) → `logd` (loka nur).
- **Malnovaj komentoj**: `SciigPermeso` (referenco al la forigita
  `malfermuEkzaktajnAlarmAgordojn`), `MainActivity` ("full-screen-sciigo").
- **Morta enumero**: `DiagnozoTipo.EXACT_ALARMS` (neniu plu generas ĝin ekde la
  permeso-forigo) forigita kune kun la du testoj, kiuj ankoraŭ referencis ĝin.
- **CHANGELOG**: la permeso-entry estis sub `[Neeldonita]`, sed ĝi estas en la
  eldonita 3.0.3 (versionCode 248) → movita al `## [3.0.3]` (`### Ŝanĝita`).
- **AGENTS.md / NOTOJ.md**: tri respektive du malnovaj lokoj pri FSI kaj ekzaktaj
  alarmoj ĝisdatigitaj; nova datita leciono en AGENTS.md (regulo A11).

### Poste farita (ankora 2026-10-12)
- Etikedo `v3.0.3` movita al `b834483` — la fonto de la eldonita APK (versionCode 248).
- Ĉiuj sekvaĵoj de la revizio komititaj kaj puŝitaj al `master`.
- La CI-eldonado (`eldonado.yml`) ankoraŭ malsukcesas: la GitHub-sekretoj
  (KEYSTORE_BASE64, KEYSTORE_PASSWORD, PLAY_SERVICE_ACCOUNT_JSON) ne estas
  konfiguritaj — eldonoj devas esti farataj loke (kiel ĉi-foje).

### Rimarko (antaŭekzista, ne de ĉi tiu korekto)
- La fallback-ringtono (`luduFallbackRingtonon`) konservas neniun tenilon kaj
  havas neniun haltigan vojon — se ĝi eksonas, ĝi ripetiĝas ĝis la procezo mortas.
  Antaŭa problemo de PR #94, ne kaŭzita de la permeso-forigo.

# Ŝanĝoj — 2026-10-08

## Agentaj incidentoj kaj iliaj lecionoj (PR: #82)

Du incidentoj okazis dum la laboro pri PR #79/#80 kaj la eldono 3.0.2 — ambaŭ
riparitaj; la lecionoj estas registritaj en AGENTS.md (nova regulo A11 kaj kvar
novaj eroj en "Teknikaj scioj lernitaj dum la laboro").

1. **Stash-perdo kaj reakiro.** `git stash list` estis malplena kvankam la stash
   "eldono 3.0.2" (versio + release-notes + CHANGELOG) ekzistis. La komito troviĝis
   inter la nereatingeblaj objektoj (`git fsck --unreachable --no-reflogs`; kunfando
   kun 3 gepatroj, mesaĝo "On master: eldono 3.0.2 ...") kaj estis reakirita per
   `git stash store -m "..." <hash>`. Enhavo kontrolita antaŭ uzo: apoversio 3.0.2,
   versionCode 246, release-notes, CHANGELOG-sekcio; teknikaj dosieroj identaj al
   la laborarbo.
2. **Backtick-katastrofo en `gh pr create --body "..."`.** Bash interpretis la
   kodo-pecojn de la PR-teksto kiel komand-anstataŭigojn kaj plenumis ilin —
   inkluzive de `git tag v3.0.1`/`v3.0.2`, `git push origin v3.0.1`/`v3.0.2` kaj
   `./gradlew :androidApp:assembleDebug`. Tio kreis ambaŭ etikedojn sur la malĝusta
   komito (16ae6d6 — la kompar-ligo estus malplena) kaj plenigis la PR-korpon per
   gradle-eliro. Riparo: forigo de ambaŭ etikedoj (fore kaj loke), rekreo sur la
   ĝustaj komitoj (`v3.0.1` → 8c5ed4c, la vera fino de la eldono 3.0.1; `v3.0.2` →
   16ae6d6, la versio-ŝanĝa komito) kaj puŝo; la PR-korpo estis reverkita per
   `gh api -X PATCH` (`gh pr edit` fiaskas: GraphQL projectCards-malrekomendo).
   Leciono: neniam kodo-pecoj en komandoj kun duoblaj citiloj — uzu `--body-file`.
3. **Laborarbo de la uzanto.** La laborarbo povas enhavi la proprajn nekomititajn
   ŝanĝojn de la uzanto (troviĝis provizora "notu"-fragmento en CHANGELOG.md —
   verŝajne la komenco de la instrukcio "notu viajn problemojn…"). Ĝi estis
   konservita flanke (stash + patch) kaj ne enmetita en la commitojn.

## Diagnoza ekrano + aŭtomata daŭrigo en la fono (PR: #79)

### Aŭtomata daŭrigo el la fono (servo)

Uzanto raportis, ke ludado ne daŭris al la venonta elsendo kiam unu finiĝis
dum la apo estis en la fono. Logcat montris, ke nek la servo nek la apo
reagis al `STATE_ENDED` — sur Samsung la ludilo estas mortigita en la fono
(bateri-optimumado/MARs; krome privata DNS rompis la DNS-rezolvon).

- `EsperantoLudadoServo` ricevis `Player.Listener`: ĉe `STATE_ENDED` ĝi vokas
  `AppStato.ludvicoRegilo?.traktiFinonPublika(LudantoStato.Finita, erara = false)`
  en `serviceScope` — la sekva elsendo lanĉiĝas eĉ se la apo-procezo dormas
  aŭ estis mortigita.
- `LudvicoRegilo`: nova `@Volatile lastaTraktitaFontoId` gardas kontraŭ duobla
  traktado de la sama fino (kaj servo kaj apo povas vidi `STATE_ENDED`);
  nova publika `traktiFinonPublika()` + privata `fontoId()`; la gardilo
  estas nuligita en `ludiElsendon`.
- Testoj: `servoVoko_traktiFinonPublika_vokasAuxtoludon`,
  `servoVoko_duoblaTraktadoDeSamaFonto_estasIgnorata`.

### Diagnoza ekrano

La uzanto ne sciis, kiujn sistem-agordojn kontroli — do la apo mem kontrolas
ilin kaj gvidas al la ĝusta paĝo.

- `DiagnozoRegilo` (expect/actual): kontrolas bateri-optimumadon
  (`PowerManager.isIgnoringBatteryOptimizations`), sciig-permeson
  (`NotificationManagerCompat`), ekzaktajn alarmojn
  (`AlarmManager.canScheduleExactAlarms`) kaj DNS (`archive.org`; nur en la
  plena kontrolo). `malfermiSistemAgordon(intenco)` malfermas la ĝustan
  sistem-paĝon. Desktop/wasmJs/iOS: no-op.
- `Vojo.Diagnozo` + `DiagnozoEkrano`: kartoj kun ikono laŭ severeco
  (INFO/AVERTO/ERARO), butono al la sistem-agordo kaj "Re-kontroli".
- Avertosigno sur la ĉefekrano (ruĝa avert-ikono en la supra breto, nur se
  estas problemo kun severeco AVERTO/ERARO) kondukas rekte al la diagnozo;
  kroma ligilo en Agordoj → Sistemo → Diagnozo. `AgordojEkrano` nun estas
  rulebla (antaŭe ne).
- `AppStato` kreas la `DiagnozoRegilon` kaj lanĉas fonan `kontroliRapide()`
  ĉe starto.
- Testoj: `DiagnozoTest` (7 testoj: modeloj + desktop no-op).

### Kontrolo

`./gradlew :shared:desktopTest` — ĉiuj testoj pasas (inkluzive de la novaj);
`./gradlew :androidApp:assembleDebug` — sukcesa.

## Frontpaĝo: aktivaj kanaloj, "Arkivo"-dividilo; markilo ĉiam montras la aĝon (PR: #78)

La "Kanaloj"-vico sur la frontpaĝo dividas la kanalojn: aktivaj (kun elsendo
dum la pasinta jaro) aperas unue, poste dividilo kun la etikedo "Arkivo",
poste la arkivaj kanaloj (lasta elsendo pli aĝa ol unu jaro). La flava markilo
sur kanalo ĉiam montras la tempon de la plej nova elsendo — neniam la
ludprogreson ("aŭdis X%").

- `HejmoViewModel` liveras `aktivajKanaloj`/`arkivajKanaloj` (anstataŭ `cxiujKanaloj`);
  la divido okazas en la pura funkcio `dividuKanalojn` (limo 365 tagoj).
- `kalkuliNovectempon` forigita; `kalkuliAĝon` (ĉiam redonas la aĝon: "hodiaŭ" …
  "11 monatoj", "1 jaro", "2 jaroj") estas uzata por la markilo sur kanaloj kaj sur
  elsendo-kartoj. Du predikatoj super la ĝenerala `estasEneDe(dato, tagoj)`:
  `estasEneDeSesMonatoj` (180 tagoj — regas "Kio novas") kaj `estasEneDeUnuJaro`
  (365 tagoj — regas aktivan kontraŭ arkivan en `dividuKanalojn`).
- `ArkivoDividilo`: vertikala linio + etikedo "Arkivo" dekstre de ĝi; aperas nur se
  estas arkivaj kanaloj. Antaŭvido ĝisdatigita (aktiva "Meznova Radio" + arkiva
  "Antikva Radio").
- Testoj: 5 puraj (`HejmoLogikoTest`: aĝo, limoj 179/180 kaj 364/365 tagoj, divido)
  + 2 UI (`HejmoEkranoTest`: dividilo kaj ordo inter nova/meznova/arkiva kanaloj;
  markilo "5 tagoj" eĉ se 42% aŭdita).
- Ĝisdatigo (nekomitita, laŭ peto de la uzanto): la sojlo aktiva/arkiva ŝanĝita
  de 6 monatoj al 1 jaro (365 tagoj) — "Kio novas" restas je 6 monatoj (180 tagoj).

## Malnova apo: Sentry 5.3.0 → 8.41.0 por 16 KB-paĝoj (PR: #78)

La malnova apo ne instaliĝis sur 16 KB-aparatoj (Android 15+): la
`libsentry.so`/`libsentry-android.so` el `sentry-android-ndk:5.3.0` havis LOAD-segmentojn
laŭliniajn nur al 4 KB (0x1000) — precize la du bibliotekoj el la erarmesaĝo.
Sentry Android SDK 8.0.0 aldonis plenan 16 KB-subtenon; la nova apo jam uzas
sentry-android 8.41.0 (per sentry-kmp 0.27.0) kaj estas en ordo.

- `malnova/app/build.gradle`: `io.sentry:sentry-android:5.3.0` → `8.41.0` (memversio
  kun la nova apo). La uzata API (`SentryAndroid.init` + `setDsn` + `setBeforeSend`,
  `Sentry.captureException`/`captureMessage`, `SentryLevel`) estas stabila en 8.x;
  minSdk 26 ≥ 21 (postulo de 8.x); compileSdk 35 ✓.
- Kontrolo: ELF-kontrolo de la .so en la nova `app-debug.apk` — ĉiuj LOAD-segmentoj
  0x4000 (arm64-v8a kaj x86_64); 5.3.0 havis 0x1000. La nova apo (8.41.0) same en ordo.

## AGENTS.md: neniu commit antaŭ trarigardo de la uzanto (PR: #78)

Regulo L6 en "Git-laborfluo" ŝanĝita: la agento ne commitas (nek puŝas, nek kreas
PR) antaŭ ol la uzanto trarigardis kaj aprobis la ŝanĝojn — eĉ se la uzanto petas
commit/push/PR; nur eksplicita "commit sen mia trarigardo" preterpasas tion.
La sama regulo aplikita en la tri lokaj klonoj (mobile-app4, EsperantoRadio, EsperantoRadio2).

---

# Ŝanĝoj — 2026-10-06

## Reprovo ĉe servileraroj 5xx sur Android (PR #76)

`ERROR_CODE_IO_BAD_HTTP_STATUS` estis klasata kiel daŭra eraro, do la ludvico
tuj saltis al la sekva elsendo — ankaŭ kiam la eraro estis pasema HTTP 500
(archive.org-Edge-nodoj intermite redonas 500 por sanaj dosieroj; dua provo
kutime sukcesas ĉar la redirekto elektas alian nodon).

Nova `EraroKlasifiko.kt` (androidApp): 5xx en la kaŭz-ĉeno
(`InvalidResponseCodeException.responseCode`) → reprovebla (la ekzista
eksponenta reprovo ĝis 10 fojojn); 4xx, formato kaj malkodiloj restas daŭraj.
6 novaj instrumentitaj testoj (`EraroKlasifikoTest`); 11 instrumentitaj
testoj entute, 0 fiaskoj.

---

# Ŝanĝoj — 2026-09-26

## Peranto: vera MP3-dosiernomo el archive.org-metadatenoj (PR #75)

La Peranto-parsilo divensis la MP3-dosiernomon de archive.org (`<identigilo>.mp3`);
en 4 el la 25 videblaj elsendoj (7 el la lastaj 50) la vera nomo estis alia
(ekz. `malapero-benda-3` → `Malapero_Benda3.mp3`) → HTTP 404, do ne ludeblis.

- `parsuPeranto` nun demandas `https://archive.org/metadata/<id>` (paralele,
  5-s-tempolimo, unufoje po identigilo) kaj uzas la veran originalan `.mp3`-nomon;
  rezulto persistata en `ArchiveOrgDosiernomoKasho` (Settings+JSON; uzata de
  `AppStato` kaj `NovajElsendojKontroloWorker`). Reteraro → retrofalo al la diveno.
- **API-ŝanĝo**: `parsuRss`, `parsuPeranto`, `leguKashitajnElsendojn`,
  `leguĈiujnKashitajnElsendojn` iĝis `suspend`; neniuj alvokantoj ŝanĝiĝis.
- Kontrolo: 202 labortablaj + 5 instrumentitaj testoj, 0 fiaskoj; Android/
  Desktop/wasmJs kompilas. Detala problem-analizo kaj kialoj: PR #75.
- **Sciate ne nia cimo**: archive.org intermite redonas 500 por kelkaj
  arkivaĵoj (difektaj edge-nodoj, ekz. `auskultu_ripetu120`); Android tiam
  klasas ĝin kiel daŭran eraron kaj saltas al la sekva elsendo.

---

# Ŝanĝoj — 2026-09-23

Ĉi tiu dokumento priskribas ĉiujn ŝanĝojn faritajn surbaze de la "Farota"-listo en `README.md`,
plus la riparon de la mini-ludilbreto post rekreo de la Activity (7) kaj la sekvajn riparojn 8–11
(alarmoj, sciigo "Ludi", Web-konstruo) kaj 12 (instrumentitaj UI-testoj).

Rezumo:

| # | Tasko | Stato |
|---|---|---|
| 1 | Malsupren-tiro refreŝigas, kun videbla indiko | ✅ |
| 2 | Alarmo ekzakta ĝis 10 minutoj | ✅ |
| 3 | Vekhorloĝo ludas podkaston (ne nur livestream) | ✅ |
| 4 | Eksponenta reprovo + klara stato ("Konektas…") | ✅ |
| 5 | Muzaiko-livestream (HLS) — kontrolo kaj riparo sur Android | ✅ |
| 6 | Revizio de malnova `eo_strings.xml` | ✅ (rezulto en README) |
| 7 | Mini-ludilbreto malaperis post rekreo de la Activity | ✅ |
| 8 | Alarmo-redaktilo perdis la etikedon | ✅ |
| 9 | Unufoja alarmo restis "aktiva" post ekigo | ✅ |
| 10 | Sciigo "Ludi" — testo sur aparato (trovis 2 kraŝojn) | ✅ |
| 11 | Web-konstruo (wasmJs) rompita | ✅ (konstruiĝas kaj rulas; CORS-limo restas) |
| 12 | `AndroidUiTest` malsukcesis ("No compose hierarchies found") | ✅ |

Testoj: **195** labortablaj (antaŭe 166), ĉiuj pasas per `./gradlew :shared:desktopTest`,
plus 5 instrumentitaj Android-testoj (`AndroidUiTest` ×4, `SciigoLudiTest`) per
`./gradlew :androidApp:connectedDebugAndroidTest`.
Android-APK konstruiĝas per `./gradlew :androidApp:assembleDebug` (JDK 17).
Ĉiuj taskoj estis mane kontrolitaj sur la emulilo (Pixel 9, API 35) — vidu "Kontrolo" ĉe ĉiu tasko.

---

## 1. Malsupren-tiro por refreŝigi

### Problemo
- Ne ekzistis malsupren-tiro (pull-to-refresh) — nek sur Hejmo, nek sur Kanaloj.
- Pli grave: `sxargxi()` en la ViewModel-oj neniam pasis `fortoRefresigi = true`. Paŝo 1 (diskkaŝmemoro)
  plenigas la memoran kaŝmemoron de `ElsendoDeponejoImpl`, do paŝo 2 ("reto") tuj revenis el la memoro
  (`ElsendoDeponejoImpl.sxargxiElsendojn`, `if (kaŝenitaj != null && !fortoRefresigi) return kaŝenitaj`).
  Tial neniu nova RSS estis elŝutita, kaj la ŝarĝ-indiko neniam estis videbla (`sxargxas` estis `true`
  nur dum kelkaj mikrosekundoj).

### Ŝanĝoj
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/ui/HejmoEkrano.kt`
  - `HejmoViewModel.sxargxi(fortoRefresigi: Boolean = false)` — pasas la parametron al
    `elsendoDeponejo.sxargxiElsendojnPorKanal(kanalo, fortoRefresigi)`; la protokolo montras la valoron.
  - La `LazyColumn` estas envolvita en Material3 `PullToRefreshBox(isRefreshing = sxargxas, onRefresh = { vm.sxargxi(fortoRefresigi = true) })`.
  - Forigita la malgranda spinilo en la TopAppBar (la PullToRefreshBox nun montras la indikon).
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/ui/KanalaroEkrano.kt` — same por `KanalaroViewModel` kaj la Kanaloj-langeto.
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/ui/KanalEkrano.kt` — same por `KanaloViewModel` kaj la kanalvido.
  - `isRefreshing = sxargxas && elsendoj.isNotEmpty()` — dum la unua ŝarĝo la granda centra spinilo jam montras la staton.
  - Forigita la inline-spinilo supre de la elsendlisto.
  - La PullToRefreshBox estas interne de ĉiu paĝo de la `HorizontalPager` (svipo inter kanaloj), do ne konfliktas.

### Testo
- `shared/src/commonTest/kotlin/dk/nordfalk/esperanto/ui/HejmoEkranoTest.kt` — nova
  `malsuprenTiroRefresxigasKunForto`: la falsa deponejo registras la `fortoRefresigi`-parametron; post
  `swipeDown` sur la radiko ĉiuj novaj vokoj havu `fortoRefresigi = true`.

### Kontrolo
Emulilo: tiro sur Hejmo kaj Kanaloj montras la rondan indikon; logcat:
`Klako: malsupren-tiro (Hejmo)` → `HejmoViewModel: Ŝargas elsendojn por 18 kanaloj (fortoRefresigi=true)` → `elŝutas RSS-fluon: …`.

---

## 2. Alarmo ekzakta ĝis 10 minutoj

### Problemo
`shared/src/androidMain/.../data/repository/AlarmoSkedilo.kt`:
- Se `canScheduleExactAlarms()` estis false (defaŭlte sur Android 14+), la alarmo **tute ne estis skedita**,
  kaj la deponejo provis `startActivity(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)` — kiu povas esti blokita
  (ekz. el `BootReceivilo`).
- Ripetantaj alarmoj ne estis re-skeditaj post ekigo — la sekva okazo venis nur post redakto aŭ restarto.
- `kalkuluNexxtemTempon` (Calendar-bazita) ne havis testojn.

### Ŝanĝoj
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/domain/model/Alarmo.kt`
  - Nova pura funkcio `Alarmo.sekvaEkigo(nun: LocalDateTime): LocalDateTime` (kotlinx-datetime).
    Unufoje: hodiaŭ se ankoraŭ ne pasis, alie morgaŭ. Ripeto: la unua tago (hodiaŭ inkluzive) kies bito
    (0x01=lundo … 0x40=dimanĉo) estas en la bitmasko kaj kies tempo estas *post* `nun`.
- `shared/src/androidMain/.../data/repository/AlarmoSkedilo.kt`
  - Kun permeso: `setAlarmClock(AlarmClockInfo(tempo, montroIntent), pi)` — escepto de Doze kaj la sistemo
    montras alarm-ikonon.
  - Sen permeso: `setWindow(RTC_WAKEUP, tempo, 10 min, pi)` — ne postulas permeson; la alarmo ekigas ene de 10 minutoj.
  - Forigita la `startActivity` el la deponejo.
  - `kalkuluNexxtemTempon` nun uzas `sekvaEkigo` + `TimeZone.currentSystemDefault()`.
- `shared/src/commonMain/.../data/repository/AlarmoSkedilo.kt` + ĉiuj `actual`-oj (android/desktop/wasmJs/ios):
  - `expect fun ekzaktajAlarmojPermesataj(): Boolean`
  - `expect fun malfermuEkzaktajnAlarmAgordojn()`
- `shared/src/commonMain/.../ui/AlarmoEkrano.kt` — nova `EkzaktajAlarmojAverto` (ruĝa karto kun butono
  "Permesi") montrata kiam estas aktivaj alarmoj kaj la permeso mankas. Rekontrolas ĉiun 2s dum la
  averto estas videbla (la uzanto donas la permeson en la sistemaj agordoj kaj revenas).
- `shared/src/androidMain/.../data/repository/AlarmoReceivilo.kt` — post ekigo, se `ripeto != 0`, skedas la
  sekvan okazon. (Poste movita al `PersistantaAlarmoDeponejo.ekigis` — vidu tasko 9.)
- `shared/src/androidMain/.../data/repository/BootReceivilo.kt`
  - Traktas ankaŭ `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` → re-skedas ĉiujn alarmojn
    (de `setWindow` al `setAlarmClock`).
  - Nova `internal fun leguPersistitajnAlarmojn()` — legas la alarmojn rekte el Settings (sen AppStato).
- `androidApp/src/androidMain/AndroidManifest.xml` — la intent-filtrilo de `BootReceivilo` ricevis
  `android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.

### Testo
- `shared/src/commonTest/kotlin/dk/nordfalk/esperanto/domain/model/AlarmoTest.kt` — 9 testoj: unufoje
  hodiaŭ/morgaŭ, sama minuto = morgaŭ, ĉiutage, semajnfino, nur merkredo, transiro de semajno, monato kaj jaro.

### Kontrolo
Emulilo: sen permeso `dumpsys alarm` montras `window=+10m0s0ms` kaj la averto videblas; post
`appops set … SCHEDULE_EXACT_ALARM allow` la BootReceivilo re-skedis (`window=0 exactAllowReason=permission`)
kaj la averto malaperis.

---

## 3. Vekhorloĝo ludas la plej freŝan podkaston

### Problemo
`MainActivity.traktuAlarmIntent` ludis nur `rektaElsendaSonoUrl`. Nur Muzaiko havas livestream, do 4 el la
6 alarm-sugestoj (peranto, polaretradio, radiohavanokubo, radiovatikana) nur ludis la ringtonon.

### Ŝanĝoj
- Nova `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/domain/player/AlarmoElekto.kt` —
  `elektuAlarmElsendon(elsendoj, ludatoj): Elsendo?`: nur elsendoj kun sono-URL; la plej nova (laŭ dato) kiu
  ne estas finaŭskultita nek erara; se ĉiuj estas aŭskultitaj, la plej nova entute.
- `androidApp/src/androidMain/kotlin/dk/nordfalk/esperanto/android/MainActivity.kt`
  - Por podkasta kanalo: nova `trovuAlarmElsendon(kanalo)` (en `Dispatchers.IO`) provas laŭvice: reton
    (`sxargxiElsendojn(kanalo, fortoRefresigi = true)`), diskkaŝmemoron, kaj elŝutitajn elsendojn — ĉar
    ĉe vekiĝo la reto ofte ankoraŭ ne pretas. Atendas ĝis 5s ke `AppStato` estu inicialigita (la alarmo
    eble lanĉis la apon).
  - Ludas per `AppStato.ludvicoRegilo.ludiElsendon(elsendo)` — kiu preferas elŝutitan dosieron kaj
    resumas de savita pozicio.
  - La ekzistanta 10s-kontrolo (Eraro → ringtono) restas.

### Testo
- `shared/src/commonTest/kotlin/dk/nordfalk/esperanto/domain/player/AlarmoElektoTest.kt` — 6 testoj.

### Kontrolo
Emulilo: simulita alarmo por `peranto` → `Alarmo ludas podkaston: peranto:2026-09-23:…` kaj
`dumpsys media_session` montris `state=PLAYING`.

---

## 4. Eksponenta reprovo kaj klara stato

### Problemo
- Neniu ludilo reprovis. `LudvicoRegilo` traktis ĉiun `Eraro` kiel finon: por podkasto ĝi saltis al la
  sekva elsendo (kaj markis la nunan erara), por livestream nenio okazis.
- Desktop: reta eraro mez-flue fariĝis `Finita` (naturfino) → la elsendo estis markita finaŭskultita.
- La UI montris "Konektas..." nur kiel teksto; la butono montris "ludi" dum konektado.

### Ŝanĝoj
- Nova `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/domain/player/ReprovoLogiko.kt` —
  `atendoMs(provo)`: 1s, 2s, 4s, 8s, 16s, poste 30s; maks 10 provoj (~3 minutoj entute); `null` = rezignu.
  La malnova `Afspiller` atendis entute nur ~10s, kio ofte ne sufiĉas por poŝtelefona reto.
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/domain/model/Modeloj.kt` —
  `LudantoStato.Eraro(mesagho, reprovebla: Boolean = true)`. Daŭraj eraroj (HTTP 404, nesubtenata formato)
  ne estas reprovataj — tiam la konduto estas kiel antaŭe (salti al la sekva).
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/domain/player/LudvicoRegilo.kt`
  - Nova `val reprovo: StateFlow<Int>` (0 = neniu reprovo).
  - Ĉe `Eraro(reprovebla = true)`: `provuReprovi()` planas reprovon de la **sama** fonto de la lasta pozicio
    (por livestream de 0). Lokaj dosieroj ne estas reprovataj.
  - Nuligo: se la uzanto dume haltigis aŭ elektis alion (stato ne plu `Eraro`, aŭ alia fonto), aŭ vokis
    `ludiElsendon`/`halti()`. Generacio-nombrilo (`reprovoGeneracio`) certigas ke nur la plej lasta
    reprovo agas.
  - Sinkrona malsukceso (Desktop: la stato restas la sama `Eraro`, do `distinctUntilChanged` ne vidas
    ŝanĝon) estas traktita en la reprova korutino mem.
  - Nuligo de la nombrilo kiam `Ludas`.
  - Nova konstruila parametro `reprovoAtendo: (Int) -> Long?` (defaŭlte `ReprovoLogiko::atendoMs`) — por testoj.
- `androidApp/.../ExoPlayerLudiloRegilo.kt` — `estasReprovebla(errorCode)`: reto, tempolimo,
  `BEHIND_LIVE_WINDOW`, nespecifita → reprovebla; HTTP-stato, dosiero ne trovita, formato, malkodilo → ne.
- `shared/src/desktopMain/.../DesktopLudiloRegilo.kt` — legado-eraro mez-flue → `Eraro("Reta eraro: …")`
  anstataŭ `Finita`; `FileNotFoundException` kaj malplena URL → `reprovebla = false`. La escepto nun
  estas protokolita per `loge` (regulo A8).
- `shared/src/wasmJsMain/.../WasmJsLudiloRegilo.kt` — `MediaError.code == 2` (MEDIA_ERR_NETWORK) → reprovebla.
- `shared/src/commonMain/.../domain/player/LudiloRegilo.kt` — `NoOpLudiloRegilo.simuluEraron(mesagho, reprovebla = false)`.
- `shared/src/commonMain/.../ui/MiniLudilbreto.kt` — nova parametro `reprovo: Int`:
  - "Konektas… (provo 3/10)" dum reprovo;
  - `CircularProgressIndicator` anstataŭ la ludi-ikono dum konektado/reprovo;
  - post rezigno: "Ne eblas ludi — kontrolu la retkonekton" (anstataŭ "Eraro: <teknika mesaĝo>").
- `shared/src/commonMain/.../App.kt` — pasas `ludvicoRegilo.reprovo` al `MiniLudilbreto`.

### Testoj
`shared/src/commonTest/.../domain/player/LudvicoRegiloTest.kt` — 6 novaj:
- `reprovo_pasemaEraroReprovasSamanElsendonDePozicio`
- `reprovo_dauxraEraroNeEstasReprovata`
- `reprovo_rezignasPostMaksKajSaltasAlSekva` (kun `CxiamEraraLudilo` — sinkrona malsukceso)
- `reprovo_rektaKanaloEstasReprovata`
- `reprovo_nuligitaSeUzantoHaltigas`
- `reprovoLogiko_eksponentaKunMaksimumo`

La ekzistantaj erar-testoj (404 → salti) pasas senŝanĝe.

### Kontrolo
Emulilo: aviadila reĝimo dum ludado → "Konektas… (provo 4/10)" kun spinilo; reto reŝaltita →
`Reprovo 5 sukcesis`, ludado daŭris de 53 s.

---

## 5. Muzaiko-livestream (HLS)

### Rezulto
- La fluo mem funkcias: `https://fluo.muzaiko.info/hls/muzaiko/live.m3u8` → HTTP 200, tri variantoj
  (aac_lofi/midfi/hifi), segmentoj ~81 KB.
- **Sur Android ĝi tute ne ludis**: logcat montris
  `MediaSessionStub: … ClassNotFoundException: androidx.media3.exoplayer.hls.HlsMediaSource$Factory`.
  La sesio silente restis en `state=NONE`.

### Ŝanĝoj
- `gradle/libs.versions.toml` — nova `androidx-media3-exoplayer-hls` (sama versio 1.5.1).
- `androidApp/build.gradle.kts` — `implementation(libs.androidx.media3.exoplayer.hls)`.

### Kontrolo
Emulilo: Kanaloj → Muzaiko → `state=PLAYING`, neniu ClassNotFoundException.

HLS sur Desktop/Web restas ne-subtenata (bezonas VLCJ/hls.js) — ekster la amplekso.

---

## 6. Revizio de `eo_strings.xml`

Analizo de ĉiuj ĉenoj en `malnova/app/src/main/res/values/eo_strings.xml`. La mankantaj uzkazoj estas
listigitaj en `README.md` sub "Farota": konigi elsendon, elŝuti nur per WiFi, tuj ludi ĉe malfermo,
loko de elŝutoj / libera spaco, konfirmo antaŭ forigo, "Ĉu ĉesi ludi? / Daŭrigi fone", nombro da novaj
elsendoj ĉe ŝatataj, "Nun estas ludata" por la livestream (`rektaElsendaPriskriboUrl` estas legata sed ne
montrata), Pri/Kontakto-ekrano, averto pri la fidindeco de la vekhorloĝo.

---

## 7. Mini-ludilbreto malaperis post rekreo de la Activity

### Problemo
`MainActivity.onCreate` kreis **novan** `ExoPlayerLudiloRegilo` ĉiufoje, kaj `onDestroy` liberigis ĝin
(`release()` → `controller = null`). Sed `AppStato` estas procez-nivela kaj `AppStato.inicialigu` estas
idempotenta, do `AppStato.ludvicoRegilo` daŭre tenis la **unuan** instancon. Post rekreo (sistemo detruis
la aktivecon dum fona ludado, alarmo lanĉis novan instancon, ktp.):

1. La UI (`MiniLudilbreto`, `ludilo.stato`) observis la novan instancon, kies `nunaFonto` estis `null` →
   la breto kaŝiĝis, kvankam la servo daŭre ludis.
2. **Pli grave:** ĉiu ludado tra `LudvicoRegilo` (hejmekrano, kanalvido, ludvico, aŭtoludo, alarmo) iris
   al la malnova instanco kun `controller = null` → `setMediaItem` estis silenta no-op. Logcat montris
   "Fiksas fonton al url: …" sed la servo daŭre ludis la malnovan elsendon.

Ankaŭ: ludado komencita de la sciigo "Ludi" (`LudiElsendoReceivilo`, propra MediaController) estis
nevidebla por la apo, ĉar nur la apo mem sciis kiun `Sonfonto` ĝi ludas.

Reproduktita sur la emulilo per `am start -f 0x10008000` (NEW_TASK|CLEAR_TASK — detruas kaj rekreas la
aktivecon, la procezo kaj la servo pluvivas).

### Ŝanĝoj
- `androidApp/src/androidMain/kotlin/dk/nordfalk/esperanto/android/ExoPlayerLudiloRegilo.kt`
  - **Procez-nivela instanco**: privata konstruilo + `ExoPlayerLudiloRegilo.akiru(context)` (duoble
    kontrolita ŝlosado). La MediaController vivas tiel longe kiel la procezo.
  - `release()` forigita — neniu plu rajtas malkonekti la komunan kontrolilon.
  - La `Sonfonto` estas konservata kiel JSON en `MediaMetadata.extras` (`getMediaMetadata`).
  - Ĉe konekto al la servo: se `nunaFonto == null`, ĝi estas restarigita el `controller.currentMediaItem`.
  - Nova `onMediaItemTransition` en la listener: se alia MediaController ŝanĝis la aĵon, `nunaFonto`
    estas ĝisdatigita el ties ekstraĵoj. (`halti()` → `clearMediaItems` → transiro al `null` → nenio
    restarigita, do la breto ne reaperas.)
- `androidApp/src/androidMain/kotlin/dk/nordfalk/esperanto/android/MainActivity.kt`
  - `ludilo = ExoPlayerLudiloRegilo.akiru(this)` anstataŭ `ExoPlayerLudiloRegilo(this)`.
  - `onDestroy` ne plu liberigas la ludilon.
- Nova `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/domain/player/SonfontoKodilo.kt` —
  `kodigu(Sonfonto): String` / `malkodigu(String?): Sonfonto?` (kotlinx.serialization; la `Sonfonto`-sigelita
  interfaco jam estis `@Serializable`). Nemalkodebla teksto → `null` + `logw`.
- `shared/src/androidMain/kotlin/dk/nordfalk/esperanto/data/repository/LudiElsendoReceivilo.kt` — konstruas
  `Elsendo` el la intent-ekstraĵoj (id, kanalo, titolo, priskribo, bildo, dato, fluo) kaj metas
  `Sonfonto.ElsendoFonto` en la MediaMetadata-ekstraĵojn.

### Konsekvenco por la servo-vivciklo
La apo nun tenas la MediaController konektita dum la tuta procezo (antaŭe: nur dum la Activity vivis).
Kiam nenio ludas, `EsperantoLudadoServo` do restas ligita (bound, ne malfona) ĝis la procezo mortas.
Tio estas senkosta: ne-malfona ligita servo ne malhelpas al Android mortigi la kaŝitan procezon.

### Testo
- `shared/src/commonTest/kotlin/dk/nordfalk/esperanto/domain/player/SonfontoKodiloTest.kt` — 2 testoj:
  ĉiuj tri Sonfonto-specoj tra kodigo/malkodigo; malplena/fuŝa/nekonata teksto → `null`.

### Kontrolo (emulilo)
| Scenaro | Antaŭe | Nun |
|---|---|---|
| Ludi → rekrei la aktivecon | breto malaperis | breto restas ("La 197a elsendo 1a parto · Ludas") |
| Post rekreo: ludi alian elsendon el Hejmo | silenta no-op, la malnova daŭris | la nova ludas, breto ĝisdatiĝas |
| Halti post rekreo | — | breto kaŝiĝas kaj ne reaperas |
| Alarmo lanĉas duan aktiveco-instancon | breto kaŝita | breto montras la alarm-elsendon |

**Ne testita sur aparato:** la sciigo-"Ludi"-vojo (`LudiElsendoReceivilo` ne estas eksportita, do ne
ekigebla per `adb`). Ĝi estas kovrita nur de la kompilo kaj `SonfontoKodiloTest`.

---

## 8. Alarmo-redaktilo perdis la etikedon

### Problemo
`AlarmoRedaktilo` (en `AlarmoEkrano.kt`) konstruis la novan `Alarmo` sen `etikedo`. Redakti ekzistantan
alarmon (ekz. la sugeston "Muzaiko matene labortage") kaj premi "Konservi" forigis la etikedon.

### Ŝanĝo
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/ui/AlarmoEkrano.kt` — `etikedo = ekzistanta?.etikedo`.
  La redaktilo ne havas kampon por la etikedo, do la ekzistanta estas konservata.

### Testo
- `AlarmoEkranoTest.redaktadoKonservasEtikedon` — klaku la alarmon, premu "Konservi", la etikedo restas.

---

## 9. Unufoja alarmo restis "aktiva" post ekigo

### Problemo
Post kiam unufoja alarmo (`ripeto == 0`) sonis, ĝi restis ŝaltita en la UI kaj en Settings — kvankam
AlarmManager ne plu havis ĝin. Post restarto `BootReceivilo` eĉ re-skedus ĝin por la sekva tago.

### Ŝanĝoj
- `shared/src/commonMain/kotlin/dk/nordfalk/esperanto/data/repository/PersistantaAlarmoDeponejo.kt` —
  nova `fun ekigis(alarmoId)` (ne `suspend`, vokebla el `onReceive`):
  - unufoja → `aktiva = false` + persisto;
  - ripetanta → `skedilo.skedi(alarmo)` por la sekva okazo;
  - nekonata aŭ malaktiva → nur protokolo.
- `shared/src/androidMain/.../data/repository/AlarmoReceivilo.kt` — vokas `ekigis`. Se la apo vivas, per
  `AppStato.alarmoDeponejo` (por ke la UI tuj vidu la ŝanĝon kaj ne poste superskribu ĝin per malnova
  listo); alie per provizora `PersistantaAlarmoDeponejo(kreuSettings(), skedilo = AlarmoSkedilo())`.
  Tio anstataŭas la re-skedan logikon de tasko 2, kiu nun estas en unu loko.

### Testo
- Nova `shared/src/desktopTest/kotlin/dk/nordfalk/esperanto/data/repository/PersistantaAlarmoDeponejoTest.kt`
  (3 testoj) kun vera persisto (`PreferencesSettings` en provizora `java.util.prefs`-nodo, forigita post la
  testo): unufoja malaktivigita **kaj** legebla tiel de nova deponejo; ripetanta restas aktiva; nekonata ID
  ŝanĝas nenion.

---

## 10. Sciigo "Ludi" — testo sur aparato

La vojo "sciigo pri nova elsendo → Ludi-butono → `LudiElsendoReceivilo`" estis netestita. La nova
instrumentita testo trovis **du antaŭekzistantajn cimojn** — la butono neniam funkciis:

1. **Kraŝo:** la Receivilo kreis `MediaController` per sia propra kunteksto. Tiu estas
   `ReceiverRestrictedContext`, kaj `bindService()` ĵetas
   `ReceiverCallNotAllowedException: BroadcastReceiver components are not allowed to bind to services`
   → la apo kraŝis ĉe ĉiu premo de "Ludi".
2. **Malĝusta fadeno:** la aŭskultanto rulis en `Executors.newSingleThreadExecutor()`, sed MediaController
   rajtas esti vokata nur el la fadeno de sia Looper → `IllegalStateException` en `setMediaItem()`
   (`verifyApplicationThread`). Eĉ sen la kraŝo nenio estus ludinta.

### Ŝanĝoj
- `shared/src/androidMain/.../data/repository/LudiElsendoReceivilo.kt` — `context.applicationContext` por
  `SessionToken` kaj `MediaController.Builder`; `ContextCompat.getMainExecutor(...)` por la aŭskultanto.

### Testo
- Nova `androidApp/src/androidTest/kotlin/dk/nordfalk/esperanto/android/SciigoLudiTest.kt` — sendas la saman
  elsendaĵon kiel la sciigo-butono (kun la samaj ekstraĵoj) kaj kontrolas ke la procez-nivela ludilo
  rekonas la elsendon (`nunaFonto` = `ElsendoFonto` kun la ĝusta id, titolo, kanalnomo). La mini-ludilbreto
  montras ĝuste `ludilo.stato.nunaFonto`, do tio kovras la breton.
  Rulu: `./gradlew :androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dk.nordfalk.esperanto.android.SciigoLudiTest`
- Rezulto sur la emulilo: unue malsukcesis kun la kraŝo, poste kun la fadeno-eraro, nun pasas.
  Logcat: `LudiElsendo: Komencis ludi: Testa elsendo el sciigo` → `Ludilo: Ludata aĵo ŝanĝiĝis ekstere — nun: ElsendoFonto`.

### Noto pri la testa infrastrukturo
La testo uzas `ActivityScenarioRule` kaj kontrolas la staton de la ludilo, ne la UI. Tiam la
Compose-testregulo ne funkciis en la projekto — vidu tasko 12 por la kaŭzo kaj riparo.

Mi provis unue ekigi la veran WorkManager-laboron (`cmd jobscheduler run -f -n androidx.work.systemjobscheduler …`),
sed perioda laboro ne rulas antaŭ sia horaro ("being executed before schedule") — tial la instrumentita testo.

---

## 11. Web-konstruo (wasmJs)

`:webApp:compileKotlinWasmJs` malsukcesis kun 55 eraroj. Kvar sendependaj kaŭzoj, poste du rultempaj:

### Kompilaj kaŭzoj
1. **navigation3 nelegebla** — la plej granda parto de la eraroj (`Unresolved reference 'navigation3'`,
   `NavKey`, `NavDisplay`…). `navigation3-runtime-wasm-js:1.1.1` estas kompilita per **Kotlin 2.3.10**
   (klib ABI 2.3.0); nia Kotlin 2.2.20 ne povas legi ĝin. Sur JVM/Android la jar-oj funkcias, tial nur Web
   rompiĝis. Mi kontrolis la `compiler_version` en la klib-manifesto de ĉiu versio:

   | JetBrains navigation3-ui | kompilita per | runtime | runtime kompilita per |
   |---|---|---|---|
   | 1.1.0-alpha02 | 2.2.20 | 1.1.0-alpha02 | 2.2.20 |
   | 1.1.0-alpha03 | 2.3.0 | 1.1.0-alpha04 | 2.3.0 |
   | 1.1.1 (antaŭe) | — | 1.1.1 | 2.3.10 |

   → `gradle/libs.versions.toml`: `multiplatform-nav3-ui = "1.1.0-alpha02"`. La API sufiĉas por la
   ekzistanta kodo (Desktop, Android kaj ĉiuj testoj pasas senŝanĝe). Ĝisdatigu nur kune kun Kotlin 2.3.
2. **`javaClass`** (nur JVM) en `App.kt` (4×) → `it::class.simpleName`.
3. **`@Volatile`** en `SciigoKontroloj.kt` sen importo — sur JVM `kotlin.jvm.Volatile` estas aŭtomate
   importita, sur Wasm ne → `import kotlin.concurrent.Volatile` (kiel `LudvicoRegilo` jam faris).
4. **`PlatformLigilo.kt` (wasmJs)** uzis `dynamic` kaj `js("encodeURIComponent")` kiel valoron — ambaŭ
   nur por Kotlin/JS. Reskribita: `private fun kodiguUriKomponanton(teksto: String): String = js("encodeURIComponent(teksto)")`
   (en Wasm `js()` devas esti la tuta korpo de supra-nivela funkcio), `catch (e: Throwable)`.
5. **`webApp/.../Main.kt`**: `CanvasBasedWindow` estas malrekomendita-kiel-eraro en Compose 1.10 →
   `ComposeViewport(viewportContainerId = "ComposeTarget")`; `index.html`: `<canvas>` → `<div>`.

### Rultempaj kaŭzoj (trovitaj per headless Chrome)
6. **`leguBundledKanalkonfiguron ne jam implementita por wasmJs`** → la apo tuj kraŝis. En la retumilo
   resurcoj legeblas nur nesinkrone, sed la `expect fun` estas sinkrona. Solvo: nova Gradle-tasko
   `generuEnigitanKanalkonfiguron` (`shared/build.gradle.kts`, sama ŝablono kiel `generuApoVersio`) generas
   `EnigitaKanalkonfiguro.kt` kun la JSONC kiel Kotlin-ĉeno (eskapitaj `\`, `"`, `$`, linifinoj) por
   `wasmJsMain` kaj `iosMain`. La fonto restas la sama JSONC-dosiero (regulo A5: neniu duobligita konfiguro).
   iOS havis la saman `error(...)` kaj nun ankaŭ uzas ĝin.
7. **`Node.js net module is not available`** por ĉiu RSS-peto — Ktor CIO en wasmJs bezonas Node.js-ingojn.
   Nova `expect val httpMotoro` (`data/repository/HttpMotoro.kt`): CIO sur Android/Desktop/iOS, `Js`
   (retumila fetch) sur wasmJs; nova dependeco `ktor-client-js` nur por `wasmJsMain`. Uzata en `AppStato`
   kaj `PreviewDatumoj`. (AGENTS.md asertis ke CIO funkcias ĉie — korektita.)
8. **Unu blokita fluo nuligis ĉiujn aliajn** (regulo A4!). La Js-motoro ĵetas `kotlin.Error("Fail to fetch")`,
   ne `Exception`; `ElsendoDeponejoImpl` kaptis nur `Exception`, do la eraro eskapis el `coroutineScope`
   kaj nuligis la paralelan ŝargadon de ĉiuj kanaloj (`JobCancellationException: Parent job is Cancelling`).
   Nun: `catch (e: CancellationException) { throw e } catch (e: Throwable) { … }`.
   Nova testo `ElsendoDeponejoToleremoTest`: Ktor-interkaptilo ĵetas `Error` por unu kanalo; la alia kanalo
   devas tamen reveni. **La testo malsukcesas kun la malnova `catch (e: Exception)`** (kontrolite) kaj pasas nun.

### Konstru-memoro
La Wasm-ligilo bezonas pli ol la defaŭltajn 1,5 GB (`OutOfMemoryError: Java heap space`). Rulu per
`-Pkotlin.daemon.jvmargs=-Xmx4g`. `gradle.properties` ne estas ŝanĝita.

### Rezulto en la retumilo
La apo ŝargiĝas kaj montras Hejmo/Kanaloj/navigadon kun **197 elsendoj el 4 kanaloj** (La Malfamuloj,
BabiBEJO, Bitmono, Sano — ĉiuj ĉe anchor.fm, kiu sendas `Access-Control-Allow-Origin: *`).

### Restanta limo — CORS (ne riparebla en la kliento)
La aliaj ~14 fontoj ne sendas `Access-Control-Allow-Origin` (kontrolite per `curl -H 'Origin: …'`), do la
retumilo blokas ilin. Bezonas servilon aŭ prokurilon — tio kongruas kun la planita arkiva servilo
(`docs/nova/06_servilo_arkivo.md`). Ankaŭ bildoj ne aperas en la Web-versio (ne esplorita).

---

## 12. `AndroidUiTest` — "No compose hierarchies found"

### Problemo
Ĉiuj 4 testoj en `androidApp/src/androidTest/.../AndroidUiTest.kt` malsukcesis kun
`IllegalStateException: No compose hierarchies found in the app`.

### Diagnozo
1. Ne versi-konflikto: `ui-test-junit4:1.7.3` estas hardkodita, sed Gradle levas ĝin al `1.11.0-alpha03`,
   same kiel la `androidx.compose.ui:ui` de la apo; la test-APK ne enhavas duoblajn
   `androidx/compose/ui/platform`-klasojn (kontrolite per `dexdump`).
2. Provizora diagnoza testo montris ke `AndroidComposeView` ekzistas kaj estas alligita, kaj ke la
   test-callback (`ViewRootForTest.onViewCreatedCallback`) estas agordita.
3. La fonto de `ComposeRootRegistry` (ui-test 1.10.6): radiko estas registrita nur ĉe `Lifecycle.Event.ON_RESUME`
   kaj **malregistrita ĉe ĉiu alia evento** (ekz. `ON_PAUSE`).
4. Logcat de la testrulo: `START … REQUEST_PERMISSIONS … GrantPermissionsActivity`. `MainActivity.petiSciigPermeson()`
   petas `POST_NOTIFICATIONS` ĉe starto → la sistema dialogo kovras MainActivity → `ON_PAUSE` → la radiko
   malregistriĝas. `connectedAndroidTest` malinstalas la apon post ĉiu rulo, do la permeso ĉiam mankas.

### Ŝanĝoj
- `androidApp/build.gradle.kts` — `androidTestImplementation("androidx.test:rules:1.6.1")` (por `GrantPermissionRule`).
- `androidApp/src/androidTest/kotlin/dk/nordfalk/esperanto/android/AndroidUiTest.kt` — reskribita:
  - `@get:Rule(order = 0) GrantPermissionRule.grant(POST_NOTIFICATIONS)` (API 33+), antaŭ la Compose-regulo (`order = 1`);
  - la testoj serĉis tekstojn kiuj ne plu ekzistas ("🔍", "⚙", "★", "Nur per WiFi") — nun ili uzas la
    malsupran navigan breton (`hasText(...) and hasClickAction()`, por ne konfuzi kun sekcio-titoloj kiel
    "Kanaloj" sur Hejmo) kaj la ikon-priskribon "Agordoj";
  - `@Before` atendas la navigan breton anstataŭ "Muzaiko" sur Hejmo — tio dependis de ŝargitaj RSS-fluoj
    (la reto); nun la testoj uzas nur la enpakitan kanalkonfiguron.
- La provizora `DiagnozoTest` estis forigita.

### Kontrolo
`./gradlew :androidApp:connectedDebugAndroidTest` — **5 testoj, 0 malsukcesoj**, trifoje sinsekve
(Pixel 9, API 35).

---

## 13. Kodrevizio 2026-10: stako de 6 PR-oj (#89–#95)

Plena kodrevizio (subagentoj) → `FAROTA.md` (92 trovindikoj: K=kriza, G=grava, C=konsilo)
→ 6 stakaj PR-oj, ĉiuj kunfanditaj per squash (1 commit po PR). Jen la incidentoj
kies historio gravas; la DAŬRE VALIDA scio staras en `NOTOJ.md`.

### Incidentoj kaj iliaj kaŭzoj

- **K2 — sciigoj ne funkciis kun fermita apo**: `appContext`/Sentry estis agordataj
  nur en `MainActivity`. En procezo lanĉita de WorkManager (la normala kazo por la
  perioda kontrolo je 7:00/16:00) `sciigPermesoDonita()` trafis neiniciigitan
  `lateinit`-kampon → `UninitializedPropertyAccessException` → senfina
  `Result.retry()`-ciklo (baterio-dreno, API 33+), aŭ `kreuSettings()` reenfalis al
  `NoOpSettings` → silenta nenio-faro (API 26-32). **Riparo**: `EsperantoRadioAplikajho`
  (Application.onCreate — ĉiu procezo).
- **G28 + realaparata regreso — alarmo 16:10 ludis nur je 16:14** (2026-10-11,
  Pixel 10 / Android 16): la alarmo ekigis ĝustatempe, sed `AlarmoReceivilo` nur
  lanĉis `MainActivity` kaj atendis. `startActivity` el fona ricevilo estas SILENTE
  blokata de la BAL-limigoj (neniu esceptoĵeto — la unua G28-riparo kun catch-fallback
  tial neniam rulis); la Activity startis en fono kaj rulis sian kodon nur kiam la
  uzanto vekis la telefonon. **Riparo** (PR #94): la ricevilo ludas mem
  (goAsync-korutino + `AppStato.inicialigu` kun la movita `ExoPlayerLudiloRegilo`),
  FSI-sciigo ĉiam, `USE_EXACT_ALARM`. **Pruvo**: emulilo API 35, ekrano malŝaltita,
  alarmo 17:09:00.022 → "Komencas ludi" 17:09:00.180 → MediaSession PLAYING;
  instrumentita testo pasis ankaŭ sur la reala Pixel 10.
- **CIO-CertificateException** (2026-10-11): kiam `network_security_config` havis
  cleartext-DOMAJNO-esceptojn, CIO paneigis por ĉIUJ https-petoj ("Domain specific
  configurations require that hostname aware checkServerTrusted is used") — la apo
  tiam provizore uzis la OkHttp-motoron. Post kiam ĉiuj URL-oj fariĝis HTTPS
  (Peranto-emblemo kopiita al javabog.dk, ĉar wp11081574.server-he.de ne havas TLS),
  la esceptoj malaperis, CIO ree funkcias kaj `ktor-client-okhttp` estis forigita.
- **Vinilkosmo-regex rompis la tutan JSONC** (2026-10-10): `"<p class="who">.+?</p>"`
  kun NEREFUTITAJ citiloj en `puriguModeloj` — la tuta kanalkonfiguro ne plu parsis;
  `KanalAgordoFumTesto` kaptis ĝin sur ĉiuj branĉoj de la stako. Korekto:
  `"<p class=\"who\">(?:(?!</p>).)*?</p>"` (temperita formo).
- **Staka-PR-kunfluo-proceduro** (tiu ĉi stako): squash-kunfando de la BAZA PR kreas
  history-diverĝon — la supraj branĉoj tiam montras realajn konfliktojn (ambaŭ
  flankoj ŝanĝis la samajn dosierojn de la komuna prapatro). Solvo: `git rebase
  --onto origin/master <malnova-supra-limo> <branĉo>` (arboido restas identa —
  kontrolita per `git rev-parse <tip>^{tree}`), forta puŝo, poste merge. Krome:
  forigo de la baz-branĉo povas FERMI la stakan infan-PR-on — retargetu ĉiujn
  infanojn al master ANTAAŬ ol kunfandi, per `gh api -X PATCH pulls/N -f base=master`.

### Kontrolrezulto

- `:shared:desktopTest` — 273 testoj, 0 malsukcesoj (264 antaŭ la stako).
- `:androidApp:assembleDebug` — konstruiĝas post ĉiu kunfando.
- CI (`testoj` + `changelog`) verda sur ĉiu PR de la stako.
- Emulilaj pruvoj: 27 kanaloj/722 elsendoj ŝargiĝas; alarmo kun malŝaltita ekrano
  ludas tuj; instrumentitaj testoj pasis sur API 26, API 35 kaj la reala Pixel 10.

---

## Dokumentado

- `README.md` — faritaj taskoj forigitaj; aldonita la listo de mankantaj uzkazoj el `eo_strings.xml` kaj
  konataj restantaj problemoj.
- `AGENTS.md` — stato-tabelo, "Kio funkcias", testnombro (195), noto pri `media3-exoplayer-hls`, pri la
  procez-nivela `ExoPlayerLudiloRegilo`, ke la debug-APK instaliĝas kiel `dk.nordfalk.esperanto.radio.alfa`,
  korektita aserto pri Ktor CIO, klib-ABI/navigation3, Web-konstruo kun `-Xmx4g`, CORS-limo, kaj la
  riparo de instrumentitaj Compose-testoj (GrantPermissionRule).

## Konataj restantaj problemoj (ne riparitaj)

- Eksponenta reprovo ne atendas je reta reveno (la malnova `venterPåAtKommeOnline`) — rezignas post ~3 minutoj.
- Web: CORS blokas plej multajn RSS-fluojn; bildoj ne aperas. Bezonas servilon/prokurilon.
- HLS sur Desktop/Web.

---

## Kio novas / Lastatempe ludata — konduto kaj lud-ekigo (PR: #85)

### Dizajno (post provo kaj rezigno de la "flugo")

Uzanto petis ke luditaj elsendoj malaperas de "Kio novas". Unue ni provis animacian
"flugon" de la karto inter la sekcioj per `SharedTransitionScope` +
`sharedElementWithCallerManagedVisibility` (stubo kun `visible=false` difinas la
komenc-pozicion, la cel-karto flugas). Sur la emulilo ĝi funkciis, sed la uzanto
trovis tion tro komplika kaj ni rezignis ĝin. La fina konduto estas pli simpla:

- **La nune ludata karto restas en "Kio novas"** (ĝis alia elsendo iĝas la nuna) —
  la klakita karto ne saltas for sub la fingro; ĝi ankaŭ aperas en "Lastatempe ludata".
- **"Lastatempe ludata" nur post vera lud-ekigo**: `lasteLudita` ne plu skribiĝas tuj
  ĉe klako sur "Ludi" (tiu skribo en `ludiElsendonInterna` estas forigita), sed nur
  kiam la ludilo atingas `Ludas` — nova `markiLudita(elsendoId, kanaloSlug)` metas
  nur la tempmarkon kaj ne tuŝas pozicion/daŭron (ne kiel `registriPozicion`, kiu
  anstataŭigus ilin). Sekve elsendo kiu nur "konektas" aŭ eraras ne aperas tro frue.
- **"Lastatempe ludata" rulas aŭtomate** al la nova karto (`animateScrollToItem(0)`
  kiam la unua ero ŝanĝiĝas).
- **"Aliaj elsendoj" ne re-miksas**: antaŭe `.shuffled()` rulis ĉe ĉiu
  `aktualigiuLudatajn` (t.e. ĉiujn 5 s dum pozicio-savado) kaj la sekcio konstante
  ŝanĝiĝis. Nun `plenigu` antaŭmiksas la kandidatojn unufoje (`shuffled(Random(enhavo-hash))`)
  kaj `aktualigiuLudatajnInterna` nur filtras — la ordo de restantaj kartoj stabilas.
