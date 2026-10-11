# NOTOJ.md — Permanentaj teknikaj notoj de la nova apo

> Ĉi tie staras la daŭre validaj decidoj, kondiĉoj kaj ĝangaloj de la nova KMP-apo,
> kiuj estis lernitaj dum la kodrevizio (2026-10) kaj kiujn estonta programado devas
> koni. La etikedoj ((G…), (K…), (C…)) referencas la trov-indikojn en
> [`FAROTA.md`](FAROTA.md) — ĉiuj tie ĉi menciataj estas FERMITAJ (jam prilaboritaj).
>
| Dosiero | Rolo |
|---|---|
| [NOTOJ.md](NOTOJ.md) | ĉi tiu dosiero — permanenta scio, laŭ temo |
| [FAROTA.md](FAROTA.md) | la RESTANTAJ (malfermaj) taskoj de la revizio |
| [SXANGXOJ.md](SXANGXOJ.md) | detala historia taglibro — problemo→kaŭzo→riparo→testo |
| [AGENTS.md](AGENTS.md) | laborfluo kaj il-mordecioj (build, CI, git) |

## Ludado-arkitekturo

- **La UI neniam vokas la ludilon rekte** — ĉiam `LudvicoRegilo` ((G13), (K3)):
  `ludiElsendon`/`ludiRektan`/`pauxziguLudadon`/`dauriguLudadon`/`haltuLudadon`.
  Rekta `ludilo.halti()`/`ludi()` ne nuligas planitan reprovon, kiu tiam rekomencigus
  la ludadon (muziko revenas post kiam la uzanto premis "Halti"), kaj lasas la
  reprovo-/eraro-kalkulon-/dedup-statojn malkoheraj.
- **Fino/eraro sen konata fonto devas esti ignorata** ((K3)): post `halti()` la ludilo
  havas neniun fonton; aŭtoludo tiam lanĉus "plej freŝan neludatan" tuj post la
  uzanto premis "Halti". `haltuLudadon()` savas la pozicion ANTAŬ ol `halti()` forigas
  la fonton (la fonto ne plu estas legebla poste).
- **Dedup de finoj estas deviga** ((G8)): la sama elsendo-fino povas esti raportita
  kaj de `EsperantoLudadoServo` (STATE_ENDED-aŭskultanto) kaj de la stato-observanto
  de `LudvicoRegilo`. `traktiFinon` markas la FINANTAN fonton kiel traktita en
  atomara `ArrayDeque` (Mutex + fenestro) — duoblaj vokoj estas ignorataj.
  Transdonu ĉiam la finantan fonton (`traktiFinonPublika(..., finantaFonto = …)`),
  ne relegu `nunaFonto` (la aŭtoludo eble jam avancis).
- **`erarojSinsekvaj` rekomenciĝas ĉe ĉiu vera sukceso** ((G27)) — sen tio 10 akumulitaj
  eraroj dum longa seanco haltigus la aŭtoludon por ĉiam.
- **Reprovo-logiko** ((G13-flanko)): pasemaj eraroj (HTTP 5xx, tempolimoj — vidu
  `estasReprobeblaEraro`) reproviĝas ĝis 10 fojoj (1s→2s→4s…maks 30s) de la sama
  pozicio; daŭraj eraroj (HTTP 4xx, format-eraroj) tuj saltas al la sekva elsendo.
- **La nuna fonto vojaĝas kun la MediaItem** en `MediaMetadata.extras`
  (`SonfontoKodilo`) — ludado komencita de iu ajn MediaController (sciigo-"Ludi",
  alarmo-ricevilo) estas rekonata de `ExoPlayerLudiloRegilo` kaj aperas en la
  mini-ludilbreto.
- **Media3- apartaĵo (1.5.1)**: post `stop()` + `clearMediaItems()` la `playerError`
  RESTAS (nur `prepare()` nuligas ĝin) — la stato-mapigo devas kontroli
  `mediaItemCount == 0` unue, alikaze "Halti" igus Eraro-staton kaj reprovo-restartigus
  la ludadon ((K3)). StateFlow estas conflated — Eraro devas esti tenata en la
  stato-ĝisdatigo, ne nur en la evento.
