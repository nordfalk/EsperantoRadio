package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * UI-testo por ElŝutitajEkrano — testas plurajn tavolojn:
 * la ekrano montras elŝutitajn elsendojn kun ĝustaj stato-tekstoj,
 * kaj la malplena stato montras helpmesaĝon.
 */
@OptIn(ExperimentalTestApi::class)
class ElŝutitajEkranoTest {

    private class FalsaElŝutDeponejo(
        elŝutoj: Map<String, ElŝutitaElsendo>
    ) : ElŝutDeponejo {
        private val _elŝutoj = MutableStateFlow(elŝutoj)
        override fun observiElŝutojn(): StateFlow<Map<String, ElŝutitaElsendo>> = _elŝutoj.asStateFlow()
        override fun observiElŝutStaton(elsendoId: String): StateFlow<ElŝutStato> =
            MutableStateFlow(_elŝutoj.value[elsendoId]?.stato ?: ElŝutStato.NeElŝutita)
        override suspend fun elŝuti(elsendo: Elsendo) {}
        override suspend fun haltigi(elsendoId: String) {}
        override suspend fun forigi(elsendoId: String) {
            _elŝutoj.value = _elŝutoj.value - elsendoId
        }
        override suspend fun getLokaDosieroVojo(elsendoId: String): String? =
            _elŝutoj.value[elsendoId]?.dosieroVojo
        override fun estasElŝutita(elsendoId: String): Boolean =
            _elŝutoj.value[elsendoId]?.stato is ElŝutStato.Preta
    }

    private val testElsendo = Elsendo(
        id = "test:2024-01-01",
        kanaloSlug = "test",
        titolo = "Mia podkasto",
        fluo = "https://x.com/a.mp3",
        dato = "2024-01-01"
    )

    private val testElsendoKunDaŭro = Elsendo(
        id = "test:2024-06-01",
        kanaloSlug = "test",
        titolo = "Podkasto kun daŭro",
        fluo = "https://x.com/b.mp3",
        dato = "2024-06-01",
        daŭro = 3725, // 1:02:05
    )

