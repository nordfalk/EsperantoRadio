# 3. Domajno kaj datumoj

> Tiu dokumento priskribas la efektivajn modelojn kaj interfacojn en la kodo.
> Por la plej ĝisdatigitan liston de dosieroj, vidu la struktursekcion en `NOTOJ.md`
> (sekcio "Deponeja koda mapo").

## Domajnmodeloj (`shared/domain/model/Modeloj.kt`)

### `Kanalo` (`@Serializable`)

```kotlin
@Serializable
data class Kanalo(
    val slug: String,                         // kodo — unika ŝlosilo (ekz. "muzaiko")
    val nomo: String,                         // vidiga nomo
    val emblemoUrl: String? = null,
    val rektaElsendaSonoUrl: String? = null,   // tuja elsendfluo (nur Muzaiko)
    val podkastaRssUrl: String? = null,        // elsendojRssUrl
    val rektaElsendaPriskriboUrl: String? = null,
    val retejoUrl: String? = null,            // hejmpaĝoButono
    val retpoŝto: String? = null,
    val ignoruTitolon: Boolean = false,        // elsendojRssIgnoruTitolon
    val montruTitolojn: Boolean = true,
    val uzuWebViewPorElsendo: Boolean = false,
) {
    val estasRekta: Boolean get() = rektaElsendaSonoUrl != null
    val havasPodkastojn: Boolean get() = podkastaRssUrl != null
}
```

> La malnova apo havis `puriguModeloj`, `iframeReguloj`, `forceHttps`, `parsStrategio`
> kiel kampojn sur la kanal-modelo. En la nova apo tiuj estas traktataj rekte en la
> parsilo (`RssParsilo`) laŭ la kanal-slug, ne kiel dateno-movitaj kampoj. Tio estas
> malpli pura sed pli simpla — la parsreguloj estas malmolaj sed bone dokumentitaj
> (vidu `04_parsado_kaj_arkivo.md`).

### `Elsendo` (`@Serializable`)

```kotlin
@Serializable
data class Elsendo(
    val id: String,                   // slug — vidu ID-konvenciojn (04_parsado_kaj_arkivo.md)
    val kanaloSlug: String,
    val kanaloNomo: String? = null,   // nomo de la kanalo (por sciigoj kaj UI)
    val titolo: String,
    val priskribo: String? = null,    // purigita plata teksto (por listoj, serĉo)
    val priskriboHtml: String? = null, // purigita HTML kun etikedoj (por detala vido)
    val bildoUrl: String? = null,
    val dato: String,                 // yyyy-MM-dd
    val daŭro: Long? = null,          // sekundoj
    val fluo: String,                 // audio-URL (mp3) — la plej grava kampo
    val retpaĝoUrl: String? = null,
    val estasRekta: Boolean = false,
)
```

### `Sonfonto` (sealed)

```kotlin
@Serializable
sealed interface Sonfonto {
    @Serializable data class RektaKanalo(val kanalo: Kanalo) : Sonfonto
    @Serializable data class ElsendoFonto(val elsendo: Elsendo) : Sonfonto
    @Serializable data class LokaElsendo(val elsendo: Elsendo, val dosieroVojo: String) : Sonfonto
}
```

Unuigas tri lud-reĝimojn (rekta radio, podkast, eksterreta dosiero) en unu tipo,
kiun la `LudiloRegilo` akceptas.

### `LudantoStato` / `LudantoInformo`

```kotlin
sealed interface LudantoStato {
    data object Haltita : LudantoStato
    data object Konektas : LudantoStato
    data object Ludas : LudantoStato
    data object Finita : LudantoStato
    data class Eraro(val mesagho: String) : LudantoStato
}

data class LudantoInformo(
    val stato: LudantoStato,
    val nunaFonto: Sonfonto? = null,
    val pozicioMs: Long = 0,
    val daŭroMs: Long = 0,
    val estasRekta: Boolean = false,
)
```

### `LudataElsendo` (`@Serializable`)

