package dk.nordfalk.esperanto.domain.repository

import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.Elsendo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface KanalDeponejo {
    fun observiKanalojn(): StateFlow<List<Kanal>>
    suspend fun getKanalojn(fortoRefresigi: Boolean = false): List<Kanal>
    suspend fun getKanal(slug: String): Kanal?
}

interface ElsendoDeponejo {
    fun observiElsendojn(kanalSlug: String): Flow<List<Elsendo>>
    suspend fun getElsendojn(kanalSlug: String, fortoRefresigi: Boolean = false): List<Elsendo>
    suspend fun getElsendo(id: String): Elsendo?
    suspend fun sercxiElsendojn(taxto: String, limo: Int = 50): List<Elsendo>
}
