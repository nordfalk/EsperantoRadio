package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testoj por la defora kanalkonfiguro: la elekto de la fonto ĉe starto
 * ([leguAgordonKunRezervo]) kaj la fona refreŝigo ([KanaloDeponejoImpl.refreŝiguDefore]),
 * modelita laŭ la «grunddata»-fluo de la malnova apo (konservita unue, rezervo
 * al la paktita, kaj neniam rompi la nunan staton ĉe fiasko).
 *
 * La persistigo estas injektata per memor-lambdoj — NENIU reala dosiero-kaŝo
 * tuŝiĝas (la DosierKaŝo-realigoj estas en la platformaj fontaroj, ne ĉi tie).
 */
class KanalkonfiguroDeforaTest {

    private val leganto = KanalAgordoLeganto()

    /** Malgranda valida konfiguro kun unu kanalo de la donita kodo. */
    private fun agordoTeksto(kodo: String) = """{"kanaloj":[{"kodo":"$kodo","nomo":"Kanalo $kodo"}]}"""

    /** Enmemora persistigo (anstataŭ DosierKaŝo). */
    private class MemorPersisto {
        var teksto: String? = null
        var forigitaj = 0
        val legu: () -> String? = { teksto }
        val forigu: () -> Unit = { teksto = null; forigitaj++ }
        val skribu: (String) -> Unit = { teksto = it }
        var skribEraro: Exception? = null
        val skribuKunEraro: (String) -> Unit = { skribEraro?.let { e -> throw e }; teksto = it }
    }

    // ---------- leguAgordonKunRezervo ----------

    @Test
    fun konservitaKonfiguroVenkasLaPaktitan() {
        val persisto = MemorPersisto()
        persisto.teksto = agordoTeksto("defora")
        val agordo = leguAgordonKunRezervo(leganto, { agordoTeksto("paktita") }, persisto.legu, persisto.forigu)
        assertEquals(listOf("defora"), agordo.kanaloj.map { it.kodo })
        assertEquals(0, persisto.forigitaj)
    }

    @Test
    fun putritaKonservitoRevenasAlLaPaktitanKajForgesiĝas() {
        val persisto = MemorPersisto()
        persisto.teksto = "{ NE estas JSON"
        val agordo = leguAgordonKunRezervo(leganto, { agordoTeksto("paktita") }, persisto.legu, persisto.forigu)
        assertEquals(listOf("paktita"), agordo.kanaloj.map { it.kodo })
        // La putrita teksto forgesiĝas — la sekva starto ne reprovos ĝin
        assertEquals(null, persisto.teksto)
        assertEquals(1, persisto.forigitaj)
    }

    @Test
    fun legFiaskoToleratas() {
        // Ekz. forigita kaŝo-dosierujo meze de legado — devas simple uzi la paktitan
        val agordo = leguAgordonKunRezervo(
            leganto,
            { agordoTeksto("paktita") },
            leguKonservitan = { throw IllegalStateException("nenia dosierujo") },
            foriguKonservitan = {},
        )
        assertEquals(listOf("paktita"), agordo.kanaloj.map { it.kodo })
    }

    @Test
    fun senKonservitoUziĝasLaPaktita() {
        val agordo = leguAgordonKunRezervo(leganto, { agordoTeksto("paktita") }, { null }, {})
        assertEquals(listOf("paktita"), agordo.kanaloj.map { it.kodo })
    }

    // ---------- evolua medio: la defora konfiguro tute ne uziĝas ----------

