package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto

/**
 * Regilo de la GLUIĜEMA sekvanta enhavo de la miniludilbreto (regulo 1).
 *
 * Kiam nenio ludas, la breto montras la LASTE MONSTRITAN enhavon: la lastan
 * vidatan detalan celon (kanalo/elsendo — sekvante ankaŭ svipojn) aŭ la lastan
 * ludatan elsendon/kanalon. Nur du eventoj ŝanĝas ĝin:
 *
 * - **vidado** de detala celo (eniro sur la ekrano aŭ svipo) — [vidisElsendon] /
 *   [vidisKanalon];
 * - **ludado** — [ludisFonton] (la ludata elsendo/kanalo iĝas la sekvanta enhavo,
 *   do ĝi restas ankaŭ post haltigo).
 *
 * Nura navigado (reen-butono, langetoj, tirkesto) NE ŝanĝas la enhavon — tio
 * estas la esenco de regulo 1: la breto ne saltas inter elsendoj je navigado.
 *
 * La [semo] liveras la elsendon montrotan post restarto (el la persistaj
 * ludatoj), kiam nenio estis vidata/ludata dum ĉi tiu sesio.
 */
class SekvantaEnhavoRegilo(
    private val semo: () -> Elsendo?,
) {
    /** La gluiĝema elsendo-montrinda (null = neniu elsendo estas la sekvanta enhavo). */
    var sekvantaElsendo: Elsendo? by mutableStateOf(null)
        private set

    /** La gluiĝema kanalo-montrinda (null = neniu kanalo estas la sekvanta enhavo). */
    var sekvantaKanalo: Kanalo? by mutableStateOf(null)
        private set

    fun vidisElsendon(elsendo: Elsendo) {
        sekvantaElsendo = elsendo
        sekvantaKanalo = null
    }

    fun vidisKanalon(kanalo: Kanalo) {
        sekvantaKanalo = kanalo
        sekvantaElsendo = null
    }

    /**
     * Ludo-evento: la ludata elsendo/kanalo iĝas la sekvanta enhavo — rekta
     * kanalo (sen elsendo) iĝas kanala sekvanta enhavo. `null` (nenio ludas)
     * ŝanĝas nenion.
     */
    fun ludisFonton(fonto: Sonfonto?) {
        when (fonto) {
            is Sonfonto.ElsendoFonto -> vidisElsendon(fonto.elsendo)
            is Sonfonto.LokaElsendo -> vidisElsendon(fonto.elsendo)
            is Sonfonto.RektaKanalo -> vidisKanalon(fonto.kanalo)
            null -> {}
        }
    }

    /**
     * La elsendo montrinda kiam neniu kanalo estas la sekvanta enhavo:
     * la gluiĝema aŭ — post restarto — la semo.
     */
    fun elsendo(): Elsendo? = sekvantaElsendo ?: semo()
}
