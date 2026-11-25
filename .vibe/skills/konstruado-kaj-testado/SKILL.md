---
name: konstruado-kaj-testado
description: Ŝargu ĉi tiun skill-on kiam vi konstruas, rulas aŭ testas la apon (Gradle-komandoj, JDK-postuloj, wasmJs-limigoj, instrumentitaj Android-testoj, Compose-UI-testoj, emulilo, logcat), kiam vi verkas testojn, aŭ kiam platform-specifa kompilado aŭ testo fiaskas.
---

# Konstruado kaj testado de EsperantoRadio

## Bazaj komandoj

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # JDK 17 devas; JDK 21 mankas jlink

# Malnova apo (ne tuŝebla — nur konstrua)
./gradlew :app:assembleDebug
./gradlew :parse:rssarkivserverJar
java -jar malnova/parse/build/libs/rssarkivserver.jar

# Nova apo
./gradlew :shared:desktopTest        # ĉiuj KMP-testoj (unuoj + UI); 4 paralelaj JVM-forkoj (2026-11-24: 18 s → ~11 s)
./gradlew :shared:desktopTest -Pekranfotoj   # inkluzivas EkranfotoTesto (PNG-oj por la dokumentaro; defaŭlte ekskludita)
./gradlew :desktopApp:run            # la desktop-apo
./gradlew :androidApp:assembleDebug  # la Android-APK
./gradlew :androidApp:connectedDebugAndroidTest            # instrumentitaj testoj (emulilo)
./gradlew :androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dk.nordfalk.esperanto.android.SciigoLudiTest
./gradlew :webApp:wasmJsBrowserDevelopmentRun -Pkotlin.daemon.jvmargs=-Xmx4g
./gradlew :desktopApp:radioTxtKomparilo   # komparas kanalkonfiguron kun esperanto-radio.com
```

Konstruu post ĉiu kunfando de master kiu alportas kod-ŝanĝojn (rulu
`:shared:desktopTest` + `:androidApp:assembleDebug`): la `changelog-kontrolo`-CI
nur kontrolas CHANGELOG-ŝanĝojn, ĝi NE konstruas. Por PR-oj nur kun tekstaj/agentaj
dosieroj (AGENTS.md, `.vibe/skills/`, `docs/`, NOTOJ ktp.) tio ne necesas — nek loka
konstruo nek atendo de `testoj`-CI (2026-10-17, regulo de la uzanto). Same por
kod-dosieroj, kiam la ŝanĝoj tuŝas NUR komentojn aŭ dokumentadon (2026-10-25,
regulo de la uzanto). **La kunfando mem restas sub L6 en AGENTS.md — ĉiam kun
aparta, eksplikita aprobo de la uzanto, ankaŭ kiam konstruo/CI-atendo ne necesas.**

Versioj ne estas listigitaj ĉi tie — unu fonto de vero:
`gradle/libs.versions.toml` (Gradle: `gradle/wrapper/gradle-wrapper.properties`;
compileSdk: `androidApp/build.gradle.kts`; staka priskribo:
`docs/nova/02_teknika_stako.md`). KMP-post `androidTarget()` bezonas
`android.newDsl=false` kaj `android.builtInKotlin=false` en `gradle.properties`
(AGP 9 — foriĝos en AGP 10;
laŭdaŭre: migrado al `com.android.kotlin.multiplatform.library`).

## Kompilado-limigoj kaj iliaj erarmesaĝoj

- **"Incremental compilation failed: The following FqNames can't be derived from
  DirtyData.dirtyLookupSymbols"** (ekz. pri `Elsendo`, `Kanalo`, `LudataElsendo`) —
  forlasita inkrementa kompilo-kaŝo post oftaj branĉo-ŝanĝoj/kunfandoj, NE koda
  eraro (konfirmite 2026-10-24: post `./gradlew clean` ĉio verda). Unua ago:
  `./gradlew clean` (aŭ `--rerun-tasks`) — ne kodo-esploro.
- **`Dispatchers.IO` ne ekzistas sur wasmJs** (nur `Default` kaj `Main`) — en
  commonMain ĝi rompas la tutan wasmJs-kompiladon. Uzu `Dispatchers.Default`.
- **`@Volatile` bezonas `import kotlin.concurrent.Volatile`** — sur JVM ĝi solvas
  per la defaŭlta `kotlin.jvm.*`, sed sur wasmJs "Unresolved reference 'Volatile'".
- **`Dispatchers.Main` sur Desktop/JVM** bezonas `kotlinx-coroutines-swing`
  (`libs.kotlinx.coroutines.swing` en desktopMain; kongrua kun la
  coroutines-versio de Compose).
- **wasmJs-ligilo**ĵetas OutOfMemoryError kun la defaŭlta 1,5 GB — ĉiam
  `-Pkotlin.daemon.jvmargs=-Xmx4g`. Enirpunkto: `ComposeViewport` (CanvasBasedWindow
  estas eraro en Compose 1.10). Nur `wasmJs`-celo ekzistas (ne `js` — Skia-eraro).
- **klib-ABI**: Wasm/Native-bibliotekoj (`.klib`) de pli nova Kotlin ne legeblas de
  la nuna → "Unresolved reference" NUR sur wasmJs (JVM/Android uzas jar).
  Kontrolu per `unzip -p <klib> default/manifest | grep compiler_version` kaj
  rulu `:webApp:compileKotlinWasmJs` post versi-bumo.
- **ksoup**: `Ksoup.parseXml(teksto, "")` / `Ksoup.parse(teksto)`;
  `selectFirst(...)` ekzistas.
- **HLS (.m3u8) sur Android** postulas `androidx.media3:media3-exoplayer-hls` —
  sen ĝi la MediaSession silente malsukcesas (`ClassNotFoundException:
  HlsMediaSource$Factory` en logcat).
- **Ktor 3**: CIO sur Android/Desktop/iOS, sed ne sur wasmJs (bezonas la Node.js
  `net`-modulon). Uzu la abnormalan `httpMotoro` (expect/actual: CIO aŭ Js = fetch).
  La Js-motoro ĵetas `kotlin.Error` (ne Exception) ĉe reteraroj/CORS — kaptu
  `Throwable` kaj reĵetu `CancellationException`.
- **CIO + domajno-esceptoj en network_security_config = paneo de ĈIUJ https-petoj**
  sur Android (CertificateException "hostname aware checkServerTrusted"). Kun nur
  `base-config` (la nuna stato) CIO funkcias; se iam re-aldoniĝas domajno-esceptoj,
  remetu `ktor-client-okhttp` kiel Android-motoron.
- **iOS ne konstrueblas sur Linux** (`Unresolved reference 'foundation'` en
  iosMain — mankas Xcode/platform-bibliotekoj) — la iOS-celoj estas por macOS.

## Compose-UI-testoj — kaptindaĵoj (validaj kaj por desktopTest kaj por instrumentitaj)

- **`ModalNavigationDrawer` restas en la semantika arbo ankaŭ kiam la tirkesto
  estas fermita** (2026-10-17) — `onNodeWithText("Agordoj")` trovas 2 nodojn
  (ekrana titolo + tirkest-ero). Asertu laŭ enhava teksto (ekz. agordo-ŝaltilo),
  ne laŭ ekrana titolo; por la tirkest-butono uzu unikan `contentDescription`
  ("Naviga menuo" — ne "Menuo", kiu ekzistas sur ĉiu karto).
- **Dialogoj kreas apartan komponan radikon** (2026-10-17): `AlertDialog`/`Dialog`
  igas `onRoot()` fiaski per "Expected exactly 1 node but found 2". Kaptu la lastan
  radikon per `onAllNodes(SemanticsMatcher("estasRadiko") { it.isRoot })` kaj
  `.get(indekso)` sur `SemanticsNodeInteractionCollection`. ATENTU: `isRoot` estas
  ECO (`val`), ne funkcio — `it.isRoot()` donas la konfuzan eraron "Cannot infer
  type for T/R … DeepRecursiveFunction.invoke". Krome `onNodeWithText` serĉas en
  ĈIUJ radikoj: buton-teksto ekzistanta kaj en la dialogo kaj en la ekrano malantaŭ
  ĝi (ekz. "Nuligi") ne estas unika — fermu la dialogon per teksto kiu aperas nur
  en ĝi (ekz. "Bone").
- **La subtitolo de MiniLudilbreto estas la kombinita teksto `"$subtitolo · $statTeksto"`
  (2026-11-02, #167)** — aserti la tutan ĉenon (ekz.
  `"20 - RadioStacio FM - … · Rekta elsendo"`), ne la partan programtitolon.
- **Rulumado de LazyRow en Desktop-testoj (2026-11-24, #205):** la fenestro estas
  ~1024×768 px; kartoj (150 dp + 12 dp interspaco) → horizontala vico bezonas ~7+
  erojn por esti rulebla. Svingu specifan vicon per
  `onRoot().performTouchInput { swipe(Offset(...y), Offset(...y)) }` kun
  y-koordinato prenita el karta nodo (`positionInRoot.y + size.height/2f`); unu
  plenlarĝa svingo flugos ĝis la fino. Du sama-tekstaj nodoj en malsamaj sekcioj
  distingeblas per `fetchSemanticsNodes().maxBy { it.positionInRoot.y }` — la pli
  suba estas la sekcio pli sube sur la ekrano. Nombro-asertoj estas nedeterminismaj
  por kartoj apud la vidkampo-limo de la vico — prefere `assertIsDisplayed` sur
  la elektita kopio.
- **Scaffold-enhavo DEVAS ricevi la `padding`-on el la content-lambda** (2026-10-17,
  PR #114): forgeso ne fiaskas en testoj (la nodo restas "displayed") — la TopAppBar
  simple kovras la unuajn liniojn de la enhavo. Sub-komponaĵoj de la enhavo bezonas
  `PaddingValues`-parametron.
- **Se fona konstruo fiaskas sed vi vidis nur filtritan eligon** (`tail`/`grep` en
  `process.start`, 2026-11-24, #205): la procez-reteno forĵetis la detalojn — rerulu
  kun plena (aŭ `grep -E "FAILED|BUILD|e: "`) eligokapto antaŭ ol komenci diagnozon.

## Medio

- `git push --force` estas blokita de hoko (amendu nur nepuŝitajn komitojn).
- `/tmp` estas blokita — uzu la skraŭĉdosierujon, ĉi tie praktike
  `server/data/privata/` (gitignored).
- Instrumentitaj testoj povas fiaski sub granda gastiganta ŝarĝo (load ~40+) —
  komparu kun master en aparta worktree antaŭ ol atribui fiaskon al PR.

## Instrumentitaj Android-testoj (emulilo)

- La emulilo estas `emulator-5554`; `adb` ĉe
  `/home/j/Android/Sdk/platform-tools/adb`. Uzu `ANDROID_SERIAL=emulator-5554`
  por ne tuŝi konektitan realan aparaton.
- La debug-APK instaliĝas kiel `dk.nordfalk.esperanto.radio.alfa` (applicationId +
  `.alfa`):
  `adb shell monkey -p dk.nordfalk.esperanto.radio.alfa -c android.intent.category.LAUNCHER 1`
- **ATENTU pri permana adb-serĉado**: `connectedDebugAndroidTest` MALINSTALAS
  la .alfa-APK-on post la testo (AGP-konduto). Aldone la emulilo havas orfan
  prototipan pakon `dk.nordfalk.esperanto.android` (v0.1, sept. 2026) — lanĉu
  ĝin NENIAM; por permana kontrolo unue instalu la debug-APK-on
  (`adb install androidApp/build/outputs/apk/debug/*.apk`) kaj uzu la .alfa-pakon.
- **GrantPermissionRule**: instrumentitaj Compose-testoj bezonas ĝin por
  `POST_NOTIFICATIONS` (vidu `AndroidUiTest`) — alie la permes-dialogo paŭzigas
  la Activity kaj la testregistro raportas "No compose hierarchies found".
- **Antaŭekzistantaj fiaskoj sur la API-26-bildo**: 3-4 `AndroidUiTest`-testoj
  fiaskas sur `API_26_Android_8(AVD)` ankaŭ sur pura master ("No compose
  hierarchies", medio-specifa — konfirmita per worktree-komparo 2026-10-13).
  Ne atribuu ilin al viaj ŝanĝoj; la aliaj testoj (AlarmoReceiviloTest,
  SciigoLudiTest, EraroKlasifikoTest) devas pasi.
- **Compose-UI-testoj + fonaj ŝargadoj**: `waitForIdle()` ne atendas korutinojn
  ekster la Compose-skopo (ekz. la ViewModel-skopoj ekde PR #109) — uzu
  `waitUntil(timeoutMillis = 5_000) { <kondiĉo> }` (ATENTU: la unua parametro
  estas `conditionDescription: String?` — nomu `timeoutMillis`).
- **uiautomator-dump povas montri malplenan tekstatributon por longa plur-linia Compose-teksto** (2026-10-31, la kruda-agorda linio en KanalInformoj): la nodo tamen ESTAS en la hierarkio — kontrolu la `bounds`, ne nur la tekstatributon, antaŭ ol konkludi ke komponanto ne bildigiĝas. Pli fidinda: la nodo-grandeco antaŭ/post klako.
- **Logcat-legado**: `adb logcat -d | grep FATAL` (kraŝoj) aŭ
  `grep -E "dk.nordfalk"` (apo). La bufro ciklas rapide dum la 27-kanala ŝargado —
  por diagnozo kaptu la logcaton frue aŭ rulu `adb logcat -c` antaŭ la testo.

## Testrapideco kaj mezurado

- **desktopTest rulas en 4 paralelaj JVM-forkoj** (`maxParallelForks` en
  shared/build.gradle.kts, PR #209). Pli da forkoj (provitaj: 8) ne plu helpas:
  la limo estas la klaso-interna sinsekvo (plej granda klaso: HejmoEkranoTest
  ~3,5 s) plus la JVM-starto de ĉiu forko. **EkranfotoTesto** estas defaŭlte
  ekskludita — ĝi ne estas konduttesto sed PNG-generatoro por la dokumentaro;
  rulu ĝin per `-Pekranfotoj`.
- **Ne mezuru test-daŭrojn en energiŝpara CPU-reĝimo** (2026-11-24, PR #209): la
  unuaj mezuroj (489 testoj «57 s») estiĝis en energiŝpara modo kaj estis ~3×
  troigitaj; en normala reĝimo la sama rulo daŭris 18 s (kun EkranfotoTesto kaj
  1 forko), 11 s kun la nova agordo. Kontrolu la potenca-reĝimon/ŝarĝon de la
  maŝino antaŭ ol atribui malrapidecon al la kodo aŭ ĝustigi Gradle-agordon
  laŭ mezuroj.

## Reta versio (wasmJs) — musa navigado kaj mezurado (2026-11-24, C56/PR #210)

- **La nuna CMP-versio ne ligas la musan radeton al `LazyRow`** (kaj muso-treno ne
  movas ruleblajn ujoj): `LazyColumn` ja ruliĝas per radeto DENASKE, sed la
  kartaj vicoj bezonas propran trakton — `ui/VicRulado.kt`
  (`rulumuViconPerMuso`/`rulumuKolumnonPerMuso`) observas `PointerEventType.Scroll`
  kaj `PointerType.Mouse` en la Initial-/Main-pasoj kaj vokas `scrollBy`.
- **Hejma kolumno ofte NE havas rulum-amplekson** en altaj fenestroj (la enhavo
  preskaŭ plenigas la ekranon) — `scrollBy` tiam konsumas 0 sen eraro. Ekzamenu
  en malalta fenestro (ekz. 420 px) antaŭ ol konkludi, ke io ne funkcias.
- **Mezuru la retan UI-n per Playwright** (`playwright-core` instalita en
  `tmp/` ĉe la radiko de la laborkopio — gitignorita, vidu servilo-arkivo;
  sistema Chrome): `page.on('console')` kaptas `logd` de la apo;
  `chromium.launch({executablePath: '/usr/bin/google-chrome'})`;
  tuŝgestoj per `CDPSession Input.dispatchTouchEvent`.
- **`grep -c ĉeno *.wasm` kasas erarojn**: kompila fiasko pere de
  `… | tail -1 && alia` montras «BUILD»-sukceson kaj publikiĝas malnova fasko —
  ĉiam kontrolu `grep -E "BUILD|e: "`, ne nur `tail`.

- **Neniam forigu kodon per plur-linia ĉeno-kongruo sen relegi la rezulton**
  (2026-11-24, PR #210): skriptita forigo de protokol-instrumentigo (`logd`-linioj)
  forviŝis KUNE ankaŭ najbaran realan kodon — la muso-kaptilon
  `if (ŝanĝo.type != PointerType.Mouse) continue` kaj `trenMezuro += abs(delto)`
  — ĉar ambaŭ kuŝis inter la forigotaj linioj; CodeRabbit prave rimarkis la
  regreson (duobla tuŝa rulado sur Android, klak-karto ĉe trenfino). Post ĉiu
  amasa skripta prilaboro de fontkodo: `git diff` plu-legu la rezultajn
  blokojn, ne nur la ŝanĝitajn liniojn.