    @Test
    fun enEvoluaMedioLaPaktitaVenkasEĉKunKonservito() {
        // Debug-konstruo/emulilo: novaj kanaloj el Git devas testiĝi sen rsync —
        // la persistigita defora konfiguro (eble plurajn semajnojn malnova) IGNORIĜAS
        val persisto = MemorPersisto()
        persisto.teksto = agordoTeksto("defora")
        AppStato.estasEvoluaMedo = true
        try {
            val agordo = leguAgordonKunRezervo(leganto, { agordoTeksto("paktita") }, persisto.legu, persisto.forigu)
            assertEquals(listOf("paktita"), agordo.kanaloj.map { it.kodo })
            // La persistigita defora teksto NE tuŝiĝas (nek legita, nek forigita):
            // ĝi validos plu por produktadaj konstruoj
            assertEquals(agordoTeksto("defora"), persisto.teksto)
            assertEquals(0, persisto.forigitaj)
        } finally {
            AppStato.estasEvoluaMedo = false
        }
    }

    @Test
    fun malpermesoDeDeforaKonfiguroFunkciasAnkaŭSenEvoluaMedoFlago() {
        // La parametron oni povas doni eksplicite (ekz. estontaj iloj) —
        // sendepende de la AppStato-flago
        val persisto = MemorPersisto()
        persisto.teksto = agordoTeksto("defora")
        val agordo = leguAgordonKunRezervo(
            leganto,
            paktitaTeksto = { agordoTeksto("paktita") },
            leguKonservitan = persisto.legu,
            foriguKonservitan = persisto.forigu,
            uzuDeforanKonfiguron = false,
        )
        assertEquals(listOf("paktita"), agordo.kanaloj.map { it.kodo })
        assertEquals(0, persisto.forigitaj)
    }

    // ---------- KanaloDeponejoImpl.refreŝiguDefore ----------

    /** Kiom da fojoj la reversevoko alFreŝaAgordo ruliĝis. */
    private var reversevokoj = 0

    private fun deponejo(
        persisto: MemorPersisto,
        komenca: String,
        elŝuto: (suspend (String) -> String)?,
    ) = KanaloDeponejoImpl(
        leganto = leganto,
        komencaAgordo = leganto.legu(komenca),
        elŝutuDefore = elŝuto,
        leguKonservitan = persisto.legu,
        skribuKonservitan = persisto.skribu,
        alFreŝaAgordo = { reversevokoj++ },
    )

    @Test
    fun refreŝigoAnstataŭigasKanalojnKajPersistigas() = runTest {
        val persisto = MemorPersisto()
        val deponejo = deponejo(persisto, agordoTeksto("malnova")) { url ->
            assertEquals(KANALKONFIGURO_URL, url)
            agordoTeksto("nova")
        }
        assertEquals(listOf("malnova"), deponejo.observiKanalojn().value.map { it.slug })

        deponejo.refreŝiguDefore()

        assertEquals(listOf("nova"), deponejo.observiKanalojn().value.map { it.slug })
        assertEquals(agordoTeksto("nova"), persisto.teksto)
        assertEquals(1, reversevokoj)
        assertEquals(listOf("nova"), deponejo.getKanalojn().map { it.slug })
        assertEquals("nova", deponejo.getKanalo("nova")?.slug)
    }

    @Test
    fun ŝanĝiĝintaRespondoĜisdatigasAnkaŭKiamKomencoEstisPaktita() = runTest {
        val persisto = MemorPersisto()
        val deponejo = deponejo(persisto, agordoTeksto("paktita")) { agordoTeksto("nova") }
        deponejo.refreŝiguDefore()
        assertEquals(listOf("nova"), deponejo.observiKanalojn().value.map { it.slug })
        // La nova teksto ankaŭ persistiĝis — la sekva starto reuzos ĝin
        assertEquals(agordoTeksto("nova"), persisto.teksto)
    }

    @Test
    fun neŝanĝitaRespondoNeĜisdatigas() = runTest {
        val persisto = MemorPersisto()
        persisto.teksto = agordoTeksto("nuna")
        val deponejo = deponejo(persisto, agordoTeksto("nuna")) { agordoTeksto("nuna") }
        deponejo.refreŝiguDefore()
        assertEquals(listOf("nuna"), deponejo.observiKanalojn().value.map { it.slug })
        assertEquals(0, reversevokoj, "alFreŝaAgordo ne devis esti alvokita por sama teksto")
    }