- **Malsamaj ludiloj emas malsamajn esceptojn**: la Js-motoro (wasmJs) ĵetas
  `kotlin.Error` (ne Exception) ĉe reteraroj — kaptu `Throwable` kaj reĵetu
  `CancellationException` ((G26)).

## Alarmoj (vekhoro)

- **Alarmo-ludado devas vivi EN `AlarmoReceivilo`** (goAsync-korutino + WakeLock),
  ne en Activity: `startActivity` el fona ricevilo estas **silente** blokata de la
  BAL-limigoj (Android 10+, neniu esceptoĵeto) kiam la apo ne estas videbla —
  la Activity komenciĝas en la fono kaj rulas sian kodon nur kiam la uzanto vekas
  la telefonon. La ricevilo ludas mem per `LudvicoRegilo` (kun elŝuto-prefero,
  pozicio-resumigo, reprovo) kaj inicialigas `AppStato` kun la procez-nivela
  `ExoPlayerLudiloRegilo.akiru()` ankaŭ en freŝa procezo ((G28)).
- **Full-screen-intent-sciigo estas la videbleca vojo** ((G28)): `CATEGORY_ALARM` +
  `USE_FULL_SCREEN_INTENT` — la sistemo mem vekas la ekranon kaj lanĉas la
  Activity-on plenekrane (normala permeso, sen dialogo; de Android 14 donata de la
  uzanto en "Plenekranaj sciigoj" — degradado al heads-up estas graca). La sono ne
  dependas de ĝi.
- **Ekzakteco**: `USE_EXACT_ALARM` (API 33+, aŭtomate donata al vekhorloĝo-apoj) +
  `SCHEDULE_EXACT_ALARM` (Android 12) → `setAlarmClock` (ekzakta, Doze-escepta,
  alarm-ikono en la statusbreto). Sen permeso: `setWindow` 10 min + averto en la
  UI. La permeso estas revokebla iam ajn — `skedi` devas fali al `setWindow` per
  try/catch de `SecurityException` ((C18)).
- **Validigo/koercado** ((C4)): `sekvaEkigo` koercas horon/minuton kaj maskas
  `ripeto` al la 7 tago-bitoj (maskado, ne limigo — la intencitaj tagoj restas);
  `parsuSugestojnPorAlarmoj` same. Korupta persistinta valoro devas neniam kraŝi
  la skedadon (`LocalTime` ĵetas `IllegalArgumentException`).
- **Rezervo de korupta alarmo-JSON** ((C4)): `PersistantaAlarmoDeponejo.legu()`
  savas la krudan valoron en `ALARMOJ_REZERVO` antaŭ ol fali al la sugestoj — sen
  tio la sekvaj `persistu()`-vokoj anstataŭigus ĝin kaj la uzantaj alarmoj
  estus definitive perditaj.
- **Alarmo-elekto** ((C14)): 1) plej nova nefinita/neerara, 2) falile plej nova
  NE-erara (reludado > erara fluo kiu tuj paneus), 3) null → fallback-ringtono
  (TYPE_ALARM + USAGE_ALARM-atributo) + vibrado.
- **Skribu al Settings ANTAŬ ol lanĉi ion** el la ricevilo: se la alarmo lanĉas
  freŝan procezon, `ekigis(alarmoId)` (unufoja → malaktiva; ripetanta → re-skedo)
  devas okazi antaŭ ol la startanta `AppStato` legas la liston.

## Reto kaj sekureco

- **`network_security_config.xml` restas kun nur `base-config`** (klara teksto
  malpermesata, neniuj esceptoj) — ĝi estas NEPRATA ĉar `minSdk = 26` (< API 28,
  kie klara teksto estas defaŭlte permesata). Novaj kanaloj devas uzi HTTPS; la
  Peranto-emblemo estas gastigita sur `javabog.dk` (la originalo ne havas TLS) ((G1)).
