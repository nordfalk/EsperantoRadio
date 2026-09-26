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
    /**
     * Platformo sur kiu la kanalo estas videbla ("android", "desktop", "web", "ios").
     * null = videbla ĉie. Uzata por kanaloj kies fluoj funkcias nur sur unu
     * platformo (ekz. CRI: HLS kiun nur ExoPlayer subtenas).
     */
    val videblaNurSur: String? = null,
    /**
     * CRI-stilaj sekci-URL-oj (regulo 6.8): la elsendoj venas per POST al la
     * CRI-API, ne el RSS-fluo. Ne-null aktivigas la CRI-parsilon.
     */
    val elsendojApiSekcioj: List<String>? = null,
) {
    val estasRekta: Boolean get() = rektaElsendaSonoUrl != null
    val havasPodkastojn: Boolean
        get() = podkastaRssUrl != null || !elsendojApiSekcioj.isNullOrEmpty()
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
    val priskribo: String? = null,      // purigita plata teksto (por listoj, serĉo)
    val priskriboHtml: String? = null,  // purigita HTML kun etikedoj (por detala vido)
    val bildoUrl: String? = null,
    val dato: String,                 // yyyy-MM-dd
    val dauro: Long? = null,          // sekundoj
    val fluo: String,              // audio-URL (mp3) — la plej grava kampo
    val retpaghoUrl: String? = null,
    val estasRekta: Boolean = false,
) {
    /**
     * Ĉu la fluo estas video (HLS-ludlisto aŭ MP4) anstataŭ pura sono —
     * ekz. CRI-elsendoj. Nur ExoPlayer (Android) povas ludi ĝin; elŝuto ne
     * eblas (ludlisto, ne dosiero), sed Android povas ankaŭ montri la bildon.
     */
    val estasVideaFluo: Boolean get() = fluo.endsWith(".m3u8") || fluo.endsWith(".mp4")
}

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
    data class Eraro(val mesagho: String) : LudantoStato
}

data class LudantoInformo(
    val stato: LudantoStato,
    val nunaFonto: Sonfonto? = null,
    val pozicioMs: Long = 0,
    val dauroMs: Long = 0,
    val estasRekta: Boolean = false,
)

/**
 * Spuras la ludstatuson de unuopa elsendo — por daŭra ludado kaj resumigo.
 *
 * @param elsendoId identigilo de la elsendo
 * @param kanaloSlug kiu kanalo
 * @param pozicioMs ĝis kiu pozicio (ms) la uzanto aŭskultis
 * @param dauroMs totala dauro (ms), aŭ 0 se nekonata
 * @param finita ĉu la elsendo estis tute ludita (naturfino)
 * @param lasteLudita tempmarko (epoch ms) de la lasta ludado
 */
@Serializable
data class LudataElsendo(
    val elsendoId: String,
    val kanaloSlug: String,
    val pozicioMs: Long = 0,
    val dauroMs: Long = 0,
    val finita: Boolean = false,
    val erara: Boolean = false,
    val lasteLudita: Long = 0,
)
