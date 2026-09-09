package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Portas elsendon de ekstera intento (sciigo) al la Compose-tavolo por navigado.
 *
 * Kiam la uzanto klakas sur sciigon, MainActivity metas la elsendon ĉi tie.
 * App.kt observas ĝin kaj puŝas Vojo.ElsendoDetalo kiam ĝi ŝanĝiĝas.
 */
object PendingElsendoNavigacio {
    private val _elsendo = MutableStateFlow<Elsendo?>(null)
    val elsendo: StateFlow<Elsendo?> = _elsendo.asStateFlow()

    fun setu(elsendo: Elsendo?) {
        _elsendo.value = elsendo
    }
}