- **CIO + domajno-esceptoj = paneo** (2026-10-11, emulila testo): se la security-config
  iam re-havos domajno-specifajn enskribojn, CIO paneigas por ĈIUJ https-petoj
  ("Domain specific configurations require that hostname aware checkServerTrusted
  is used") — tiam remetu `ktor-client-okhttp`. Kun nur base-config CIO funkcias.
- **URL-oj el nefidinda RSS-enhavo estas validigataj** ((G2)): nur `http://`/`https://`
  trairas (`saniguRetUrlon`) — `file://`, `content://`, `javascript://`, `data:`,
  `intent://` kaj obfuskita `java\tscript:` ne. ExoPlayer/Coil scipovas malfermi
  lokajn URI-ojn — malica fluo devas ne igi nin ludi/montri lokan enhavon.
- **Sama por HTML- ligiloj en priskriboj** ((G4)): nur http(s) kaj `mailto:` en
  `retpaghoUrl`/ligiloj.
- **Ktor-tempolimoj** ((C2)): `requestTimeout`, `connectTimeout` KAJ `socketTimeout` —
  servilo kiu akceptas kaj poste silentegas povas alie teni la peton viva pli longe
  ol la limo.
- **Fiaskpaĝo ne estas fluo** ((K1)): kontrolu HTTP-statuson; 4xx/5xx/kaptiva-portalo
  devas ne esti parsata kiel RSS — la per-kanala catch protokolas kaj daŭrigas
  kun la aliaj kanaloj (regulo 4: unu mortinta fonto ne paneigu la apot).

## Parsado (RSS/Atom)

- **Skip-listo anstataŭas unuopajn kontrolaĵojn** ((G32)): nesubtenataj video-gastigantoj
  estas saltataj per komuna listo (`estasSaltitaGastiganto`), kiel en la malnova
  `RomePodcastParser` (regulo 2).
- **`<audio>` serĉendas en la HTML-enhavo, ne en la XML-dokumento** ((G30)):
  `content:encoded`/`description` estas teksta nodo (CDATA/eskapita HTML) — uzu
  `Ksoup.parse(priskriboKruda)`, ne `ero.selectFirst("audio")`.
- **`enclosure` prefere kun `type="audio/…"`** ((C12)): iuj fluoj metas la bildo-enclosure
  unue.
- **`puriguModeloj` (regulo 6.6) aplikiĝas NUR al la priskribo** ((G15)): la
  fluo-ekstraktado (ekz. `<audio>`-fontoj el Varsovia Vento) devas uzi la KRUDAN
  enhavon — purigita enhavo perdas la ludilojn.
- **Titolo venas el `<title>`** ((G29)); derivaĵo el la enhavo nur ĉe
  `ignoruTitolon` (kaj tiam `trim`-ita, maks 200).
- **Datoj**: akceptataj formoj — `yyyy-MM-dd` (sen T), `…T…`, kaj `d Mon yyyy`
  ankaŭ kun 4-literaj monatoj ("Sept") ((C35)). Ne-rekonata dato forĵetas la EREON
  (ne la tutan fluon).
- **Daŭro** (`itunes:duration`): ankaŭ `HH:MM:SS.ss` (frakciaj sekundoj) ((C37)).
- **Atom-`<link>`**: la teksto de `<link href=…/>` estas malplena ĉeno —
  `?.text()?.ifEmpty { null }` antaŭ ol fali al `attr("href")` ((C36)).
- **archive.org-metadaten-petoj estas limigitaj** al 8 samtempaj (Semaphore) —
  centoj da petoj ricevas 429/timumuron ((G33)); ĉiu peto devas havi try/catch kaj
  fali al divenita dosiernomo ((G34-flanko)).
- **Re-parsado de la diskkaŝo bezonas la httpKlienton** ((G34)): sen ĝi la
  Peranto-elsendoj ricevus DIVENITAJN (ofte 404-ajn) dosiernomojn ĝis la unua
  sukcesa reta refreŝo.

## Persisto (Settings + kaŝmemoroj)

- **Nemutablaj momentfotoj** por la publikaj mapoj de `ElsendoDeponejoImpl` ((G35)):
  legoj (serĉo/getElsendo) okazas dum alia korutino skribas — kun mutableMapo tio
  estis ConcurrentModificationException-risko. Skriboj nur per Mutex.
- **Mutex ĉirkaŭ legu-modifu-skribu** en `PersistaLudatojDeponejo` ((G36)):
  `registriPozicio` (ĉiujn 5 s) kaj `markiFinita`/`markiErara` povas interplektiĝi.
- **URL-ESKAPADO de Esperantaj supersignoj** en alarm-sugestoj ((K5)):
  `%C4%A5`=`ĥ`, `%C5%9C`=`Ŝ`, `%C5%AC`=`Ŭ` — kaj ĝ, ĵ, ŝ, Ĥ. Jam-malkoditaj
  signoj (kodo > 127) trairas senŝanĝe.
- **Dosiernomoj** ((C3)): sankigita ID maks 120 signoj + 8-signa haketaĵo de la
  PLENA ID (unico konserviĝas; RSS-id-oj ofte estas plenaj URL-oj > 255 bajtoj),
  neniuj nul-bajtoj, Windows-rezervitaj nomoj prefiksitaj, finaj punktoj forigitaj.

## Sciigoj kaj intents

- **`INTENT_NONCE`** ((G3)): ĉiuj intencoj al la eksportita `MainActivity`
  (alarmo, sciigo-"Ludi") portas po-instalan hazardan nomon el Settings — sen ĝi
  iu ajn apo povus forĝi la ekstraĵojn (ludi ajnan URI-on, volumo-boost).
  `MainActivity` kontrolas `IntentSxlosilo.estasFidinda(intent)`.
- **`SCIIG_ID` kreskanta kaj persistita** ((C19)): `hashCode()`-kolizioj anstataŭigis
  sciigojn kaj la "Ludi"-PendingIntent de sciigo A celus elsendon B.
- **`goAsync()` por nesinkrona ricevila laboro** (MediaController-konekto, Reto):
  sen ĝi la sistemo detruas la ricevilon antaŭ ol la korutino finiĝas.
- **Reuzata MediaController** ((G22)): `MediaController` kaj ĝia Binder-konekto estas
  pezaj kaj neniam liberiĝas silente — reuzu unu procez-nivelan (alarmo, sciigo-"Ludi").
- **`PendingIntent`-ID-oj devas esti unikaj** (sciigId + 1 por la dua ago de la sama
  sciigo) — koliziantaj RequestCode-oj anstataŭigas la PendingIntent-ojn.
- **`applicationContext`, NE la ricevilan kuntekston**, por bindi servojn:
  `ReceiverRestrictedContext` ĵetas `ReceiverCallNotAllowedException`.

## Konkuro kaj korutinoj

- **`CancellationException` ĉiam reĵetiĝas** el catch-blokoj ((G26)) — engluti ĝin
  mortigas la nuligo-semantikon (korutino ne finiĝas, sed ankaŭ ne plu funkcias).
- **`Dispatchers.IO` ne ekzistas sur wasmJs** — komuna kodo uzas `Dispatchers.Default`
  (la Ktor-motoro mem taskas al siaj fadenoj).
- **WakeLock-dungado el ricevilo** estas transdonata al la korutino kaj liberigata
  en `finally` (kun tempolimo kiel sekureca reto).

## Protokolado kaj Sentry

- **Atendataj malsukcesoj estas `logw`, ne `loge`** ((C1)): unu mortinta kanal-fluo
  estas normala okazaĵo (regulo 4) kaj devas NE iĝi erar-nivela Sentry-evento —
  alikaze oni ne trovas la verajn paneojn. `loge` = paneo.
- **`logd` por la datum-tavolo** (RssParsilo, ElsendoDeponejo): alie Sentry-`beforeSendLog`
  inundeble raportas ĉiun kanal-ŝargadon.
- ** Medio laŭ la DEBUGGING-flago** (G5), ne la build-tipo: debug-versio → "evoluo",
  release → "produktado" (10 % specimenado).
