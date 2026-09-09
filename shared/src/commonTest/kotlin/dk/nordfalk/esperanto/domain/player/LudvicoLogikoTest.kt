package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Testoj por LudvicoLogiko — la pura decidlogiko por aŭtomata sekva-ludado.
 *
 * Tri prioritatoj:
 * 1. Sekva elsendo de la sama kanalo
 * 2. Plej freŝa nefinita elsendo el ŝatataj kanaloj
 * 3. Plej freŝa elsendo ankoraŭ ne ludata
 */
class LudvicoLogikoTest {

    /** Helpa funkcio por krei elsendon. */
    private fun elsendo(id: String, kanalo: String, dato: String, titolo: String = id) = Elsendo(
        id = id,
        kanaloSlug = kanalo,
        titolo = titolo,
        fluo = "https://x.com/$id.mp3",
        dato = dato,
    )

    /** Helpa funkcio por krei LudataElsendo (ludstatuson). */
    private fun ludato(id: String, kanalo: String, pozicio: Long = 0, finita: Boolean = false) =
        LudataElsendo(
            elsendoId = id,
            kanaloSlug = kanalo,
            pozicioMs = pozicio,
            finita = finita,
        )

    // =========================================================================
    // Priority 1: Sekva elsendo de la sama kanalo
    // =========================================================================

    @Test
    fun sekvaSamkanala_ludasSekvanElsendon() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k1:2024-02-01", "k1", "2024-02-01")
        val e3 = elsendo("k1:2024-01-01", "k1", "2024-01-01")
        val samkanalaj = listOf(e1, e2, e3) // plej-freŝe-unue

        val sekva = LudvicoLogiko.deciduSekvan(e1, samkanalaj, samkanalaj, emptySet(), emptyMap())

