package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ElsendoDeponejo-implentaĵo. Elŝutas RSS-fluojn per Ktor, parsas per RssParsilo,
 * kaŝenas en memoro (StateFlow). Tolerema: eraro → liveri kaŝenitan datumon.
 */
class ElsendoDeponejoImpl(
    private val httpKliento: HttpClient,
    private val parsilo: RssParsilo = RssParsilo(),
) : ElsendoDeponejo {

    private val kaŝmemoro = mutableMapOf<String, List<Elsendo>>()
    private val fluoj = mutableMapOf<String, MutableStateFlow<List<Elsendo>>>()

    override fun observiElsendojn(kanalSlug: String): StateFlow<List<Elsendo>> {
        return fluoj.getOrPut(kanalSlug) { MutableStateFlow(kaŝmemoro[kanalSlug] ?: emptyList()) }.asStateFlow()
    }

    override suspend fun getElsendojn(kanalSlug: String, fortoRefresigi: Boolean): List<Elsendo> {
        val kaŝenitaj = kaŝmemoro[kanalSlug]
        if (kaŝenitaj != null && !fortoRefresigi) {
            return kaŝenitaj
        }
        // Ni bezonas la kanal-URL por elŝuti. La kanal estas provizita ekstere.
        // Tiu metodon estos vokita kun la kanal-URL jam konata.
        return kaŝenitaj ?: emptyList()
    }

    /**
     * Elŝutas kaj parsas la RSS-fluon por specifa kanal.
     * Tolerema: eraro → liveri kaŝenitan datumon, ne ĵeti.
     */
    suspend fun sxargxiElsendojn(kanal: Kanal, fortoRefresigi: Boolean = false): List<Elsendo> {
        val url = kanal.podkastaRssUrl ?: return kaŝmemoro[kanal.slug] ?: emptyList()
        val kaŝenitaj = kaŝmemoro[kanal.slug]

        if (kaŝenitaj != null && !fortoRefresigi) {
            return kaŝenitaj
        }

        return try {
            val respondo = httpKliento.get(url).bodyAsText()
            val elsendoj = parsilo.parsRss(respondo, kanal) { urlD ->
                httpKliento.get(urlD).bodyAsText()
            }
            kaŝmemoro[kanal.slug] = elsendoj
            fluoj.getOrPut(kanal.slug) { MutableStateFlow(emptyList()) }.value = elsendoj
            elsendoj
        } catch (e: Exception) {
            // Toleremeco: liveri kaŝenitan datumon se haveblan
            kaŝenitaj ?: emptyList()
        }
    }

    override suspend fun getElsendo(id: String): Elsendo? {
        for ((_, elsendoj) in kaŝmemoro) {
            val e = elsendoj.find { it.id == id }
            if (e != null) return e
        }
        return null
    }

    override suspend fun sercxiElsendojn(taxto: String, limo: Int): List<Elsendo> {
        val ĉiuj = kaŝmemoro.values.flatten()
        return ĉiuj.filter {
            it.titolo.contains(taxto, ignoreCase = true) ||
            (it.priskribo?.contains(taxto, ignoreCase = true) ?: false)
        }.take(limo)
    }
}
