package dk.nordfalk.esperanto.navigation

import androidx.navigation3.runtime.NavKey
import dk.nordfalk.esperanto.domain.model.Kanalo

/**
 * La paĝotitolo (langeta titolo) por la nuna vojo — sur la retumilo ĝi iĝas
 * `document.title`, do ĝi estu homlegebla anstataŭ teknika.
 * La beta-sufikso spegulas la retversian supran breton (HejmoEkrano).
 */
fun vojaTitolo(vojo: NavKey?, kanaloj: List<Kanalo>): String {
    val bazo = if (vojo == null) "EsperantoRadio beta" else "EsperantoRadio"
    fun kanalNomo(slug: String) = kanaloj.firstOrNull { it.slug == slug }?.nomo ?: slug
    return when (vojo) {
        null, is Vojo.Hejmo -> bazo
        is Vojo.Kanalaro -> "Ĉiuj kanaloj · EsperantoRadio"
        is Vojo.Plejŝatataj -> "Plej ŝatataj · EsperantoRadio"
        is Vojo.Serĉo -> "Serĉo · EsperantoRadio"
        is Vojo.Elŝutoj -> "Elŝutoj · EsperantoRadio"
        is Vojo.Ludvico -> "Ludvico · EsperantoRadio"
        is Vojo.Alarmoj -> "Alarmoj · EsperantoRadio"
        is Vojo.Agordoj -> "Agordoj · EsperantoRadio"
        is Vojo.Diagnozo -> "Diagnozo · EsperantoRadio"
        is Vojo.KanaloDetalo -> "${kanalNomo(vojo.slug)} · EsperantoRadio"
        is Vojo.ElsendoDetalo -> "${kanalNomo(vojo.kanaloSlug)} · EsperantoRadio"
        else -> bazo
    }
}

/** Kiom da historiajn enirojn la retumilo devas iri reen (0 = nenio). */
fun historiaReenNiveloj(nunaProfundo: Int, novaProfundo: Int): Int =
    (nunaProfundo - novaProfundo).coerceAtLeast(0)

/** Ĉu puŝi novan historian eniron (la nav-stako profundiĝis)? */
fun estasHistoriaPuŝo(nunaProfundo: Int, novaProfundo: Int): Boolean = novaProfundo > nunaProfundo

/**
 * Ĉu la nav-reenigo bezonas `history.back()`? NE, se la retumilo MEM jam iris reen
 * (popstate → onReen): tiam la historia profundo jam egalas la novan nav-profundon,
 * kaj dua `back()` forlasus la paĝon. Nur la enpropraj reen-agoj (reen-sago,
 * tirkesta restartigo) retrompu la historian.
 */
fun estuHistoryBack(
    memoritaNavProfundo: Int,
    novaNavProfundo: Int,
    historiaProfundoNun: Int,
): Boolean = novaNavProfundo < memoritaNavProfundo && historiaProfundoNun > novaNavProfundo

/**
 * Kiom da vojojn la APO devas popi kiam popstate-evento alvenas (la retumilo
 * navigis — eble plurajn nivelojn samtempe, per la historia menuo de la retumilo:
 * longa klako sur la reen-butono aŭ la historia flanka breto).
 * 0 = ne navigu: trairo ANTAŬEN, aŭ nia propra sinkronigo (la memorita
 * profundo jam ĝisdatiĝis antaŭ la evento alvenis).
 */
fun popstateReenigoj(historiaProfundo: Int, memoritaNavProfundo: Int): Int =
    (memoritaNavProfundo - historiaProfundo).coerceAtLeast(0)

/**
 * Integrigo inter la nav-stako kaj la RETUMILO (Historio-API, `document.title`,
 * ŝarĝekrano). Nur la wasmJs-aktualigo faras ion; sur Android, Desktop kaj iOS
 * ĉiuj metodoj estas no-op.
 *
 * Kiel ĝi funkcias (unu historia eniro po nav-nivelo):
 * je starto la nuna eniro ricevas staton `{d:1}`; ĉiu puŝo faras `history.pushState`;
 * ĉiu reenigo (el la propra reen-sago) faras `history.back()`; `popstate`
 * kun PLI MALALTA `d` ol la memorita profundo signifas retumilan reen-butonon →
 * voku la saman reen-logikon. Ĉe la radika ekrano [Hejmo] neniuj eniroj restas —
 * due retumila reen forlasas la paĝon, kiel „eliri el la apo" sur Android.
 */
internal expect object RetumilaNavigado {
    /**
     * Unufoja starto: je la retumilo anstataŭigu la historian staton per {d:1},
     * registru la popstate-aŭskultilon kaj kaŝu la ŝarĝekranon.
     * [onReen] vokiĝu kiam la retumilo mem iras reen (reen-butono aŭ gestoj).
     */
    fun startu(onReen: () -> Unit)

    /**
     * La nav-stako ŝanĝiĝis (puŝo, reenigo, langet-ŝanĝo) — sinkronigu la
     * historian de la retumilo kaj ĝisdatigu `document.title`.
     */
    fun stakŜanĝiĝis(novaProfundo: Int, titolo: String?)
}
