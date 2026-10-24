package dk.nordfalk.esperanto.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testoj por la sciig-limoj (maks. 7 samtempaj, maks. 3 po kanalo — la
 * plej malnovaj malaperas kiam la limoj atingiĝas).
 */
class SciigLimojTest {

    private fun eroj(vararg paroj: Pair<Int, String>) =
        paroj.map { SciigRegistrejoEro(it.first, it.second) }

    @Test
    fun unuaSciigoRestasSola() {
        val rezulto = limiguSciigojn(
            aktivaj = emptyList(),
            nova = SciigRegistrejoEro(1000, "kernpunkto"),
        )
        assertEquals(eroj(1000 to "kernpunkto"), rezulto.aktivaj)
        assertTrue(rezulto.forigotaj.isEmpty(), "Neniu forigenda ĉe la unua sciigo")
    }

    @Test
    fun subLaLimojNeniuMalaperas() {
        val aktivaj = eroj(
            1000 to "kernpunkto", 1001 to "kernpunkto", 1002 to "kernpunkto",
            1003 to "muzaiko", 1004 to "muzaiko", 1005 to "muzaiko",
            1006 to "varsoviavento",
        )
        val rezulto = limiguSciigojn(aktivaj, SciigRegistrejoEro(1007, "sano"))
        assertEquals(7, rezulto.aktivaj.size, "7 aktivaj + nova = 8 — la plej malnova foriras")
        // 1006 estas la 7-a kaj la nova faras 8 — la SUMA limo forigas 1000
        // (kaj ne la 4-a de iu kanalo, ĉar neniu kanalo havas 4)
        assertEquals(listOf(1000), rezulto.forigotaj)
    }

    @Test
    fun sumaLimoForigasPlejMalnovajn() {
        val aktivaj = eroj(
            1000 to "kernpunkto", 1001 to "muzaiko", 1002 to "sano",
            1003 to "kernpunkto", 1004 to "muzaiko", 1005 to "sano",
            1006 to "kernpunkto",
        )
        val rezulto = limiguSciigojn(aktivaj, SciigRegistrejoEro(1007, "varsoviavento"))
        assertEquals(listOf(1000), rezulto.forigotaj, "8 sciigoj — nur la unua super la suma limo foriras")
        assertEquals(7, rezulto.aktivaj.size)
        assertEquals(
            eroj(1001 to "muzaiko", 1002 to "sano", 1003 to "kernpunkto", 1004 to "muzaiko", 1005 to "sano", 1006 to "kernpunkto", 1007 to "varsoviavento"),
            rezulto.aktivaj,
        )
    }

    @Test
    fun poKanalaLimoForigasPlejMalnovajnDeTiujKanaloj() {
        val aktivaj = eroj(
            1000 to "kernpunkto", 1001 to "kernpunkto", 1002 to "kernpunkto",
            1003 to "muzaiko", 1004 to "muzaiko",
            1005 to "sano",
        )
        // nova sciigo de kernpunkto → 4 de tiu kanalo → la plej malnova (1000) foriras
        val rezulto = limiguSciigojn(aktivaj, SciigRegistrejoEro(1006, "kernpunkto"))
        assertEquals(listOf(1000), rezulto.forigotaj)
        assertEquals(
            eroj(1001 to "kernpunkto", 1002 to "kernpunkto", 1003 to "muzaiko", 1004 to "muzaiko", 1005 to "sano", 1006 to "kernpunkto"),
            rezulto.aktivaj,
        )
    }

    @Test
    fun poKanalaLimoNeTuŝasAliajnKanalojn() {
        val aktivaj = eroj(
            1000 to "muzaiko", 1001 to "muzaiko", 1002 to "muzaiko",
        )
        val rezulto = limiguSciigojn(aktivaj, SciigRegistrejoEro(1003, "kernpunkto"))
        assertTrue(rezulto.forigotaj.isEmpty(), "Nova kanalo ne forigas sciigojn de aliaj kanaloj")
        assertEquals(4, rezulto.aktivaj.size)
    }

    @Test
    fun ambaŭLimojKombinitaj() {
        // 3 de kernpunkto + 3 de muzaiko + 1 de sano = 7 aktivaj; nova de kernpunkto
        // unue forigas la plej malnovan de kernpunkto (po-kanala limo) — tio jam
        // sufiĉas por la suma limo (8 - 1 = 7), do muzaiko restas netuŝita
        val aktivaj = eroj(
            1000 to "kernpunkto", 1001 to "muzaiko", 1002 to "sano",
            1003 to "kernpunkto", 1004 to "muzaiko", 1005 to "muzaiko",
            1006 to "kernpunkto",
        )
        val rezulto = limiguSciigojn(aktivaj, SciigRegistrejoEro(1007, "kernpunkto"))
        assertEquals(listOf(1000), rezulto.forigotaj)
        assertEquals(
            eroj(1001 to "muzaiko", 1002 to "sano", 1003 to "kernpunkto", 1004 to "muzaiko", 1005 to "muzaiko", 1006 to "kernpunkto", 1007 to "kernpunkto"),
            rezulto.aktivaj,
        )
    }

    @Test
    fun samaIdAnstatauigasLaMalnovanEron() {
        val aktivaj = eroj(1000 to "kernpunkto", 1001 to "muzaiko")
        val rezulto = limiguSciigojn(aktivaj, SciigRegistrejoEro(1001, "sano"))
        assertEquals(
            eroj(1000 to "kernpunkto", 1001 to "sano"),
            rezulto.aktivaj,
            "Sama ID anstataŭigas, ne duobligas",
        )
        assertTrue(rezulto.forigotaj.isEmpty())
    }
}
