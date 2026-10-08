package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Testoj de la mini-ludilbreto: elfaldo, serĉbreto, reen-/antaŭen-butonoj, laŭteco.
 *
 * Rulu per: ./gradlew :shared:desktopTest --tests "*MiniLudilbretoTesto*"
 */
@OptIn(ExperimentalTestApi::class)
class MiniLudilbretoTesto {

    private val testElsendo = Elsendo(
        id = "kernpunkto:2024-01-01",
        kanaloSlug = "kernpunkto",
        titolo = "KP204 Pigmentoj kaj koloroj en la naturo",
        priskribo = "Hodiaux ni parolas pri pigmentoj kaj koloroj en la naturon.",
        fluo = "https://kern.punkto.info/podkasto.mp3",
        dato = "2024-01-01",
        dauro = 1200,
    )

    /** Falsa ludilo kiu registras saltiAl/fiksiLauxtecon-vokojn (subtenas saltadon, kiel ExoPlayer). */
    private class TestaLudiloRegilo(initial: LudantoInformo) : LudiloRegilo {
        private val _stato = MutableStateFlow(initial)
        override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
        val saltVokoj = mutableListOf<Long>()
        val lauxtecoVokoj = mutableListOf<Float>()
        private var lauxteco = 1f
        override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {}
        override fun ludi() {}
        override fun pauxzigi() {}
        override fun halti() {}
        override fun saltiAl(pozicioMs: Long) {
            saltVokoj.add(pozicioMs)
            _stato.value = _stato.value.copy(pozicioMs = pozicioMs)
        }
        override fun fiksiLauxtecon(volumeno: Float) {
            lauxtecoVokoj.add(volumeno)
            lauxteco = volumeno
        }
        override fun leguLauxtecon(): Float = lauxteco
    }

    private fun ludilo(pozicioMs: Long = 0, dauroMs: Long = 1_200_000L) = TestaLudiloRegilo(
        LudantoInformo(
            stato = LudantoStato.Ludas,
            nunaFonto = Sonfonto.ElsendoFonto(testElsendo),
            pozicioMs = pozicioMs,
            dauroMs = dauroMs,
            estasRekta = false,
        )
    )

    @Test
    fun sercxbreto_mankas_enfoldigite() = runComposeUiTest {
        val ludilo = ludilo(pozicioMs = 0)
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo) // komence enfoldigita
            }
        }
        waitForIdle()

        onNodeWithTag("sercxbreto").assertDoesNotExist()
        onNodeWithContentDescription("Elfaldigi la ludilon").assertExists()
    }

    @Test
    fun sercxbreto_saltas_al_elektita_pozicio() = runComposeUiTest {
        val dauroMs = 1_200_000L
        val ludilo = ludilo(pozicioMs = 0, dauroMs = dauroMs)
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, komenceElfaldita = true)
            }
        }
        waitForIdle()

        // Ŝovu la serĉbreton al 25% de la elsendo
        onNodeWithTag("sercxbreto")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        waitForIdle()

        assertEquals(1, ludilo.saltVokoj.size, "saltiAl devas esti vokita unufoje")
        val atendaPozicio = dauroMs / 4
        assertTrue(
            abs(ludilo.saltVokoj[0] - atendaPozicio) < dauroMs / 100,
            "saltiAl devas celi 25% (${atendaPozicio} ms), estis ${ludilo.saltVokoj[0]} ms",
        )
    }

    @Test
    fun sercxbreto_mankas_por_rekta_elsendo() = runComposeUiTest {
        val ludilo = TestaLudiloRegilo(
            LudantoInformo(
                stato = LudantoStato.Ludas,
                nunaFonto = Sonfonto.RektaKanalo(
                    Kanalo(
                        slug = "muzaiko",
                        nomo = "Muzaiko",
                        rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8",
                    )
                ),
                estasRekta = true,
            )
        )
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, komenceElfaldita = true)
            }
        }
        waitForIdle()

        onNodeWithTag("sercxbreto").assertDoesNotExist()
        onNodeWithContentDescription("Reen 5%").assertDoesNotExist()
        onNodeWithContentDescription("Antaŭen 5%").assertDoesNotExist()
        // La laŭteco-regilo funkcias ankaŭ ĉe rekta elsendo
        onNodeWithTag("lauxteco").assertExists()
    }

    @Test
    fun sago_elfaldas_kaj_enfoldigas() = runComposeUiTest {
        val ludilo = ludilo()
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo)
            }
        }
        waitForIdle()

        onNodeWithTag("sercxbreto").assertDoesNotExist()
        onNodeWithContentDescription("Elfaldigi la ludilon").performClick()
        waitForIdle()
        onNodeWithTag("sercxbreto").assertExists()

        onNodeWithContentDescription("Enfoldigi la ludilon").performClick()
        waitForIdle()
        onNodeWithTag("sercxbreto").assertDoesNotExist()
    }

    @Test
    fun saltbutonoj_saltas_5procentojn() = runComposeUiTest {
        val dauroMs = 1_200_000L
        val ludilo = ludilo(pozicioMs = 600_000, dauroMs = dauroMs) // 50%
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, komenceElfaldita = true)
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Reen 5%").performClick()
        onNodeWithContentDescription("Antaŭen 5%").performClick()
        waitForIdle()

        assertEquals(2, ludilo.saltVokoj.size)
        val kvinProcentoj = dauroMs / 20
        assertEquals(600_000 - kvinProcentoj, ludilo.saltVokoj[0], "reen devas salti 5% de la dauro")
        assertEquals(600_000, ludilo.saltVokoj[1], "antaŭen devas resalti al la origina pozicio")
    }

    @Test
    fun elfoldighas_glate() = runComposeUiTest {
        val ludilo = ludilo()
        setContent {
            MaterialTheme {
                // Malsupra alcentrigo: la pozicio de la breto moviĝas kiam ĝia alteco ŝanĝiĝas
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                    MiniLudilbreto(ludilo = ludilo)
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false

        val yEnfoldigita = onNodeWithTag("ludilbreto").fetchSemanticsNode().positionInRoot.y
        onNodeWithContentDescription("Elfaldigi la ludilon").performClick()
        mainClock.advanceTimeBy(100) // mezo de la 250-ms tween
        val yMeza = onNodeWithTag("ludilbreto").fetchSemanticsNode().positionInRoot.y
        mainClock.advanceTimeBy(400) // preter la fino de la animacio
        val yElfaldigita = onNodeWithTag("ludilbreto").fetchSemanticsNode().positionInRoot.y

        assertTrue(
            yMeza < yEnfoldigita && yElfaldigita < yMeza,
            "la supra rando de la breto devas supreniri glate: " +
                "$yEnfoldigita → $yMeza → $yElfaldigita",
        )
    }

    @Test
    fun lauxtecobreto_fiksas_lauxtecon() = runComposeUiTest {
        val ludilo = ludilo()
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, komenceElfaldita = true)
            }
        }
        waitForIdle()

        onNodeWithTag("lauxteco")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.4f) }
        waitForIdle()

        assertEquals(1, ludilo.lauxtecoVokoj.size, "fiksiLauxtecon devas esti vokita unufoje")
        assertTrue(
            abs(ludilo.lauxtecoVokoj[0] - 0.4f) < 0.05f,
            "fiksiLauxtecon devas ricevi ~0,4, estis ${ludilo.lauxtecoVokoj[0]}",
        )
    }
}
