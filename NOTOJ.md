# NOTOJ.md — Permanentaj teknikaj notoj de la nova apo

> Ĉi tie staras la daŭre validaj decidoj, kondiĉoj kaj ĝangaloj de la nova KMP-apo,
> kiuj estis lernitaj dum la kodrevizio (2026-10) kaj kiujn estonta programado devas
> koni. La PR-referencoj montras al la PR kies priskribo kaj kommitaj mesaĝoj
> entenas la plenan klarigon (pruvojn, kialojn, kaj la difinojn de la etikedoj
> K…/G…/C… el la kodrevizio).
>
| Dosiero | Rolo |
|---|---|
| [NOTOJ.md](NOTOJ.md) | ĉi tiu dosiero — permanenta scio, laŭ temo |
| [FAROTA.md](FAROTA.md) | la RESTANTAJ (malfermaj) taskoj de la revizio |
| [SXANGXOJ.md](SXANGXOJ.md) | detala historia taglibro — problemo→kaŭzo→riparo→testo |
| [AGENTS.md](AGENTS.md) | laborfluo kaj il-kutimoj (build, CI, git) |

## Ludado-arkitekturo

- **La UI neniam vokas la ludilon rekte** — ĉiam `LudvicoRegilo` ([PR #92](https://github.com/nordfalk/EsperantoRadio/pull/92), [PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)):
  `ludiElsendon`/`ludiRektan`/`pauxziguLudadon`/`dauriguLudadon`/`haltuLudadon`.
  Rekta `ludilo.halti()`/`ludi()` ne nuligas planitan reprovon, kiu tiam rekomencigus
  la ludadon (muziko revenas post kiam la uzanto premis "Halti"), kaj lasas la
  reprovo-/eraro-kalkulon-/dedup-statojn malkoheraj.
- **Fino/eraro sen konata fonto devas esti ignorata** ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)): post `halti()` la ludilo
  havas neniun fonton; aŭtoludo tiam lanĉus "plej freŝan neludatan" tuj post la
  uzanto premis "Halti". `haltuLudadon()` savas la pozicion ANTAŬ ol `halti()` forigas
  la fonton (la fonto ne plu estas legebla poste).
- **Dedup de finoj estas deviga** ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)): la sama elsendo-fino povas esti raportita
  kaj de `EsperantoLudadoServo` (STATE_ENDED-aŭskultanto) kaj de la stato-observanto
  de `LudvicoRegilo`. `traktiFinon` markas la FINANTAN fonton kiel traktita en
  atoma `ArrayDeque` (Mutex + fenestro) — duoblaj vokoj estas ignorataj.
  Transdonu ĉiam la finantan fonton (`traktiFinonPublika(..., finantaFonto = …)`),
  ne relegu `nunaFonto` (la aŭtoludo eble jam avancis).
- **`erarojSinsekvaj` rekomenciĝas ĉe ĉiu vera sukceso** ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)) — sen tio 10 akumulitaj
  eraroj dum longa seanco haltigus la aŭtoludon por ĉiam.
- **Reprovo-logiko** ([PR #92](https://github.com/nordfalk/EsperantoRadio/pull/92)): pasemaj eraroj (HTTP 5xx, tempolimoj — vidu
  `estasReprobeblaEraro`) reproviĝas ĝis 10 fojoj (1s→2s→4s…maks 30s) de la sama
  pozicio; daŭraj eraroj (HTTP 4xx, format-eraroj) tuj saltas al la sekva elsendo.
- **La nuna fonto vojaĝas kun la MediaItem** en `MediaMetadata.extras`
  (`SonfontoKodilo`) — ludado komencita de iu ajn MediaController (sciigo-"Ludi",
  alarmo-ricevilo) estas rekonata de `ExoPlayerLudiloRegilo` kaj aperas en la
  mini-ludilbreto.
- **Media3- apartaĵo (1.5.1)**: post `stop()` + `clearMediaItems()` la `playerError`
  RESTAS (nur `prepare()` nuligas ĝin) — la stato-mapigo devas kontroli
  `mediaItemCount == 0` unue, alikaze "Halti" igus Eraro-staton kaj reprovo-restartigus
  la ludadon ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)). StateFlow estas conflated — Eraro devas esti tenata en la
  stato-ĝisdatigo, ne nur en la evento.
- **Malsamaj ludiloj emas malsamajn esceptojn**: la Js-motoro (wasmJs) ĵetas
  `kotlin.Error` (ne Exception) ĉe reteraroj — kaptu `Throwable` kaj reĵetu
  `CancellationException` ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)).

