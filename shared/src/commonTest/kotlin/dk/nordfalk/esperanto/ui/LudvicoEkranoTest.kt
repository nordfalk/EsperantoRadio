package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.player.LudvicoRegilo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LudvicoEkranoTest {

    @Test
    fun montrasMalplenanMesagxonKiamVicoMalplenas() = runComposeUiTest {
        val regilo = kreuRegilon()
        setContent { pTemo { LudvicoEkrano(ludvicoRegilo = regilo, onReen = {}) } }
        waitForIdle()

        onNodeWithText("Ludvico estas malplena").assertIsDisplayed()
        onNodeWithText("Ludvico (0)").assertIsDisplayed()
    }

    @Test
    fun montrasElsendojnKiamVicoNeMalplenas() = runComposeUiTest {
        val e1 = elsendo("e1", "Unua elsendo")
        val e2 = elsendo("e2", "Dua elsendo")
        val regilo = kreuRegilon(komenceVico = listOf(e1, e2))
        setContent { pTemo { LudvicoEkrano(ludvicoRegilo = regilo, onReen = {}) } }
        waitForIdle()

        // e1 estas tuj ludita (nenio ludas), do nur e2 restas en la vico
        onNodeWithText("Ludvico (1)").assertIsDisplayed()
        onNodeWithText("Dua elsendo").assertIsDisplayed()
    }

    private fun elsendo(id: String, titolo: String = "Elsendo $id") = Elsendo(
        id = id,
        kanaloSlug = "k1",
        kanaloNomo = "Kanalnomo",
        titolo = titolo,
        fluo = "https://x.com/$id.mp3",
        dato = "2024-01-01",
    )

    private fun kreuRegilon(
        komenceVico: List<Elsendo> = emptyList(),
    ): LudvicoRegilo {
        val regilo = LudvicoRegilo(
            ludilo = dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo(),
            elsendoDeponejo = object : dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo {
                override fun observiElsendojn(kanaloSlug: String) = MutableStateFlow(emptyList<Elsendo>())
                override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean) = emptyList<Elsendo>()
                override suspend fun getElsendo(id: String) = null
                override suspend fun sercxiElsendojn(teksto: String, limo: Int) = emptyList<Elsendo>()
                override suspend fun sxargxiElsendojnPorKanal(kanalo: dk.nordfalk.esperanto.domain.model.Kanalo, fortoRefresigi: Boolean) = emptyList<Elsendo>()
            },
            kanaloDeponejo = object : dk.nordfalk.esperanto.domain.repository.KanaloDeponejo {
                override fun observiKanalojn() = MutableStateFlow(emptyList<dk.nordfalk.esperanto.domain.model.Kanalo>())
                override suspend fun getKanalojn(fortoRefresigi: Boolean) = emptyList<dk.nordfalk.esperanto.domain.model.Kanalo>()
                override suspend fun getKanalo(slug: String) = null
            },
            plejsatatajDeponejo = dk.nordfalk.esperanto.data.repository.PlejsatatajDeponejoImpl(),
            ludatojDeponejo = dk.nordfalk.esperanto.data.repository.LudatojDeponejoMaketo(),
        )
        komenceVico.forEach { runBlocking { regilo.aldoniAlVico(it) } }
        return regilo
    }
}
