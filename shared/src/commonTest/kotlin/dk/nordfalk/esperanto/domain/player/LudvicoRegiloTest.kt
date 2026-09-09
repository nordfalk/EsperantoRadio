package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.data.repository.LudatojDeponejoMaketo
import dk.nordfalk.esperanto.data.repository.PlejsatatajDeponejoImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por LudvicoRegilo — pozicio-spurado, resumigo, aŭtoludo, ludvico.
 *
 * Uzas NoOpLudiloRegilo (kun simuluFinon/simuluPozicion) kaj memorajn deponejojn.
 */
class LudvicoRegiloTest {

    /** Helpa funkcio por krei elsendon. */
    private fun elsendo(id: String, kanalo: String = "k1", dato: String = "2024-01-01") = Elsendo(
        id = id,
        kanaloSlug = kanalo,
        titolo = "Elsendo $id",
        fluo = "https://x.com/$id.mp3",
        dato = dato,
    )

    /** Kreas la LudvicoRegilo kun memoraj deponejoj. */
    private fun kreuRegilon(
        ludilo: NoOpLudiloRegilo = NoOpLudiloRegilo(),
        elsendoj: Map<String, List<Elsendo>> = emptyMap(),
        kanaloj: List<Kanalo> = emptyList(),
        plejsatataj: Set<String> = emptySet(),
        ludatojDeponejo: LudatojDeponejo = LudatojDeponejoMaketo(),
        lokaDosiero: Map<String, String> = emptyMap(),
        scope: TestScope,
    ): Triple<LudvicoRegilo, NoOpLudiloRegilo, LudatojDeponejo> {
        val regiloScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val plejDeponejo = object : PlejsatatajDeponejo {
            private val _set = MutableStateFlow(plejsatataj)
            override fun observiPlejsatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejsaton(kanaloSlug: String) {}
            override suspend fun estasPlejsatata(kanaloSlug: String) = kanaloSlug in _set.value
        }
        val elsendoDeponejo = object : ElsendoDeponejo {
            override fun observiElsendojn(kanaloSlug: String): StateFlow<List<Elsendo>> =
                MutableStateFlow(elsendoj[kanaloSlug] ?: emptyList())
            override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean) =
                elsendoj[kanaloSlug] ?: emptyList()
            override suspend fun getElsendo(id: String) = elsendoj.values.flatten().find { it.id == id }
            override suspend fun sercxiElsendojn(teksto: String, limo: Int) = emptyList<Elsendo>()
            override suspend fun sxargxiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean) =
                elsendoj[kanalo.slug] ?: emptyList()
        }
        val kanaloDeponejo = object : KanaloDeponejo {
            private val _kanaloj = MutableStateFlow(kanaloj)
            override fun observiKanalojn() = _kanaloj.asStateFlow()
            override suspend fun getKanalojn(fortoRefresigi: Boolean) = kanaloj
            override suspend fun getKanalo(slug: String) = kanaloj.find { it.slug == slug }
        }
        val regilo = LudvicoRegilo(
            ludilo = ludilo,
            elsendoDeponejo = elsendoDeponejo,
            kanaloDeponejo = kanaloDeponejo,
            plejsatatajDeponejo = plejDeponejo,
            ludatojDeponejo = ludatojDeponejo,
            getLokaDosieroVojo = { id -> lokaDosiero[id] },
            scope = regiloScope,
        )
        return Triple(regilo, ludilo, ludatojDeponejo)
    }

    // =========================================================================
    // Resumigo — komenci de savita pozicio
    // =========================================================================

    @Test
    fun resumigxo_komencasDeSavitaPozicio() = runTest {
        val ludilo = NoOpLudiloRegilo()
        val ludatoj = LudatojDeponejoMaketo()
        val e = elsendo("e1")
        // Simulas ke la uzanto antaŭe aŭskultis ĝis 30 sekundoj
        ludatoj.registriPozicion(e.id, e.kanaloSlug, 30_000, 300_000)

        val (regilo, _, _) = kreuRegilon(ludilo, ludatojDeponejo = ludatoj, scope = this)

        regilo.ludiElsendon(e)

        // fiksiFonton devus esti vokita kun komencoPozicioMs = 30000
        assertEquals(30_000, ludilo.stato.value.pozicioMs, "Devus resumi de 30s")
        assertEquals(LudantoStato.Ludas, ludilo.stato.value.stato)
    }

    @Test
    fun resumigxo_komencasDeNuloSeNeniuSavitaPozicio() = runTest {
        val ludilo = NoOpLudiloRegilo()
        val e = elsendo("e1")

        val (regilo, _, _) = kreuRegilon(ludilo, scope = this)

        regilo.ludiElsendon(e)

        assertEquals(0, ludilo.stato.value.pozicioMs, "Devus komenci de 0 se neniu savita pozicio")
    }

    @Test
    fun resumigxo_komencasDeNuloSeFinita() = runTest {
        val ludilo = NoOpLudiloRegilo()
        val ludatoj = LudatojDeponejoMaketo()
        val e = elsendo("e1")
        // Simulas ke la elsendo estis finita
        ludatoj.registriPozicion(e.id, e.kanaloSlug, 280_000, 300_000)
        ludatoj.markiFinita(e.id, e.kanaloSlug)

        val (regilo, _, _) = kreuRegilon(ludilo, ludatojDeponejo = ludatoj, scope = this)

        regilo.ludiElsendon(e)

        assertEquals(0, ludilo.stato.value.pozicioMs, "Finita elsendo devus komenci de 0")
    }

    // =========================================================================
    // Ludvico — aldoni, forigi, ludi sekvan
    // =========================================================================

    @Test
    fun vico_aldoniAlVico() = runTest {
        val e1 = elsendo("e1")
        val e2 = elsendo("e2")
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(ludilo, scope = this)

        // e1 estas tuj ludita (nenio ludas) → forigita de vico
        regilo.aldoniAlVico(e1)
        // Nun io ludas → e2 restas en vico
        regilo.aldoniAlVico(e2)

        assertEquals(1, regilo.vico.value.size, "Vico devus havi 1 eron (e2; e1 estas ludata)")
        assertEquals("e2", regilo.vico.value[0].id)
        assertEquals(LudantoStato.Ludas, ludilo.stato.value.stato, "e1 devus ludi")
    }

    @Test
    fun vico_aldoniNeAldonasDuplikatojn() = runTest {
        val e1 = elsendo("e1")
        val (regilo, _, _) = kreuRegilon(scope = this)

        regilo.aldoniAlVico(e1)
        regilo.aldoniAlVico(e1) // duplikato

        assertEquals(1, regilo.vico.value.size, "Ne devus aldoni duplikaton")
    }

    @Test
    fun vico_forigiElVico() = runTest {
        val e1 = elsendo("e1")
        val e2 = elsendo("e2")
        val (regilo, _, _) = kreuRegilon(scope = this)

        regilo.aldoniAlVico(e1)
        regilo.aldoniAlVico(e2)
        regilo.forigiElVico("e1")

        assertEquals(1, regilo.vico.value.size)
        assertEquals("e2", regilo.vico.value[0].id)
    }

    @Test
    fun vico_malplenigi() = runTest {
        val e1 = elsendo("e1")
        val (regilo, _, _) = kreuRegilon(scope = this)

        regilo.aldoniAlVico(e1)
        regilo.malplenigiVicon()

        assertEquals(0, regilo.vico.value.size)
    }

    @Test
    fun vico_ludasSekvanElVicoKiamFinita() = runTest {
        val e1 = elsendo("e1")
        val e2 = elsendo("e2")
        val elsendoj = mapOf("k1" to listOf(e1, e2))
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(ludilo, elsendoj = elsendoj, scope = this)

        regilo.komenci()


        // Unue ludu e1
        regilo.ludiElsendon(e1)


        // Aldonu e2 al vico dum e1 ludas
        regilo.aldoniAlVico(e2)


        // Simulu finon de e1
        ludilo.simuluFinon()


        // La sekva devus esti e2 (el la vico)
        assertEquals(LudantoStato.Ludas, ludilo.stato.value.stato)
        val nunaFonto = ludilo.stato.value.nunaFonto
        assertTrue(nunaFonto is Sonfonto.ElsendoFonto, "Devus ludi elsendon")
        assertEquals("e2", (nunaFonto as Sonfonto.ElsendoFonto).elsendo.id)
        assertEquals(0, regilo.vico.value.size, "Vico devus esti malplena post ludi la unuan")
    }

    @Test
    fun vico_aldoniTujLudasSeNenioLudas() = runTest {
        val e1 = elsendo("e1")
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(ludilo, scope = this)

        regilo.komenci()

        regilo.aldoniAlVico(e1)


        // Aldono al malplena vico dum nenio ludas devus tuj ludi
        assertEquals(LudantoStato.Ludas, ludilo.stato.value.stato)
    }

    // =========================================================================
    // Aŭtoludo — aŭtomata sekva-ludado post Finita
    // =========================================================================

    @Test
    fun auxtoludo_ludasSekvanSamkanalan() = runTest {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k1:2024-02-01", "k1", "2024-02-01")
        val elsendoj = mapOf("k1" to listOf(e1, e2))
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, ludatoj) = kreuRegilon(ludilo, elsendoj = elsendoj, scope = this)

        regilo.komenci()

        regilo.ludiElsendon(e1)


        // Simulu naturfinon
        ludilo.simuluFinon()


        // Devus ludi e2 (sekva samkanala)
        assertEquals(LudantoStato.Ludas, ludilo.stato.value.stato)
        val fonto = ludilo.stato.value.nunaFonto as Sonfonto.ElsendoFonto
        assertEquals("k1:2024-02-01", fonto.elsendo.id)

        // e1 devus esti markita finita
        assertTrue(ludatoj.estasFinita(e1.id), "e1 devus esti markita finita")
    }

    @Test
    fun auxtolodo_ludasElSxatatajKiamNeniuSekvaSamkanala() = runTest {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val sx1_e = elsendo("sx1:2024-02-15", "sx1", "2024-02-15")
        val elsendoj = mapOf(
            "k1" to listOf(e1), // e1 estas la sola en k1
            "sx1" to listOf(sx1_e)
        )
        val kanaloj = listOf(
            Kanalo(slug = "k1", nomo = "K1"),
            Kanalo(slug = "sx1", nomo = "SX1", podkastaRssUrl = "https://x.com/sx1.xml")
        )
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(
            ludilo, elsendoj = elsendoj, kanaloj = kanaloj,
            plejsatataj = setOf("sx1"), scope = this
        )

        regilo.komenci()

        regilo.ludiElsendon(e1)


        ludilo.simuluFinon()


        // Devus ludi sx1_e (el ŝatata kanalo)
        assertEquals(LudantoStato.Ludas, ludilo.stato.value.stato)
        val fonto = ludilo.stato.value.nunaFonto as Sonfonto.ElsendoFonto
        assertEquals("sx1:2024-02-15", fonto.elsendo.id)
    }

    @Test
    fun auxtolodo_ludasPlejFreshanNeludatanKielLastaResurso() = runTest {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val k2_e = elsendo("k2:2024-02-15", "k2", "2024-02-15")
        val elsendoj = mapOf(
            "k1" to listOf(e1), // e1 sola, neniuj ŝatataj
            "k2" to listOf(k2_e)
        )
        val kanaloj = listOf(
            Kanalo(slug = "k1", nomo = "K1"),
            Kanalo(slug = "k2", nomo = "K2", podkastaRssUrl = "https://x.com/k2.xml")
        )
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(
            ludilo, elsendoj = elsendoj, kanaloj = kanaloj,
            plejsatataj = emptySet(), scope = this
        )

        regilo.komenci()

        regilo.ludiElsendon(e1)


        ludilo.simuluFinon()


        // Devus ludi k2_e (plej freŝa neludata)
        assertEquals(LudantoStato.Ludas, ludilo.stato.value.stato)
        val fonto = ludilo.stato.value.nunaFonto as Sonfonto.ElsendoFonto
        assertEquals("k2:2024-02-15", fonto.elsendo.id)
    }

    @Test
    fun auxtolodo_haltasSeNenioPorLudi() = runTest {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val ludatoj = LudatojDeponejoMaketo()
        // Marku e1 kiel finita anticipe
        ludatoj.markiFinita(e1.id, e1.kanaloSlug)
        val elsendoj = mapOf("k1" to listOf(e1))
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(
            ludilo, elsendoj = elsendoj, ludatojDeponejo = ludatoj, scope = this
        )

        regilo.komenci()

        regilo.ludiElsendon(e1)


        ludilo.simuluFinon()


        // Devus esti haltita — nenio por ludi
        assertEquals(LudantoStato.Haltita, ludilo.stato.value.stato)
    }

    // =========================================================================
    // Pozicio-spurado
    // =========================================================================

    @Test
    fun pozicio_registritaKiamLudiElsendon() = runTest {
        val e = elsendo("e1")
        val ludatoj = LudatojDeponejoMaketo()
        val (regilo, _, _) = kreuRegilon(ludatojDeponejo = ludatoj, scope = this)

        regilo.ludiElsendon(e)

        // registriPozicio estas vokita en ludiElsendon
        val ludato = ludatoj.getLudato(e.id)
        assertTrue(ludato != null, "Ludanto devus esti registrita")
        assertEquals("e1", ludato!!.elsendoId)
    }

    @Test
    fun pozicio_markitaFinitaKiamLudadoFinitas() = runTest {
        val e = elsendo("e1")
        val ludatoj = LudatojDeponejoMaketo()
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(ludilo, ludatojDeponejo = ludatoj, scope = this)

        regilo.komenci()

        regilo.ludiElsendon(e)


        // Simulu pozicio-progreson kaj finon
        ludilo.simuluPozicion(120_000, 300_000)
        ludilo.simuluFinon()


        assertTrue(ludatoj.estasFinita(e.id), "Elsendo devus esti markita finita")
    }

    // =========================================================================
    // Loka dosiero (elŝutita)
    // =========================================================================

    @Test
    fun lokaDosiero_uzasLokanElsendonSeEkzistas() = runTest {
        val e = elsendo("e1")
        val lokaVojo = "/tmp/elshutoj/e1.mp3"
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(
            ludilo, lokaDosiero = mapOf("e1" to lokaVojo), scope = this
        )

        regilo.ludiElsendon(e)

        val fonto = ludilo.stato.value.nunaFonto
        assertTrue(fonto is Sonfonto.LokaElsendo, "Devus uzi lokan elsendon")
        assertEquals(lokaVojo, (fonto as Sonfonto.LokaElsendo).dosieroVojo)
    }

    @Test
    fun lokaDosiero_uzasElsendoFontonSeNeniuLoka() = runTest {
        val e = elsendo("e1")
        val ludilo = NoOpLudiloRegilo()
        val (regilo, _, _) = kreuRegilon(ludilo, scope = this)

        regilo.ludiElsendon(e)

        val fonto = ludilo.stato.value.nunaFonto
        assertTrue(fonto is Sonfonto.ElsendoFonto, "Devus uzi ElsendoFonton")
    }
}
