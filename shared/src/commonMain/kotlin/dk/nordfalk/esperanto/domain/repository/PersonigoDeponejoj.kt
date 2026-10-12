package dk.nordfalk.esperanto.domain.repository

import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlejŝatatajDeponejo {
    fun observiPlejŝatatajn(): StateFlow<Set<String>>  // kanalo-slugs
    suspend fun baskuliPlejŝaton(kanaloSlug: String)
    suspend fun estasPlejŝatata(kanaloSlug: String): Boolean
}

interface LastAuxskultitajDeponejo {
    fun observiLastAuxskultitajn(): StateFlow<List<Elsendo>>
    suspend fun registri(elsendo: Elsendo)
    suspend fun getPozicio(elsendoId: String): Long?
}

/**
 * Spuras ludstatuson de elsendoj — pozicio, ĉu finita, tempmarko.
 * Uzata de LudvicoRegilo por resumigo kaj aŭtomata sekva-ludado.
 */
interface LudatojDeponejo {
    fun observiLudatojn(): StateFlow<Map<String, LudataElsendo>>
    suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, dauroMs: Long)

    /**
     * Metas la tempmarkon lasteLudita al nun — sen tuŝi pozicion aŭ daŭron.
     * Vokata kiam la ludado vere ekis (ne jam ĉe klako sur "Ludi"),
     * por ke la elsendo aperu en "Lastatempe ludata" nur tiam.
     */
    suspend fun markiLudita(elsendoId: String, kanaloSlug: String)
    suspend fun markiFinita(elsendoId: String, kanaloSlug: String)
    suspend fun markiErara(elsendoId: String, kanaloSlug: String)
    suspend fun malmarkiFinita(elsendoId: String, kanaloSlug: String)
    suspend fun getLudato(elsendoId: String): LudataElsendo?
    suspend fun estasFinita(elsendoId: String): Boolean
    suspend fun getPozicio(elsendoId: String): Long?
}

interface SercxoDeponejo {
    suspend fun sercxi(teksto: String, limo: Int = 50): List<Elsendo>
}

interface AgordojDeponejo {
    val temo: StateFlow<String>
    val sciigoj: StateFlow<Boolean>
    val auxtomataDaurigo: StateFlow<Boolean>
    val evoluo: StateFlow<Boolean>

    /**
     * Problemoj kiujn la uzanto kaŝis per la 'x'-butono de la avertokarto sur
     * la ĉefekrano. La avertoj ĉiam restas videblaj en Agordoj → Diagnozo.
     */
    val kasxitajAvertoj: StateFlow<Set<DiagnozoTipo>>

    fun fiksiTemon(temo: String)
    fun fiksiSciigojn(sxaltita: Boolean)
    fun fiksiAuxtomatanDaurigon(sxaltita: Boolean)
    fun fiksiEvoluon(sxaltita: Boolean)

    /** Kaŝas la donitajn problemo-tipojn (anstataŭigas la tutan aron). */
    fun kasxuAvertojn(tipoj: Set<DiagnozoTipo>)

    /** Forgesas la kaŝojn de problemo-tipoj kiuj ne plu ekzistas */
    fun puriguKasxitajnAvertojn(ekzistantaj: Set<DiagnozoTipo>)
}

/**
 * Elŝut-deponejo. Platform-specifaj implementoj:
 * - Android/Desktop: Ktor → dosiero
 * - wasmJs/iOS: NoOp (ne haveblas)
 */
interface ElshutDeponejo {
    fun observiElshutojn(): StateFlow<Map<String, ElshutitaElsendo>>  // elsendoId → stato
    fun observiElshutStaton(elsendoId: String): StateFlow<ElshutStato>
    suspend fun elshuti(elsendo: Elsendo)
    suspend fun haltigi(elsendoId: String)
    suspend fun forigi(elsendoId: String)
    suspend fun getLokaDosieroVojo(elsendoId: String): String?
    fun estasElshutita(elsendoId: String): Boolean
}

/**
 * Vekhorloĝo-deponejo. Platform-specifaj implementoj:
 * - Android: AlarmManager + BroadcastReceiver (estonte)
 * - Desktop/wasmJs/iOS: NoOp (nur UI, ne planas vere)
 */
interface AlarmoDeponejo {
    fun observiAlarmojn(): StateFlow<List<Alarmo>>
    suspend fun krei(alarmo: Alarmo)
    suspend fun ghisdatigi(alarmo: Alarmo)
    suspend fun forigi(alarmoId: Int)
    suspend fun baskuliAktivon(alarmoId: Int)
}
