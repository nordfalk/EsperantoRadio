# 2. Teknika stako

## Kerno

| Teknologio | Kialo |
|---|---|
| **Kotlin Multiplatform** | Komuna kodo trans Android/iOS/Desktop/Web |
| **Compose Multiplatform** | Deklarativa UI, komuna trans platformoj |
| **Kotlin Coroutines + Flow** | Asinhronio, reaktiva stato (`StateFlow`/`SharedFlow`) |
| **Koin** | Malpeza dependen-injekto (KMP-kongrua) |
| **Ktor Client** | Reta komunikado (komuna) |
| **kotlinx.serialization** | JSON-parsado (kanalkonfiguro, DTOj) |
| **kotlinx-datetime** | Datoj (anstataŭ Joda-Time) |
| **ksoup** (`com.fleeksoft.ksoup`) | HTML/XML-purigado kaj iframe-skrapado (KMP, anstataŭ Jsoup) |
| **Coil 3** | Bildoj (kanalemblemoj, elsendobildoj) — multiplatforma, anstataŭ Picasso/AQuery |
| **SQLDelight** | Persisto (plejŝatataj, lastaŭskultitaj, elŝutoj, elsendo-kaŝmemoro) — multiplatforma, tipsignifa |
| **multiplatform-settings** | Agordoj (anstataŭ SharedPreferences), + DataStore sur Android |
| **Decompose** (aŭ compose-navigation-multiplatform) | Plursistema navigado, stak/backstack, testebla |
| **kotlin.test + Turbine** | Testado, Flow-asertoj |

## Platform-specifa

### Android

| Teknologio | Kialo |
|---|---|
| **Media3 ExoPlayer** | Sonludado (HLS + MP3) |
| **Media3 MediaSessionService** | Malfona ludado, mediasciigo, mediabutonoj, sonfokuso (ĉio "senpaga") |
| **DataStore (Preferences)** | Agordoj (anstataŭ SharedPreferences) |
| **DownloadManager** aŭ **WorkManager** | Elŝutoj |
| **AlarmManager** | Vekhorloĝo |
| **AndroidX Lifecycle** | ViewModel, lifecycle |

### iOS

| Teknologio | Kialo |
|---|---|
| **AVPlayer / AVFoundation** | Sonludado (HLS + MP3) |
| **AVAudioSession** + **MPNowPlayingInfoCenter** + **RemoteCommandCenter** | Malfona sono, "Now Playing", mediabutonoj |
| **URLSession** (background) | Elŝutoj |
| **UNUserNotificationCenter** | Sciigoj |
| **BackgroundTasks** | Malfona procezado |

### Desktop (JVM)

| Teknologio | Kialo |
|---|---|
| **VLCJ** (aŭ Media3 JVM) | Sonludado — plej bona HLS-subteno por livestream |
| **java.awt / JavaFX** | Sistempletoj, sciigoj |

### Web (Wasm)

| Teknologio | Kialo |
|---|---|
| **`HTMLAudioElement`** | Sonludado (MP3) |
| **hls.js** | HLS-subteno por Muzaiko-livestream (se necesa) |
| — | Neniu elŝuto, neniu malfona ludado, neniu widget/vekhoro |

## Konstruilo

| Ilo | Kialo |
|---|---|
| **Gradle (Kotlin DSL)** | Konstruado |
| **KMP-aldonaĵo** | Plursistema |
| **Compose Multiplatform-aldonaĵo** | UI |
| **Ktor-aldonaĵo** | Serilatigado |
| **Serialization-aldonaĵo** | JSON |
| **Versikatalogo (`libs.versions.toml`)** | Versi-administrado (anstataŭ malmolaj versioj) |

## Reta tavolo

### `HttpClientFactory` (expect/actual)

- **Android:** OkHttp-motoro
- **iOS:** Darwin-motoro
- **Desktop:** Java- aŭ CIO-motoro
- **Web:** JS-motoro (browser fetch)

Instalitaj Ktor-plugin-oj:
- `Logging` (INFO)
- `ContentNegotiation` kun `Json { prettyPrint, isLenient, ignoreUnknownKeys }`
- `HttpTimeout` (30s legado / 10s konekto)
- `HttpCache`
- `UserAgent` (identiĝo kiel `EsperantoRadio/3.0`)
- `IfModifiedSince`-kapoj (heredaĵo de `FilCache`) por kaŝena valideco

### Baza URL

La kanalkonfiguro estas elŝutata de `https://javabog.dk/privat/esperantoradio_kanaloj_v9.json`
kun ene-enigita rezervo en `commonMain/resources/`.

## RSS-parsado

Vidu `04_parsado_kaj_arkivo.md` por la sep parsregoloj. La parsilo mem:

- **KSM-XML** (`xmlutil` / `kxml2`) aŭ propra XmlPullParser-envolvaĵo por komuna
  RSS/Atom-parsado (kXML2 estas Android/JVM; por iOS uzu komunan `kotlinx`-XML
  aŭ `ksoup`).
- **Jsoup** (`com.fleeksoft.ksoup` por KMP) por HTML-purigado kaj iframe-skrapado
  (archive.org-embed, Google Drive).
- Per-kanalaj apartaĵoj estas **dateno-movitaj** el agordo, ne malmolaj.

## Dateno-movita kanalagordo

La nova apo reuzu `esperantoradio_kanaloj_v9.json` (kun komentoj). Du opcioj:

1. **Strippi `//`-komentojn** antaŭ `kotlinx.serialization`-parsado, aŭ
2. uzi **indulgentan JSONC-parsilon** (ekz. `kotlinx.serialization` kun `isLenient`
   + antaŭprocezo, aŭ `encoding/json5`).

Po-kanalaj purig-modeloj (regulo 6.6) kaj iframe-gastigant-reguloj estu
**en agordo** (JSON-kampo `puriguModeloj: [...]`, `iframeGastigantoj: {...}`),
ne en kodo. Tiel oni povas ĝisdatigi la fonto-konon sen rekonstrui la apot.

## Kial Koin (ne Dagger/Hilt)

- Hilt estas Android-nura. Koin estas pura-Kotlin kaj funkcias en ĉiuj KMP-celoj.
- Koin 4 subtenas KMP-native (čekite `koin-core`, `koin-compose`).

## Persisto-strategio

La malnova apo stokas malmulte — plejparte en SharedPreferences + Java-seriigo.
La nova apo uzas:
- **multiplatform-settings** (+ DataStore sur Android) por agordoj
- **SQLDelight** por strukturitaj datumoj (plejŝatataj, lastaŭskultitaj kun
  daŭriga pozicio, elŝutstato, elsendo-kaŝmemoro) — tipsignifa, multiplatforma
- **en-memora `StateFlow`** por vivaj kanaloj/elsendoj (plenigitaj el SQLDelight
  + defora refreŝigo)

Komencu simple; SQLDelight-skemo kreskas laŭbezone.

## Versikatalogo (proponita `gradle/libs.versions.toml`)

```toml
[versions]
kotlin = "2.1.0"
compose-multiplatform = "1.7.0"
koin = "4.0.0"
ktor = "3.0.3"
kotlinx-serialization = "1.7.3"
kotlinx-datetime = "0.6.1"
media3 = "1.5.1"
ksoup = "0.4.0"
coil = "3.0.0"
sqldelight = "2.0.2"
multiplatform-settings = "1.2.0"
decompose = "3.2.0"
turbine = "1.2.0"
androidx-datastore = "1.1.1"
```
(Vidu la nunajn stabilajn versiojn dum efektivigado.)
