package dk.nordfalk.esperanto.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unuaj testoj por [TirkestaTiroSojlo] — la sojlo-kalkulo kiu malfermas la
 * navigan tirkeston per dekstra tro-tiro sur horizontala vico.
 */
class TirkestaTiroSojloTest {

    @Test
    fun malfermasKiamAkumulitaTiroTranspasasLaSojlon() {
        val kalkulo = TirkestaTiroSojlo(sojloPx = 100f)
        // 3 deltoj po 40px = 120px > 100px sojlo
        assertFalse(kalkulo.akceptu(40f, povasRuliReen = false))
        assertFalse(kalkulo.akceptu(40f, povasRuliReen = false))
        assertTrue(kalkulo.akceptu(40f, povasRuliReen = false))
    }

    @Test
    fun neFarasKionSeLaVicoAnkorauxPovasRuliReen() {
        val kalkulo = TirkestaTiroSojlo(sojloPx = 100f)
        // Deltoj dum la vico povas ruli reen estas nuligitaj — ne akumuliĝas
        kalkulo.akceptu(80f, povasRuliReen = true)
        // Nun la vico atingis sian komencon: la sojlo kalkuliĝas de nulo
        assertFalse(kalkulo.akceptu(80f, povasRuliReen = false))
        assertTrue(kalkulo.akceptu(40f, povasRuliReen = false))
    }

    @Test
    fun nuligasLaAkumulonJeMalkontrauxaDelto() {
        val kalkulo = TirkestaTiroSojlo(sojloPx = 100f)
        kalkulo.akceptu(60f, povasRuliReen = false)
        // Mallontra (maldekstra) delto nuligas la akumulitan tro-tiron
        kalkulo.akceptu(-10f, povasRuliReen = false)
        assertFalse(kalkulo.akceptu(60f, povasRuliReen = false), "akumulo devis esti nuligita")
        assertTrue(kalkulo.akceptu(50f, povasRuliReen = false))
    }

    @Test
    fun rekomencasDeNuloPostMalfermo() {
        val kalkulo = TirkestaTiroSojlo(sojloPx = 100f)
        assertTrue(kalkulo.akceptu(150f, povasRuliReen = false))
        // Post malfermo la kalkulo rekomenciĝas — bezonas la tutan sojlon denove
        assertFalse(kalkulo.akceptu(60f, povasRuliReen = false))
        assertTrue(kalkulo.akceptu(60f, povasRuliReen = false))
    }
}
