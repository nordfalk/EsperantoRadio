package dk.nordfalk.esperanto.domain.model

import kotlinx.serialization.Serializable

/**
 * Vekhorloĝo (alarmo).
 *
 * @param id unika identigilo
 * @param horo horo (0-23)
 * @param minuto minuto (0-59)
 * @param ripeto bitmasko: 0x01=lundo ... 0x40=dimanĉo, 0x7f=cxiutage, 0x00=unufoje
 * @param kanalSlug kanal-slugo por ludi
 * @param aktiva cxu la alarmo estas sxaltita
 * @param etikedo vidiga nomo (opcia)
 */
@Serializable
data class Alarmo(
    val id: Int,
    val horo: Int,
    val minuto: Int,
    val ripeto: Int = 0,
    val kanalSlug: String,
    val aktiva: Boolean = true,
    val etikedo: String? = null,
) {
    val ripetoTeksto: String get() = when {
        ripeto == 0 -> "Unufoje"
        ripeto == 0x7f -> "Cxiutage"
        else -> buildList {
            if (ripeto and 0x01 != 0) add("Lu")
            if (ripeto and 0x02 != 0) add("Ma")
            if (ripeto and 0x04 != 0) add("Me")
            if (ripeto and 0x08 != 0) add("Ja")
            if (ripeto and 0x10 != 0) add("Ve")
            if (ripeto and 0x20 != 0) add("Sa")
            if (ripeto and 0x40 != 0) add("Di")
        }.joinToString(" ")
    }

    val tempoTeksto: String get() = "${horo.toString().padStart(2, '0')}:${minuto.toString().padStart(2, '0')}"
}
