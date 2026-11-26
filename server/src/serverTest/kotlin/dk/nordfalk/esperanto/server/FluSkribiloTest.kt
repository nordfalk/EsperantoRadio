package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * FluSkribilo — la enhavo de la arkiva flue: ero-nivelaj <itunes:image> (PR #214:
 * spegulitaj elsendlaj bildetoj, samdevenaj por la retversio) kaj la kanal-emblemo.
 */
class FluSkribiloTest {

    private fun provizoraFuo(): File = createTempFile("fluo", ".xml").toFile()

    private fun elsendo(id: String, bildo: String?) = Elsendo(
        id = id,
        kanaloSlug = "provo",
        kanaloNomo = "Provo",
        titolo = "Titolo de $id",
        priskribo = "Priskribo <img src=\"https://ekstera.example/bildo.jpg\">",
        bildoUrl = bildo,
        dato = "2026-11-25",
        fluo = "https://ekstera.example/elsendo.mp3",
    )

    @Test
    fun eroKunBildetoRicevasItunesImage() {
        val celo = provizoraFuo()
        FluSkribilo.skribu(
            Kanalo(nomo = "Provo", slug = "provo"),
            listOf(elsendo("provo:1", "https://javabog.dk/esperanto-radio/bildoj/elsendoj/abc123.jpg")),
            emptyMap(), emptyMap(), celo,
        )
        val teksto = celo.readText()
        assertTrue(
            "https://javabog.dk/esperanto-radio/bildoj/elsendoj/abc123.jpg" in teksto,
            "la spegulita bildeto devas aperi kiel <itunes:image> en la ero: $teksto",
        )
        celo.delete()
    }

    @Test
    fun eroSenBildetoNeRicevasItunesImage() {
        val celo = provizoraFuo()
        FluSkribilo.skribu(
            Kanalo(nomo = "Provo", slug = "provo"),
            listOf(elsendo("provo:1", bildo = null)),
            emptyMap(), emptyMap(), celo,
        )
        val erParto = celo.readText().substringAfter("<item>")
        assertFalse("itunes:image" in erParto, "ero sen bildeto ne havu <itunes:image>: $erParto")
        celo.delete()
    }

    @Test
    fun bildetaRadikoEstasDeterminismaKajMalsamaPorMalsamajUrloj() {
        val a = Emblemoj.bildetaRadiko("https://ekstera.example/bildo.jpg")
        val b = Emblemoj.bildetaRadiko("https://ekstera.example/bildo.jpg")
        val c = Emblemoj.bildetaRadiko("https://alia.example/bildo.jpg")
        assertEquals(40, a.length, "SHA-1 = 40 hex-signoj")
        assertTrue(a.all { it in "0123456789abcdef" }, "nur hex-signoj")
        assertEquals(a, b, "sama URL ≠ sama radiko — la ekzisto-kontrolo dependas de tio")
        assertFalse(a == c, "malsamaj URL-oj devas malsami")
    }
}
