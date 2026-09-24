package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.PreferencesSettings
import dk.nordfalk.esperanto.domain.model.Alarmo
import kotlinx.coroutines.test.runTest
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testoj por [PersistantaAlarmoDeponejo.ekigis] — kio okazas kiam alarmo sonis.
 * Uzas veran persiston (java.util.prefs en provizora nodo), por kontroli ke la ŝanĝo
 * estas videbla ankaŭ por nova deponejo (kiel post restarto de la procezo).
 */
class PersistantaAlarmoDeponejoTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @AfterTest
    fun purigu() = nodo.removeNode()

    private suspend fun deponejoKun(vararg alarmoj: Alarmo) = PersistantaAlarmoDeponejo(settings).apply {
        alarmoj.forEach { krei(it) }
    }

    @Test
    fun unufojaAlarmoEstasMalaktivigitaKajPersistita() = runTest {
        val deponejo = deponejoKun(Alarmo(id = 0, horo = 7, minuto = 0, ripeto = 0, kanaloSlug = "peranto"))
        val id = deponejo.observiAlarmojn().value.single().id

        deponejo.ekigis(id)

        assertFalse(deponejo.observiAlarmojn().value.single().aktiva)
        // Nova deponejo (kiel post restarto) legas la saman staton el Settings
        assertFalse(PersistantaAlarmoDeponejo(settings).observiAlarmojn().value.single().aktiva)
    }

    @Test
    fun ripetantaAlarmoRestasAktiva() = runTest {
        val deponejo = deponejoKun(Alarmo(id = 0, horo = 7, minuto = 0, ripeto = 0x7f, kanaloSlug = "muzaiko"))
        val id = deponejo.observiAlarmojn().value.single().id

        deponejo.ekigis(id)

        assertTrue(deponejo.observiAlarmojn().value.single().aktiva)
    }

    @Test
    fun nekonataAlarmoNeSxangxasIon() = runTest {
        val deponejo = deponejoKun(Alarmo(id = 0, horo = 7, minuto = 0, ripeto = 0, kanaloSlug = "peranto"))
        val antaux = deponejo.observiAlarmojn().value

        deponejo.ekigis(-42)

        assertEquals(antaux, deponejo.observiAlarmojn().value)
    }
}
