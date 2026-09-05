package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.data.repository.MemorAlarmoDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * Naviga UI-testo kiu montras cxiujn ekranojn.
 *
 * Rulu per: ./gradlew :shared:desktopTest --tests "*NavigaTesto*"
 */
@OptIn(ExperimentalTestApi::class)
class NavigaTesto {

    private val testKanaloj = listOf(
        Kanal(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"),
        Kanal(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"),
        Kanal(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://www.podkasto.net/feed/"),
    )

    private val testElsendo = Elsendo(
        id = "test:2024-01-01",
        kanalSlug = "kernpunkto",
        titolo = "KP204 Pigmentoj",
        priskribo = "Hodiaux ni parolas pri pigmentoj kaj koloroj en la naturon.",
        stream = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
        dato = "2024-01-01",
        dauro = 6916,
    )

    private fun falsaKanalDeponejo(kanaloj: List<Kanal> = testKanaloj) = object : KanalDeponejo {
        private val _kanaloj = MutableStateFlow(kanaloj)
        override fun observiKanalojn() = _kanaloj.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = _kanaloj.value
        override suspend fun getKanal(slug: String) = _kanaloj.value.find { it.slug == slug }
    }

    @Test
    fun montrasKanalaron() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanalDeponejo())
        setContent { KanalaroEkrano(viewModel = viewModel) }
        waitForIdle()
        onNodeWithText("Muzaiko").assertIsDisplayed()
        onNodeWithText("Kernpunkto").assertIsDisplayed()
        onNodeWithText("Rekta elsendo").assertIsDisplayed()
        onAllNodesWithText("Podkasto").assertCountEquals(2)
        Thread.sleep(500)
    }

    @Test
    fun montrasElsendoDetalon() = runComposeUiTest {
        setContent {
            ElsendoEkrano(elsendo = testElsendo, onReen = {}, onLudi = {}, onElshuti = {})
        }
        waitForIdle()
        onAllNodesWithText("KP204 Pigmentoj").assertCountEquals(2)
        Thread.sleep(500)
    }

    @Test
    fun montrasSerchon() = runComposeUiTest {
        val sercxoDeponejo = object : dk.nordfalk.esperanto.domain.repository.SercxoDeponejo {
            override suspend fun sercxi(taxto: String, limo: Int) =
                if (taxto.length >= 2) listOf(testElsendo) else emptyList()
        }
        setContent { SercxoEkrano(sercxoDeponejo = sercxoDeponejo, onReen = {}, onElsendo = {}) }
        waitForIdle()
        onNodeWithText("Serĉi").assertIsDisplayed()
        Thread.sleep(500)
    }

    @Test
    fun montrasPlejsatatajn() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo {
            private val _set = MutableStateFlow(setOf("muzaiko"))
            override fun observiPlejsatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejsaton(kanalSlug: String) {
                _set.value = if (kanalSlug in _set.value) _set.value - kanalSlug else _set.value + kanalSlug
            }
            override suspend fun estasPlejsatata(kanalSlug: String) = kanalSlug in _set.value
        }
        setContent {
            PlejsatatajEkrano(plejsatatajDeponejo = plejDeponejo, kanalDeponejo = falsaKanalDeponejo(), onReen = {}, onKanal = {})
        }
        waitForIdle()
        onNodeWithText("Muzaiko").assertIsDisplayed()
        Thread.sleep(500)
    }

    @Test
    fun montrasElshutitajn() = runComposeUiTest {
        val elshutDeponejo = object : ElshutDeponejo {
            private val _elshutoj = MutableStateFlow(
                mapOf(testElsendo.id to ElshutitaElsendo(testElsendo, "/tmp/test.mp3", ElshutStato.Preta))
            )
            override fun observiElshutojn() = _elshutoj.asStateFlow()
            override fun observiElshutStaton(elsendoId: String) = MutableStateFlow(ElshutStato.Preta).asStateFlow()
            override suspend fun elshuti(elsendo: Elsendo) {}
            override suspend fun haltigi(elsendoId: String) {}
            override suspend fun forigi(elsendoId: String) { _elshutoj.value = _elshutoj.value - elsendoId }
            override suspend fun getLokaDosieroVojo(elsendoId: String) = "/tmp/test.mp3"
            override fun estasElshutita(elsendoId: String) = true
        }
        setContent { ElshutitajEkrano(elshutDeponejo = elshutDeponejo, onReen = {}, onLudi = {}, onElsendo = {}) }
        waitForIdle()
        onNodeWithText("KP204 Pigmentoj").assertIsDisplayed()
        onNodeWithText("Preta").assertIsDisplayed()
        Thread.sleep(500)
    }

    @Test
    fun montrasAlarmojn() = runComposeUiTest {
        val alarmoDeponejo = MemorAlarmoDeponejo(
            listOf(dk.nordfalk.esperanto.domain.model.Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanalSlug = "muzaiko", aktiva = true, etikedo = "Matene"))
        )
        setContent {
            AlarmoEkrano(alarmoDeponejo = alarmoDeponejo, kanalDeponejo = falsaKanalDeponejo(), onReen = {})
        }
        waitForIdle()
        onNodeWithText("06:45").assertIsDisplayed()
        onNodeWithText("Cxiutage").assertIsDisplayed()
        Thread.sleep(500)
    }

    @Test
    fun montrasAgordojn() = runComposeUiTest {
        val agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl()
        setContent { AgordojEkrano(agordojDeponejo = agordojDeponejo, onReen = {}) }
        waitForIdle()
        onNodeWithText("Agordoj").assertIsDisplayed()
        onNodeWithText("Lingvo").assertIsDisplayed()
        onNodeWithText("Esperanto").assertIsDisplayed()
        Thread.sleep(500)
    }

    @Test
    fun montrasMiniLudilbreton() = runComposeUiTest {
        val ludilo = NavigaTestLudiloRegilo(
            LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.ElsendoFonto(testElsendo), pozicioMs = 30000, dauroMs = 6916000, estasRekta = false)
        )
        setContent { MiniLudilbreto(ludilo = ludilo) }
        waitForIdle()
        onNodeWithText("KP204 Pigmentoj").assertIsDisplayed()
        onNodeWithText("Ludas").assertIsDisplayed()
        Thread.sleep(500)
    }
}

internal class NavigaTestLudiloRegilo(initial: LudantoInformo = LudantoInformo(stato = LudantoStato.Haltita)) : LudiloRegilo {
    private val _stato = MutableStateFlow(initial)
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = fonto, pozicioMs = komencoPozicioMs, estasRekta = fonto is Sonfonto.RektaKanalo)
    }
    override fun ludi() { _stato.value = _stato.value.copy(stato = LudantoStato.Ludas) }
    override fun pauxzigi() { _stato.value = _stato.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { _stato.value = _stato.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLauxtecon(volumeno: Float) {}
}