## Alarmoj (vekhoro)

- **Alarmo-ludado devas vivi EN `AlarmoReceivilo`** (goAsync-korutino + WakeLock),
  ne en Activity: `startActivity` el fona ricevilo estas **silente** blokata de la
  BAL-limigoj (Android 10+, neniu esceptoĵeto) kiam la apo ne estas videbla —
  la Activity komenciĝas en la fono kaj rulas sian kodon nur kiam la uzanto vekas
  la telefonon. La ricevilo ludas mem per `LudvicoRegilo` (kun elŝuto-prefero,
  pozicio-resumigo, reprovo) kaj inicialigas `AppStato` kun la procez-nivela
  `ExoPlayerLudiloRegilo.akiru()` ankaŭ en freŝa procezo ([PR #94](https://github.com/nordfalk/EsperantoRadio/pull/94)).
- **Alt-prioritate sciigo estas la videbleca vojo** (ĝis 3.0.2: full-screen-intent,
  [PR #94](https://github.com/nordfalk/EsperantoRadio/pull/94)): `CATEGORY_ALARM` +
  `PRIORITY_MAX` + kanalo `IMPORTANCE_HIGH` — aperas kiel heads-up kiam la ekrano
  estas ŝaltita. `USE_FULL_SCREEN_INTENT` estas **forigita en 3.0.3** (Google Play
  postulas deklaron pri full-screen-intent; la kompromiso estis akceptita): la
  ekrano ne vekiĝas aŭtomate, sed la sono ĉiam ludas. La sciigo estas daŭra
  (ongoing) dum la alarmo sonas kaj estas nuligata kiam la ludado haltiĝas aŭ
  finiĝas (gardo en `AlarmoReceivilo.onReceive`) — alie ĝi restus en la ombrumo
  por ĉiam, ĉar daŭraj sciigoj ne estas forviŝeblaj per svingo. La sono ne
  dependas de ĝi.
- **Ekzakteco**: `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` estas **intence ne
  deklaritaj ekde 3.0.3** (same kiel supre — la Play-deklaro). Tial sur Android 12+
  la skedilo uzas `setWindow` kun 10-minuta fenestro (la alarmo tamen ekigas, eble
  ĝis 10 min malfrue); sur Android ≤11 `setAlarmClock` (ekzakta, Doze-escepta,
  alarm-ikono en la statusbreto). Informa karto en `AlarmoEkrano` klarigas tion
  al la uzanto. (Antaŭ 3.0.3: sen permeso `setWindow` 10 min + averto kun
  "Permesi"-butono; la permeso estis revokebla iam ajn — `skedi` falu al `setWindow`
  per try/catch de `SecurityException`, [PR #93](https://github.com/nordfalk/EsperantoRadio/pull/93).)
- **Validigo/koercado** ([PR #95](https://github.com/nordfalk/EsperantoRadio/pull/95)): `sekvaEkigo` koercas horon/minuton kaj maskas
  `ripeto` al la 7 tago-bitoj (maskado, ne limigo — la intencitaj tagoj restas);
  `parsuSugestojnPorAlarmoj` same. Korupta persistinta valoro devas neniam kraŝi
  la skedadon (`LocalTime` ĵetas `IllegalArgumentException`).
- **Rezervo de korupta alarmo-JSON** ([PR #95](https://github.com/nordfalk/EsperantoRadio/pull/95)): `PersistantaAlarmoDeponejo.legu()`
  savas la krudan valoron en `ALARMOJ_REZERVO` antaŭ ol fali al la sugestoj — sen
  tio la sekvaj `persistu()`-vokoj anstataŭigus ĝin kaj la uzantaj alarmoj
  estus definitive perditaj.
- **Alarmo-elekto** ([PR #95](https://github.com/nordfalk/EsperantoRadio/pull/95)): 1) plej nova nefinita/neerara, 2) fale la plej nova
  NE-erara (reludado > erara fluo kiu tuj paneus), 3) null → fallback-ringtono
  (TYPE_ALARM + USAGE_ALARM-atributo) + vibrado.
- **Skribu al Settings ANTAŬ ol lanĉi ion** el la ricevilo: se la alarmo lanĉas
  freŝan procezon, `ekigis(alarmoId)` (unufoja → malaktiva; ripetanta → re-skedo)
  devas okazi antaŭ ol la startanta `AppStato` legas la liston.

## Reto kaj sekureco

- **`network_security_config.xml` restas kun nur `base-config`** (klara teksto
  malpermesata, neniuj esceptoj) — ĝi estas NEPRIGATA ĉar `minSdk = 26` (< API 28,
  kie klara teksto estas defaŭlte permesata). Novaj kanaloj devas uzi HTTPS; la
  Peranto-emblemo estas gastigita sur `javabog.dk` (la originalo ne havas TLS) ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)).
- **CIO + domajno-esceptoj = paneo** (2026-10-11, emulila testo): se la security-config
  iam re-havos domajno-specifajn enskribojn, CIO paneigas por ĈIUJ https-petoj
  ("Domain specific configurations require that hostname aware checkServerTrusted
  is used") — tiam remetu `ktor-client-okhttp`. Kun nur base-config CIO funkcias.
- **URL-oj el nefidinda RSS-enhavo estas validigataj** ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)): nur `http://`/`https://`
  trairas (`saniguRetUrlon`) — `file://`, `content://`, `javascript://`, `data:`,
  `intent://` kaj obfuskita `java\tscript:` ne. ExoPlayer/Coil scipovas malfermi
  lokajn URI-ojn — malica fluo devas ne igi nin ludi/montri lokan enhavon.
- **Sama por HTML- ligiloj en priskriboj** ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)): nur http(s) kaj `mailto:` en
  `retpaghoUrl`/ligiloj.
- **Ktor-tempolimoj** ([PR #93](https://github.com/nordfalk/EsperantoRadio/pull/93)): `requestTimeout`, `connectTimeout` KAJ `socketTimeout` —
  servilo kiu akceptas kaj poste silentegas povas alie teni la peton viva pli longe
  ol la limo.
- **Fiaskpaĝo ne estas fluo** ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)): kontrolu HTTP-statuson; 4xx/5xx/kaptiva-portalo
  devas ne esti parsata kiel RSS — la per-kanala catch protokolas kaj daŭrigas
  kun la aliaj kanaloj (regulo 4: unu mortinta fonto ne paneigu la apot).

## Parsado (RSS/Atom)

- **Skip-listo anstataŭas unuopajn kontrolaĵojn** ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)): nesubtenataj video-gastigantoj
  estas saltataj per komuna listo (`estasSaltitaGastiganto`), kiel en la malnova
  `RomePodcastParser` (regulo 2).
- **`<audio>` serĉendas en la HTML-enhavo, ne en la XML-dokumento** ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)):
  `content:encoded`/`description` estas teksta nodo (CDATA/eskapita HTML) — uzu
  `Ksoup.parse(priskriboKruda)`, ne `ero.selectFirst("audio")`.
- **`enclosure` prefere kun `type="audio/…"`** ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)): iuj fluoj metas la bildo-enclosure
  unue.
