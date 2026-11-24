package dk.nordfalk.esperanto.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Versio-komparado por la ĝisdatigo-indikilo (FAROTA C61a).
 */
class VersioTest {

    @Test
    fun malplenaVersioNeniamĜisdatigas() {
        assertFalse(ĉuNovaVersio("3.2.0", ""))
        assertFalse(ĉuNovaVersio("3.2.0", "   "))
        // Neparsebla instalita versio — prefere neniu averto ol falsa averto
        assertFalse(ĉuNovaVersio("", "9.9.9"))
    }

    @Test
    fun samaVersioNeĜisdatigas() {
        assertFalse(ĉuNovaVersio("3.2.0", "3.2.0"))
    }

    @Test
    fun pliNovaVersioĜisdatigas() {
        assertTrue(ĉuNovaVersio("3.1.9", "3.2.0"))
        assertTrue(ĉuNovaVersio("3.1.9", "3.10.0")) // 10 > 9 numere, ne leksikografie
        assertTrue(ĉuNovaVersio("2.9.9", "3.0.0"))
        assertTrue(ĉuNovaVersio("3.2", "3.2.1")) // mankanta komponanto = 0
    }

    @Test
    fun malnovaVersioEnKonfiguroNeĜisdatigas() {
        // la konfiguro estas pli malnova ol la apo (ekz. defora ne ĝisdatigita)
        assertFalse(ĉuNovaVersio("3.2.0", "3.1.9"))
    }

    @Test
    fun betaSufiksoNeIgasPliNova() {
        // „3.1.1-beta29okt“ — la sufikso ne estas parto de la numerado
        assertFalse(ĉuNovaVersio("3.1.1-beta29okt", "3.1.1"))
        // „3.2.0-beta.1“ validas kiel EGALE al „3.2.0“ — NE kiel pli nova
        // (recenza trovindaĵo: spliti pri ĉiu punkto vidas la „1“ kiel 4an komponanton)
        assertFalse(ĉuNovaVersio("3.2.0", "3.2.0-beta.1"))
    }

    @Test
    fun betaSufiksoNeKaŝasPliNovanKernon() {
        // la sufikso estas forigata, sed la kerno ja kompariĝas
        assertTrue(ĉuNovaVersio("3.1.1-beta29okt", "3.2.0"))
        assertTrue(ĉuNovaVersio("3.2.0", "3.2.1-beta.1"))
    }

    @Test
    fun sufiksoEnLaKonfiguroNeĜisdatigas() {
        // la konfiguro portas antaŭ-eldonan version — ne instigu ĝisdatigon al beta
        assertFalse(ĉuNovaVersio("3.2.0", "3.2.0-beta1"))
    }

    @Test
    fun fuŝitaKonfiguroNeĜisdatigas() {
        // toleremo (A4): fuŝita valoro en la konfiguro ne kaŭzu averton
        assertFalse(ĉuNovaVersio("3.2.0", "3.3.o"))
        assertFalse(ĉuNovaVersio("3.2.0", "3.o.2"))
        assertFalse(ĉuNovaVersio("3.2.0", "3..2"))
        assertFalse(ĉuNovaVersio("3.2.0", "3.2."))
        assertFalse(ĉuNovaVersio("3.2.0", "velura versio"))
        assertFalse(ĉuNovaVersio("3.2.0", "99999999999999")) // tro granda por Int
    }
}
