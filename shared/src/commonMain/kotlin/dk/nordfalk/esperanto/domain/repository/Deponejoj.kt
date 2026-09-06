package dk.nordfalk.esperanto.domain.repository

import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Elsendo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface KanaloDeponejo {
    fun observiKanalojn(): StateFlow<List<Kanalo>>
    suspend fun getKanalojn(fortoRefresigi: Boolean = false): List<Kanalo>
    suspend fun getKanalo(slug: String): Kanalo?
}

interface ElsendoDeponejo {
    fun observiElsendojn(kanaloSlug: String): Flow<List<Elsendo>>
    suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean = false): List<Elsendo>
    suspend fun getElsendo(id: String): Elsendo?
    suspend fun sercxiElsendojn(teksto: String, limo: Int = 50): List<Elsendo>

    /**
     * Elŝutas kaj parsas la RSS-fluon por specifa kanalo.
     * Tolerema: eraro → liveri kaŝenitan datumon, ne ĵeti.
     */
    suspend fun sxargxiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean = false): List<Elsendo>
}
