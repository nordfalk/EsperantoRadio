# 3. Domajno kaj datumoj

## Domajnmodeloj (pura Kotlin, `shared/domain/model`)

### `Kanal` (`@Serializable`)

```kotlin
@Serializable
data class Kanal(
    val slug: String,                 // kodo — unika ŝlosilo (ekz. "muzaiko")
    val nomo: String,                 // vidiga nomo
    val priskribo: String? = null,
    val emblemoUrl: String? = null,
    val rektaElsendaSonoUrl: String? = null,   // livestream (nur Muzaiko)
    val podkastaRssUrl: String? = null,        // elsendojRssUrl
    val retejoUrl: String? = null,            // hejmpaĝoButono
    val retposhto: String? = null,
    val datumFonto: String? = null,           // "rss" aŭ "radio.txt"
    val ignoruTitolon: Boolean = false,       // elsendojRssIgnoruTitolon
    val montruTitolojn: Boolean = true,
    val uzuWebViewPorElsendo: Boolean = false,
    val rektaElsendaPriskriboUrl: String? = null,
    // dateno-movitaj purig-modeloj (regulo 6.6)
    val puriguModeloj: List<String> = emptyList(),
    // dateno-movitaj iframe-gastigant-reguloj (regulo 6.3)
    val iframeReguloj: Map<String, IframeRegulo> = emptyMap(),
) {
    val estasRekta: Boolean get() = rektaElsendaSonoUrl != null
    val havasPodkastojn: Boolean get() = podkastaRssUrl != null
}
```

### `Elsendo` (`@Serializable`)

```kotlin
@Serializable
data class Elsendo(
    val id: String,                   // slug — vidu id-konvenciojn (04_parsado_kaj_arkivo.md)
    val kanalSlug: String,
    val titolo: String,
    val priskribo: String? = null,    // purigita HTML/teksto
    val bildUrl: String? = null,
    val dato: LocalDate,              // publikigdato, yyyy-MM-dd
    val komencoTempo: Instant? = null,
    val dauro: Long? = null,         // sekundoj
    val stream: String,              // audio-URL — la plej grava kampo
    val retpaghoUrl: String? = null,
    val estasRekta: Boolean = false,
) {
    val formatoDato: String get() = ...
    val finoTempo: Instant? get() = ...
}
```

### `Sonfonto` (sealed — anstataŭigas la malnovan `Lydkilde`)

```kotlin
@Serializable
sealed interface Sonfonto {
    @Serializable data class RektaKanalo(val kanal: Kanal) : Sonfonto        // livestream (Muzaiko)
    @Serializable data class Elsendo(val elsendo: Elsendo) : Sonfonto         // podkast-epizodo
}
```

Unuigas la du lud-reĝimojn (rekta vs podkast) en unu tipo, kiun la `LudiloRegilo`
akceptas. La malnova apo havis `Lydkilde`-klason; ĉi tie ĝi iĝas sealed hierarkio.

### `LudantoStato`

```kotlin
sealed interface LudantoStato {
    data object Haltita : LudantoStato
    data object Konektas : LudantoStato
    data object Ludas : LudantoStato
    data class Eraro(val mesagho: String) : LudantoStato
}

data class LudantoInformo(
    val stato: LudantoStato,
    val nunaFonto: Sonfonto?,
    val pozicioMs: Long,
    val dauroMs: Long,
    val bufroProcento: Int? = null,        // por Konektas-stato
    val estasRekta: Boolean,
)
```

### Alikampo-modeloj

```kotlin
@Serializable data class Alarmo(val id, val horo, val minuto, val ripeto, val kanalSlug, val aktiva)
@Serializable data class ElsutoStato(val elsendoId, val stato, val progres, val dosieroPath)
@Serializable data class Agordoj(val lingvo, val elsuthejjo, val nurWifi, val sonEfikoj, val devigiPortreton)
```

## Deponej-interfacoj (`shared/domain/repository`)

