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
 * @param ripeto bitmasko: 0x01=lundo ... 0x40=dimanĉo, 0x7f=ĉiutage, 0x00=unufoje
 * @param kanaloSlug kanalo-slugo por ludi
 * @param aktiva ĉu la alarmo estas ŝaltita
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
    /**
     * Teksto priskribanta la ripeton, generata el la ripet-masko (ĉiam kongrua
     * kun la agordo — neniu libera teksto el la konfiguro).
     */
    val ripetoTeksto: String get() = when (ripeto) {
        0x00 -> "Unufoje"
        0x7f -> "Ĉiutage"
        0x1f -> "Labortage"
        0x60 -> "Semajnfine"
        0x01 -> "Lunde"
        0x02 -> "Marde"
        0x04 -> "Merkrede"
        0x08 -> "Ĵaŭde"
        0x10 -> "Vendrede"
        0x20 -> "Sabate"
        0x40 -> "Dimanĉe"
        else -> {
            val nomoj = tagNomoj(ripeto)
            nomoj.dropLast(1).joinToString(", ") + " kaj " + nomoj.last()
        }
    }

    /** Plenaj nomoj de la elektitaj tagoj, laŭvice (Lu → Di). */
    private fun tagNomoj(ripeto: Int): List<String> = buildList {
        if (ripeto and 0x01 != 0) add("Lundo")
        if (ripeto and 0x02 != 0) add("Mardo")
        if (ripeto and 0x04 != 0) add("Merkredo")
        if (ripeto and 0x08 != 0) add("Ĵaŭdo")
        if (ripeto and 0x10 != 0) add("Vendredo")
        if (ripeto and 0x20 != 0) add("Sabato")
        if (ripeto and 0x40 != 0) add("Dimanĉo")
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
    // (PR #95) koercu — korupta agordo (ekz. horo=99 el mana JSON-redakto) devas
    // ne kraŝi la skedadon (LocalTime ĵetas IllegalArgumentException). La validigo
    // de NOVAJ alarmoj okazas en parsuSugestojnPorAlarmoj kaj AlarmoEkrano; tio ĉi
    // estas la kraŝo-defendo por jam persistintaj valoroj.
    val tempo = LocalTime(horo.coerceIn(0, 23), minuto.coerceIn(0, 59))
    val sekuraRipeto = ripeto and 0x7f
    for (i in 0..7) {
        val tago = nun.date.plus(i, DateTimeUnit.DAY)
        val kandidato = LocalDateTime(tago, tempo)
        if (kandidato <= nun) continue
        val bito = 1 shl (tago.dayOfWeek.isoDayNumber - 1)
        if (sekuraRipeto == 0 || sekuraRipeto and bito != 0) return kandidato
    }
    // Ne atingebla por valida bitmasko (0x01..0x7f); sekurece: morgaŭ
    return LocalDateTime(nun.date.plus(1, DateTimeUnit.DAY), tempo)
}