        assertEquals(e2, sekva, "Devus ludi la sekvan (pli malnovan) elsendon")
    }

    @Test
    fun sekvaSamkanala_saltasFinitajn() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k1:2024-02-01", "k1", "2024-02-01")
        val e3 = elsendo("k1:2024-01-01", "k1", "2024-01-01")
        val samkanalaj = listOf(e1, e2, e3)
        val ludatoj = mapOf(
            "k1:2024-02-01" to ludato("k1:2024-02-01", "k1", finita = true)
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, samkanalaj, samkanalaj, emptySet(), ludatoj)

        assertEquals(e3, sekva, "Devus salti la finitan e2 kaj ludi e3")
    }

    @Test
    fun sekvaSamkanala_ludasParteLudatanNeFinitan() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k1:2024-02-01", "k1", "2024-02-01")
        val samkanalaj = listOf(e1, e2)
        val ludatoj = mapOf(
            "k1:2024-02-01" to ludato("k1:2024-02-01", "k1", pozicio = 30000, finita = false)
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, samkanalaj, samkanalaj, emptySet(), ludatoj)

        assertEquals(e2, sekva, "Parte ludata sed ne finita devus esti elektebla")
    }

    @Test
    fun sekvaSamkanala_nenioSeLastaElsendo() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val samkanalaj = listOf(e1)

        val sekva = LudvicoLogiko.deciduSekvan(e1, samkanalaj, samkanalaj, emptySet(), emptyMap())

        assertNull(sekva, "Se estas la sola elsendo, ne ekzistas sekva")
    }

    @Test
    fun sekvaSamkanala_nenioSeNulaNunaElsendo() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val samkanalaj = listOf(e1)

        // Sen nuna elsendo, priority 1 ne aplikiĝas, sed priority 3 trovas e1
        val sekva = LudvicoLogiko.deciduSekvan(null, samkanalaj, samkanalaj, emptySet(), emptyMap())

        assertEquals(e1, sekva, "Sen nuna elsendo, priority 3 trovas la unuan neludatan")
    }

    @Test
    fun sekvaSamkanala_nenioSeChiujFinitaj() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k1:2024-02-01", "k1", "2024-02-01")
        val e3 = elsendo("k1:2024-01-01", "k1", "2024-01-01")
        val samkanalaj = listOf(e1, e2, e3)
        val ludatoj = mapOf(
            "k1:2024-02-01" to ludato("k1:2024-02-01", "k1", finita = true),
            "k1:2024-01-01" to ludato("k1:2024-01-01", "k1", finita = true),
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, samkanalaj, samkanalaj, emptySet(), ludatoj)

        assertNull(sekva, "Se ĉiuj sekvaj estas finitaj, priority 1 donas nenion")
    }

    // =========================================================================
    // Priority 2: Plej freŝa nefinita elsendo el ŝatataj kanaloj
    // =========================================================================

    @Test
    fun elSxatataj_plejFreshaNefinita() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("sx1:2024-02-15", "sx1", "2024-02-15")
        val e3 = elsendo("sx1:2024-02-01", "sx1", "2024-02-01")
        val cxiuj = listOf(e1, e2, e3)
        val sxatataj = setOf("sx1")

        // e1 estas lasta en k1 (ne ŝatata), do priority 1 donas nenion
        // Priority 2 devus trovi la plej freŝan el sx1
        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, sxatataj, emptyMap())

        assertEquals(e2, sekva, "Devus ludi plej freŝan nefinitan elsendon el ŝatata kanalo")
    }

    @Test
    fun elSxatataj_saltasFinitajn() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val sx1_nova = elsendo("sx1:2024-02-15", "sx1", "2024-02-15")
        val sx1_malnova = elsendo("sx1:2024-02-01", "sx1", "2024-02-01")
        val cxiuj = listOf(e1, sx1_nova, sx1_malnova)
        val sxatataj = setOf("sx1")
        val ludatoj = mapOf(
            "sx1:2024-02-15" to ludato("sx1:2024-02-15", "sx1", finita = true)
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, sxatataj, ludatoj)

        assertEquals(sx1_malnova, sekva, "Devus salti la finitan kaj ludi la malnovan")
    }

    @Test
    fun elSxatataj_parteLudataElektebla() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val sx1_e = elsendo("sx1:2024-02-15", "sx1", "2024-02-15")
        val cxiuj = listOf(e1, sx1_e)
        val sxatataj = setOf("sx1")
        val ludatoj = mapOf(
            "sx1:2024-02-15" to ludato("sx1:2024-02-15", "sx1", pozicio = 60000, finita = false)
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, sxatataj, ludatoj)

        assertEquals(sx1_e, sekva, "Parte ludata sed ne finita el ŝatata kanalo estas elektebla")
    }

    @Test
    fun elSxatataj_nenioSeNeniuSxatataKanalo() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val cxiuj = listOf(e1)

        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, emptySet(), emptyMap())

        assertNull(sekva, "Sen ŝatataj kanaloj, priority 2 ne donas nenion")
    }

    @Test
    fun elSxatataj_elektasPlejFreshanElPlurajSxatatajKanaloj() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val sx1_e = elsendo("sx1:2024-02-01", "sx1", "2024-02-01")
        val sx2_e = elsendo("sx2:2024-02-15", "sx2", "2024-02-15")
        val cxiuj = listOf(e1, sx1_e, sx2_e)
        val sxatataj = setOf("sx1", "sx2")

        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, sxatataj, emptyMap())

        assertEquals(sx2_e, sekva, "Devus elekti la plej freŝan (2024-02-15 > 2024-02-01)")
    }

    // =========================================================================
    // Priority 3: Plej freŝa elsendo ankoraŭ ne ludata
    // =========================================================================

    @Test
    fun plejFreshaNeludata_trovigxasKiamNeniuSxatata() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k2:2024-02-15", "k2", "2024-02-15")
        val e3 = elsendo("k3:2024-02-01", "k3", "2024-02-01")
        val cxiuj = listOf(e1, e2, e3)

        // e1 estas la nuna, neniuj ŝatataj, neniuj ludatoj
        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, emptySet(), emptyMap())

        assertEquals(e2, sekva, "Devus ludi la plej freŝan neludatan (e2, 2024-02-15)")
    }

    @Test
    fun plejFreshaNeludata_ignorasLuditajn() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k2:2024-02-15", "k2", "2024-02-15")
        val e3 = elsendo("k3:2024-02-01", "k3", "2024-02-01")
        val cxiuj = listOf(e1, e2, e3)
        val ludatoj = mapOf(
            "k2:2024-02-15" to ludato("k2:2024-02-15", "k2", pozicio = 1000, finita = false)
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, emptySet(), ludatoj)

        assertEquals(e3, sekva, "Devus ignari e2 (jam ludata) kaj ludi e3")
    }

    @Test
    fun plejFreshaNeludata_nenioSeChiujLuditaj() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k2:2024-02-15", "k2", "2024-02-15")
        val cxiuj = listOf(e1, e2)
        val ludatoj = mapOf(
            "k2:2024-02-15" to ludato("k2:2024-02-15", "k2", pozicio = 1000, finita = false)
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, emptySet(), ludatoj)

        assertNull(sekva, "Se ĉiuj estis luditaj, priority 3 donas nenion")
    }

    // =========================================================================
    // Prioritato-ordo (integraj testoj)
    // =========================================================================

    @Test
    fun prioritato1_superasPrioritaton2() {
        // Priority 1 (sekva samkanala) ekzistas → uzu ĝin, ne priority 2
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val sekva_k1 = elsendo("k1:2024-02-01", "k1", "2024-02-01")
        val sx1_e = elsendo("sx1:2024-02-15", "sx1", "2024-02-15")
        val cxiuj = listOf(e1, sekva_k1, sx1_e)
        val sxatataj = setOf("sx1")

        val sekva = LudvicoLogiko.deciduSekvan(e1, cxiuj, cxiuj, sxatataj, emptyMap())

        assertEquals(sekva_k1, sekva, "Priority 1 (samkanala) devus superi priority 2 (ŝatata)")
    }

    @Test
    fun prioritato2_superasPrioritaton3() {
        // Neniu sekva samkanala, sed ekzistas nefinita el ŝatata kanalo
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val sx1_e = elsendo("sx1:2024-02-15", "sx1", "2024-02-15")
        val k2_e = elsendo("k2:2024-02-20", "k2", "2024-02-20") // pli freŝa sed ne ŝatata
        val cxiuj = listOf(e1, sx1_e, k2_e)
        val sxatataj = setOf("sx1")

        val sekva = LudvicoLogiko.deciduSekvan(e1, listOf(e1), cxiuj, sxatataj, emptyMap())

        assertEquals(sx1_e, sekva, "Priority 2 (ŝatata nefinita) devus superi priority 3 (freŝa neludata)")
    }

    @Test
    fun nenioSeChiujFinitajKajLuditaj() {
        val e1 = elsendo("k1:2024-03-01", "k1", "2024-03-01")
        val e2 = elsendo("k1:2024-02-01", "k1", "2024-02-01")
        val e3 = elsendo("k2:2024-02-15", "k2", "2024-02-15")
        val cxiuj = listOf(e1, e2, e3)
        val sxatataj = setOf("k2")
        val ludatoj = mapOf(
            "k1:2024-02-01" to ludato("k1:2024-02-01", "k1", finita = true),
            "k2:2024-02-15" to ludato("k2:2024-02-15", "k2", finita = true),
        )

        val sekva = LudvicoLogiko.deciduSekvan(e1, cxiuj, cxiuj, sxatataj, ludatoj)

        assertNull(sekva, "Se ĉiuj estas finitaj/luditaj, nenio por ludi")
    }

    @Test
    fun malplenaKajNula() {
        assertNull(LudvicoLogiko.deciduSekvan(null, emptyList(), emptyList(), emptySet(), emptyMap()))
        assertNull(LudvicoLogiko.deciduSekvan(null, emptyList(), emptyList(), setOf("k1"), emptyMap()))
    }

    // =========================================================================
    // trovSekvanSamkanalan — detala testado
    // =========================================================================

    @Test
    fun trovSekvanSamkanalan_nulaNuna() {
        assertNull(LudvicoLogiko.trovSekvanSamkanalan(null, listOf(elsendo("a", "k", "2024-01-01")), emptyMap()))
    }

    @Test
    fun trovSekvanSamkanalan_nunaNeEnListo() {
        val e = elsendo("k1:2024-01-01", "k1", "2024-01-01")
        val aliaj = listOf(elsendo("k1:2024-02-01", "k1", "2024-02-01"))
        assertNull(LudvicoLogiko.trovSekvanSamkanalan(e, aliaj, emptyMap()))
    }

    // =========================================================================
    // trovNefinitanElSxatataj — detala testado
    // =========================================================================

    @Test
    fun trovNefinitanElSxatataj_elektasPlejFreshan() {
        val malnova = elsendo("sx:2024-01-01", "sx", "2024-01-01")
        val nova = elsendo("sx:2024-03-01", "sx", "2024-03-01")
        val cxiuj = listOf(malnova, nova)

        val rezulto = LudvicoLogiko.trovNefinitanElSxatataj(cxiuj, setOf("sx"), emptyMap())

        assertEquals(nova, rezulto)
    }

    @Test
    fun trovNefinitanElSxatataj_neniuSxatata() {
        val e = elsendo("k1:2024-01-01", "k1", "2024-01-01")
        assertNull(LudvicoLogiko.trovNefinitanElSxatataj(listOf(e), emptySet(), emptyMap()))
    }

    // =========================================================================
    // trovPlejFresxanNeludatan — detala testado
    // =========================================================================

    @Test
    fun trovPlejFresxanNeludatan_neniuNeludata() {
        val e = elsendo("k1:2024-01-01", "k1", "2024-01-01")
        val ludatoj = mapOf("k1:2024-01-01" to ludato("k1:2024-01-01", "k1"))
        assertNull(LudvicoLogiko.trovPlejFresxanNeludatan(listOf(e), ludatoj))
    }

    @Test
    fun trovPlejFresxanNeludatan_elektasPlejFreshan() {
        val malnova = elsendo("k1:2024-01-01", "k1", "2024-01-01")
        val meza = elsendo("k2:2024-02-01", "k2", "2024-02-01")
        val nova = elsendo("k3:2024-03-01", "k3", "2024-03-01")
        val cxiuj = listOf(malnova, meza, nova)

        val rezulto = LudvicoLogiko.trovPlejFresxanNeludatan(cxiuj, emptyMap())

        assertEquals(nova, rezulto)
    }
}
