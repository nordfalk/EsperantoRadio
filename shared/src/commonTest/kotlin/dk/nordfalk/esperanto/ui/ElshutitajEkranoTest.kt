package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.Sonfonto
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

    @Test
    fun montrasHelpmesagxonKiamMalplena() = runComposeUiTest {
        val deponejo = FalsaElshutDeponejo(emptyMap())
        setContent {
            ElshutitajEkrano(
                elshutDeponejo = deponejo,
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithText("Neniu elŝutita elsendo. Premu ⬇ sur elsendo por elŝuti.").assertIsDisplayed()
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
                onReen = {},
                onLudi = {},
                onElsendo = {}
            )
        }
        waitForIdle()
        onNodeWithText("Mia podkasto").assertIsDisplayed()
        onNodeWithText("Eraro: HTTP 404").assertIsDisplayed()
    }
}