```kotlin
@Serializable
data class LudataElsendo(
    val elsendoId: String,
    val kanaloSlug: String,
    val pozicioMs: Long = 0,
    val daŭroMs: Long = 0,
    val finita: Boolean = false,
    val erara: Boolean = false,
    val lasteLudita: Long = 0,  // epoch ms
)
```

Spuras la ludstatuson de unuopa elsendo — por daŭra ludado kaj resumigo.
Uzata de `LudvicoRegilo`.

### Alikampo-modeloj

```kotlin
@Serializable data class Alarmo(val id: Int, val horo: Int, val minuto: Int, val ripeto: String, val kanaloSlug: String, val aktiva: Boolean)
@Serializable data class ElŝutStato(val elsendoId: String, val stato: ElŝutStatoEnum, val progres: Float, val dosieroVojo: String?)
@Serializable data class ElŝutitaElsendo(val elsendo: Elsendo, val dosieroVojo: String, val grandeco: Long, val daŭroMs: Long?)
```

## Deponej-interfacoj (`shared/domain/repository/`)

```kotlin
interface KanaloDeponejo {
    fun observiKanalojn(): StateFlow<List<Kanalo>>
    suspend fun getKanalojn(fortoRefresigi: Boolean = false): List<Kanalo>
    suspend fun getKanalo(slug: String): Kanalo?
}

interface ElsendoDeponejo {
    fun observiElsendojn(kanaloSlug: String): Flow<List<Elsendo>>
    suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean = false): List<Elsendo>
    suspend fun getElsendo(id: String): Elsendo?
    suspend fun serĉiElsendojn(teksto: String, limo: Int = 50): List<Elsendo>
    suspend fun ŝargiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean = false): List<Elsendo>
}
```

Personigo-interfacoj (`PersonigoDeponejoj.kt`):

```kotlin
interface PlejŝatatajDeponejo {
    fun observiPlejŝatatajn(): StateFlow<Set<String>>  // kanalo-slugs
    suspend fun baskuliPlejŝaton(kanaloSlug: String)
    suspend fun estasPlejŝatata(kanaloSlug: String): Boolean
}

interface LastAŭskultitajDeponejo {
    fun observiLastAŭskultitajn(): StateFlow<List<Elsendo>>
    suspend fun registri(elsendo: Elsendo)
    suspend fun getPozicio(elsendoId: String): Long?
}

interface LudatojDeponejo {
    fun observiLudatojn(): StateFlow<Map<String, LudataElsendo>>
    suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, daŭroMs: Long)
    suspend fun markiFinita(elsendoId: String, kanaloSlug: String)
    suspend fun markiErara(elsendoId: String, kanaloSlug: String)
    suspend fun malmarkiFinita(elsendoId: String, kanaloSlug: String)
    suspend fun getLudato(elsendoId: String): LudataElsendo?
    suspend fun estasFinita(elsendoId: String): Boolean
    suspend fun getPozicio(elsendoId: String): Long?
}

interface SerĉoDeponejo {
    suspend fun serĉi(teksto: String, limo: Int = 50): List<Elsendo>
}

interface AgordojDeponejo {
    val temo: StateFlow<String>
    val sciigoj: StateFlow<Boolean>
    val aŭtomataDaŭrigo: StateFlow<Boolean>
    val evoluo: StateFlow<Boolean>
    fun fiksiTemon(temo: String)
    fun fiksiSciigojn(ŝaltita: Boolean)
    fun fiksiAŭtomatanDaŭrigon(ŝaltita: Boolean)
    fun fiksiEvoluon(ŝaltita: Boolean)
}

interface ElŝutDeponejo {
    fun observiElŝutojn(): StateFlow<Map<String, ElŝutitaElsendo>>
    fun observiElŝutStaton(elsendoId: String): StateFlow<ElŝutStato>
    suspend fun elŝuti(elsendo: Elsendo)
    suspend fun haltigi(elsendoId: String)
    suspend fun forigi(elsendoId: String)
    suspend fun getLokaDosieroVojo(elsendoId: String): String?
    fun estaElŝutita(elsendoId: String): Boolean
}

interface AlarmoDeponejo {
    fun observiAlarmojn(): StateFlow<List<Alarmo>>
    suspend fun krei(alarmo: Alarmo)
    suspend fun ĝisdatigi(alarmo: Alarmo)
    suspend fun forigi(alarmoId: Int)
    suspend fun baskuliAktivon(alarmoId: Int)
}
```

