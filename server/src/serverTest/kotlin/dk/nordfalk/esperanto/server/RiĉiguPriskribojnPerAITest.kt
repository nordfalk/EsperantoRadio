package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Riĉigo de priskriboj per AI-rezumo (ArkivKonstruilo.riĉigu): la sojlo 150 signoj,
 * la konservado de la originala priskribo kaj la averto inter ĝi kaj la rezumo.
 * La rezumo-kaŝo estas provizora dosierujo — neniu dependeco de realaj datumoj.
 */
class RiĉiguPriskribojnPerAITest {

    private val averto = "Jen rezumo farita de la sondosiero per AI. Eraroj povas okazi:"

    private fun provizorujo(rezumo: String?): File {
        val dir = File.createTempFile("ricxigu", "").let { it.delete(); it.mkdir(); it }
        val kanalo = Kanalo(slug = "tests", nomo = "Testo")
        val elsendo = Elsendo(
            id = "tests:abc",
            kanaloSlug = kanalo.slug,
            kanaloNomo = kanalo.nomo,
            titolo = "Elsendo 01.01.2026",
            priskribo = if (rezumo == null) "" else "Komuna priskribo de la elsendo kun sufiĉe da frazoj. Jen la dua frazo. Kaj tria frazo pri la enhavo. Kvara frazo por plenigi la rezumon.",
            dato = "2026-01-01",
            fluo = "https://ekzemplo/a.mp3",
        )
        if (rezumo != null) {
            val nomo = ArkivKonstruilo.transkriptoDosieroNomo(elsendo.id)
            File(dir, "rezumoj/${kanalo.slug}").mkdirs()
            File(dir, "rezumoj/${kanalo.slug}/$nomo.txt").writeText(rezumo)
        }
        return dir
    }

    @Test
    fun mallongaPriskriboRicevasRezumonSubLaOriginalo() {
        val dir = provizorujo("Temo de la elsendo.\n- unua punkto\n- dua punkto")
        try {
            val kanalo = Kanalo(slug = "tests", nomo = "Testo")
            val elsendo = Elsendo(
                id = "tests:abc", kanaloSlug = kanalo.slug, kanaloNomo = kanalo.nomo,
                titolo = "Elsendo 01.01.2026", priskribo = "Mallonga priskribo.", dato = "2026-01-01", fluo = "https://x/a.mp3",
            )
            val (priskriboj, _) = ArkivKonstruilo.riĉigu(kanalo, listOf(elsendo), dir)
            val html = priskriboj[elsendo.id]!!
            assertTrue(html.startsWith("<p>Mallonga priskribo.</p>"), html)
            assertTrue("<p><em>$averto</em></p>" in html, html)
            assertTrue("<li>unua punkto</li>" in html, html)
            // la averto staras INTER la originalo kaj la rezumo
            assertTrue(html.indexOf("Mallonga priskribo") < html.indexOf("<em>$averto"), html)
            assertTrue(html.indexOf("<em>$averto") < html.indexOf("unua punkto"), html)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun malplenaPriskriboRicevasNurAvertonKajRezumon() {
        val dir = provizorujo("Temo de la elsendo.\n- unua punkto")
        try {
            val kanalo = Kanalo(slug = "tests", nomo = "Testo")
            val elsendo = Elsendo(
                id = "tests:abc", kanaloSlug = kanalo.slug, kanaloNomo = kanalo.nomo,
                titolo = "Elsendo 01.01.2026", priskribo = "", dato = "2026-01-01", fluo = "https://x/a.mp3",
            )
            val (priskriboj, _) = ArkivKonstruilo.riĉigu(kanalo, listOf(elsendo), dir)
            val html = priskriboj[elsendo.id]!!
            assertTrue(html.startsWith("<p><em>$averto</em></p>"), html)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun precizoLimoKvindekRicevasRezumo() {
        val dir = provizorujo("Temo.\n- punkto unu")
        try {
            val kanalo = Kanalo(slug = "tests", nomo = "Testo")
            val priskribo = "x".repeat(150)
            val elsendo = Elsendo(
                id = "tests:abc", kanaloSlug = kanalo.slug, kanaloNomo = kanalo.nomo,
                titolo = "Elsendo 01.01.2026", priskribo = priskribo, dato = "2026-01-01", fluo = "https://x/a.mp3",
            )
            val (priskriboj, _) = ArkivKonstruilo.riĉigu(kanalo, listOf(elsendo), dir)
            assertTrue(elsendo.id in priskriboj)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun troLongaPriskriboRestasSenRezumo() {
        val dir = provizorujo("Temo.\n- punkto unu")
        try {
            val kanalo = Kanalo(slug = "tests", nomo = "Testo")
            val elsendo = Elsendo(
                id = "tests:abc", kanaloSlug = kanalo.slug, kanaloNomo = kanalo.nomo,
                titolo = "Elsendo 01.01.2026", priskribo = "x".repeat(151), dato = "2026-01-01", fluo = "https://x/a.mp3",
            )
            val (priskriboj, _) = ArkivKonstruilo.riĉigu(kanalo, listOf(elsendo), dir)
            assertEquals(emptyMap(), priskriboj)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun senRezumoKajSenTransskriboNenioGeneratas() {
        val dir = provizorujo(null)
        try {
            val kanalo = Kanalo(slug = "tests", nomo = "Testo")
            val elsendo = Elsendo(
                id = "tests:abc", kanaloSlug = kanalo.slug, kanaloNomo = kanalo.nomo,
                titolo = "Elsendo 01.01.2026", priskribo = "", dato = "2026-01-01", fluo = "https://x/a.mp3",
            )
            val (priskriboj, _) = ArkivKonstruilo.riĉigu(kanalo, listOf(elsendo), dir)
            assertEquals(emptyMap(), priskriboj)
        } finally {
            dir.deleteRecursively()
        }
    }
}
