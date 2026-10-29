package dk.nordfalk.esperanto.domain.model

import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.Serializable

/**
 * Kanalo-modelo. Pura Kotlin, @Serializable, komuna trans ĉiuj platformoj.
 *
 * La kampoj kongruas kun `esperantoradio_kanaloj_v9.json`.
 */
@Serializable
data class Kanalo(
    val slug: String,                         // kodo — unika ŝlosilo
    val nomo: String,                          // vidiga nomo
    val priskribo: String? = null,              // mallonga priskribo (1-2 linioj) el la kanalkonfiguro
    val emblemoUrl: String? = null,
    val rektaElsendaSonoUrl: String? = null,   // tuja elsendfluo (nur Muzaiko)
    val podkastaRssUrl: String? = null,        // elsendojRssUrl
    val rektaElsendaPriskriboUrl: String? = null,
    val retejoUrl: String? = null,             // hejmpaĝoButono
    @JsonNames("retposhto") val retpoŝto: String? = null,
    val ignoruTitolon: Boolean = false,         // elsendojRssIgnoruTitolon
    val montruTitolojn: Boolean = true,
    val uzuWebViewPorElsendo: Boolean = false,
    /** Kanal-specifaj purigaj regex-modeloj por la priskribo (regulo P6.6) — datum-movita, el la JSONC. */
    val puriguModeloj: List<String> = emptyList(),
    /**
     * Kiom da sekundoj transsalti ĉe la komenco de ĉiu elsendo de tiu kanalo —
     * por ripetiĝantaj enkondukoj (FAROTA C59a; datum-movita, el la JSONC).
     * 0 = ne transsalti.
     */
    val transsaltuKomenceSekundojn: Int = 0,
    @JsonNames("defaltSxatata") val defaŭltŜatata: Boolean = false,
) {
    val estasRekta: Boolean get() = rektaElsendaSonoUrl != null
    val havasPodkastojn: Boolean get() = podkastaRssUrl != null
}

/**
 * Elsendo-modelo. La kontrakto kiun la parsilo devas plenigi.
 */
@Serializable
data class Elsendo(
    val id: String,                   // slug — vidu ID-konvenciojn
    val kanaloSlug: String,
    val kanaloNomo: String? = null,    // nomo de la kanalo (por sciigoj kaj UI)
    val titolo: String,
    val priskribo: String? = null,      // purigita plata teksto (por listoj, serĉo)
    val priskriboHtml: String? = null,  // purigita HTML kun etikedoj (por detala vido)
    val bildoUrl: String? = null,
    val dato: String,                 // yyyy-MM-dd
    @JsonNames("dauro") val daŭro: Long? = null,          // sekundoj
    val fluo: String,              // audio-URL (mp3) — la plej grava kampo
    @JsonNames("retpaghoUrl") val retpaĝoUrl: String? = null,
    val estasRekta: Boolean = false,
)

/**
 * Sonfonto — unuigas rekta ludado, podkast-ludado kaj eksterreta ludado.
 */
@Serializable
sealed interface Sonfonto {
    @Serializable data class RektaKanalo(val kanalo: Kanalo) : Sonfonto
    @Serializable data class ElsendoFonto(val elsendo: Elsendo) : Sonfonto
    @Serializable data class LokaElsendo(val elsendo: Elsendo, val dosieroVojo: String) : Sonfonto
}

/**
 * Ludanto-stato.
 */
sealed interface LudantoStato {
    data object Haltita : LudantoStato
    data object Konektas : LudantoStato
    data object Ludas : LudantoStato
    data object Finita : LudantoStato
    /**
     * @param reprovebla true se la eraro eble estas pasema (reto perdiĝis, tempolimo) kaj
     *   [dk.nordfalk.esperanto.domain.player.LudvicoRegilo] reprovu; false por daŭraj eraroj
     *   (HTTP 404, nesubtenata formato) — tiam tuj saltu al la sekva.
     */
    data class Eraro(val mesaĝo: String, val reprovebla: Boolean = true) : LudantoStato
}

data class LudantoInformo(
    val stato: LudantoStato,
    val nunaFonto: Sonfonto? = null,
    val pozicioMs: Long = 0,
    val daŭroMs: Long = 0,
    val estasRekta: Boolean = false,
)

/**
 * Spuras la ludstatuson de unuopa elsendo — por daŭra ludado kaj resumigo.
 *
 * @param elsendoId identigilo de la elsendo
 * @param kanaloSlug kiu kanalo
 * @param pozicioMs ĝis kiu pozicio (ms) la uzanto aŭskultis
 * @param daŭroMs totala daŭro (ms), aŭ 0 se nekonata
 * @param finita ĉu la elsendo estis tute ludita (naturfino)
 * @param lasteLudita tempmarko (epoch ms) de la lasta ludado
 */
@Serializable
data class LudataElsendo(
    val elsendoId: String,
    val kanaloSlug: String,
    val pozicioMs: Long = 0,
    @JsonNames("dauroMs") val daŭroMs: Long = 0,
    val finita: Boolean = false,
    val erara: Boolean = false,
    val lasteLudita: Long = 0,
)
