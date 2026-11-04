package dk.nordfalk.esperanto.ui

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [supraBildoUrl] — la bildo por la supro de la detala elsendo-ekrano:
 * se la bildeto de la elsendo ankaŭ aperas en ĝia priskribo-HTML, la bildo
 * estus montrata duoble (supre + en la priskribo); tiam la kanal-emblemo
 * anstataŭas. La epizoda bildeto mem restas por la listoj.
 */
class ElsendoEkranoSupraBildoTest {

    private val kanalo = Kanalo(
        slug = "vv",
        nomo = "Varsovia Vento",
        emblemoUrl = "https://ekzemplo.com/emblemo.png",
    )

    private fun elsendo(bildoUrl: String?, priskriboHtml: String? = null) = Elsendo(
        id = "e1",
        kanaloSlug = "vv",
        titolo = "Titolo",
        fluo = "https://ekzemplo.com/a.mp3",
        dato = "2024-01-01",
        bildoUrl = bildoUrl,
        priskriboHtml = priskriboHtml,
    )

    @Test
    fun bildetoElHTMLMontrasKanalEmblemonAnstataŭe() {
        // Bildeto elektita el la HTML (ekz. Varsovia Vento) — ĝi aperas ankaŭ
        // en la priskribo-HTML → la detala ekrano montras la kanal-emblemon
        val e = elsendo(
            "https://ekzemplo.com/bildo.jpg",
            priskriboHtml = "<p><img src=\"https://ekzemplo.com/bildo.jpg\" /></p><p>Teksto</p>",
        )
        assertEquals("https://ekzemplo.com/emblemo.png", supraBildoUrl(e, kanalo))
    }

    @Test
    fun bildetoSamasKunBildoEnHTMLMontrasKanalEmblemonAnstataŭe() {
        // <itunes:image> kiu samas kun bildo de la HTML → same deduobligita
        val e = elsendo(
            "https://ekzemplo.com/emblemo.jpg",
            priskriboHtml = "<p>Teksto kaj <img src=\"https://ekzemplo.com/emblemo.jpg\" /></p>",
        )
        assertEquals("https://ekzemplo.com/emblemo.png", supraBildoUrl(e, kanalo))
    }

    @Test
    fun propraBildetoNeEnHTMLMontriĝas() {
        // Normala epizoda bildeto (ekz. el <itunes:image>), kiu NE aperas en la
        // priskribo-HTML → montrata supre sen ŝanĝo
        val e = elsendo(
            "https://ekzemplo.com/itunes.jpg",
            priskriboHtml = "<p>Priskribo sen bildo</p>",
        )
        assertEquals("https://ekzemplo.com/itunes.jpg", supraBildoUrl(e, kanalo))
    }

    @Test
    fun senKanaloLaBildetoMemMontriĝas() {
        // Sen kanal-konteksto (ekz. elŝutitaj elsendoj) — neniu emblemo haveblas;
        // retrofalo al la epizoda bildeto
        val e = elsendo(
            "https://ekzemplo.com/bildo.jpg",
            priskriboHtml = "<p><img src=\"https://ekzemplo.com/bildo.jpg\" /></p>",
        )
        assertEquals("https://ekzemplo.com/bildo.jpg", supraBildoUrl(e, null))
    }

    @Test
    fun senBildetoRezultasNulon() {
        assertNull(supraBildoUrl(elsendo(null), kanalo))
    }
}
