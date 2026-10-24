package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
        priskribo = "Hodiaŭ ni parolas pri pigmentoj kaj koloroj en la naturon.",
        fluo = "https://kern.punkto.info/podkasto.mp3",
        dato = "2024-01-01",
        daŭro = 1200,
    )

    /** Falsa ludilo kiu registras saltiAl/fiksiLaŭtecon-vokojn (subtenas saltadon, kiel ExoPlayer). */
    private class TestaLudiloRegilo(initial: LudantoInformo) : LudiloRegilo {
        private val _stato = MutableStateFlow(initial)
        override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
        val saltVokoj = mutableListOf<Long>()
        val laŭtecoVokoj = mutableListOf<Float>()
        private val _laŭteco = MutableStateFlow(1f)
        override val laŭteco: StateFlow<Float> = _laŭteco.asStateFlow()
        override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {}
        override fun ludi() {}
        override fun paŭzigi() {}
        override fun halti() {}
        override fun saltiAl(pozicioMs: Long) {
            saltVokoj.add(pozicioMs)
            _stato.value = _stato.value.copy(pozicioMs = pozicioMs)
        }
        override fun fiksiLaŭtecon(volumeno: Float) {
            laŭtecoVokoj.add(volumeno)
            _laŭteco.value = volumeno
        }

        /** Simulas eksteran laŭteco-ŝanĝon (ekz. hardvaran klavon). */
        fun simuluEksteranLaŭtecon(volumeno: Float) { _laŭteco.value = volumeno }
    }

    private fun ludilo(pozicioMs: Long = 0, daŭroMs: Long = 1_200_000L) = TestaLudiloRegilo(
        LudantoInformo(
            stato = LudantoStato.Ludas,
            nunaFonto = Sonfonto.ElsendoFonto(testElsendo),
            pozicioMs = pozicioMs,
            daŭroMs = daŭroMs,
            estasRekta = false,
        )
    )

    @Test
    fun serĉbreto_mankas_enfoldigite() = runComposeUiTest {
        val ludilo = ludilo(pozicioMs = 0)
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo) // komence enfoldigita
            }
        }
        waitForIdle()

        onNodeWithTag("serĉbreto").assertDoesNotExist()
        onNodeWithContentDescription("Elfaldigi la ludilon").assertExists()
    }

    @Test
    fun serĉbreto_saltas_al_elektita_pozicio() = runComposeUiTest {
        val daŭroMs = 1_200_000L
        val ludilo = ludilo(pozicioMs = 0, daŭroMs = daŭroMs)
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()

        // Ŝovu la serĉbreton al 25% de la elsendo
        onNodeWithTag("serĉbreto")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        waitForIdle()

        assertEquals(1, ludilo.saltVokoj.size, "saltiAl devas esti vokita unufoje")
        val atendaPozicio = daŭroMs / 4
        assertTrue(
            abs(ludilo.saltVokoj[0] - atendaPozicio) < daŭroMs / 100,
            "saltiAl devas celi 25% (${atendaPozicio} ms), estis ${ludilo.saltVokoj[0]} ms",
        )
    }

    @Test
    fun serĉbreto_tuŝetas_por_salti() = runComposeUiTest {
        val daŭroMs = 1_200_000L
        val ludilo = ludilo(pozicioMs = 0, daŭroMs = daŭroMs)
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()

        // Tuŝeto je 50% de la larĝo
        onNodeWithTag("serĉbreto").performTouchInput {
            click(percentOffset(0.5f, 0.5f))
        }
        waitForIdle()

        assertEquals(1, ludilo.saltVokoj.size, "saltiAl devas esti vokita unufoje post la tuŝeto")
        val atendaPozicio = daŭroMs / 2
        assertTrue(
            abs(ludilo.saltVokoj[0] - atendaPozicio) < daŭroMs / 100,
            "saltiAl devas celi ~50% (${atendaPozicio} ms), estis ${ludilo.saltVokoj[0]} ms",
        )
    }

    @Test
    fun serĉbreto_ŝovas_per_treno() = runComposeUiTest {
        val daŭroMs = 1_200_000L
        val ludilo = ludilo(pozicioMs = 0, daŭroMs = daŭroMs)
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()

        // Treno sur la serĉbreto de 75% ĝis 50% de la larĝo. La norma Slider finas
        // la valoron kelkajn rastrumerojn (tuŝ-slop) post la fingro, do tolero 3%.
        onNodeWithTag("serĉbreto").performTouchInput {
            swipe(percentOffset(0.75f, 0.5f), percentOffset(0.5f, 0.5f))
        }
        waitForIdle()

        assertEquals(1, ludilo.saltVokoj.size, "saltiAl devas esti vokita unufoje post la treno")
        val atendaPozicio = daŭroMs / 2
        assertTrue(
            abs(ludilo.saltVokoj[0] - atendaPozicio) < daŭroMs * 3 / 100,
            "saltiAl devas celi ~50% (${atendaPozicio} ms), estis ${ludilo.saltVokoj[0]} ms",
        )
    }

    @Test
    fun serĉbreto_mankas_por_rekta_elsendo() = runComposeUiTest {
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
                MiniLudilbreto(ludilo = ludilo, elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()

        onNodeWithTag("serĉbreto").assertDoesNotExist()
        onNodeWithContentDescription("Reen 5%").assertDoesNotExist()
        onNodeWithContentDescription("Antaŭen 5%").assertDoesNotExist()
        // La laŭteco-regilo funkcias ankaŭ ĉe rekta elsendo
        onNodeWithTag("laŭteco").assertExists()
    }

    @Test
    fun sago_elfaldas_kaj_enfoldigas() = runComposeUiTest {
        val ludilo = ludilo()
        setContent {
            MaterialTheme {
                var elfaldita by remember { mutableStateOf(false) }
                MiniLudilbreto(ludilo = ludilo, elfaldita = elfaldita, onElfalditaŜanĝo = { elfaldita = it })
            }
        }
        waitForIdle()

        onNodeWithTag("serĉbreto").assertDoesNotExist()
        onNodeWithContentDescription("Elfaldigi la ludilon").performClick()
        waitForIdle()
        onNodeWithTag("serĉbreto").assertExists()

        onNodeWithContentDescription("Enfoldigi la ludilon").performClick()
        waitForIdle()
        onNodeWithTag("serĉbreto").assertDoesNotExist()
    }

    @Test
    fun saltbutonoj_saltas_5procentojn() = runComposeUiTest {
        val daŭroMs = 1_200_000L
        val ludilo = ludilo(pozicioMs = 600_000, daŭroMs = daŭroMs) // 50%
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Reen 5%").performClick()
        onNodeWithContentDescription("Antaŭen 5%").performClick()
        waitForIdle()

        assertEquals(2, ludilo.saltVokoj.size)
        val kvinProcentoj = daŭroMs / 20
        assertEquals(600_000 - kvinProcentoj, ludilo.saltVokoj[0], "reen devas salti 5% de la daŭro")
        assertEquals(600_000, ludilo.saltVokoj[1], "antaŭen devas resalti al la origina pozicio")
    }

    @Test
    fun elfoldiĝas_glate() = runComposeUiTest {
        val ludilo = ludilo()
        setContent {
            MaterialTheme {
                var elfaldita by remember { mutableStateOf(false) }
                // Malsupra alcentrigo: la pozicio de la breto moviĝas kiam ĝia alteco ŝanĝiĝas
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                    MiniLudilbreto(ludilo = ludilo, elfaldita = elfaldita, onElfalditaŜanĝo = { elfaldita = it })
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
    fun laŭtecobreto_fiksas_laŭtecon() = runComposeUiTest {
        val ludilo = ludilo()
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()

        onNodeWithTag("laŭteco")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.4f) }
        waitForIdle()

        assertEquals(1, ludilo.laŭtecoVokoj.size, "fiksiLaŭtecon devas esti vokita unufoje")
        assertTrue(
            abs(ludilo.laŭtecoVokoj[0] - 0.4f) < 0.05f,
            "fiksiLaŭtecon devas ricevi ~0,4, estis ${ludilo.laŭtecoVokoj[0]}",
        )
    }

    @Test
    fun laŭtecobreto_sekvas_eksterajn_ŝanĝojn() = runComposeUiTest {
        val ludilo = ludilo()
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo, elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()

        // La montrilo komenciĝas je la defaŭlta laŭteco (1)
        assertEquals(
            1f,
            onNodeWithTag("laŭteco").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current,
        )

        // Ekstera ŝanĝo (ekz. hardvara laŭteco-klavo sur Android) devas speguliĝi en la regilo
        ludilo.simuluEksteranLaŭtecon(0.3f)
        waitForIdle()
        assertEquals(
            0.3f,
            onNodeWithTag("laŭteco").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current,
        )
    }

    @Test
    fun sekvanta_montras_titolon_kaj_ludi_butonon() = runComposeUiTest {
        val ludilo = TestaLudiloRegilo(
            LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = null)
        )
        setContent {
            MaterialTheme {
                MiniLudilbreto(
                    ludilo = ludilo,
                    sekvantaTitolo = "Varsovia Vento",
                    sekvantaSubtitolo = "Kanalo",
                    onLudiSekvantan = {},
                )
            }
        }
        waitForIdle()

        onNodeWithTag("ludilbreto").assertExists()
        onNodeWithContentDescription("Ludi").assertExists()
        // Ne montru halti- aŭ ludvico-butonojn en sekvanta reĝimo
        onNodeWithContentDescription("Halti").assertDoesNotExist()
        onNodeWithContentDescription("Ludvico").assertDoesNotExist()
    }

    @Test
    fun sekvanta_mankas_kiam_neniu_sekvanta() = runComposeUiTest {
        val ludilo = TestaLudiloRegilo(
            LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = null)
        )
        setContent {
            MaterialTheme {
                MiniLudilbreto(ludilo = ludilo)
            }
        }
        waitForIdle()

        onNodeWithTag("ludilbreto").assertDoesNotExist()
    }

    @Test
    fun sekvanta_ludi_butono_vokas_onLudiSekvantan() = runComposeUiTest {
        val ludilo = TestaLudiloRegilo(
            LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = null)
        )
        var ludiVokita = false
        setContent {
            MaterialTheme {
                MiniLudilbreto(
                    ludilo = ludilo,
                    sekvantaTitolo = "Kern.punkto",
                    sekvantaSubtitolo = "Kanalo",
                    onLudiSekvantan = { ludiVokita = true },
                )
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Ludi").performClick()
        waitForIdle()
        assertTrue(ludiVokita, "onLudiSekvantan devas esti vokita klakante la ludi-butonon")
    }
}
