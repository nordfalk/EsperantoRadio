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
    suspend fun serĉiElsendojn(teksto: String, limo: Int = 50): List<Elsendo>

    /**
     * C60: serĉo grupigita — `first` = titolo-trovoj, `second` = nur-priskribo-trovoj
     * (priskribo-trovo, kies titolo ALSO kongruas, aperas nur en `first`).
     * Defaŭlte: titolo = serĉiElsendojn (trovos ambaŭ kampojn — sufiĉas por falsaj
     * deponejoj en testoj), priskribo = malplena.
     */
    suspend fun serĉiElsendojnGrupigite(teksto: String, limo: Int = 50): Pair<List<Elsendo>, List<Elsendo>> =
        serĉiElsendojn(teksto, limo) to emptyList()

    /**
     * Elŝutas kaj parsas la RSS-fluon por specifa kanalo.
     * Tolerema: eraro → liveri kaŝenitan datumon, ne ĵeti.
     */
    suspend fun ŝargiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean = false): List<Elsendo>
}
