package dk.nordfalk.esperanto.domain

import dk.nordfalk.esperanto.domain.model.DiagnozoAgo
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Severeco
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por la diagnoza sistemo.
 *
 * La Desktop-implemento de DiagnozoRegilo redonas malplenan liston (nenio specifa),
 * do sur Desktop ni nur testas la modelojn kaj la malplenan rezulton.
 * La Android-implemento estas testata per instrumentoj sur la aparato.
 */
class DiagnozoTest {

    @Test
    fun desktopDiagnozoRegilo_redonasMalplenanListon() = runTest {
        val regilo = DiagnozoRegilo()
        val rezultoj = regilo.kontroli()
        assertTrue(rezultoj.isEmpty(), "Desktop devas redoni malplenan liston, sed: $rezultoj")
    }

    @Test
    fun desktopDiagnozoRegilo_problemojKomenceMalplena() {
        val regilo = DiagnozoRegilo()
        assertTrue(regilo.problemoj.value.isEmpty(), "Problemoj komence devas esti malplena")
    }

    @Test
    fun desktopDiagnozoRegilo_kontroliRapide_estasMalplena() = runTest {
        val regilo = DiagnozoRegilo()
        val rezultoj = regilo.kontroliRapide()
        assertTrue(rezultoj.isEmpty(), "Desktop kontroliRapide devas redoni malplenan liston")
        assertTrue(regilo.problemoj.value.isEmpty(), "Problemoj restas malplena post kontroliRapide")
    }

    @Test
    fun diagnozoRezulto_havasKorektajnKampojn() {
        val ago = DiagnozoAgo("Malfermi agordojn", "battery")
        val rezulto = DiagnozoRezulto(
            tipo = DiagnozoTipo.BATERIO_OPTIMIZATION,
            titolo = "Bateri-optimumado estas ŝaltita",
            priskribo = "La sistemo povas limigi la aplikon en la fono.",
            severeco = Severeco.ERARO,
            ago = ago,
        )
        assertEquals(DiagnozoTipo.BATERIO_OPTIMIZATION, rezulto.tipo)
        assertEquals("Bateri-optimumado estas ŝaltita", rezulto.titolo)
        assertEquals(Severeco.ERARO, rezulto.severeco)
        assertNotNull(rezulto.ago)
        assertEquals("battery", rezulto.ago!!.intenco)
    }

    @Test
    fun diagnozoRezulto_agoPovasEstiNull() {
        val rezulto = DiagnozoRezulto(
            tipo = DiagnozoTipo.DNS,
            titolo = "DNS-eraro",
            priskribo = "Ne povas solvi nomojn.",
            severeco = Severeco.AVERTO,
        )
        assertNull(rezulto.ago, "Ago povas esti null por nuraj informoj")
    }

    @Test
    fun severeco_havasTriNivelojn() {
        assertEquals(3, Severeco.entries.size)
        assertTrue(Severeco.entries.contains(Severeco.INFO))
        assertTrue(Severeco.entries.contains(Severeco.AVERTO))
        assertTrue(Severeco.entries.contains(Severeco.ERARO))
    }

    @Test
    fun diagnozoTipo_havasKvinTipojn() {
        assertEquals(5, DiagnozoTipo.entries.size)
        assertTrue(DiagnozoTipo.entries.contains(DiagnozoTipo.BATERIO_OPTIMIZATION))
        assertTrue(DiagnozoTipo.entries.contains(DiagnozoTipo.NOTIFICATIONS))
        assertTrue(DiagnozoTipo.entries.contains(DiagnozoTipo.EXACT_ALARMS))
        assertTrue(DiagnozoTipo.entries.contains(DiagnozoTipo.DNS))
        assertTrue(DiagnozoTipo.entries.contains(DiagnozoTipo.RETO))
    }
}
