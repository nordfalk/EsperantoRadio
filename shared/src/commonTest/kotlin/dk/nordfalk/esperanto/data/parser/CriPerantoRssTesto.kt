package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Ortesto por la CRI-peranto: la RSS kiun la estonta transkoda servo
 * generos (vidu docs/nova/08_cri_esperanto_kanalo.md kaj
 * desktopApp/…/CriTranskodaDemo.kt) devas esti konsumebla de la apo per
 * la ĝenerala parsregulo 6.1 — sen aparta CRI-parsilo.
 *
 * La fiksaĵo `feeds/cri_peranto_feed.xml` estas reala eliro de
 * `./gradlew :desktopApp:criTranskodaDemo` (2026-09-25, unuaj 3 eroj).
 */
class CriPerantoRssTesto {

    private val parsilo = RssParsilo()

    private fun leguFiksaĵon(): String {
        val fluo = CriPerantoRssTesto::class.java.classLoader
            ?.getResourceAsStream("feeds/cri_peranto_feed.xml")
            ?: error("Fiksaĵo cri_peranto_feed.xml ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    @Test
    fun criRssParsasPerRegulo61() {
        val fluo = leguFiksaĵon()
        val kanalo = Kanalo(
            slug = "cri",
            nomo = "CRI — Ĉina Radio Internacia",
            podkastaRssUrl = "https://ekzemplo.eo/cri/cri_demo.rss"
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(3, elsendoj.size, "La fiksaĵo havas 3 elsendojn")

        val unua = elsendoj.first()
        assertTrue(unua.titolo.startsWith("Ĉina-usona kunlaboro"), "Titolo: ${unua.titolo}")
        assertEquals("2026-09-25", unua.dato)
        assertEquals(
            "https://ekzemplo.eo/cri/sonoj/ARTI1790328553999767.mp3",
            unua.fluo,
            "La elsendo devas ludi la MP3-on de la servo, ne la HLS-fluon de CRI"
        )
        assertTrue(unua.id.contains("ARTI1790328553999767"), "Id uzu la GUID: ${unua.id}")
        assertTrue(!unua.priskribo.isNullOrEmpty(), "Priskribo ne estu malplena")
        assertTrue(
            unua.retpaghoUrl == "https://esperanto.cri.cn/2026/09/25/ARTI1790328553999767",
            "Ligo al la originala CRI-artikolo: ${unua.retpaghoUrl}"
        )
    }

    @Test
    fun criRssHavasDaurojnKajBildon() {
        val elsendoj = parsilo.parsuRss(leguFiksaĵon(), Kanalo(slug = "cri", nomo = "CRI"))

        val unua = elsendoj.first()
        assertNotNull(unua.dauro, "Dauro devas veni de itunes:duration")
        assertEquals(137L, unua.dauro, "0:02:17 devas iĝi 137 sekundoj")
        assertEquals("https://ekzemplo.eo/cri/cri.png", unua.bildoUrl)
    }
}
