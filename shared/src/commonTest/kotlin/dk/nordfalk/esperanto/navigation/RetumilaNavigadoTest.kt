package dk.nordfalk.esperanto.navigation

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testoj de la retumila historia sinkronigo (unu historia eniro po nav-nivelo)
 * kaj de la paĝotitolo-mapigo. La wasmJs-aktualigo de RetumilaNavigado
 * uzas ĉi tiujn purajn funkciojn, do la konduto estas testebla sen retumilo.
 */
class RetumilaNavigadoTest {

    // --- historia sinkronigo ---

    @Test
    fun puŝoProfundigasLaHistorion() {
        assertTrue(estasHistoriaPuŝo(nunaProfundo = 1, novaProfundo = 2))
        assertTrue(estasHistoriaPuŝo(nunaProfundo = 2, novaProfundo = 4))
        assertFalse(estasHistoriaPuŝo(nunaProfundo = 2, novaProfundo = 2)) // langet-ŝanĝo
    }

    @Test
    fun reenigoPostulasHistoryBack() {
        assertEquals(1, historiaReenNiveloj(nunaProfundo = 3, novaProfundo = 2))
        assertEquals(2, historiaReenNiveloj(nunaProfundo = 3, novaProfundo = 1)) // ekz. tirkesta restartigo
        assertEquals(0, historiaReenNiveloj(nunaProfundo = 2, novaProfundo = 2))
        assertEquals(0, historiaReenNiveloj(nunaProfundo = 1, novaProfundo = 3)) // neniam negativa
    }

    @Test
    fun enpropraReenSagoRetrompuSedRetumilaReenNe() {
        // Enpropra reen-sago: la historio ankoraŭ staras sur la malnova profundo
        assertTrue(estuHistoryBack(memoritaNavProfundo = 2, novaNavProfundo = 1, historiaProfundoNun = 2))
        // Retumila reen-butono: la historio JAM egalas la novan profundo — ne iru reen dufoje!
        assertFalse(estuHistoryBack(memoritaNavProfundo = 2, novaNavProfundo = 1, historiaProfundoNun = 1))
        // Langet-ŝanĝo (sama profundo) neniam retrompas
        assertFalse(estuHistoryBack(memoritaNavProfundo = 2, novaNavProfundo = 2, historiaProfundoNun = 2))
    }

    @Test
    fun popstateNaviguReenNurKiamLaRetumiloIrisReen() {
        // Retumila reen-butono: la historia profundo malantaŭas la nav-stakon
        assertTrue(popstateBezonasReenigon(historiaProfundo = 1, memoritaNavProfundo = 2))
        // Nia propra history.back() post la reen-sago: la profundo estis
        // ĝisdatigita antaŭ la evento — la valoroj kongruas
        assertFalse(popstateBezonasReenigon(historiaProfundo = 1, memoritaNavProfundo = 1))
        // Antaŭen (aŭ eksterstaka) — ne tuŝas la nav-stakon
        assertFalse(popstateBezonasReenigon(historiaProfundo = 2, memoritaNavProfundo = 1))
    }

    // --- paĝotitolo (langeta titolo) ---

    private fun kanalo(slug: String, nomo: String) = Kanalo(slug = slug, nomo = nomo)
    private val kanaloj = listOf(kanalo("kern-punkto", "Kern-Punkto"))

    @Test
    fun radikaKajBazajEkranojOrdigasSin() {
        assertEquals("EsperantoRadio beta", vojaTitolo(null, kanaloj))
        assertEquals("EsperantoRadio", vojaTitolo(Vojo.Hejmo, kanaloj))
        assertEquals("Ĉiuj kanaloj · EsperantoRadio", vojaTitolo(Vojo.Kanalaro, kanaloj))
        assertEquals("Serĉo · EsperantoRadio", vojaTitolo(Vojo.Serĉo, kanaloj))
    }

    @Test
    fun detalojUzasLaKanalanNomonSeKonatan() {
        val vojo = Vojo.KanaloDetalo(slug = "kern-punkto")
        assertEquals("Kern-Punkto · EsperantoRadio", vojaTitolo(vojo, kanaloj))
        // Nekonata kanalo (fluo ankoraŭ ne ŝargita) — falu al la slug
        assertEquals("alslug · EsperantoRadio", vojaTitolo(Vojo.KanaloDetalo(slug = "alslug"), kanaloj))
        assertEquals("Kern-Punkto · EsperantoRadio",
            vojaTitolo(Vojo.ElsendoDetalo(id = "e", kanaloSlug = "kern-punkto"), kanaloj))
    }
}
