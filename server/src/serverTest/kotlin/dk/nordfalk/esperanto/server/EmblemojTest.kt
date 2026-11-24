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

    @Test
    fun sufiksoElTipoValorasPliOlElUrl() {
        assertEquals("png", Emblemoj.sufikso("image/png", "https://x.example/bildo.jpg"))
        assertEquals("jpg", Emblemoj.sufikso("image/jpeg; charset=utf-8", "https://x.example/emblemo"))
        assertEquals("webp", Emblemoj.sufikso("image/webp", "https://x.example/b"))
        assertEquals("gif", Emblemoj.sufikso(null, "https://x.example/bildo.gif?size=300"))
        assertEquals("jpg", Emblemoj.sufikso("text/html", "https://x.example/bildo.php"))
        // nur konataj bild-sufiksoj akceptiĝas el la URL — .php/ajnaj ne iĝu dosiernomo
        assertEquals("jpg", Emblemoj.sufikso(null, "https://x.example/emblemo"))
        assertEquals("png", Emblemoj.sufikso(null, "https://x.example/emblemo.PNG?grando=120"))
        assertEquals("svg", Emblemoj.sufikso("image/svg+xml", "https://x.example/l.svg"))
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
