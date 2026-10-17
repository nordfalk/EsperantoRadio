---
name: konstruado-kaj-testado
description: Ŝargu ĉi tiun skill-on kiam vi konstruas, rulas aŭ testas la apon (Gradle-komandoj, JDK-postuloj, wasmJs-limigoj, instrumentitaj Android-testoj, emulilo, logcat) aŭ kiam platform-specifa kompilado aŭ testo fiaskas.
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
./gradlew :shared:desktopTest        # ĉiuj KMP-testoj (unuoj + UI)
./gradlew :desktopApp:run            # la desktop-apo
./gradlew :androidApp:assembleDebug  # la Android-APK
./gradlew :androidApp:connectedDebugAndroidTest            # instrumentitaj testoj (emulilo)
./gradlew :androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=dk.nordfalk.esperanto.android.SciigoLudiTest
./gradlew :webApp:wasmJsBrowserDevelopmentRun -Pkotlin.daemon.jvmargs=-Xmx4g
./gradlew :desktopApp:radioTxtKomparilo   # komparas kanalkonfiguron kun esperanto-radio.com
```

Konstruu post ĉiu kunfando de master kiu alportas kod-ŝanĝojn:
`changelog-kontrolo`-CI nur kontrolas CHANGELOG-ŝanĝojn, ĝi NE konstruas — rulu
`:shared:desktopTest` + `:androidApp:assembleDebug` post ĉiu merge. Por PR-oj
nur kun tekstaj/agentaj dosieroj (AGENTS.md, `.vibe/skills/`, `docs/`, NOTOJ ktp.)
tio ne necesas — nek loka konstruo nek atendo de la `testoj`-CI; merĝu tuj
(2026-10-17, regulo de la uzanto).

Aktualaj versioj (unu fonto de vero: `gradle/libs.versions.toml`; staka priskribo:
`docs/nova/02_teknika_stako.md`): Kotlin 2.4.21, Gradle 9.8.1, AGP 9.4.1,
compileSdk 37, Compose Multiplatform 1.12.2, Material3 1.12.0-alpha03,
Ktor 3.6.0, Media3 1.11.1, Coil 3.6.3, kotlinx-datetime 0.8.0,
kotlinx-coroutines 1.11.0, ksoup 0.2.6, nav3 1.1.2 (ĝisdatigo 2026-10-17).
KMP-post `androidTarget()` bezonas `android.newDsl=false` kaj
`android.builtInKotlin=false` en `gradle.properties` (AGP 9 — foriĝos en AGP 10;
laŭdaŭre: migrado al `com.android.kotlin.multiplatform.library`).

## Kompilado-limigoj kaj iliaj erarmesaĝoj

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
  Kontrolu per `unzip -p <klib> default/manifest | grep compiler_version`.
  (2026-10-17: nav3 1.1.2 + Kotlin 2.4.21 bone kompilas sur wasmJs — la malnova
  timo ke ĉiu nav3-versio pli nova ol 1.1.0-alpha02 rompos wasmJs ne konfirmiĝis.
  Tamen kontrolu wasmJs-kompilon (`:webApp:compileKotlinWasmJs`) post versi-bumo.)
- **ksoup 0.2.6** funkcias kun Kotlin 2.4.21 kaj CMP 1.12.2 (CI-verda kombino,
  2026-10-17).
  `Ksoup.parseXml(teksto, "")` / `Ksoup.parse(teksto)`; `selectFirst(...)` ekzistas.
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

## Instrumentitaj Android-testoj (emulilo)

- La emulilo estas `emulator-5554`; `adb` ĉe
  `/home/j/Android/Sdk/platform-tools/adb`. Uzu `ANDROID_SERIAL=emulator-5554`
  por ne tuŝi konektitan realan aparaton.
- La debug-APK instaliĝas kiel `dk.nordfalk.esperanto.radio.alfa` (applicationId +
  `.alfa`):
  `adb shell monkey -p dk.nordfalk.esperanto.radio.alfa -c android.intent.category.LAUNCHER 1`
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
- **Logcat-legado**: `adb logcat -d | grep FATAL` (kraŝoj) aŭ
  `grep -E "dk.nordfalk"` (apo). La bufro ciklas rapide dum la 27-kanala ŝargado —
  por diagnozo kaptu la logcaton frue aŭ rulu `adb logcat -c` antaŭ la testo.
