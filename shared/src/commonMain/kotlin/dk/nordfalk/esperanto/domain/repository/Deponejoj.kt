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
     * C60: serĉo nur en la elsendtitoloj resp. nur en la priskriboj — por la
     * grupigita serĉ-rezulto ([SerĉoRezulto]). Defaŭlte: titolo = serĉiElsendojn
     * (tio trovas ambaŭ kampojn — sufiĉas por falsaj deponejoj en testoj),
     * priskribo = malplena (la inversa aprioro ne kongruas kun la titolo-grupo).
     */
    suspend fun serĉiEnTitoloj(teksto: String, limo: Int = 50): List<Elsendo> = serĉiElsendojn(teksto, limo)
    suspend fun serĉiEnPriskriboj(teksto: String, limo: Int = 50): List<Elsendo> = emptyList()

    /**
     * Elŝutas kaj parsas la RSS-fluon por specifa kanalo.
     * Tolerema: eraro → liveri kaŝenitan datumon, ne ĵeti.
     */
    suspend fun ŝargiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean = false): List<Elsendo>
}
