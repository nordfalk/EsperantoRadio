package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.data.repository.LudatojDeponejoMaketo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ElsendoEkranoSavitaPozicioTest {

    private fun elsendo(id: String = "e1", dauro: Long? = 300) = Elsendo(
        id = id,
        kanaloSlug = "k1",
        titolo = "Testa elsendo",
        fluo = "https://x.com/$id.mp3",
        dato = "2024-01-01",
        dauro = dauro,
    )

    @Test
    fun montrasDauxrigiKiamSavitaPozicioEkzistas() = runComposeUiTest {
        val ludatoj = LudatojDeponejoMaketo()
        runBlocking { ludatoj.registriPozicion("e1", "k1", 120_000, 300_000) } // 2:00 / 5:00

        setContent {
            pTemo {
                ElsendoEkrano(
                    elsendo = elsendo(),
                    onReen = {},
                    ludatojDeponejo = ludatoj,
                )
            }
        }
        waitForIdle()

        onNodeWithText("Daŭrigi de 2:00").assertIsDisplayed()
        onNodeWithText("▶ Daŭrigi").assertIsDisplayed()
    }

    @Test
    fun montrasAusklutiKiamNeniuSavitaPozicio() = runComposeUiTest {
        val ludatoj = LudatojDeponejoMaketo()

        setContent {
            pTemo {
                ElsendoEkrano(
                    elsendo = elsendo(),
                    onReen = {},
                    ludatojDeponejo = ludatoj,
                )
            }
        }
        waitForIdle()

        onNodeWithText("▶ Aŭskulti").assertIsDisplayed()
    }

    @Test
    fun montrasAusklutiKiamFinita() = runComposeUiTest {
        val ludatoj = LudatojDeponejoMaketo()
        runBlocking {
            ludatoj.registriPozicion("e1", "k1", 280_000, 300_000)
            ludatoj.markiFinita("e1", "k1")
        }

        setContent {
            pTemo {
                ElsendoEkrano(
                    elsendo = elsendo(),
                    onReen = {},
                    ludatojDeponejo = ludatoj,
                )
            }
        }
        waitForIdle()

        onNodeWithText("▶ Aŭskulti").assertIsDisplayed()
    }
}
