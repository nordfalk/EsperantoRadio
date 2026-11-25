package dk.nordfalk.esperanto.navigation

import androidx.navigation3.runtime.NavKey
import dk.nordfalk.esperanto.domain.model.Kanalo

/**
 * La paĝotitolo (langeta titolo) por la nuna vojo — sur la retumilo ĝi iĝas
 * `document.title`, do ĝi estu homlegebla anstataŭ teknika. La beta-averto de
 * la retversio vivas en la videbla supra breto kaj en index.html, ne en la titolo.
 */
fun vojaTitolo(vojo: NavKey?, kanaloj: List<Kanalo>): String {
    val bazo = "EsperantoRadio"
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

/**
 * Kiom da historiajn nivelojn la retumilo devas iri reen kiam la nav-stako
 * malpliiĝis pro ENPROPA reen-ago (reen-sago, tirkesta restartigo) — aŭ NULL,
 * se la historio devas resti netuŝita: la retumilo MEM jam iris reen
 * (popstate direktis la nav-popon, la profundoj kongruas) kaj dua `back()`
 * forlasus la paĝon.
 */
fun historiaReeniro(
    memoritaNavProfundo: Int,
    novaNavProfundo: Int,
    historiaProfundoNun: Int,
): Int? =
    if (novaNavProfundo < memoritaNavProfundo && historiaProfundoNun > novaNavProfundo) {
        memoritaNavProfundo - novaNavProfundo
    } else {
        null
    }

/**
 * Kiel traktu popstate-eventon (la retumilo navigis — eventuale plurajn
 * nivelojn samtempe, per la historia menuo de la retumilo).
 */
sealed interface PopstateTraktado {
    /**
     * Neniu ago: antaŭen-trairo, aŭ nia propra sinkronigo (`history.back()` el
     * la stak-efekto — la memorita profundo jam ĝisdatiĝis ĝis la evento alvenis).
     */
    data object Nenio : PopstateTraktado

    /**
     * UNU-nivela reen: la internaj traktiloj de la apo rajtas unue konsumi ĝin
     * (kolapso de la elfaldita mini-ludilbreto — paralelo de la `NavigationBackHandler`-konduto
     * sur Android); nur se neniu konsumas, la nav-stako popiĝas.
     */
    data object Ununivela : PopstateTraktado

    /**
     * Plurnivela salto (historia menuo): popu [nombro] da vojoj; la internaj
     * traktiloj ricevas nur la eblon konsumi sen pop-efiko (kolapso de la breto
     * ne ŝanĝas la profundecon).
     */
    data class Plurnivela(val nombro: Int) : PopstateTraktado
}

fun popstateTraktado(memoritaNavProfundo: Int, historiaProfundo: Int): PopstateTraktado = when {
    historiaProfundo >= memoritaNavProfundo -> PopstateTraktado.Nenio
    historiaProfundo == memoritaNavProfundo - 1 -> PopstateTraktado.Ununivela
    else -> PopstateTraktado.Plurnivela(memoritaNavProfundo - historiaProfundo)
}



/**
 * Integrigo inter la nav-stako kaj la RETUMILO (Historio-API, `document.title`,
 * ŝarĝekrano). Nur la wasmJs-aktualigo faras ion; sur Android, Desktop kaj iOS
 * ĉiuj metodoj estas no-op.
 *
 * Kiel ĝi funkcias (unu historia eniro po nav-nivelo):
 * je starto la nuna eniro ricevas staton `{d:1}`; ĉiu puŝo faras `history.pushState`;
 * ĉiu enpropra reenigo (reen-sago) faras `history.back()`; `popstate` kun PLI MALALTA
 * `d` ol la memorita profundo signifas retumilan reen-butonon: UNU-nivele la internaj
 * traktiloj rajtas unue konsumi ĝin (kolapso de la miniludilbreto; la historia eniro
 * tiam REARMIĜAS per `pushState`), alie la nav-stako popiĝas samkiel sur Android.
 * Ĉe la radika ekrano [Hejmo] neniuj eniroj restas — due retumila reen forlasas
 * la paĝon, kiel „eliri el la apo“ sur Android.
 */
internal expect object RetumilaNavigado {
    /**
     * Unufoja starto: je la retumilo anstataŭigu la historian staton per {d:1},
     * registru la popstate-aŭskultilon kaj kaŝu la ŝarĝekranon.
     *
     * @param onReen popu UNU vojon el la nav-stako (unu retumila reen-paŝo).
     * @param onReenProponata proponu la reen-agon al la internaj traktiloj de la
     *   apo SEN tuŝi la nav-stakon (kolapso de la elfaldita mini-ludilbreto — la
     *   reta paralelo de la NavigationBackHandler-konduto sur Android). Revenu ĉu
     *   konsumita; se jes, la retumila historia eniro rearmiĝas (pushState) kaj
     *   la stako restas netuŝita.
     */
    fun startu(onReen: () -> Unit, onReenProponata: () -> Boolean)

    /**
     * La nav-stako ŝanĝiĝis (puŝo, reenigo, langet-ŝanĝo) — sinkronigu la
     * historian de la retumilo kaj ĝisdatigu `document.title`.
     */
    fun stakŜanĝiĝis(novaProfundo: Int, titolo: String?)
}
