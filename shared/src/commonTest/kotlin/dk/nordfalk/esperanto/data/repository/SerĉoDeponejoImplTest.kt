package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * C60: la serĉ-rezulto estas grupigita laŭ graveco — unue kanaltitoloj, poste
 * kanalpriskriboj, poste elsendtitoloj, poste elsendpriskriboj; ero aperas nur
 * en sia unua grupo (dedup).
 */
class SerĉoDeponejoImplTest {

    private fun kanalo(slug: String, nomo: String, priskribo: String? = null) =
        Kanalo(slug = slug, nomo = nomo, priskribo = priskribo, podkastaRssUrl = "https://x.example/$slug")

    private fun elsendo(id: String, titolo: String, priskribo: String? = null) = Elsendo(
        id = id, kanaloSlug = "test", titolo = titolo, priskribo = priskribo,
        fluo = "https://x.example/$id.mp3", dato = "2026-10-30"
    )

    private class FalsaKanaloDeponejo(private val kanaloj: List<Kanalo>) : KanaloDeponejo {
        override fun observiKanalojn() = MutableStateFlow(kanaloj).asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = kanaloj
        override suspend fun getKanalo(slug: String) = kanaloj.find { it.slug == slug }
    }

    private class FalsaElsendoDeponejo(private val elsendoj: List<Elsendo>) : ElsendoDeponejo {
        override fun observiElsendojn(kanaloSlug: String): Flow<List<Elsendo>> =
            MutableStateFlow(elsendoj.filter { it.kanaloSlug == kanaloSlug }).asStateFlow()
        override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean) =
            elsendoj.filter { it.kanaloSlug == kanaloSlug }
        override suspend fun getElsendo(id: String) = elsendoj.find { it.id == id }
        override suspend fun serĉiElsendojn(teksto: String, limo: Int) =
            elsendoj.filter {
                it.titolo.contains(teksto, ignoreCase = true) ||
                    (it.priskribo?.contains(teksto, ignoreCase = true) ?: false)
            }.take(limo)
        override suspend fun serĉiElsendojnGrupigite(teksto: String, limo: Int): Pair<List<Elsendo>, List<Elsendo>> {
            val (titolo, resto) = elsendoj.partition { it.titolo.contains(teksto, ignoreCase = true) }
            return titolo.take(limo) to resto
                .filter { it.priskribo?.contains(teksto, ignoreCase = true) == true }
                .take(limo)
        }
        override suspend fun ŝargiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean) = emptyList<Elsendo>()
    }

    @Test
    fun rezultoGrupigitaLaŭGraveco_kajDedupita() = runTest {
        val kanaloj = listOf(
            kanalo("titola", "Esperanto Radio", "pri lingvo"),                       // titolo + priskribo → nur titolo
            kanalo("priskriba", "Podkasto", "pri Esperanto kaj podkastoj"),           // nur priskribo
            kanalo("alia", "Muziko", "sen rilato")
        )
        val elsendoj = listOf(
            elsendo("e-titolo", "Esperanto-podkasto"),
            elsendo("e-priskribo", "Vortoj", "pri Esperanto en la praktiko"),         // nur priskribo
            elsendo("e-ambaŭ", "Esperanto-historio", "ĉio pri esperanto")             // ambaŭ → nur titolo
        )
        val deponejo = SerĉoDeponejoImpl(FalsaElsendoDeponejo(elsendoj), FalsaKanaloDeponejo(kanaloj))

        val r = deponejo.serĉi("esperanto")

        assertEquals(listOf("titola"), r.kanalojLaŭTitolo.map { it.slug }, "kanaltitoloj unue")
        assertEquals(listOf("priskriba"), r.kanalojLaŭPriskribo.map { it.slug }, "kanalpriskriboj poste, sen dedup-eron")
        assertEquals(listOf("e-titolo", "e-ambaŭ"), r.elsendojLaŭTitolo.map { it.id }, "elsendtitoloj poste")
        assertEquals(listOf("e-priskribo"), r.elsendojLaŭPriskribo.map { it.id }, "elsendpriskriboj laste, sen dedup-eron")
    }

    @Test
    fun serĉoSensenta() = runTest {
        val kanaloj = listOf(kanalo("titola", "Esperanto Radio", "pri lingvo"))
        val deponejo = SerĉoDeponejoImpl(FalsaElsendoDeponejo(emptyList()), FalsaKanaloDeponejo(kanaloj))

        val r = deponejo.serĉi("ESPERANTO") // uskleco ne gravas

        assertEquals(listOf("titola"), r.kanalojLaŭTitolo.map { it.slug })
        assertEquals(true, r.estasVacia == false)
    }

    @Test
    fun nenioTrovita() = runTest {
        val deponejo = SerĉoDeponejoImpl(
            FalsaElsendoDeponejo(listOf(elsendo("e1", "Vortoj"))),
            FalsaKanaloDeponejo(listOf(kanalo("k1", "Muziko")))
        )
        val r = deponejo.serĉi("esperanto")
        assertEquals(true, r.estasVacia)
    }
}
