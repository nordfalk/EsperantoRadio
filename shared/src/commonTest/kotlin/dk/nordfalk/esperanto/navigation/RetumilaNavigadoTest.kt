package dk.nordfalk.esperanto.navigation

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals(1, historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 1, planitaHistoriaProfundo = 2))
        assertEquals(2, historiaReeniro(memoritaNavProfundo = 3, novaNavProfundo = 1, planitaHistoriaProfundo = 3))
        // Post antaŭen-trairo la planita pozicio povas superi la memoritan
        // profundecon (memorita=2, planita=3): la paŝo devas eliri de la pozicio
        assertEquals(2, historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 1, planitaHistoriaProfundo = 3))
        // DU RAPIDAJ reen-agoj: la dua eliras de la pozicio, kiun la unua jam
        // PLANIS (ne de history.state — kiu ankoraŭ montras la malnovan) —
        // sume oni atingas la celon, ne la eliron el la paĝo
        assertEquals(1, historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 1, planitaHistoriaProfundo = 2))
        // Retumila reen-butono (popstate direktis la nav-popon): la historio JAM
        // egalas la novan profundecon — nul = ne iru reen dufoje (tio forlasus la paĝon)
        assertNull(historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 1, planitaHistoriaProfundo = 1))
        // Defende: la plano neniam iras sub historian pozicion 1 (nova=0 ne
        // povas okazi — la nav-stako ĉiam havas almenaŭ Hejmon)
        assertEquals(2, historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 0, planitaHistoriaProfundo = 3))
        // Puŝo aŭ langet-ŝanĝo (novaj profundoj ne malpliiĝis) neniam tuŝas la historion
        assertNull(historiaReeniro(memoritaNavProfundo = 2, novaNavProfundo = 2, planitaHistoriaProfundo = 2))
        assertNull(historiaReeniro(memoritaNavProfundo = 1, novaNavProfundo = 3, planitaHistoriaProfundo = 1))
    }

    @Test
    fun puŝoKreasUnuEnironPoNiveloKajReuzasAntaŭenajn() {
        // Normala puŝo: unu nova eniro po nivelo
        assertEquals(listOf(3, 4), historiajPuŝoj(memoritaNavProfundo = 2, planitaHistoriaProfundo = 2, novaNavProfundo = 4).toList())
        assertEquals(listOf(2, 3), historiajPuŝoj(memoritaNavProfundo = 1, planitaHistoriaProfundo = 1, novaNavProfundo = 3).toList())
        // Post antaŭen-trairo (memorita=2, retumilo ĉe 3): PUŜO al profundo 3
        // REUZAS la ekzistan antaŭen-eniron — alie estiĝus duobla {d:3} kaj la
        // sekva retumila reenigo ŝajnus nenio (du premoj por unu ekrano)
        assertTrue(historiajPuŝoj(memoritaNavProfundo = 2, planitaHistoriaProfundo = 3, novaNavProfundo = 3).isEmpty())
        // ...kaj puŝo du nivelojn reuzas nur la unuan
        assertEquals(listOf(4), historiajPuŝoj(memoritaNavProfundo = 2, planitaHistoriaProfundo = 3, novaNavProfundo = 4).toList())
        // Malpliiĝo (reenigo) neniam puŝas
        assertTrue(historiajPuŝoj(memoritaNavProfundo = 3, planitaHistoriaProfundo = 2, novaNavProfundo = 2).isEmpty())
    }

    @Test
    fun puŝoPostPlurnivelaAntaŭenigoKorektigasLaPozicion() {
        // La retumilo restis ANTAŬE de la nova profundo (1→4, reen al 2, antaŭen
        // al 4, poste puŝo al 3): necesa korekta trairo al la reuzebla eniro {d:3}
        assertEquals(1, historiaKorekto(planitaHistoriaProfundo = 4, novaNavProfundo = 3))
        assertEquals(2, historiaKorekto(planitaHistoriaProfundo = 5, novaNavProfundo = 3))
        // Pura puŝo aŭ reuzo (la retumilo NE antaŭas la novan profundecon): neniom
        assertNull(historiaKorekto(planitaHistoriaProfundo = 2, novaNavProfundo = 3))
        assertNull(historiaKorekto(planitaHistoriaProfundo = 3, novaNavProfundo = 3))
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