- **`puriguModeloj` (regulo 6.6) aplikiĝas NUR al la priskribo** ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)): la
  fluo-ekstraktado (ekz. `<audio>`-fontoj el Varsovia Vento) devas uzi la KRUDAN
  enhavon — purigita enhavo perdas la ludilojn.
- **Titolo venas el `<title>`** ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)); derivaĵo el la enhavo nur ĉe
  `ignoruTitolon` (kaj tiam `trim`-ita, maks 200).
- **Datoj**: akceptataj formoj — `yyyy-MM-dd` (sen T), `…T…`, kaj `d Mon yyyy`
  ankaŭ kun 4-literaj monatoj ("Sept") ([PR #95](https://github.com/nordfalk/EsperantoRadio/pull/95)). Ne-rekonata dato forĵetas la EREON
  (ne la tutan fluon).
- **Daŭro** (`itunes:duration`): ankaŭ `HH:MM:SS.ss` (frakciaj sekundoj) ([PR #95](https://github.com/nordfalk/EsperantoRadio/pull/95)).
- **Atom-`<link>`**: la teksto de `<link href=…/>` estas malplena ĉeno —
  `?.text()?.ifEmpty { null }` antaŭ ol fali al `attr("href")` ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)).
- **archive.org-metadaten-petoj estas limigitaj** al 8 samtempaj (Semaphore) —
  centoj da petoj ricevas 429/timumuron ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)); ĉiu peto devas havi try/catch kaj
  fali al divenita dosiernomo ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)).
- **Re-parsado de la diskkaŝo bezonas la httpKlienton** ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)): sen ĝi la
  Peranto-elsendoj ricevus DIVENITAJN (ofte 404-ajn) dosiernomojn ĝis la unua
  sukcesa reta refreŝo.

## Persisto (Settings + kaŝmemoroj)

