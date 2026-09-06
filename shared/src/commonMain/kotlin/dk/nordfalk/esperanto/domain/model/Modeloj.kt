package dk.nordfalk.esperanto.domain.model

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
    val emblemoUrl: String? = null,
    val rektaElsendaSonoUrl: String? = null,   // livestream (nur Muzaiko)
    val podkastaRssUrl: String? = null,        // elsendojRssUrl
    val rektaElsendaPriskriboUrl: String? = null,
    val retejoUrl: String? = null,             // hejmpaĝoButono
    val retposhto: String? = null,
    val ignoruTitolon: Boolean = false,         // elsendojRssIgnoruTitolon
    val montruTitolojn: Boolean = true,
    val uzuWebViewPorElsendo: Boolean = false,
) {
    val estasRekta: Boolean get() = rektaElsendaSonoUrl != null
    val havasPodkastojn: Boolean get() = podkastaRssUrl != null
}

/**
 * Elsendo-modelo. La kontrakto kiun la parsilo devas plenigi.
 */
@Serializable
data class Elsendo(
    val id: String,                   // slug — vidu id-konvenciojn
    val kanaloSlug: String,
    val kanaloNomo: String? = null,    // nomo de la kanalo (por sciigoj kaj UI)
    val titolo: String,
    val priskribo: String? = null,    // purigita HTML/teksto
    val bildoUrl: String? = null,
    val dato: String,                 // yyyy-MM-dd
    val dauro: Long? = null,          // sekundoj
    val fluo: String,              // audio-URL (mp3) — la plej grava kampo
    val retpaghoUrl: String? = null,
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
    data class Eraro(val mesagho: String) : LudantoStato
}

data class LudantoInformo(
    val stato: LudantoStato,
    val nunaFonto: Sonfonto? = null,
    val pozicioMs: Long = 0,
    val dauroMs: Long = 0,
    val estasRekta: Boolean = false,
)
