package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.SerĉoDeponejo
import kotlin.test.Test

/**
 * Testoj por la serĉ-ekrano (PR #93): debounce kaj rezulto-ĝisdatiĝo.
 *
 * La falsa deponejo respondas ATENDANTE — la debounce (300 ms) devas kolekti
 * klavpremojn kaj montri nur la rezulton de la LASTA teksto.
 */
@OptIn(ExperimentalTestApi::class)
class SerĉoEkranoTest {

    private fun elsendo(titolo: String) = Elsendo(
        id = "test:$titolo",
        kanaloSlug = "test",
        titolo = titolo,
        fluo = "https://x.com/${titolo.take(8)}.mp3",
        dato = "2024-01-01"
    )

    @Test
    fun malpliOlDuSignojNeSerĉas() = runComposeUiTest {
        val deponejo = FalsaSerĉoDeponejo()
        setContent { SerĉoEkrano(serĉoDeponejo = deponejo, onElsendo = {}) }
        waitForIdle()

        onNodeWithText("Trovu elsendon...").performTextInput("a")

        mainClock.advanceTimeBy(1000)
        waitForIdle()
        assertEqualsKieta(0, deponejo.petoj.size, "Unu signo ne devas serĉi")
    }

    @Test
    fun debounceKolektasKlavpremojnKajMontrasRezulton() = runComposeUiTest {
        val deponejo = FalsaSerĉoDeponejo()
        setContent { SerĉoEkrano(serĉoDeponejo = deponejo, onElsendo = {}) }
        waitForIdle()

        val kampo = onNodeWithText("Trovu elsendon...")
        kampo.performTextInput("es")
        mainClock.advanceTimeBy(100)  // mezo de la debounce-Atendo — pliaj signoj venos
        kampo.performTextInput("per")
        mainClock.advanceTimeBy(1000)  // preter la debounce-Atendo
        waitForIdle()

        // Unu serĉo por la fina teksto "esper" — ne por "es" (PR #93: antaŭe ĉiu klavpremo serĉis)
        assertEqualsKieta(1, deponejo.petoj.size, "Debounce kolektas klavpremojn")
        kotlin.test.assertEquals("esper", deponejo.petoj.last(), "Nur la fina teksto serĉiĝu")
        // La rezulto montriĝas
        onAllNodesWithText("Esperanto-podkasto").assertCountEquals(1)
    }

    /** Falsa deponejo kiu registras la serĉitajn tekstojn. */
    private class FalsaSerĉoDeponejo : SerĉoDeponejo {
        val petoj = mutableListOf<String>()
        override suspend fun serĉi(teksto: String, limo: Int): List<Elsendo> {
            petoj.add(teksto)
            return if (teksto == "esper") {
                listOf(Elsendo(id = "test:esper", kanaloSlug = "test", titolo = "Esperanto-podkasto", fluo = "https://x.com/e.mp3", dato = "2024-01-01"))
            } else emptyList()
        }
    }

    private fun assertEqualsKieta(atendata: Int, fakta: Int, msg: String) =
        kotlin.test.assertEquals(atendata, fakta, msg)

}