```kotlin
interface KanalDeponejo {
    fun observiKanalojn(): Flow<List<Kanal>>
    suspend fun getKanalojn(almiro: Boolean = false): List<Kanal>
    suspend fun getKanal(slug: String): Kanal?
    suspend fun refresigi(): List<Kanal>
}

interface ElsendoDeponejo {
    fun observiElsendojn(kanalSlug: String): Flow<List<Elsendo>>
    suspend fun getElsendojn(kanalSlug: String, pagho: Int = 0, fortoRefresigi: Boolean = false): List<Elsendo>
    suspend fun getElsendo(id: String): Elsendo?
    suspend fun sercxiElsendojn(taxto: String, limo: Int = 50): List<Elsendo>
    suspend fun getPliajnElsendojn(kanalSlug: String, lastaElsendo: Elsendo): List<Elsendo>
}

interface LudantoDeponejo {
    val stato: StateFlow<LudantoInformo>
    suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long = 0)   // anstataŭ ludi()/ludiRektan()
    suspend fun ludi(elsendo: Elsendo) = fiksiFonton(Sonfonto.Elsendo(elsendo))
    suspend fun ludiRektan(kanal: Kanal) = fiksiFonton(Sonfonto.RektaKanalo(kanal))
    suspend fun pauxzigi()
    suspend fun daurigi()
    suspend fun halti()
    suspend fun saltiAl(pozicioMs: Long)
    suspend fun antauxa()     // podkast: salti 5% malantaŭen; rekta: antaŭa kanal
    suspend fun sekva()      // podkast: salti 5% antaŭen;   rekta: sekva kanal
    suspend fun fiksiLauxtecon(volumeno: Float)
}

interface ElsutoDeponejo {
    fun observiElsutojn(): Flow<List<ElsutoStato>>
    suspend fun elsuti(elsendo: Elsendo)
    suspend fun forigiElsuton(elsendoId: String)
    suspend fun getElsutojn(): List<ElsutoStato>
}

interface PlejsatatajDeponejo {
    fun observiPlejsatatajn(): Flow<List<String>>         // kanal-slugs
    suspend fun baskuliPlejsaton(kanalSlug: String)
    suspend fun estasPlejsatata(kanalSlug: String): Boolean
    suspend fun getNombroNovajn(kanalSlug: String): Int
}

interface LastAuxskultitajDeponejo {
    fun observiLastAuxskultitajn(): Flow<List<Elsendo>>
    suspend fun registri(elsendo: Elsendo, pozicioMs: Long)
    suspend fun getPozicio(elsendoId: String): Long?
}

interface AlarmoDeponejo {
    fun observiAlarmojn(): Flow<List<Alarmo>>
    suspend fun krei(alarmo: Alarmo)
    suspend fun forigi(id: String)
    suspend fun getAktivajn(): List<Alarmo>
}

interface AgordojDeponejo {
    val agordoj: StateFlow<Agordoj>
    suspend fun ghisdatigi(agordoj: Agordoj)
}
```

## Uzkazoj (`shared/domain/usecase`)

```kotlin
class GetKanalojnUseCase(val dep: KanalDeponejo)
class GetElsendojnUseCase(val dep: ElsendoDeponejo)
class LudiFontonUseCase(val dep: LudantoDeponejo)
class ElsutiElsendonUseCase(val dep: ElsutoDeponejo)
class BaskuliPlejsatonUseCase(val dep: PlejsatatajDeponejo)
class KreiAlarmoUseCase(val dep: AlarmoDeponejo)
class SercxiElsendojnUseCase(val dep: ElsendoDeponejo)
class GetLastauxskultitajnUseCase(val dep: LastAuxskultitajDeponejo)
```

Uzkazoj estas maldikaj envolvaĵoj — la logiko vivas en la deponejoj. Ili ĉefe
helpas testmokadon kaj separas la deponejojn de la ViewModel-oj. Dependencaĵoj
estas transdonitaj permane en konstruktiloj, sen DI-framintervalo.

## Datentavolo (`shared/data`)

### Reto

```kotlin
interface RadioApiServo {
    suspend fun getKanalojn(): String          // la JSONC-konfiguro
    suspend fun getRadioTxt(): String          // radio.txt
    suspend fun getRssFluo(url: String): String
}

class RadioApiServoImpl(val http: HttpClient) : RadioApiServo { ... }
```

### DTOj + mapilo

```kotlin
@Serializable data class KanalDto(val kodo, val nomo, val emblemoUrl, val elsendojRssUrl, ...)
@Serializable data class KanalAgordoDto(val android, val kanaloj, val FORPRENITAJ_KANALOJ, val elsendojUrl, ...)

fun KanalDto.alKanal(): Kanal = ...
```

### Datumfontoj

```kotlin
class DeforaKanalDatumfonto(val api: RadioApiServo, val parsilo: KanalAgordoParsilo)
class LokaKanalDatumfonto(val kaŝmemoro: KanalKaŝmemoro)
```

### Deponej-implementaĵoj

