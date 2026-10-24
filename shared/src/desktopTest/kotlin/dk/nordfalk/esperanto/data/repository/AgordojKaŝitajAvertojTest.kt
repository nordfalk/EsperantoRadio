package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.PreferencesSettings
import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testoj por la kaŝo de diagnozaj avertoj en [AgordojDeponejoImpl] (PR #105):
 * 'x' kaŝas la NUNAJN problemo-tipojn; kiam problemo malaperas kaj revenas,
 * la kaŝo forgesiĝas kaj la avertokarto reaperas sur la ĉefekrano.
 * Uzas veran persiston (provizora nodo) por kontroli la inter-restartan konduton.
 */
class AgordojKaŝitajAvertojTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @AfterTest
    fun purigu() = nodo.removeNode()

    @Test
    fun defaŭlteNeniomEstasKaŝita() {
        assertTrue(AgordojDeponejoImpl(settings).kaŝitajAvertoj.value.isEmpty())
    }

    @Test
    fun kaŝoAnstataŭigasLaTutanAronKajPersistas() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.kaŝuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS), deponejo.kaŝitajAvertoj.value)
        deponejo.kaŝuAvertojn(setOf(DiagnozoTipo.NOTIFICATIONS))
        assertEquals(setOf(DiagnozoTipo.NOTIFICATIONS), deponejo.kaŝitajAvertoj.value)
        // Persistas trans restarto — nova deponejo legas la saman agordon
        assertEquals(setOf(DiagnozoTipo.NOTIFICATIONS), AgordojDeponejoImpl(settings).kaŝitajAvertoj.value)
    }

    @Test
    fun purigoForgesasKaŝojnDeMalaperintajProblemoj() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.kaŝuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        // Uzanto riparas la DNS-problemon — nur baterio plu ekzistas
        deponejo.puriguKaŝitajnAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION), deponejo.kaŝitajAvertoj.value)
        // DNS revenas — ĝia kaŝo estis forgesita, do ĝi ne plu estas kaŝita
        // kaj la avertokarto reaperos sur la ĉefekrano
        deponejo.puriguKaŝitajnAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION), deponejo.kaŝitajAvertoj.value)
        // ...sed la 'x' de la reaperinta karto re-kaŝos ambaŭ
        deponejo.kaŝuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS), deponejo.kaŝitajAvertoj.value)
    }

    @Test
    fun purigoSenŜanĝoNeSkribas() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.kaŝuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION))
        val antaŭ = deponejo.kaŝitajAvertoj.value
        // Samaj tipoj — neniu ŝanĝo, neniu skribo
        deponejo.puriguKaŝitajnAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.NOTIFICATIONS))
        assertEquals(antaŭ, deponejo.kaŝitajAvertoj.value)
    }
}