- **Nemutablaj momentfotoj** por la publikaj mapoj de `ElsendoDeponejoImpl` ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)):
  legoj (serĉo/getElsendo) okazas dum alia korutino skribas — kun mutableMapo tio
  estis ConcurrentModificationException-risko. Skriboj nur per Mutex.
- **Mutex ĉirkaŭ legu-modifu-skribu** en `PersistaLudatojDeponejo` ([PR #91](https://github.com/nordfalk/EsperantoRadio/pull/91)):
  `registriPozicio` (ĉiujn 5 s) kaj `markiFinita`/`markiErara` povas interplektiĝi.
- **URL-ESKAPADO de Esperantaj supersignoj** en alarm-sugestoj ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)):
  `%C4%A5`=`ĥ`, `%C5%9C`=`Ŝ`, `%C5%AC`=`Ŭ` — kaj ĝ, ĵ, ŝ, Ĥ. Jam-malkoditaj
  signoj (kodo > 127) trairas senŝanĝe.
- **Dosiernomoj** ([PR #95](https://github.com/nordfalk/EsperantoRadio/pull/95)): sankigita ID maks 120 signoj + 8-signa haketaĵo de la
  PLENA ID (unico konserviĝas; RSS-id-oj ofte estas plenaj URL-oj > 255 bajtoj),
  neniuj nul-bajtoj, Windows-rezervitaj nomoj prefiksitaj, finaj punktoj forigitaj.

## Sciigoj kaj intents

- **`INTENT_NONCE`** ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)): ĉiuj intencoj al la eksportita `MainActivity`
  (alarmo, sciigo-"Ludi") portas po-instalan hazardan nomon el Settings — sen ĝi
  iu ajn apo povus forĝi la ekstraĵojn (ludi ajnan URI-on, volumo-boost).
  `MainActivity` kontrolas `IntentSxlosilo.estasFidinda(intent)`.