```kotlin
class KanalDeponejoImpl(
    val defora: DeforaKanalDatumfonto,
    val loka: LokaKanalDatumfonto,
) : KanalDeponejo {
    // Unue legas lokan kaŝmemoron, poste refreŝigas defore.
    // Eraro en defora → liveri kaŝenitan (toleremeco).
}

class ElsendoDeponejoImpl(
    val api: RadioApiServo,
    val parsilo: ElsendoParsilo,        // vidu 04_parsado_kaj_arkivo.md
    val kaŝmemoro: ElsendoKaŝmemoro,
) : ElsendoDeponejo { ... }
```

### Kaŝmemoro

```kotlin
class KanalKaŝmemoro {
    private val kanaloj = MutableStateFlow<List<Kanal>>(emptyList())
    fun get(): List<Kanal> = kanaloj.value
    fun observi(): Flow<List<Kanal>> = kanaloj
    fun set(kanaloj: List<Kanal>) { kanaloj.value = kanaloj }
}
```

## Toleremeco al putrantaj fontoj

Ĉiu deponejo-implementaĵo devas:
1. Kapti retajn erarojn kaj **liveri kaŝenitan datumon** (ne ĵeti).
2. Se unu kanal-fluo malsukcesas, marki tiun kanalon kiel `erara` sed **ne haltigi la aliajn**.
3. Refreŝigi malsinkrone — la UI montras datumojn tuj el kaŝmemoro dum refreŝigo kuras.

```kotlin
// Skema ekzemplo
suspend fun getElsendojn(kanalSlug: String, fortoRefresigi: Boolean): List<Elsendo> {
    val kaŝenitaj = kaŝmemoro.get(kanalSlug)
    if (kaŝenitaj.isNotEmpty() && !fortoRefresigi) {
        refresigiFone(kanalSlug)  // ne blokas
        return kaŝenitaj
    }
    return try {
        val fluo = api.getRssFluo(kanal.podkastaRssUrl)
        val elsendoj = parsilo.parsRss(fluo, kanal)
        kaŝmemoro.set(kanalSlug, elsendoj)
        elsendoj
    } catch (e: Exception) {
        protokolo("Kanalo $kanalSlug fiaskis: ${e.message}")
        kaŝenitaj  // aŭ malplena listo — UI montras erar-indikilon por tiu kanal
    }
}
```

## Platform-ludila abstraktado (`LudiloRegilo`, expect/actual)

La `LudantoDeponejo` estas la domajn-interfaco; la efektiva sonludado estas
platform-specifa kaj vivas kiel `expect`/`actual`:

```kotlin
// commonMain — expect
expect class LudiloRegilo {
    val stato: StateFlow<LudantoInformo>
    fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long = 0)
    fun ludi(); fun pauxzigi(); fun halti()
    fun saltiAl(ms: Long); fun fiksiLauxtecon(v: Float)
    fun antauxa(); fun sekva()
}
```

- **Android:** Media3 ExoPlayer + `MediaSessionService` → malfona ludado,
  mediasciigo, mediabutonoj, sonfokuso, kapaŭskultil/alvok-traktado ĉio "senpaga".
- **iOS:** AVPlayer + AVAudioSession + MPNowPlayingInfoCenter + RemoteCommandCenter.
- **Desktop:** VLCJ (plej bona HLS-subteno) aŭ Media3 (JVM).
- **Web:** `HTMLAudioElement` (+ hls.js por HLS se necesa).

## Erar-trakto en ludado (heredaĵo)

La malnova `Afspiller` havis eksponentan backoff (ĝis 10 provoj) kaj rekomencis
se la konekto perdiĝis dum malpli ol 5 minutoj. Tiu konduto devas esti konservita:

```kotlin
// Skema
suspend fun ludiKunRepro(fonto: Sonfonto, maxProvoj: Int = 10) {
    var provo = 0
    var prokrasto = 1000L  // 1s, duoblita ĉiu provo
    while (provo < maxProvoj) {
        try {
            regilo.fiksiFonton(fonto)
            regilo.ludi()
            return
        } catch (e: Exception) {
            provo++
            protokolo("Ludado fiaskis (provo $provo): ${e.message}")
            delay(prokrasto)
            prokrasto = minOf(prokrasto * 2, 30_000)  // maks 30s
        }
    }
    // transdonu eraron al UI
}
```

Se la reto perdiĝis dum ludado kaj revenas ene de 5 minutoj, rekomencu el la
lasta pozicio. Se pli longe, traktu kiel novan ludadon.
