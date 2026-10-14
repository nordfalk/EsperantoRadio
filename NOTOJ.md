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
