package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import dk.nordfalk.esperanto.domain.model.Elsendo

/**
 * Stato de la plenekrana videa vidigo.
 *
 * [ElsendoEkrano] malfermas ĝin kiam la uzanto klakas la filmeton; la
 * kovrilo en [dk.nordfalk.esperanto.App] tiam kovras la tutan ekranon —
 * ankaŭ la mini-ludilon, la navigan breton kaj la supran breton, ĉar tiuj
 * vivas ekster la ElsendoEkrano en la ap-nivela aranĝo.
 *
 * La sama ludilo daŭras (neniu reŝargo): la VideoVido simple ligiĝas al la
 * ludilo de la ludado-servo per [VideoLudiloPonto].
 */
object PlenekranaVido {
    var elsendo: Elsendo? by mutableStateOf(null)
        private set

    fun malfermu(elsendo: Elsendo) {
        this.elsendo = elsendo
    }

    fun fermu() {
        elsendo = null
    }
}
