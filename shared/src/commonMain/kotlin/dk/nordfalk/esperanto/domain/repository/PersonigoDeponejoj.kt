package dk.nordfalk.esperanto.domain.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
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
