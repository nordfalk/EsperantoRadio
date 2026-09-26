package dk.nordfalk.esperanto.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

/**
 * Vekhorloĝo (alarmo).
 *
 * @param id unika identigilo
 * @param horo horo (0-23)
 * @param minuto minuto (0-59)
 * @param ripeto bitmasko: 0x01=lundo ... 0x40=dimanĉo, 0x7f=cxiutage, 0x00=unufoje
 * @param kanaloSlug kanalo-slugo por ludi
 * @param aktiva cxu la alarmo estas sxaltita
 * @param etikedo vidiga nomo (opcia)
 */
@Serializable
data class Alarmo(
    val id: Int,
    val horo: Int,
    val minuto: Int,
    val ripeto: Int = 0,
    val kanaloSlug: String,
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

/**
 * Kalkulas la sekvan lokan tempon (post [nun]) kiam la alarmo ekigu.
 * - ripeto == 0 (unufoje): hodiaŭ se la tempo ankoraŭ ne pasis, alie morgaŭ.
 * - ripeto != 0: la unua tago (hodiaŭ inkluzive) kies bito estas en la bitmasko
 *   (0x01=lundo ... 0x40=dimanĉo) kaj kies tempo estas post [nun].
 */
fun Alarmo.sekvaEkigo(nun: LocalDateTime): LocalDateTime {
    val tempo = LocalTime(horo, minuto)
    for (i in 0..7) {
        val tago = nun.date.plus(i, DateTimeUnit.DAY)
        val kandidato = LocalDateTime(tago, tempo)
        if (kandidato <= nun) continue
        val bito = 1 shl (tago.dayOfWeek.isoDayNumber - 1)
        if (ripeto == 0 || ripeto and bito != 0) return kandidato
    }
    // Ne atingebla por valida bitmasko (0x01..0x7f); sekurece: morgaŭ
    return LocalDateTime(nun.date.plus(1, DateTimeUnit.DAY), tempo)
}
