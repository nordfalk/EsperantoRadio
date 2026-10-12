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
class AgordojKasxitajAvertojTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @AfterTest
    fun purigu() = nodo.removeNode()

    @Test
    fun defauxlteNeniomEstasKasxita() {
        assertTrue(AgordojDeponejoImpl(settings).kasxitajAvertoj.value.isEmpty())
    }

    @Test
    fun kasxoAnstatauxigasLaTutanAronKajPersistas() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.kasxuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS), deponejo.kasxitajAvertoj.value)
        deponejo.kasxuAvertojn(setOf(DiagnozoTipo.EXACT_ALARMS))
        assertEquals(setOf(DiagnozoTipo.EXACT_ALARMS), deponejo.kasxitajAvertoj.value)
        // Persistas trans restarto — nova deponejo legas la saman agordon
        assertEquals(setOf(DiagnozoTipo.EXACT_ALARMS), AgordojDeponejoImpl(settings).kasxitajAvertoj.value)
    }

    @Test
    fun purigoForgesasKasxojnDeMalaperintajProblemoj() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.kasxuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        // Uzanto riparas la DNS-problemon — nur baterio plu ekzistas
        deponejo.puriguKasxitajnAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION), deponejo.kasxitajAvertoj.value)
        // DNS revenas — ĝia kaŝo estis forgesita, do ĝi ne plu estas kaŝita
        // kaj la avertokarto reaperos sur la ĉefekrano
        deponejo.puriguKasxitajnAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION), deponejo.kasxitajAvertoj.value)
        // ...sed la 'x' de la reaperinta karto re-kaŝos ambaŭ
        deponejo.kasxuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS))
        assertEquals(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.DNS), deponejo.kasxitajAvertoj.value)
    }

    @Test
    fun purigoSenSxangxoNeSkribas() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.kasxuAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION))
        val antaŭ = deponejo.kasxitajAvertoj.value
        // Samaj tipoj — neniu ŝanĝo, neniu skribo
        deponejo.puriguKasxitajnAvertojn(setOf(DiagnozoTipo.BATERIO_OPTIMIZATION, DiagnozoTipo.NOTIFICATIONS))
        assertEquals(antaŭ, deponejo.kasxitajAvertoj.value)
    }
}