> **Neniu uzkaz-tavolo.** La malnova plano havis `GetKanalojnUseCase`,
> `LudiFontonUseCase` ktp. Tiuj ne estis implementitaj. La logiko vivas rekte
> en la deponej-implementajhoj kaj ViewModel-oj. `LudvicoLogiko` estas la sola
> pura decidlogiko apartigita kiel klaso (por testeblo).

## Datentavolo (`shared/data/`)

La datentavolo estas pli simpla ol la originala plano. Ne ekzistas apartaj
`RadioApiServo`, `DeforaKanalDatumfonto`/`LokaKanalDatumfonto`, nek DTO+mapilo-tavolo.

### Kanal-deponejo

`KanaloDeponejoImpl` legas la JSONC-kanalkonfiguron (paktita risurco) per
`KanalAgordoLeganto` kaj liveras `StateFlow<List<Kanalo>>`.

### Elsendo-deponejo

`ElsendoDeponejoImpl` faras ĉion: Ktor-peto → RSS-parsado → diska kaŝmemoro.
Ne ekzistas aparta reto-interfaco — la `HttpClient` estas enketigata rekte.

```kotlin
class ElsendoDeponejoImpl(
    val httpKliento: HttpClient,
    val dosierKaŝo: DosierKaŝo,
) : ElsendoDeponejo { ... }
```

### Persisto

- **multiplatform-settings** por agordoj (temo, sciigoj, daŭrigo, evoluo)
- **JSON-dosieroj** (legitaj/skribitaj per Settings + kotlinx.serialization) por:
  alarmoj, ludpozicioj (`LudataElsendo`), elŝutitaj elsendoj, plejŝatataj
- **dosierkaŝmemoro** (`DosierKaŝo`, expect/actual) por servil-respondoj

## Toleremeco al putrantaj fontoj

Vidu regulojn A4 kaj A8 en `AGENTS.md`. Mallonge: ĉiu deponejo kaptas retajn
erarojn kaj liveras kaŝenitan datumon; unu kanal-eraro ne haltigas la aliajn;
ĉiu `catch` devas protokoli per `loge`/`logw`.

## Platform-ludila abstraktado (`LudiloRegilo`, expect/actual)

La sonludado estas platform-specifa kaj vivas kiel `expect`/`actual`:

```kotlin
// commonMain — interfaco
interface LudiloRegilo {
    val stato: StateFlow<LudantoInformo>
    suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long = 0)
    fun ludi()
    fun paŭzigi()
    fun halti()
    fun saltiAl(pozicioMs: Long)
    fun fiksiLaŭtecon(volumeno: Float)
}

// commonMain — expect-fabriko
expect fun kreuDefaŭltanLudiloRegilon(): LudiloRegilo
```

- **Android:** Media3 ExoPlayer + `MediaSessionService` → malfona ludado, ludada sciigo, ludbutonoj.
- **Desktop:** `DesktopLudiloRegilo` — mp3spi + SourceDataLine (pura Java, fluas MP3 super HTTP).
- **Web (wasmJs):** `WasmJsLudiloRegilo` — HTMLAudioElement.
- **iOS:** `NoOpLudiloRegilo` (estonte: AVPlayer).
- **Testoj/Preview:** `NoOpLudiloRegilo` — kun `simuluFinon()`/`simuluPozicion()` por testoj.

> **Eksponenta repro-logiko** estas implementita: `LudvicoRegilo` reprovas pasemajn
> erarojn ĝis 10 fojoj (1s → 2s → 4s … maks. 30s, de la sama pozicio; `ReprovoLogiko`).

> Detalojn pri ludvico, pozicio-spurado kaj aŭtoludo vidu en
> `NOTOJ.md` (sekcio "Sonludado kaj ludado — arkitekturaj resumoj").
