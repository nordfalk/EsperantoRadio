package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.data.repository.LudantojDeponejoMaketo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.LudantojDeponejo
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
        val ludantoj = LudantojDeponejoMaketo()
        runBlocking { ludantoj.registriPozicion("e1", "k1", 120_000, 300_000) } // 2:00 / 5:00

        setContent {
            pTemo {
                ElsendoEkrano(
                    elsendo = elsendo(),
                    onReen = {},
                    ludantojDeponejo = ludantoj,
                )
            }
        }
        waitForIdle()

        onNodeWithText("Daŭrigi de 2:00").assertIsDisplayed()
        onNodeWithText("▶ Daŭrigi").assertIsDisplayed()
    }

    @Test
    fun montrasAusklutiKiamNeniuSavitaPozicio() = runComposeUiTest {
        val ludantoj = LudantojDeponejoMaketo()

        setContent {
            pTemo {
                ElsendoEkrano(
                    elsendo = elsendo(),
                    onReen = {},
                    ludantojDeponejo = ludantoj,
                )
            }
        }
        waitForIdle()

        onNodeWithText("▶ Aŭskulti").assertIsDisplayed()
    }

    @Test
    fun montrasAusklutiKiamFinita() = runComposeUiTest {
        val ludantoj = LudantojDeponejoMaketo()
        runBlocking {
            ludantoj.registriPozicion("e1", "k1", 280_000, 300_000)
            ludantoj.markiFinita("e1", "k1")
        }

        setContent {
            pTemo {
                ElsendoEkrano(
                    elsendo = elsendo(),
                    onReen = {},
                    ludantojDeponejo = ludantoj,
                )
            }
        }
        waitForIdle()

        onNodeWithText("▶ Aŭskulti").assertIsDisplayed()
    }
}
