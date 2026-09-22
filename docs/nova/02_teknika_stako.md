# 2. Teknika stako

## Kerno

| Teknologio | Kialo |
|---|---|
| **Kotlin Multiplatform** | Komuna kodo trans Android/iOS/Desktop/Web |
| **Compose Multiplatform** | Deklarativa UI, komuna trans platformoj |
| **Kotlin Coroutines + Flow** | Asinhronio, reaktiva stato (`StateFlow`/`SharedFlow`) |
| *(permana injektado)* | Dependencaĵoj transdonitaj permane en konstruktiloj — kiel la ekzistanta kodbazo. Neniu DI-framintervalo |
| **Ktor 3 Client** | Reta komunikado (komuna) |
| **kotlinx.serialization** | JSON-parsado (kanalkonfiguro, DTOj) |
| **kotlinx-datetime** | Datoj (anstataŭ Joda-Time) |
| **ksoup** (`com.fleeksoft.ksoup`) | HTML/XML-purigado + iframe-skrapado (KMP) |
| **Coil 3** | Bildoj — multiplatforma, defaŭlta memora+diska kaŝmemoro |
| **multiplatform-settings** | Agordoj (daŭrigo, sciigoj, evoluo, temo) + JSON-persisto |
| **Navigation 3** (`androidx.navigation3`) | Plursistema navigado — `NavKey`/`NavDisplay`/`entryProvider`, stato persistebla per `SavedStateConfiguration` |
| **kotlin.test + Turbine** | Testado, Flow-asertoj |
| **Sentry.io** (`sentry-kmp`) | Erarmonitorado trans ĉiuj platformoj |

## Platform-specifa

### Android

| Teknologio | Kialo |
|---|---|
| **Media3 ExoPlayer 1.5.1** | Sonludado (HLS + MP3); MediaSessionService por malfona ludado |
| **Media3 MediaSessionService** | Malfona ludado, mediasciigo, mediabutonoj, sonfokuso (ĉio "senpaga") |
| **DownloadManager** / **WorkManager** | Elŝutoj / sciigoj pri novaj elsendoj |
| **AlarmManager** | Vekhorloĝo |

### iOS

| Teknologio | Kialo |
|---|---|
| **AVPlayer / AVFoundation** | Sonludado (HLS + MP3) |
| **AVAudioSession** + **MPNowPlayingInfoCenter** + **RemoteCommandCenter** | Malfona sono, "Now Playing", mediabutonoj |
| **URLSession** (background) | Elŝutoj |

### Desktop (JVM)

| Teknologio | Kialo |
|---|---|
| **mp3spi + SourceDataLine** | Sonludado — pura Java, fluas MP3 super HTTP. Neniu nacia dependeco. Ne subtenas HLS nek seek |
| **java.awt** | Sistempletoj (se necese) |

> VLCJ estis origine planita sed forlasita — ĝi postulas VLC instalitan.
> JavaFX MediaPlayer estis ankaŭ provita sed donas `ERROR_MEDIA_AUDIO_FORMAT_UNSUPPORTED` por MP3 sur Linukso.

### Web (Wasm)

| Teknologio | Kialo |
|---|---|
| **`HTMLAudioElement`** | Sonludado (MP3) |
| **retumilo** | HLS por Muzaiko-livestream (nativa en la retumilo) |

Elŝutoj, malfona ludado, widget, vekhorloĝo ne haveblas sur Web.

## Konstruilo

Gradle (Kotlin DSL), KMP-aldonaĵo, Compose Multiplatform-aldonaĵo,
Serialization-aldonaĵo, versikatalogo (`libs.versions.toml`).

## Reta tavolo

Unu Ktor-kliento en `commonMain` — neniu `expect`/`actual`. La **CIO**-motoro
estas la sola motoro kiu funkcias trans ĉiuj kvar celoj (JVM, Android, Native,
JS, WasmJs). Ĝi subtenas nur HTTP/1.x, sed tio sufiĉas: la apo elŝutas JSON
kaj RSS-fluojn, kaj HLS-ludado por Muzaiko okazas en la platforma ludilo
(Media3/AVPlayer), ne tra la HTTP-kliento.

```kotlin
val client = HttpClient(CIO) {
    install(Logging) { level = LogLevel.INFO }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 10_000 }
    install(HttpCache)
    install(UserAgent) { agent = "EsperantoRadio/3.0" }
}
```

`IfModifiedSince`-kapoj por kaŝena valideco (heredaĵo de `FilCache`).

Kanalkonfiguro elŝutata de `https://javabog.dk/privat/esperantoradio_kanaloj_v9.json`
kun ene-enigita rezervo en `commonMain/resources/`.

## Persisto

La kliento ne bezonas datumbazon. Ĝi simple konservas la respondojn de la
servilo (kanal-JSON, RSS-fluoj) kiel kaŝenitajn dosierojn — aŭ reŝarĝas ilin
se la kaŝmemoro mankas aŭ estas malnova.

- **multiplatform-settings** por agordoj (daŭrigo, sciigoj, evoluo, temo) + JSON-persisto (alarmoj, ludpozicioj, ludvico)
- **dosierkaŝmemoro** por servil-respondoj (kiel la malnova `FilCache` — kun `If-Modified-Since`)
- **en-memora `StateFlow`** por vivaj kanaloj/elsendoj (plenigitaj el kaŝmemoro + defora refreŝigo)

Por strukturitaj datumoj kiuj vere bezonas persiston (lastaŭskultita pozicio,
plejŝatataj), konservu kiel JSON-dosiero aŭ en `multiplatform-settings`. Aldonu
datumbazon nur se la datumo vere kreskas.

## RSS-parsado

Parsregoloj kaj parser-kontrakto estas en `04_parsado_kaj_arkivo.md`. La parsilo
uzas **ksoup** por RSS/Atom + HTML-purigado/iframe-skrapado. Per-kanalaj
apartaĵoj estas **dateno-movitaj** el la kanalagordo (JSON-kampoj
`puriguModeloj`, `iframeReguloj`, `forceHttps`, `parsStrategio`).

La kanalkonfiguro (`esperantoradio_kanaloj_v9.json`) estas **JSON kun komentoj** —
stripi `//`-komentojn antaŭ `kotlinx.serialization`-parsado, aŭ uzi indulgentan
JSONC-parsilon.

## Versikatalogo

Versioj rapide malaktualiĝas. Vidu la nunajn stabilajn versiojn dum efektivigado.
Nuna `gradle/libs.versions.toml` (septembro 2026):
```toml
[versions]
kotlin = "2.2.20"
compose-multiplatform = "1.10.0"
compose-material3 = "1.10.0-alpha05"
ktor = "3.1.3"
kotlinx-serialization = "1.7.3"
kotlinx-datetime = "0.7.0"
media3 = "1.5.1"
ksoup = "0.2.2"
coil = "3.0.4"
multiplatform-settings = "1.2.0"
turbine = "1.2.0"
mp3spi = "1.9.5.4"
sentry-kmp = "0.27.0"
multiplatform-nav3 = "1.1.1"
```

> **ksoup 0.2.2** estas la versio kongrua kun Kotlin 2.2.20 (0.2.6+ postulas Kotlin 2.3+).
> **Material3** havas apartan version (`1.10.0-alpha05`), ne la saman kiel `compose-multiplatform`.
