package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SonfontoKodiloTest {

    private val elsendo = Elsendo(
        id = "peranto:2026-09-23", kanaloSlug = "peranto", kanaloNomo = "Esperanta Retradio",
        titolo = "Skeĉo", dato = "2026-09-23", fluo = "https://x/skecxo.mp3", bildoUrl = "https://x/b.jpg",
    )

    @Test
    fun cxiujSpecojTraMalkodigo() {
        val fontoj = listOf(
            Sonfonto.RektaKanalo(Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://x/live.m3u8")),
            Sonfonto.ElsendoFonto(elsendo),
            Sonfonto.LokaElsendo(elsendo, "/data/elsendoj/skecxo.mp3"),
        )
        for (fonto in fontoj) {
            assertEquals(fonto, SonfontoKodilo.malkodigu(SonfontoKodilo.kodigu(fonto)))
        }
    }

    @Test
    fun malplenaAuxFusxaTekstoDonasNull() {
        assertNull(SonfontoKodilo.malkodigu(null))
        assertNull(SonfontoKodilo.malkodigu(""))
        assertNull(SonfontoKodilo.malkodigu("{ne json"))
        assertNull(SonfontoKodilo.malkodigu("""{"type":"nekonata"}"""))
    }
}