    @Test
    fun elŝutFiaskoNeTuŝasLaNunanStaton() = runTest {
        val persisto = MemorPersisto()
        persisto.teksto = agordoTeksto("nuna")
        val deponejo = deponejo(persisto, agordoTeksto("nuna")) { throw IllegalStateException("nenia reto") }
        deponejo.refreŝiguDefore() // ne devas ĵeti
        assertEquals(listOf("nuna"), deponejo.observiKanalojn().value.map { it.slug })
        assertEquals(0, reversevokoj)
    }

    @Test
    fun jsMotorEraroKapatiĝasKielThrowable() = runTest {
        // La retumila Ktor-motoro (Js) ĵetas kotlin.Error (ne Exception) ĉe CORS —
        // kaptado de Exception ne sufiĉus (vidu ElsendoDeponejoToleremoTest).
        val persisto = MemorPersisto()
        val deponejo = deponejo(persisto, agordoTeksto("nuna")) { throw Error("Fail to fetch") }
        deponejo.refreŝiguDefore() // ne devas ĵeti
        assertEquals(listOf("nuna"), deponejo.observiKanalojn().value.map { it.slug })
    }

    @Test
    fun neParsigeblaRespondoNenionAnstataŭigas() = runTest {
        val persisto = MemorPersisto()
        persisto.teksto = agordoTeksto("nuna")
        val deponejo = deponejo(persisto, agordoTeksto("nuna")) { "<html>Eraropaĝo de la servilo</html>" }
        deponejo.refreŝiguDefore()
        assertEquals(listOf("nuna"), deponejo.observiKanalojn().value.map { it.slug })
        // La venena respondo NE anstataŭigis la persistitan version
        assertEquals(agordoTeksto("nuna"), persisto.teksto)
        assertEquals(0, reversevokoj)
    }

    @Test
    fun persistFiaskoNeBlokasLaVivanĜisdatigon() = runTest {
        // Ekz. plena disko: la kaŝo ne skribeblas, sed la kanallisto tamen ĝisdatigu
        val persisto = MemorPersisto()
        persisto.skribEraro = IllegalStateException("plena disko")
        val deponejo = deponejo(persisto, agordoTeksto("malnova")) { agordoTeksto("nova") }
        deponejo.refreŝiguDefore()
        assertEquals(listOf("nova"), deponejo.observiKanalojn().value.map { it.slug })
        assertEquals(1, reversevokoj)
    }

    @Test
    fun senElŝutiloRefreŝigoNenionFaras() = runTest {
        val persisto = MemorPersisto()
        val deponejo = deponejo(persisto, agordoTeksto("nuna"), elŝuto = null)
        deponejo.refreŝiguDefore()
        assertEquals(listOf("nuna"), deponejo.observiKanalojn().value.map { it.slug })
        assertEquals(null, persisto.teksto)
        assertTrue(reversevokoj == 0)
    }

    // ---------- Gardiloj kontraŭ fuŝaj respondoj ----------

    @Test
    fun malplenaRespondoNeĜisdatigas() = runTest {
        val persisto = MemorPersisto()
        val deponejo = deponejo(persisto, agordoTeksto("nuna")) { "   " }
        deponejo.refreŝiguDefore()
        assertEquals(listOf("nuna"), deponejo.observiKanalojn().value.map { it.slug })
        assertEquals(0, reversevokoj)
    }

    @Test
    fun konfiguroSenKanalojNeĜisdatigas() = runTest {
        val persisto = MemorPersisto()
        // Erarpaĝo aŭ duon-skribita konfiguro, HTTP 200, parseblas — sed neniom da kanaloj
        val deponejo = deponejo(persisto, agordoTeksto("nuna")) { """{"kanaloj":[]}""" }
        deponejo.refreŝiguDefore()
        assertEquals(listOf("nuna"), deponejo.observiKanalojn().value.map { it.slug })
        assertEquals(null, persisto.teksto, "Fuŝa konfiguro NE persistiĝu")
        assertEquals(0, reversevokoj)
    }
}
