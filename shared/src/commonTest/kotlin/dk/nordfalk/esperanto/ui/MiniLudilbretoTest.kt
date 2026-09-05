package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MiniLudilbretoTest {

    @Test
    fun neMontrigxasKiamNenioLudigxas() = runComposeUiTest {
        val ludilo = NoOpLudiloRegilo()
        setContent { MiniLudilbreto(ludilo = ludilo) }
        waitForIdle()
        // Neniu fonto → la breto ne montriĝas → neniu teksto
        onAllNodesWithText("Ludas").assertCountEquals(0)
    }

    @Test
    fun montrasTitolonKajStaton() = runComposeUiTest {
        val elsendo = Elsendo(
            id = "test:2024-01-01",
            kanalSlug = "test",
            titolo = "Mia podkasto",
            stream = "https://x.com/a.mp3",
            dato = "2024-01-01"
        )
        val ludilo = TestLudiloRegilo(LudantoInformo(
            stato = LudantoStato.Ludas,
            nunaFonto = Sonfonto.ElsendoFonto(elsendo),
            pozicioMs = 0,
            dauroMs = 60000,
            estasRekta = false
        ))

        setContent { MiniLudilbreto(ludilo = ludilo) }
        waitForIdle()

        onNodeWithText("Mia podkasto").assertIsDisplayed()
        onNodeWithText("Ludas").assertIsDisplayed()
    }

    @Test
    fun montrasRektaElsendoTeksto() = runComposeUiTest {
        val kanal = dk.nordfalk.esperanto.domain.model.Kanal(
            slug = "muzaiko",
            nomo = "Muzaiko",
            rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"
        )
        val ludilo = TestLudiloRegilo(LudantoInformo(
            stato = LudantoStato.Ludas,
            nunaFonto = Sonfonto.RektaKanalo(kanal),
            pozicioMs = 0,
            dauroMs = 0,
            estasRekta = true
        ))

        setContent { MiniLudilbreto(ludilo = ludilo) }
        waitForIdle()

        onNodeWithText("Muzaiko").assertIsDisplayed()
        onNodeWithText("Rekta elsendo").assertIsDisplayed()
    }

    @Test
    fun montrasLokaElsendonTitolon() = runComposeUiTest {
        val elsendo = Elsendo(
            id = "test:2024-01-01",
            kanalSlug = "test",
            titolo = "Eksterreta podkasto",
            stream = "https://x.com/a.mp3",
            dato = "2024-01-01"
        )
        val ludilo = TestLudiloRegilo(LudantoInformo(
            stato = LudantoStato.Ludas,
            nunaFonto = Sonfonto.LokaElsendo(elsendo, "/tmp/test.mp3"),
            pozicioMs = 0,
            dauroMs = 60000,
            estasRekta = false
        ))

        setContent { MiniLudilbreto(ludilo = ludilo) }
        waitForIdle()

        onNodeWithText("Eksterreta podkasto").assertIsDisplayed()
        onNodeWithText("Ludas").assertIsDisplayed()
    }
}

/** Test-ludilo kun antaŭdifinita stato. */
private class TestLudiloRegilo(initial: LudantoInformo) : LudiloRegilo {
    private val _stato = MutableStateFlow(initial)
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        _stato.value = LudantoInformo(
            stato = LudantoStato.Haltita,
            nunaFonto = fonto,
            pozicioMs = komencoPozicioMs,
            estasRekta = fonto is Sonfonto.RektaKanalo
        )
    }
    override fun ludi() { _stato.value = _stato.value.copy(stato = LudantoStato.Ludas) }
    override fun pauxzigi() { _stato.value = _stato.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { _stato.value = _stato.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLauxtecon(volumeno: Float) {}
}
