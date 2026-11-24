package dk.nordfalk.esperanto.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Emblemo-spegulado (FAROTA C56) kaj flua URL-normigo — la helpoj, kiujn la
 * konstruilo uzas por gastigi bildojn samdevene kaj forigi http-ligilojn.
 */
class EmblemojTest {

    @Test
    fun bazaBildaUrlDerivasLaRadikon() {
        assertEquals("https://javabog.dk/esperanto-radio", Emblemoj.bazaBildaUrl("https://javabog.dk/esperanto-radio/kanaloj/"))
        assertEquals("https://javabog.dk/esperanto-radio", Emblemoj.bazaBildaUrl("https://javabog.dk/esperanto-radio/kanaloj"))
        // sentraila bazo: restas, kiel ĝi estas (neniu dua divenado)
        assertEquals("https://alia.example", Emblemoj.bazaBildaUrl("https://alia.example"))
    }

    private fun bitokoj(vararg v: Int) = v.map { it.toByte() }.toByteArray()

    @Test
    fun formatoLaŭEnhavoRekonasMagiajnSignojn() {
        // PNG (89 50 4E 47 0D 0A 1A 0A)
        assertEquals("png", Emblemoj.formatoLaŭEnhavo(bitokoj(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + bitokoj(1, 2, 3)))
        // JPEG (FF D8 FF — SOI + marko)
        assertEquals("jpg", Emblemoj.formatoLaŭEnhavo(bitokoj(0xFF, 0xD8, 0xFF, 0xE0) + bitokoj(0, 0x10)))
        // GIF89a kaj GIF87a
        assertEquals("gif", Emblemoj.formatoLaŭEnhavo("GIF89a1x".encodeToByteArray()))
        assertEquals("gif", Emblemoj.formatoLaŭEnhavo("GIF87a1x".encodeToByteArray()))
    }

    @Test
    fun formatoLaŭEnhavoRifuzasNeBildojn() {
        // SVG — la XSS-vektoro (CWE-79): ne akceptebla eĉ kun image/png-kapo
        assertEquals(null, Emblemoj.formatoLaŭEnhavo("<svg onload=alert(1)>".encodeToByteArray()))
        // HTML-erarpaĝo, malplena/trunkita enhavo, nura kapo-prefikso
        assertEquals(null, Emblemoj.formatoLaŭEnhavo("<html><body>404</body></html>".encodeToByteArray()))
        assertEquals(null, Emblemoj.formatoLaŭEnhavo("GIF".encodeToByteArray())) // tro mallonga (nur 3 signoj)
        assertEquals(null, Emblemoj.formatoLaŭEnhavo(bitokoj()))
        assertEquals(null, Emblemoj.formatoLaŭEnhavo(bitokoj(0xFF, 0xD8))) // tro mallonga JPEG
    }

    @Test
    fun resskriboAnstataŭigasNurPrenitajnValorojn() {
        val teksto = """
            {
              "kanaloj": [
                { "slug": "a", "emblemoUrl": "https://a.example/e.png" },
                { "slug": "b", "emblemoUrl": "https://b.example/e.png" }
              ]
            }
        """.trimIndent()
        val nova = Emblemoj.resskribuEmblemojn(
            teksto,
            mapOf("https://a.example/e.png" to "https://javabog.dk/esperanto-radio/bildoj/a.png"),
        )
        assertTrue(nova.contains("\"https://javabog.dk/esperanto-radio/bildoj/a.png\""), nova)
        assertTrue(nova.contains("\"https://b.example/e.png\""), "ne-prenita URL restu: $nova")
        assertFalse(nova.contains("\"https://a.example/e.png\""), nova)
    }

    @Test
    fun resskriboAnstataŭigasĈiujnAperojnDeSamaURL() {
        val teksto = """
            { "a": "https://komuna.example/e.png", "b": "https://komuna.example/e.png" }
        """.trimIndent()
        val nova = Emblemoj.resskribuEmblemojn(
            teksto,
            mapOf("https://komuna.example/e.png" to "https://javabog.dk/esperanto-radio/bildoj/a.png"),
        )
        assertEquals(2, Regex("\"https://javabog.dk/esperanto-radio/bildoj/a.png\"").findAll(nova).count(), nova)
    }

    @Test
    fun fluoArchiveOrgEkzistasEnHttps() {
        assertEquals(
            "https://archive.org/download/UniversealaLindo001/UniversealaLindo001.mp3",
            FluSkribilo.normiguFluanUrl("http://archive.org/download/UniversealaLindo001/UniversealaLindo001.mp3"),
        )
        // aliaj http-URL-oj ne tuŝiĝas — ne ĉiu fonto havas https-ekvivalenton
        assertEquals("http://plain.example/epizodo.mp3", FluSkribilo.normiguFluanUrl("http://plain.example/epizodo.mp3"))
        assertEquals("https://kern.punkto.info/d.mp3", FluSkribilo.normiguFluanUrl("https://kern.punkto.info/d.mp3"))
    }
}
