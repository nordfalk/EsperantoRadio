package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * UI-testo por ElshutitajEkrano — testas plurajn tavolojn:
 * la ekrano montras elŝutitajn elsendojn kun ĝustaj stato-tekstoj,
 * kaj la malplena stato montras helpmesaĝon.
 */
@OptIn(ExperimentalTestApi::class)
class ElshutitajEkranoTest {

    private class FalsaElshutDeponejo(
        elshutoj: Map<String, ElshutitaElsendo>
    ) : ElshutDeponejo {
        private val _elshutoj = MutableStateFlow(elshutoj)
        override fun observiElshutojn(): StateFlow<Map<String, ElshutitaElsendo>> = _elshutoj.asStateFlow()
        override fun observiElshutStaton(elsendoId: String): StateFlow<ElshutStato> =
            MutableStateFlow(_elshutoj.value[elsendoId]?.stato ?: ElshutStato.NeElshutita)
        override suspend fun elshuti(elsendo: Elsendo) {}
        override suspend fun haltigi(elsendoId: String) {}
        override suspend fun forigi(elsendoId: String) {
            _elshutoj.value = _elshutoj.value - elsendoId
        }
        override suspend fun getLokaDosieroVojo(elsendoId: String): String? =
            _elshutoj.value[elsendoId]?.dosieroVojo
        override fun estasElshutita(elsendoId: String): Boolean =
            _elshutoj.value[elsendoId]?.stato is ElshutStato.Preta
    }

    private val testElsendo = Elsendo(
        id = "test:2024-01-01",
        kanaloSlug = "test",
        titolo = "Mia podkasto",
        fluo = "https://x.com/a.mp3",
        dato = "2024-01-01"
    )

    private val testElsendoKunDauro = Elsendo(
        id = "test:2024-06-01",
        kanaloSlug = "test",
        titolo = "Podkasto kun daŭro",
        fluo = "https://x.com/b.mp3",
        dato = "2024-06-01",
        dauro = 3725, // 1:02:05
    )

    @Test
    fun montrasHelpmesagxonKiamMalplena() = runComposeUiTest {
        val deponejo = FalsaElshutDeponejo(emptyMap())
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
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
        val deponejo = FalsaElshutDeponejo(
            mapOf(
                testElsendo.id to ElshutitaElsendo(testElsendo, "/tmp/test.mp3", ElshutStato.Preta)
            )
        )
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
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
    fun montrasElshutantanElsendon() = runComposeUiTest {
        val deponejo = FalsaElshutDeponejo(
            mapOf(
                testElsendo.id to ElshutitaElsendo(
                    testElsendo, "/tmp/test.mp3",
                    ElshutStato.Elshutanta(0.5f, 500L, 1000L)
                )
            )
        )
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
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
        val deponejo = FalsaElshutDeponejo(
            mapOf(
                testElsendo.id to ElshutitaElsendo(
                    testElsendo, "/tmp/test.mp3",
                    ElshutStato.Eraro("HTTP 404")
                )
            )
        )
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
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
    fun montrasGrandecxonKajDauron() = runComposeUiTest {
        val deponejo = FalsaElshutDeponejo(
            mapOf(
                testElsendoKunDauro.id to ElshutitaElsendo(
                    testElsendoKunDauro, "/tmp/test.mp3",
                    ElshutStato.Preta, dosierGrando = 55_300_000
                )
            )
        )
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
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
    fun montrasPauxzButononKiamLudas() = runComposeUiTest {
        val deponejo = FalsaElshutDeponejo(
            mapOf(
                testElsendo.id to ElshutitaElsendo(testElsendo, "/tmp/test.mp3", ElshutStato.Preta)
            )
        )
        val ludilo = object : LudiloRegilo {
            private val s = MutableStateFlow(
                LudantoInformo(
                    stato = LudantoStato.Ludas,
                    nunaFonto = Sonfonto.LokaElsendo(testElsendo, "/tmp/test.mp3"),
                )
            )
            override val stato: StateFlow<LudantoInformo> = s.asStateFlow()
            override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {}
            override fun ludi() { s.value = s.value.copy(stato = LudantoStato.Ludas) }
            override fun pauxzigi() { s.value = s.value.copy(stato = LudantoStato.Haltita) }
            override fun halti() { s.value = LudantoInformo(stato = LudantoStato.Haltita) }
            override fun saltiAl(pozicioMs: Long) {}
            override fun fiksiLauxtecon(volumeno: Float) {}
        }
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
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
        val deponejo = FalsaElshutDeponejo(
            mapOf(
                testElsendo.id to ElshutitaElsendo(testElsendo, "/tmp/test.mp3", ElshutStato.Preta)
            )
        )
        val ludilo = object : LudiloRegilo {
            private val s = MutableStateFlow(
                LudantoInformo(
                    stato = LudantoStato.Haltita,
                    nunaFonto = Sonfonto.LokaElsendo(testElsendo, "/tmp/test.mp3"),
                )
            )
            override val stato: StateFlow<LudantoInformo> = s.asStateFlow()
            override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {}
            override fun ludi() { s.value = s.value.copy(stato = LudantoStato.Ludas) }
            override fun pauxzigi() { s.value = s.value.copy(stato = LudantoStato.Haltita) }
            override fun halti() { s.value = LudantoInformo(stato = LudantoStato.Haltita) }
            override fun saltiAl(pozicioMs: Long) {}
            override fun fiksiLauxtecon(volumeno: Float) {}
        }
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
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