- **`SCIIG_ID` kreskanta kaj persistita** ([PR #93](https://github.com/nordfalk/EsperantoRadio/pull/93)): `hashCode()`-kolizioj anstataŭigis
  sciigojn kaj la "Ludi"-PendingIntent de sciigo A celus elsendon B.
- **`goAsync()` por nesinkrona ricevila laboro** (MediaController-konekto, Reto):
  sen ĝi la sistemo detruas la ricevilon antaŭ ol la korutino finiĝas.
- **Reuzata MediaController** ([PR #92](https://github.com/nordfalk/EsperantoRadio/pull/92)): `MediaController` kaj ĝia Binder-konekto estas
  pezaj kaj neniam liberiĝas silente — reuzu unu procez-nivelan (alarmo, sciigo-"Ludi").
- **`PendingIntent`-ID-oj devas esti unikaj** (sciigId + 1 por la dua ago de la sama
  sciigo) — koliziantaj RequestCode-oj anstataŭigas la PendingIntent-ojn.
- **`applicationContext`, NE la ricevilan kuntekston**, por bindi servojn:
  `ReceiverRestrictedContext` ĵetas `ReceiverCallNotAllowedException`.

## Konkuro kaj korutinoj

- **`CancellationException` ĉiam reĵetiĝas** el catch-blokoj ([PR #89](https://github.com/nordfalk/EsperantoRadio/pull/89)) — engluti ĝin
  mortigas la nuligo-semantikon (korutino ne finiĝas, sed ankaŭ ne plu funkcias).
- **`Dispatchers.IO` ne ekzistas sur wasmJs** — komuna kodo uzas `Dispatchers.Default`
  (la Ktor-motoro mem taskas al siaj fadenoj).
- **WakeLock-akiro el la ricevilo** estas transdonata al la korutino kaj liberigata
  en `finally` (kun tempolimo kiel sekureca reto).

## Protokolado kaj Sentry

- **Atendataj malsukcesoj estas `logw`, ne `loge`** ([PR #93](https://github.com/nordfalk/EsperantoRadio/pull/93)): unu mortinta kanal-fluo
  estas normala okazaĵo (regulo 4) kaj devas NE iĝi erar-nivela Sentry-evento —
  alikaze oni ne trovas la verajn paneojn. `loge` = paneo.
- **`logd` por la datum-tavolo** (RssParsilo, ElsendoDeponejo): alie Sentry-`beforeSendLog`
  inundiĝus per raportoj pri ĉiu kanal-ŝargado.
- ** Medio laŭ la DEBUGGING-flago** ([PR #93](https://github.com/nordfalk/EsperantoRadio/pull/93)), ne la build-tipo: debug-versio → "evoluo",
  release → "produktado" (10 % specimenado).

---

## Projekta stato kaj fazoj (ĝis 2026-10-13; translokigita el AGENTS.md)

> Notu: la fazo-tabelo kaj la listo "kio funkcias/kio ne funkcias" estas
> historia portreto de la 2026-10-13-epoko; la aktuala resumo staras en AGENTS.md.


La nova KMP-apo estas en konstruado. Jen la fazoj kaj ilia stato:

| Fazo | Priskribo | Stato | PR |
|---|---|---|---|
| 0 | KMP-strukturo (shared, androidApp, desktopApp, webApp) | ✅ Farita | #5 |
| 1a | Domajnmodeloj, JSONC-leganto, kanalaro-UI | ✅ Farita | #6 |
| 1b | RSS-parsilo (reguloj 6.1, 6.2, 6.4–6.7) + golden-testoj | ✅ Farita | #7 |
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
| — | Malsupren-tiro, ekzaktaj alarmoj, podkasto ĉe alarmo, eksponenta reprovo, HLS sur Android | ✅ Farita | — |

### Kio funkcias nun

- **Malnova apo** (`malnova/app/`): konstruiĝas kaj funkcias (APK, 18 MB)
- **Nova apo — kanalaro**: montras la realajn kanalojn el la JSONC-konfiguro (Desktop + Android)
- **Nova apo — RSS-parsilo**: parsas ĈIUJN 7 parsregulojn (inkl. Peranto/archive.org)
- **Nova apo — ludado**: vera sonludado sur Android (Media3 ExoPlayer), Web (HTMLAudioElement), Desktop (mp3spi + SourceDataLine)
- **Nova apo — navigado**: kanalaro → kanal → elsendo + serĉo + plejŝatataj + elŝutoj + alarmoj + agordoj (uzas `navigation3` — `NavKey`/`NavDisplay`/`entryProvider`)
- **Nova apo — elŝutoj**: fluanta elŝuto (Ktor→FileOutputStream), persisto inter restartoj (JSON-metadateno), eksterreta ludado (prefero por loka dosiero)
- **Nova apo — vekhorloĝo**: alarmoj kun sugestoj el JSONC, persisto inter restartoj (Settings+JSON), UI kun kreilo/redaktilo, AlarmManager-skedado (Android), aŭtomata ludado (livestream, aŭ la plej freŝa neaŭskultita podkasto — `elektuAlarmElsendon`), fallback ringtono, volumo-boost. `setAlarmClock` sur Android ≤11, `setWindow` kun 10-minuta fenestro sur Android 12+ (la ekzaktaj-alarm-permesoj estas intence ne deklaritaj — vidu la lecionon sube) + informa karto en AlarmoEkrano; ripetantaj alarmoj re-skediĝas en `AlarmoReceivilo`
- **Nova apo — sciigoj**: WorkManager kontrolas ŝatatajn kanalojn por novaj elsendoj (skedita je 7:00 kaj 16:00), sciigo kun "Ludi"-butono kiu lanĉas la elsendon rekte per `MediaController` (`LudiElsendoReceivilo`)
- **Nova apo — emblemoj**: Coil 3-bildoj en kanalaro kaj kanalvido
- **Nova apo — ludvico**: aŭtomata sekva-ludado post naturfino (3 prioritatoj: samkanala → ŝatataj → plej freŝa), pozicio-spurado (ĉiu 5s) kun resumigo, eksplicita ludvico per ludvico-butono (Material 3 ikono), "Lastatempe ludata" sekcio sur HejmoEkrano, "Daŭrigi de X:XX" en ElsendoEkrano
- **Nova apo — HTML**: riĉa HTML-vidigo de priskriboj en ElsendoEkrano (`HtmlVido`/`HtmlTeksto`)
- **Nova apo — gestoj**: horizontala svipo inter elsendoj de sama kanalo (ElsendoEkrano) kaj inter kanaloj (KanalEkrano)
- **Nova apo — evolua reĝimo**: ŝaltilo en agordoj + elektiloj por priskribo-fonto kaj vidmaniero
- **Nova apo — Sentry.io**: erarmonitorado trans ĉiuj platformoj
- **Nova apo — refreŝigo**: malsupren-tiro (`PullToRefreshBox`) sur Hejmo, Kanaloj kaj kanalvido — `sxargxi(fortoRefresigi = true)` preterpasas la memoran kaŝmemoron
- **Nova apo — reprovo**: `LudvicoRegilo` reprovas pasemajn erarojn (`LudantoStato.Eraro.reprovebla`) ĝis 10 fojojn (1s, 2s, 4s … maks 30s, `ReprovoLogiko`), de la sama pozicio; MiniLudilbreto montras "Konektas… (provo n/10)". Daŭraj eraroj (HTTP 404, formato) tuj saltas al la sekva.
- **Testoj**: KMP-testoj sur Desktop, ĉiuj pasas
- **Web (wasmJs)**: konstruiĝas kaj rulas per `./gradlew :webApp:wasmJsBrowserDevelopmentRun -Pkotlin.daemon.jvmargs=-Xmx4g`.
  Montras nur kanalojn kies fluoj permesas CORS (nun la anchor.fm-podkastoj) — vidu "Kio NE funkcias"
- **radioTxtKomparilo**: Desktop-ilo kiu komparas la kanalkonfiguron kun `esperanto-radio.com/radio.txt` — identigas mankantajn kanalojn kaj elsendojn (`./gradlew :desktopApp:radioTxtKomparilo`)

### Kio NE funkcias ankoraŭ

- Aŭtomata resumigo: pozicio-restarigo okazas nur kiam oni reiras al la elsendo kaj klakas "Aŭskulti"; ne aŭtomate kiam oni malfermas la apoon kaj la ludilo daŭras en fono
- iOS-ludado (no-op, bezonas AVPlayer)
- Web: plej multaj RSS-fluoj estas blokitaj de CORS (neniu `Access-Control-Allow-Origin`) — bezonas
  servilon/prokurilon (vidu `docs/nova/06_servilo_arkivo.md`); bildoj ankaŭ ne aperas
- Hejmekrana widget, Chromecast, parolsintezo

## Deponeja koda mapo (translokigita el AGENTS.md, 2026-10-14)


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
│   └── src/commonTest/    #   Testoj + desktopTest (ĉiuj pasas)
├── settings.gradle.kts     # Kotlin-DSL-build (unuecigita: malnova + nova; `iosApp`/`server` komentitaj)
├── build.gradle.kts        # Radika build (KMP + Compose + AGP aldonaĵoj)
├── gradle/libs.versions.toml # Versikatalogo (inkl. versionName por Sentry-release)
├── RssArkivServer/        # Kaŝenitaj RSS-fluoj (XML-fiksaĵoj)
├── RssArkivServer-filcache/ # Kaŝenitaj realaj fluoj = golden fixtures (NE versiigitaj — for de la repo per .gitignore; kiu bezonas pliajn fiksaĵojn kopiu ilin al shared/src/commonTest/resources/feeds/)
├── docs/malnova/           # Esperanta superrigordo de la malnova apo
├── docs/nova/              # Esperanta plano por Compose Multiplatform + servilo
├── .github/workflows/       # CI (changelog-kontrolo)
├── CHANGELOG.md             # Konciza, uzantvida ŝanĝoprotokolo (Keep a Changelog)
├── SXANGXOJ.md              # Detala teknika labortaglibro de la ŝanĝoj
├── NOTOJ.md                 # Permanentaj teknikaj notoj de la nova apo (laŭ temo, kun etikedoj)
├── FAROTA.md                # La RESTANTAJ malfermaj trovoj el la kodrevizio (finitaj → NOTOJ/SXANGXOJ)
└── AGENTS.md               # Tiu ĉi dosiero
```

### La nova kodo — dosiero-mapo


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
│   ├── parser/RssParsilo.kt          # RSS/Atom-parsilo (sep reguloj)
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

## Sonludado kaj ludado — arkitekturaj resumoj (translokigitaj el AGENTS.md)

- **ksoup 0.2.2** estas la versio kongrua kun Kotlin 2.2.20 (0.2.6+ postulas Kotlin 2.3+).
  La API: `Ksoup.parseXml(teksto, "")` por XML, `Ksoup.parse(teksto)` por HTML.
  `selectFirst(...)` ekzistas (ne nur `select(...).firstOrNull()`).
- **`ExoPlayerLudiloRegilo` estas procez-nivela** (`ExoPlayerLudiloRegilo.akiru(context)`), ne po Activity, kaj
  NE estas liberigita en `onDestroy`. `AppStato.ludvicoRegilo` tenas ĝin dum la tuta procezo. La nuna `Sonfonto`
  estas JSON en `MediaMetadata.extras` (`SonfontoKodilo`), do ludado komencita de alia MediaController
  (ekz. `LudiElsendoReceivilo`) estas rekonata. Vidu `SXANGXOJ.md`.

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

## Vekhorloĝo kaj Android-limoj (translokigitaj el AGENTS.md)

- **Regex-`"`-signoj en JSONC devas esti refutitaj** (`\"`): la koment-striptigilo forigas
  nur `//`-liniojn, sed la valoroj mem devas esti valida JSON post striptigo. Regex kiel
  `<p class="who">.+?</p>` rekte enskribita en `puriguModeloj` rompas la tutan kanalkonfiguron
  (KanalAgordoFumTesto malsukcesas per `JsonDecodingException` ĉe `puriguModeloj[0]`).
  (2026-10-10: tia eraro enŝoviĝis en la vinilkosmo-regex kaj rompis ĉiujn branĉojn de la
  riparo-stako; kaptita de la fum-testo kaj la CI-laborfluo (PR #89). Uzu la temperitan formon
  `<p class=\"who\">(?:(?!</p>).)*?</p>` kiel ĉe varsoviavento.)
- **`startActivity` el fona BroadcastReceiver estas SILENTE blokata** (BAL-limigoj, Android 10+):
  ĝi ne ĵetas escepton — la Activity startas en la fono kaj ĝia kodo (onCreate, intent-traktado)
  plenumiĝas nur kiam la uzanto vekas la telefonon. Do neniam metu alarm-ludon (aŭ alian
  tempokritikan logikon) en Activity lanĉota el ricevilo — ludu rekte en la ricevilo
  (goAsync-korutino); la videblecon certigu alt-prioritate DAŬRA sciigo (`CATEGORY_ALARM` +
  `PRIORITY_MAX`; sen `USE_FULL_SCREEN_INTENT` la ekrano ne vekiĝas aŭtomate, sed la
  sciigo aperas kiel heads-up kaj restas dum la alarmo sonas — nuligata kiam la
  ludado haltiĝas aŭ finiĝas). (2026-10-11: alarmo 16:10 ludis nur je 16:14 kiam la uzanto
  vekis la telefonon; la "catch"-fallback neniam rulis ĉar ne estis escepto. Riparita en PR #94.)

- **`startActivity` el fona BroadcastReceiver estas SILENTE blokata** (BAL-limigoj, Android 10+):
  ĝi ne ĵetas escepton — la Activity startas en la fono kaj ĝia kodo (onCreate, intent-traktado)
  plenumiĝas nur kiam la uzanto vekas la telefonon. Do neniam metu alarm-ludon (aŭ alian
  tempokritikan logikon) en Activity lanĉota el ricevilo — ludu rekte en la ricevilo
  (goAsync-korutino); la videblecon certigu alt-prioritate DAŬRA sciigo (`CATEGORY_ALARM` +
  `PRIORITY_MAX`; sen `USE_FULL_SCREEN_INTENT` la ekrano ne vekiĝas aŭtomate, sed la
  sciigo aperas kiel heads-up kaj restas dum la alarmo sonas — nuligata kiam la
  ludado haltiĝas aŭ finiĝas). (2026-10-11: alarmo 16:10 ludis nur je 16:14 kiam la uzanto
  vekis la telefonon; la "catch"-fallback neniam rulis ĉar ne estis escepto. Riparita en PR #94.)
- **Vekhorloĝaj permesoj intence NE deklaritaj (ekde 3.0.3, 2026-10-12)**: Google Play
  rifuzas la alŝuton se la apo deklaras `USE_FULL_SCREEN_INTENT` sen la respondo al la
  demando "full-screen intent" ("You must let us know whether your app uses any
  full-screen intent permissions", eraro 403). Tial `SCHEDULE_EXACT_ALARM`,
  `USE_EXACT_ALARM` kaj `USE_FULL_SCREEN_INTENT` estas forigitaj el la manifest
  (kompromiso akceptita de la uzanto). Sekvoj sur Android 12+: alarmoj povas ekigi
  ĝis 10 minutojn malfrue (`setWindow` anstataŭ `setAlarmClock`) kaj la ekrano ne
  vekiĝas aŭtomate — la sono ĉiam ludas kaj la sciigo aperas kiel heads-up
  (`CATEGORY_ALARM` + `PRIORITY_MAX`). Sur Android ≤11 nenio ŝanĝiĝas. (La malnova
  lernita fakto — `USE_EXACT_ALARM` aŭtomate donata al vekhorloĝo-apoj, `SCHEDULE_EXACT_ALARM`
  defaŭlte donata sed revokebla sur Android 12 — plu validas, sed ne plu aplikiĝas,
  ĉar ni simple ne deklaras la permesojn.)

## Git- kaj agent-lecionoj — historiaj (translokigitaj el AGENTS.md)

- **`gh pr edit` ne funkcias en ĉi tiu deponejo** (GraphQL-eraro pri la
  projectCards-malrekomendo) — uzu la REST API (vidu supre).
- **Stash-reakiro post perdo**: se `git stash list` estas malplena sed stash
  atendatas, serĉu nereatingeblajn komitojn per
  `git fsck --unreachable --no-reflogs | grep commit` (stash-komito = kunfando kun
  2–3 gepatroj, mesaĝo "On <branĉo>: ...") kaj reakiru per
  `git stash store -m "..." <hash>`. (2026-10-08: la stash "eldono 3.0.2" tiel reakiriĝis.)

- **Commitigu nur viajn proprajn dosierojn** (`git add <dosiero>`, ne blinda
  `git add -A`), kiam la laborarbo povas enhavi nekomititajn ŝanĝojn de la
  uzanto; ne enmetu ilin sen aprobo. (2026-10-08: provizora "notu"-fragmento en
  CHANGELOG.md — konservita en la stash, ne enmetita.)
- **`git stash pop` post master-ĝisdatigo povas silente duobligi sekcionojn**:
  se via kaj la nova master-ŝanĝo tuŝas apudajn regionojn de la sama listo
  (ekz. CHANGELOG.md), la kunfando konservas ambaŭ sen konfliktmarkoj — la
  dosiero ricevas du `### Aldonita`/`### Riparita`-sekciojn. Post ĉiu stash pop
  kontrolu `grep -n "^## \|^### " CHANGELOG.md` kaj kunigu mane. Ankaŭ kontrolu
  importojn: viaj forigitaj/aldonitaj importoj povas resti/rediĉiĝi en dosieroj
  kiujn vi ne plu celas ŝanĝi. (2026-10-11: duoblaj sekcioj + restinta
  `fonaDispatcher`-importo en AppStato.kt.)

## Sentry-kvoto — historia evoluo (translokigita el AGENTS.md)

- **Forigi ŝlosilo-linion el la JSONC per skripto lasas vostan komon**, se ĝi estis
  la lasta ero de la objekto — `KanalAgordoFumTesto` kaptas tion per
  `JsonDecodingException: Trailing comma`. Post ĉia programema redakto de la
  konfiguro rulu `./gradlew :shared:desktopTest` antaŭ ol fidi la rezulton.
  (2026-10-12: forigo de la eksa statusmesaĝo-ŝlosilo postlasis `,` post
  `apaj_novaĵoj`.)

- **Sentry-kvoto: `logi` kondiĉa, `logGrava` ĉiam** (2026-10-12): `logi` sendas al
  Sentry nur se la agordo "detala protokolado" estas ŝaltita (`protokoloDetala` en
  `ProtokoloUtil.kt`, sinkronigita de `AgordojDeponejoImpl`); `logw`/`loge` kaj la nova
  `logGrava` (malfermo de la apo, ekludo de elsendo kun kanalo, vekiĝo) ĉiam sendas.
  Debug-konstruoj kaj emuliloj ne raportas al Sentry.

- **Sentry-kvoto: `logi` kondiĉa, `logGrava` ĉiam** (2026-10-12): `logi` sendas al
  Sentry nur se la agordo "detala protokolado" estas ŝaltita (`protokoloDetala` en
  `ProtokoloUtil.kt`, sinkronigita de `AgordojDeponejoImpl`); `logw`/`loge` kaj la nova
  `logGrava` (malfermo de la apo, ekludo de elsendo kun kanalo, vekiĝo) ĉiam sendas.
  Debug-konstruoj kaj emuliloj ne raportas al Sentry.
- **Eraroj-kvoto: `loge(tag,msg,e)` = erar-evento** (2026-10-13): `loge(tag, msg, e)`
  vokas `Sentry.captureException` kaj konsumas la MONATAN ERAROJ-KVOTON (5 000);
  `loge(tag, msg)` sen escepto kaj `Sentry.logger.*` estas nur protokol-eventoj
  (aparta kvoto). Atendataj fiaskoj (reto, ludad-, elŝut-, serĉ-fiaskoj) uzu
  `logw(tag, msg, e)` (nur loka) — neniam loge. En produktado eraroj estas
  50 %-specimenataj (`options.sampleRate` — rekte duonigas la kvot-konsumon;
  `options.tracesSampleRate` specimenas nur SPUROJN, apartan kvoton, ne erarojn).
  Versio-bazita forĵeto (beforeSend) bezonas ĝisdateblan centran liston
  (FAROTA C45) — ne malmolu versio-listojn en la apo.
