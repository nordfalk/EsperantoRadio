package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo
import dk.nordfalk.esperanto.data.repository.MemorAlarmoDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * Naviga UI-testo kiu testas ĉiujn ekranojn.
 *
 * Rulu per: ./gradlew :shared:desktopTest --tests "*NavigaTesto*"
 */
@OptIn(ExperimentalTestApi::class)
class NavigaTesto {

    private val testKanaloj = listOf(
        Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"),
        Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"),
        Kanalo(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://www.podkasto.net/feed/"),
    )

    private val testElsendo = Elsendo(
        id = "test:2024-01-01",
        kanaloSlug = "kernpunkto",
        titolo = "KP204 Pigmentoj",
        priskribo = "Hodiaŭ ni parolas pri pigmentoj kaj koloroj en la naturon.",
        fluo = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
        dato = "2024-01-01",
        daŭro = 6916,
    )

    private fun falsaKanaloDeponejo(kanaloj: List<Kanalo> = testKanaloj) = object : KanaloDeponejo {
        private val _kanaloj = MutableStateFlow(kanaloj)
        override fun observiKanalojn() = _kanaloj.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = _kanaloj.value
        override suspend fun getKanalo(slug: String) = _kanaloj.value.find { it.slug == slug }
    }

    @Test
    fun montrasKanalaron() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanaloDeponejo())
        setContent { KanalaroEkrano(viewModel = viewModel) }
        waitForIdle()
        onNodeWithText("Muzaiko").assertIsDisplayed()
        onNodeWithText("Kernpunkto").assertIsDisplayed()
        onNodeWithText("Rekta elsendo").assertIsDisplayed()
        onAllNodesWithText("Podkasto").assertCountEquals(2)
    }

    @Test
    fun montrasElsendoDetalon() = runComposeUiTest {
        setContent {
            ElsendoEkrano(elsendo = testElsendo, onReen = {}, onLudi = {}, onElŝuti = {})
        }
        waitForIdle()
        onAllNodesWithText("KP204 Pigmentoj").assertCountEquals(2)
    }

    @Test
    fun elsendoEkranoMontrasPaŭzonSeLudas() = runComposeUiTest {
        val ludilo = NavigaTestLudiloRegilo(
            LudantoInformo(
                stato = LudantoStato.Ludas,
                nunaFonto = Sonfonto.ElsendoFonto(testElsendo),
                pozicioMs = 30000, daŭroMs = 6916000, estasRekta = false,
            )
        )
        setContent {
            ElsendoEkrano(elsendo = testElsendo, onReen = {}, onLudi = {}, ludilo = ludilo)
        }
        waitForIdle()
        // La ludo-butono ŝanĝiĝis al paŭzo
        onNodeWithText(" Paŭzigi").assertIsDisplayed()
    }

    @Test
    fun elsendoEkranoMontrasDaŭrigiSePaŭzita() = runComposeUiTest {
        val ludilo = NavigaTestLudiloRegilo(
            LudantoInformo(
                stato = LudantoStato.Haltita,
                nunaFonto = Sonfonto.ElsendoFonto(testElsendo),
                pozicioMs = 30000, daŭroMs = 6916000, estasRekta = false,
            )
        )
        setContent {
            ElsendoEkrano(elsendo = testElsendo, onReen = {}, onLudi = {}, ludilo = ludilo)
        }
        waitForIdle()
        // La ludo-butono montras "Daŭrigi" ĉar la elsendo estas paŭzita
        onNodeWithText(" Daŭrigi").assertIsDisplayed()
    }

    @Test
    fun montrasSerĉon() = runComposeUiTest {
        val serĉoDeponejo = object : dk.nordfalk.esperanto.domain.repository.SerĉoDeponejo {
            override suspend fun serĉi(teksto: String, limo: Int) =
                if (teksto.length >= 2) dk.nordfalk.esperanto.domain.repository.SerĉoRezulto(elsendoj = listOf(testElsendo)) else dk.nordfalk.esperanto.domain.repository.SerĉoRezulto()
        }
        setContent { SerĉoEkrano(serĉoDeponejo = serĉoDeponejo, onElsendo = {}) }
        waitForIdle()
        onNodeWithText("Serĉi").assertIsDisplayed()
    }

    @Test
    fun montrasPlejŝatatajn() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            private val _set = MutableStateFlow(setOf("muzaiko"))
            override fun observiPlejŝatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {
                _set.value = if (kanaloSlug in _set.value) _set.value - kanaloSlug else _set.value + kanaloSlug
            }
            override suspend fun estasPlejŝatata(kanaloSlug: String) = kanaloSlug in _set.value
        }
        setContent {
            PlejŝatatajEkrano(plejŝatatajDeponejo = plejDeponejo, kanaloDeponejo = falsaKanaloDeponejo(), onKanalo = {})
        }
        waitForIdle()
        onNodeWithText("Muzaiko").assertIsDisplayed()
    }

    @Test
    fun plejŝatatajMontrasSciiganKlarigon() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            private val _set = MutableStateFlow(setOf("muzaiko"))
            override fun observiPlejŝatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
            override suspend fun estasPlejŝatata(kanaloSlug: String) = kanaloSlug in _set.value
        }
        setContent {
            PlejŝatatajEkrano(plejŝatatajDeponejo = plejDeponejo, kanaloDeponejo = falsaKanaloDeponejo(), onKanalo = {})
        }
        waitForIdle()
        onNodeWithText("Vi ricevos sciigon kiam aperas nova elsendo el ŝatata kanalo.").assertIsDisplayed()
    }

    @Test
    fun plejŝatatajMalplenaNeMontrasKlarigon() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            private val _set = MutableStateFlow<Set<String>>(emptySet())
            override fun observiPlejŝatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
            override suspend fun estasPlejŝatata(kanaloSlug: String) = false
        }
        setContent {
            PlejŝatatajEkrano(plejŝatatajDeponejo = plejDeponejo, kanaloDeponejo = falsaKanaloDeponejo(), onKanalo = {})
        }
        waitForIdle()
        onNodeWithText("Neniu plej ŝatata kanalo. Premu la koron sur kanalo por aldoni.").assertIsDisplayed()
    }

    @Test
    fun kanalEkranoMontrasŜatButononNeŜatata() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            private val _set = MutableStateFlow<Set<String>>(emptySet())
            override fun observiPlejŝatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
            override suspend fun estasPlejŝatata(kanaloSlug: String) = false
        }
        val elsendoDeponejo = PreviewElsendoDeponejo(listOf(testElsendo))
        setContent {
            KanaloEkrano(
                kanalo = testKanaloj[1],
                elsendoDeponejo = elsendoDeponejo,
                plejŝatatajDeponejo = plejDeponejo,
                agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl(),
                onReen = {},
            )
        }
        waitForIdle()
        // ♡ = ne ŝatata
        onNodeWithContentDescription("Aldoni ŝaton").assertIsDisplayed()
    }

    @Test
    fun kanalEkranoMontrasŜatButononŜatata() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            private val _set = MutableStateFlow(setOf("kernpunkto"))
            override fun observiPlejŝatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
            override suspend fun estasPlejŝatata(kanaloSlug: String) = kanaloSlug in _set.value
        }
        val elsendoDeponejo = PreviewElsendoDeponejo(listOf(testElsendo))
        setContent {
            KanaloEkrano(
                kanalo = testKanaloj[1],
                elsendoDeponejo = elsendoDeponejo,
                plejŝatatajDeponejo = plejDeponejo,
                agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl(),
                onReen = {},
            )
        }
        waitForIdle()
        // ♥ = ŝatata
        onNodeWithContentDescription("Forigi ŝaton").assertIsDisplayed()
    }

    @Test
    fun kanalEkranoMontrasKrudanAgordonEnEvoluaReĝimo() = runComposeUiTest {
        val agordoj = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl()
        agordoj.fiksiEvoluon(true)
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            override fun observiPlejŝatatajn() = MutableStateFlow(emptySet<String>()).asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
            override suspend fun estasPlejŝatata(kanaloSlug: String) = false
        }
        // Vera kodo el la pakita konfiguro — la kruda ero estas legata el ĝi
        val kanalo = dk.nordfalk.esperanto.domain.model.Kanalo(slug = "polaretradio", nomo = "Pola Retradio")
        setContent {
            KanaloEkrano(
                kanalo = kanalo,
                elsendoDeponejo = PreviewElsendoDeponejo(emptyList()),
                plejŝatatajDeponejo = plejDeponejo,
                agordojDeponejo = agordoj,
                onReen = {},
            )
        }
        waitForIdle()
        // La agord-datumoj de la kanalo (1-linia, elfaldebla)
        onNodeWithText("Kanalo(slug=polaretradio", substring = true).assertIsDisplayed()
        onNodeWithText("transsaltuKomenceSekundojn=[]", substring = true).assertIsDisplayed()
    }

    @Test
    fun kanalEkranoNeMontrasKrudanAgordonSenEvoluaReĝimo() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            override fun observiPlejŝatatajn() = MutableStateFlow(emptySet<String>()).asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
            override suspend fun estasPlejŝatata(kanaloSlug: String) = false
        }
        val kanalo = dk.nordfalk.esperanto.domain.model.Kanalo(slug = "polaretradio", nomo = "Pola Retradio")
        setContent {
            KanaloEkrano(
                kanalo = kanalo,
                elsendoDeponejo = PreviewElsendoDeponejo(emptyList()),
                plejŝatatajDeponejo = plejDeponejo,
                agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl(),
                onReen = {},
            )
        }
        waitForIdle()
        onAllNodesWithText("Kanalo(slug=polaretradio", substring = true).assertCountEquals(0)
    }

    @Test
    fun kanalEkranoSnackbarNavigasAlAgordoj() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            private val _set = MutableStateFlow<Set<String>>(emptySet())
            override fun observiPlejŝatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {
                _set.value = _set.value + kanaloSlug
            }
            override suspend fun estasPlejŝatata(kanaloSlug: String) = kanaloSlug in _set.value
        }
        val elsendoDeponejo = PreviewElsendoDeponejo(listOf(testElsendo))
        var navigisAlAgordoj = false
        setContent {
            KanaloEkrano(
                kanalo = testKanaloj[1],
                elsendoDeponejo = elsendoDeponejo,
                plejŝatatajDeponejo = plejDeponejo,
                agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl(),
                onReen = {},
                onAgordoj = { navigisAlAgordoj = true },
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Aldoni ŝaton").performClick()
        waitForIdle()
        onNodeWithText("Malŝalti en la agordoj").assertIsDisplayed()
        onNodeWithText("Malŝalti en la agordoj").performClick()
        waitForIdle()
        kotlin.test.assertTrue(navigisAlAgordoj)
    }

    @Test
    fun agordojNeMontrasSciigojnSurDesktop() = runComposeUiTest {
        val agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl()
        setContent { AgordojEkrano(agordojDeponejo = agordojDeponejo, onReen = {}) }
        waitForIdle()
        // Sur Desktop sciigoj ne estas subtenataj, do la sekcio ne devas aperi
        if (!dk.nordfalk.esperanto.data.repository.subtenasSciigojn) {
            org.junit.Assert.assertEquals(0, onAllNodesWithText("Ricevi sciigojn").fetchSemanticsNodes().size)
        }
    }

    @Test
    fun montrasElŝutitajn() = runComposeUiTest {
        val elŝutDeponejo = object : ElŝutDeponejo {
            private val _elŝutoj = MutableStateFlow(
                mapOf(testElsendo.id to ElŝutitaElsendo(testElsendo, "/tmp/test.mp3", ElŝutStato.Preta))
            )
            override fun observiElŝutojn() = _elŝutoj.asStateFlow()
            override fun observiElŝutStaton(elsendoId: String) = MutableStateFlow(ElŝutStato.Preta).asStateFlow()
            override suspend fun elŝuti(elsendo: Elsendo) {}
            override suspend fun haltigi(elsendoId: String) {}
            override suspend fun forigi(elsendoId: String) { _elŝutoj.value = _elŝutoj.value - elsendoId }
            override suspend fun getLokaDosieroVojo(elsendoId: String) = "/tmp/test.mp3"
            override fun estasElŝutita(elsendoId: String) = true
        }
        setContent { ElŝutitajEkrano(elŝutDeponejo = elŝutDeponejo, ludilo = NoOpLudiloRegilo(), onReen = {}, onLudi = {}, onElsendo = {}) }
        waitForIdle()
        onNodeWithText("KP204 Pigmentoj").assertIsDisplayed()
        onNodeWithText("Preta").assertIsDisplayed()
    }

    @Test
    fun montrasAlarmojn() = runComposeUiTest {
        val alarmoDeponejo = MemorAlarmoDeponejo(
            listOf(dk.nordfalk.esperanto.domain.model.Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanaloSlug = "muzaiko", aktiva = true, etikedo = "Matene"))
        )
        setContent {
            AlarmoEkrano(alarmoDeponejo = alarmoDeponejo, kanaloDeponejo = falsaKanaloDeponejo(), onReen = {})
        }
        waitForIdle()
        onNodeWithText("06:45").assertIsDisplayed()
        onNodeWithText("Ĉiutage").assertIsDisplayed()
    }

    @Test
    fun montrasAgordojn() = runComposeUiTest {
        val agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl()
        setContent { AgordojEkrano(agordojDeponejo = agordojDeponejo, onReen = {}) }
        waitForIdle()
        onNodeWithText("Agordoj").assertIsDisplayed()
        onNodeWithText("Ludado").assertIsDisplayed()
        // La sekcio „Temo” povas esti sub la faldebla rando — volu al ĝi
        onNodeWithText("Temo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun montrasMiniLudilbreton() = runComposeUiTest {
        val ludilo = NavigaTestLudiloRegilo(
            LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.ElsendoFonto(testElsendo), pozicioMs = 30000, daŭroMs = 6916000, estasRekta = false)
        )
        setContent { MiniLudilbreto(ludilo = ludilo) }
        waitForIdle()
        onNodeWithText("KP204 Pigmentoj").assertIsDisplayed()
        onNodeWithText("Ludas").assertIsDisplayed()
    }
}

internal class NavigaTestLudiloRegilo(initial: LudantoInformo = LudantoInformo(stato = LudantoStato.Haltita)) : LudiloRegilo {
    private val _stato = MutableStateFlow(initial)
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
    override val laŭteco: StateFlow<Float> = MutableStateFlow(1f)
    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = fonto, pozicioMs = komencoPozicioMs, estasRekta = fonto is Sonfonto.RektaKanalo)
    }
    override fun ludi() { _stato.value = _stato.value.copy(stato = LudantoStato.Ludas) }
    override fun paŭzigi() { _stato.value = _stato.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { _stato.value = _stato.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLaŭtecon(volumeno: Float) {}
}
