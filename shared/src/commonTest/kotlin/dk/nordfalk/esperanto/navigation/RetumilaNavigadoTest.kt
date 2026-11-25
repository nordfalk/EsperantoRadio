package dk.nordfalk.esperanto.navigation

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Testoj de la retumila historia sinkronigo (unu historia eniro po nav-nivelo)
 * kaj de la paĝotitolo-mapigo. La wasmJs-aktualigo de RetumilaNavigado
 * uzas ĉi tiujn purajn funkciojn, do la konduto estas testebla sen retumilo.
 */
class RetumilaNavigadoTest {

    // --- historia sinkronigo ---

    @Test
    fun enpropraReenigoRetrompasLaHistorionRetumilaNe() {
        // Enpropra reen-ago (reen-sago, tirkesta restartigo): la retumilo NE iris
        // reen — la historio postrestas kaj oni devas fari history.back()
        assertEquals(1, historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 1, historiaProfundoNun = 2))
        assertEquals(2, historiaReeniro(memoritaNavProfundo = 3, novaNavProfundo = 1, historiaProfundoNun = 3))
        // Retumila reen-butono (popstate direktis la nav-popon): la historio JAM
        // egalas la novan profundecon — nul = ne iru reen dufoje (tio forlasus la paĝon)
        assertNull(historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 1, historiaProfundoNun = 1))
        // Puŝo aŭ langet-ŝanĝo (novaj profundoj ne malpliiĝis) neniam tuŝas la historion
        assertNull(historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 2, historiaProfundoNun = 2))
        assertNull(historiaReeniro(memoritaNavProfundo = 1, novaNavProfundo = 3, historiaProfundoNun = 1))
    }

    @Test
    fun popstateTraktadoDistingasNivelonKajDirekton() {
        // UNU retumila reen-paŝo: la internaj traktiloj rajtas unue konsumi ĝin
        assertEquals(PopstateTraktado.Ununivela, popstateTraktado(memoritaNavProfundo = 2, historiaProfundo = 1))
        assertEquals(PopstateTraktado.Ununivela, popstateTraktado(memoritaNavProfundo = 5, historiaProfundo = 4))
        // PLURAJ niveloj samtempe (historia menuo de la retumilo): pura plurigo
        assertEquals(PopstateTraktado.Plurnivela(2), popstateTraktado(memoritaNavProfundo = 3, historiaProfundo = 1))
        assertEquals(PopstateTraktado.Plurnivela(3), popstateTraktado(memoritaNavProfundo = 3, historiaProfundo = 0))
        // Antaŭen-trairo aŭ nia propra sinkronigo (history.back() el la
        // stak-efekto): neniu ago — la valoroj kongruas
        assertEquals(PopstateTraktado.Nenio, popstateTraktado(memoritaNavProfundo = 1, historiaProfundo = 1))
        assertEquals(PopstateTraktado.Nenio, popstateTraktado(memoritaNavProfundo = 1, historiaProfundo = 2))
    }

    // --- paĝotitolo (langeta titolo) ---

    private fun kanalo(slug: String, nomo: String) = Kanalo(slug = slug, nomo = nomo)
    private val kanaloj = listOf(kanalo("kern-punkto", "Kern-Punkto"))

    @Test
    fun radikaKajBazajEkranojOrdigasSin() {
        // null (malplena stako) ne povas okazi dum rulado — same kiel Hejmo
        assertEquals("EsperantoRadio", vojaTitolo(null, kanaloj))
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
