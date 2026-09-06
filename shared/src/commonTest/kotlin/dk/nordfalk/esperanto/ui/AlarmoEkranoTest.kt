package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.AlarmoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * UI-testoj por AlarmoEkrano — testas plurajn tavolojn (UI + deponejo).
 */
@OptIn(ExperimentalTestApi::class)
class AlarmoEkranoTest {

    private class FalsaAlarmoDeponejo(alarmoj: List<Alarmo>) : AlarmoDeponejo {
        private val _alarmoj = MutableStateFlow(alarmoj)
        override fun observiAlarmojn(): StateFlow<List<Alarmo>> = _alarmoj.asStateFlow()
        override suspend fun krei(alarmo: Alarmo) { _alarmoj.value = _alarmoj.value + alarmo }
        override suspend fun ghisdatigi(alarmo: Alarmo) {
            _alarmoj.value = _alarmoj.value.map { if (it.id == alarmo.id) alarmo else it }
        }
        override suspend fun forigi(alarmoId: Int) {
            _alarmoj.value = _alarmoj.value.filter { it.id != alarmoId }
        }
        override suspend fun baskuliAktivon(alarmoId: Int) {
            _alarmoj.value = _alarmoj.value.map {
                if (it.id == alarmoId) it.copy(aktiva = !it.aktiva) else it
            }
        }
    }

    private class FalsaKanaloDeponejo(kanaloj: List<Kanalo>) : KanaloDeponejo {
        private val _kanaloj = MutableStateFlow(kanaloj)
        override fun observiKanalojn(): StateFlow<List<Kanalo>> = _kanaloj.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanalo> = _kanaloj.value
        override suspend fun getKanalo(slug: String): Kanalo? = _kanaloj.value.find { it.slug == slug }
    }

    private val testKanaloj = listOf(
        Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "x"),
        Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "y"),
    )

    @Test
    fun montrasHelpmesagxonKiamMalplena() = runComposeUiTest {
        val deponejo = FalsaAlarmoDeponejo(emptyList())
        setContent {
            AlarmoEkrano(
                alarmoDeponejo = deponejo,
                kanaloDeponejo = FalsaKanaloDeponejo(testKanaloj),
                onReen = {}
            )
        }
        waitForIdle()
        onNodeWithText("Neniu alarmo. Premu + por krei.").assertIsDisplayed()
    }

    @Test
    fun montrasAlarmojnEnListo() = runComposeUiTest {
        val alarmoj = listOf(
            Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanaloSlug = "muzaiko", aktiva = true, etikedo = "Matene"),
            Alarmo(id = 2, horo = 22, minuto = 0, ripeto = 0, kanaloSlug = "kernpunkto", aktiva = false),
        )
        setContent {
            AlarmoEkrano(
                alarmoDeponejo = FalsaAlarmoDeponejo(alarmoj),
                kanaloDeponejo = FalsaKanaloDeponejo(testKanaloj),
                onReen = {}
            )
        }
        waitForIdle()
        onNodeWithText("06:45").assertIsDisplayed()
        onNodeWithText("Muzaiko").assertIsDisplayed()
        onNodeWithText("Cxiutage").assertIsDisplayed()
        onNodeWithText("22:00").assertIsDisplayed()
        onNodeWithText("Unufoje").assertIsDisplayed()
    }
}