    @Test
    fun montrasHelpmesaĝonKiamMalplena() = runComposeUiTest {
        val deponejo = FalsaElŝutDeponejo(emptyMap())
        setContent {
            ElŝutitajEkrano(
                elŝutDeponejo = deponejo,
                ludilo = NoOpLudiloRegilo(),
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithText("Neniu elŝutita elsendo. Premu la elŝutbutonon sur elsendo por elŝuti.").assertIsDisplayed()
    }

    @Test
    fun montrasPretanElsendon() = runComposeUiTest {
        val deponejo = FalsaElŝutDeponejo(
            mapOf(
                testElsendo.id to ElŝutitaElsendo(testElsendo, "/tmp/test.mp3", ElŝutStato.Preta)
            )
        )
        setContent {
            ElŝutitajEkrano(
                elŝutDeponejo = deponejo,
                ludilo = NoOpLudiloRegilo(),
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithText("Mia podkasto").assertIsDisplayed()
        onNodeWithText("Preta").assertIsDisplayed()
    }

    @Test
    fun montrasElŝutantanElsendon() = runComposeUiTest {
        val deponejo = FalsaElŝutDeponejo(
            mapOf(
                testElsendo.id to ElŝutitaElsendo(
                    testElsendo, "/tmp/test.mp3",
                    ElŝutStato.Elŝutanta(0.5f, 500L, 1000L)
                )
            )
        )
        setContent {
            ElŝutitajEkrano(
                elŝutDeponejo = deponejo,
                ludilo = NoOpLudiloRegilo(),
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithText("Mia podkasto").assertIsDisplayed()
        onNodeWithText("Elŝutas... 50%").assertIsDisplayed()
    }

    @Test
    fun montrasEraranElsendon() = runComposeUiTest {
        val deponejo = FalsaElŝutDeponejo(
            mapOf(
                testElsendo.id to ElŝutitaElsendo(
                    testElsendo, "/tmp/test.mp3",
                    ElŝutStato.Eraro("HTTP 404")
                )
            )
        )
        setContent {
            ElŝutitajEkrano(
                elŝutDeponejo = deponejo,
                ludilo = NoOpLudiloRegilo(),
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithText("Mia podkasto").assertIsDisplayed()
        onNodeWithText("Eraro: HTTP 404").assertIsDisplayed()
    }

    @Test
    fun montrasGrandeconKajDaŭron() = runComposeUiTest {
        val deponejo = FalsaElŝutDeponejo(
            mapOf(
                testElsendoKunDaŭro.id to ElŝutitaElsendo(
                    testElsendoKunDaŭro, "/tmp/test.mp3",
                    ElŝutStato.Preta, dosierGrando = 55_300_000
                )
            )
        )
        setContent {
            ElŝutitajEkrano(
                elŝutDeponejo = deponejo,
                ludilo = NoOpLudiloRegilo(),
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithText("Podkasto kun daŭro").assertIsDisplayed()
        onNodeWithText("52.7 MB").assertIsDisplayed()
        onNodeWithText("1:02:05").assertIsDisplayed()
    }

    @Test
    fun montrasPaŭzButononKiamLudas() = runComposeUiTest {
        val deponejo = FalsaElŝutDeponejo(
            mapOf(
                testElsendo.id to ElŝutitaElsendo(testElsendo, "/tmp/test.mp3", ElŝutStato.Preta)
            )
        )
        val ludilo = object : LudiloRegilo {
            override val laŭteco: StateFlow<Float> = MutableStateFlow(1f)
            private val s = MutableStateFlow(
                LudantoInformo(
                    stato = LudantoStato.Ludas,
                    nunaFonto = Sonfonto.LokaElsendo(testElsendo, "/tmp/test.mp3"),
                )
            )
            override val stato: StateFlow<LudantoInformo> = s.asStateFlow()
            override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {}
            override fun ludi() { s.value = s.value.copy(stato = LudantoStato.Ludas) }
            override fun paŭzigi() { s.value = s.value.copy(stato = LudantoStato.Haltita) }
            override fun halti() { s.value = LudantoInformo(stato = LudantoStato.Haltita) }
            override fun saltiAl(pozicioMs: Long) {}
            override fun fiksiLaŭtecon(volumeno: Float) {}
        }
        setContent {
            ElŝutitajEkrano(
                elŝutDeponejo = deponejo,
                ludilo = ludilo,
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Paŭzigi").assertIsDisplayed()
    }

    @Test
    fun montrasLudButononKiamNeLudas() = runComposeUiTest {
        val deponejo = FalsaElŝutDeponejo(
            mapOf(
                testElsendo.id to ElŝutitaElsendo(testElsendo, "/tmp/test.mp3", ElŝutStato.Preta)
            )
        )
        val ludilo = object : LudiloRegilo {
            override val laŭteco: StateFlow<Float> = MutableStateFlow(1f)
            private val s = MutableStateFlow(
                LudantoInformo(
                    stato = LudantoStato.Haltita,
                    nunaFonto = Sonfonto.LokaElsendo(testElsendo, "/tmp/test.mp3"),
                )
            )
            override val stato: StateFlow<LudantoInformo> = s.asStateFlow()
            override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {}
            override fun ludi() { s.value = s.value.copy(stato = LudantoStato.Ludas) }
            override fun paŭzigi() { s.value = s.value.copy(stato = LudantoStato.Haltita) }
            override fun halti() { s.value = LudantoInformo(stato = LudantoStato.Haltita) }
            override fun saltiAl(pozicioMs: Long) {}
            override fun fiksiLaŭtecon(volumeno: Float) {}
        }
        setContent {
            ElŝutitajEkrano(
                elŝutDeponejo = deponejo,
                ludilo = ludilo,
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithContentDescription("Ludi").assertIsDisplayed()
    }
}
