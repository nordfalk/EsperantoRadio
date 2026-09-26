# Ŝanĝoj — 2026-09-26

## Peranto: vera MP3-dosiernomo el archive.org-metadatenoj (PR #75)

**Problemo.** 'Malapero de aktoro Benda (3/3)' ludeblis ĉe
https://esperantaretradio.blogspot.com/ sed ne en la apo. La Peranto-parsilo
(regulo 6.3, `RssParsilo.parsuPeranto`) konstruis la fluo-URL-on per la supozo
"la MP3-dosiero nomiĝas same kiel la arkivaĵo":

```
archive.org/embed/malapero-benda-3 → archive.org/download/malapero-benda-3/malapero-benda-3.mp3
```

Sed la arkivaĵo `malapero-benda-3` entenas `Malapero_Benda3.mp3` → la kunstruita
URL redonis HTTP 404. La ero aperis en la listo (validaj titolo/dato), do la
uzanto vidis ĝin, sed ludo/elŝuto fiaskis. Sistemeca kontrolo de ĉiuj 50
archive.org-eroj en la fluo (`curl -L archive.org/download/<id>/<id>.mp3`)
trovis **7 rompitajn** pro alia dosiernomo: `malapero-benda-2`,
`malapero-benda-3`, `fulmo-diasendito`, `konkursa-travivajxo`, `murd-atenco`,
`plenluno-superplenluno`, `tragedio-universo`. La malnova apo ne havis tiun
cimon: `RomePodcastParser.kt` elŝutis la embed-paĝon kaj eltiris la veran
MP3-URL-on el ĝia HTML — ĝi neniam divenis.

**Riparo.** `parsuPeranto` iĝis dufaza:

1. kolekto — kiel antaŭe, sed por archive.org la dosiernomo estas nur divenita;
2. korekto — ĉiuj identigiloj estas demandataj ĉe
   `https://archive.org/metadata/<id>` (paralele per `coroutineScope/async`,
   unufoje po identigilo); la unua *originala* `.mp3`-dosiero el `files` uziĝas;
3. konstruado — la elsendoj ricevas la korektitan URL-on
   (`encodeURLPathPart` por la dosiernomo).

La rezulto estas persistata en nova
`ArchiveOrgDosiernomoKasho` (Settings+JSON, ŝablono de
`PersistaLudatojDeponejo`) — unu metadaten-peto po identigilo por ĉiam, ankaŭ
trans restartoj. Reteraro (inkl. `kotlin.Error` de la Js-motoro ĉe CORS) aŭ
forestata MP3 → retrofalo al la divenita nomo; fiaskoj ne estas kaŝitaj, do oni
reprovas kiam la reto revenas (regulo 4: unu arkivaĵo ne paneigas la fluon).

**Apriora dezajnodiskuto.** Ripari nur en `sxargxiElsendojn` (retvojo) ne
sufiĉus: je starto `leguKashitajnElsendojn` plenigas la memoran kaŝmemoron el
la diskkaŝo kaj `sxargxiElsendojn` tiam frue revenas sen la reto — la uzanto
vidus malĝustajn URL-ojn ĝis malsupren-tiro. Ĉar la korekto okazas en la
parsilo mem, ĝi kovras ĉiujn vojojn (kanalvido, Hejmo, elŝutoj, alarmoj,
sciigoj), kaj ankaŭ la diskkaŝan re-parsadon.

**API-ŝanĝo.** `parsuRss`, `parsuPeranto`, `leguKashitajnElsendojn` kaj
`leguĈiujnKashitajnElsendojn` iĝis `suspend` (necesaj por la metadaten-petoj).
Ĉiuj alvokantoj jam estis suspend-kuntekstoj (`KanaloViewModel`,
`HejmoViewModel`, `KanalaroViewModel`, `MainActivity.trovuAlarmElsendon`,
`NovajElsendojKontroloWorker`, `RadioTxtKomparilo` en `runBlocking`), do neniuj
alvokantoj devis ŝanĝiĝi. La 24 parsilaj testoj kaj la DiskKasho-testoj estis
mekanike envolvitaj per `runTest` (sen kondutŝanĝo).

**Kontrolo.**

- 201 testoj, 0 fiaskoj (`./gradlew :shared:desktopTest`), inkluzive 3 novajn
  parsilajn testojn (vera dosiernomo el metadatenoj; tolero de `Error("Fail to
  fetch")`; malplenaj metadatenoj `{}`) kaj 2 plurtavolajn kestajn testojn
  (`ArchiveOrgDosiernomoKashoTest`, desktopTest kun vera persisto:
  unufoja peto ankaŭ post "restarto", fiasko ne kaŝiĝas).
- `./gradlew :androidApp:assembleDebug` (JDK 17), `:desktopApp:compileKotlinDesktop`
  kaj `:webApp:compileKotlinWasmJs` pasas.
- La metadaten-formato estas kontrolita kontraŭ la reala arkivaĵo:
  `curl https://archive.org/metadata/malapero-benda-3` → `files` kun
  `{"name":"Malapero_Benda3.mp3","source":"original","format":"VBR MP3",...}`.

**Sciate ne riparita.** `auskultu_ripetu120`, `ekulturaj_eventoj` kaj
`transdono_inteligenteco` havas ĝustan dosiernomon, sed la ĉefa datennodo de
archive.org redonas 500 (transira problemo ĉe archive.org; alia nodo havas la
dosieron). Ne nia cimo; resaniĝos ĉe archive.org.

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
  estas protokolita per `loge` (regulo 8).
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
   `wasmJsMain` kaj `iosMain`. La fonto restas la sama JSONC-dosiero (regulo 5: neniu duobligita konfiguro).
   iOS havis la saman `error(...)` kaj nun ankaŭ uzas ĝin.
7. **`Node.js net module is not available`** por ĉiu RSS-peto — Ktor CIO en wasmJs bezonas Node.js-ingojn.
   Nova `expect val httpMotoro` (`data/repository/HttpMotoro.kt`): CIO sur Android/Desktop/iOS, `Js`
   (retumila fetch) sur wasmJs; nova dependeco `ktor-client-js` nur por `wasmJsMain`. Uzata en `AppStato`
   kaj `PreviewDatumoj`. (AGENTS.md asertis ke CIO funkcias ĉie — korektita.)
8. **Unu blokita fluo nuligis ĉiujn aliajn** (regulo 4!). La Js-motoro ĵetas `kotlin.Error("Fail to fetch")`,
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
