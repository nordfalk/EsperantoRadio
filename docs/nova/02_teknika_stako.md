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
| **Media3 ExoPlayer** | Sonludado (HLS + MP3); MediaSessionService por malfona ludado |
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

Parsreguloj kaj parser-kontrakto estas en `04_parsado_kaj_arkivo.md`. La parsilo
uzas **ksoup** por RSS/Atom + HTML-purigado/iframe-skrapado. Per-kanalaj
apartaĵoj estas **dateno-movitaj** el la kanalagordo (JSON-kampoj
`puriguModeloj`, `iframeReguloj`, `forceHttps`, `parsStrategio`).

La kanalkonfiguro (`esperantoradio_kanaloj_v9.json`) estas **JSON kun komentoj** —
stripi `//`-komentojn antaŭ `kotlinx.serialization`-parsado, aŭ uzi indulgentan
JSONC-parsilon.

## Versikatalogo

Ĉiuj versioj vivas en **unu fonto de vero**: [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml)
(Gradle-versio: `gradle/wrapper/gradle-wrapper.properties`; compileSdk:
`androidApp/build.gradle.kts`). Ili ne estas ripetataj ĉi tie — versioj rapide
malaktualiĝas kaj duobla skribo kreus malakordon.

> **Material3** havas apartan version de `compose-multiplatform` — prenu la
> parigon el la CMP-release-notoj (tabelo „Libraries"), ne el la CMP-versinombro.
> **AGP 9 + KMP**: `gradle.properties` enhavas la necesajn pretervojojn
> `android.newDsl=false` kaj `android.builtInKotlin=false` (vidu la lecionon en
> AGENTS.md).
