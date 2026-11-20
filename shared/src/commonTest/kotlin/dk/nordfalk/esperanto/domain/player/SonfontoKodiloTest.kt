package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SonfontoKodiloTest {

    private val elsendo = Elsendo(
        id = "peranto:2026-09-23", kanaloSlug = "peranto", kanaloNomo = "Esperanta Retradio",
        titolo = "Skeĉo", dato = "2026-09-23", fluo = "https://x/skeĉo.mp3", bildoUrl = "https://x/b.jpg",
    )

    @Test
    fun ĉiujSpecojTraMalkodigo() {
        val fontoj = listOf(
            Sonfonto.RektaKanalo(Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://x/live.m3u8")),
            Sonfonto.ElsendoFonto(elsendo),
            Sonfonto.LokaElsendo(elsendo, "/data/elsendoj/skeĉo.mp3"),
        )
        for (fonto in fontoj) {
            assertEquals(fonto, SonfontoKodilo.malkodigu(SonfontoKodilo.kodigu(fonto)))
        }
    }

    @Test
    fun malplenaAŭFuŝaTekstoDonasNull() {
        assertNull(SonfontoKodilo.malkodigu(null))
        assertNull(SonfontoKodilo.malkodigu(""))
        assertNull(SonfontoKodilo.malkodigu("{ne json"))
        assertNull(SonfontoKodilo.malkodigu("""{"type":"nekonata"}"""))
    }

    @Test // (FAROTA C11) la pezaj priskriboj ne vojaĝas trans Binder en MediaMetadata.extras
    fun kodiguForigasPezajnPriskribojn() {
        val grandaPriskribo = "<p>".repeat(20_000) // ~60 KB da HTML kun <audio>-elementoj
        val peza = elsendo.copy(priskribo = grandaPriskribo, priskriboHtml = grandaPriskribo)
        val fonto = Sonfonto.ElsendoFonto(peza)

        val kodita = SonfontoKodilo.kodigu(fonto)

        assertTrue(kodita.length < 2_000, "La kodita fonto devas resti malgranda, estis ${kodita.length} signoj")
        assertFalse(kodita.contains("<p>"), "La HTML-priskribo ne rajtas aperi en la kodita teksto")

        val malkodita = SonfontoKodilo.malkodigu(kodita) as Sonfonto.ElsendoFonto
        // La necesaj kampoj restas, la pezaj estas forigitaj
        assertEquals(elsendo.id, malkodita.elsendo.id)
        assertEquals(elsendo.titolo, malkodita.elsendo.titolo)
        assertEquals(elsendo.fluo, malkodita.elsendo.fluo)
        assertEquals(elsendo.kanaloSlug, malkodita.elsendo.kanaloSlug)
        assertEquals(elsendo.kanaloNomo, malkodita.elsendo.kanaloNomo)
        assertEquals(elsendo.dato, malkodita.elsendo.dato)
        assertEquals(elsendo.bildoUrl, malkodita.elsendo.bildoUrl)
        assertNull(malkodita.elsendo.priskribo)
        assertNull(malkodita.elsendo.priskriboHtml)
    }

    @Test // (FAROTA C11) LokaElsendo: la dosier-vojo restas, la priskriboj foriras
    fun kodiguLokaElsendoAnkaŭMaldikas() {
        val peza = elsendo.copy(priskriboHtml = "<audio src='x'></audio>".repeat(1000))
        val kodita = SonfontoKodilo.kodigu(Sonfonto.LokaElsendo(peza, "/data/elsendoj/x.mp3"))
        val malkodita = SonfontoKodilo.malkodigu(kodita) as Sonfonto.LokaElsendo
        assertEquals("/data/elsendoj/x.mp3", malkodita.dosieroVojo)
        assertNull(malkodita.elsendo.priskriboHtml)
        assertTrue(kodita.length < 2_000)
    }
}
