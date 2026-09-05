package dk.nordfalk.esperanto.domain.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.Alarmo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlejsatatajDeponejo {
    fun observiPlejsatatajn(): StateFlow<Set<String>>  // kanal-slugs
    suspend fun baskuliPlejsaton(kanalSlug: String)
    suspend fun estasPlejsatata(kanalSlug: String): Boolean
}

interface LastAuxskultitajDeponejo {
    fun observiLastAuxskultitajn(): StateFlow<List<Elsendo>>
    suspend fun registri(elsendo: Elsendo)
    suspend fun getPozicio(elsendoId: String): Long?
}

interface SercxoDeponejo {
    suspend fun sercxi(taxto: String, limo: Int = 50): List<Elsendo>
}

interface AgordojDeponejo {
    val lingvo: StateFlow<String>
    val nurWifi: StateFlow<Boolean>
    fun fiksiLingvon(lingvo: String)
    fun fiksiNurWifi(nurWifi: Boolean)
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
