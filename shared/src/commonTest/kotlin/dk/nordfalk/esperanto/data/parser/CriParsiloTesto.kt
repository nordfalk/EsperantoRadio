package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Ortestoj por parsregulo 6.8 (CRI). La fiksaĵo `feeds/cri_aktualajo.json`
 * estas reala respondo de `POST https://esperanto.cri.cn/api/getData` por
 * la sekcia paĝo aktualajo (2026-09-25, malgrandigita al 3 kartoj po
 * kartgrupo — la strukturo estas netuŝita).
 */
class CriParsiloTesto {

    private val parsilo = CriParsilo()
    private val kanalo = Kanalo(
        slug = "cri",
        nomo = "CRI — Ĉina Radio Internacia",
        emblemoUrl = "https://ekzemplo.eo/cri.png",
    )

    private fun leguFiksaĵon(): String {
        val fluo = CriParsiloTesto::class.java.classLoader
            ?.getResourceAsStream("feeds/cri_aktualajo.json")
            ?: error("Fiksaĵo cri_aktualajo.json ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    @Test
    fun parsasLudeblajnKartojnKajIgnorasAliajn() {
        val elsendoj = parsilo.parsu(leguFiksaĵon(), kanalo)

        // 9 unikaj ludeblaj kartoj; la fiksaĵo enhavas 2 duoblaĵojn kaj
        // kartojn kun isPlay=0 aŭ sen elsendo-ligilo (navigaj kartoj)
        assertEquals(9, elsendoj.size, "Duoblaĵoj devas esti deduplitaj: ${elsendoj.map { it.id }}")

        // Ordigitaj de la plej nova
        val unua = elsendoj.first()
        assertEquals("cri:2026-09-22:ARTI1790076169550487", unua.id)
        assertEquals("2026-09-22", unua.dato)
        assertTrue(unua.titolo.startsWith("Transpaso de montoj"), "Titolo: ${unua.titolo}")
        assertTrue(unua.fluo.endsWith(".m3u8"), "Fluo devas esti HLS: ${unua.fluo}")
        assertEquals(208L, unua.dauro, "Dauro 208.84 s → 208 s")
        assertEquals("CRI — Ĉina Radio Internacia", unua.kanaloNomo)
        assertTrue(unua.retpaghoUrl!!.contains("ARTI1790076169550487"), "Ligo al la artikolo")

        // Malnovaj programoj estas MP4, ne HLS — ankaŭ ludeblaj per ExoPlayer
        val mp4 = elsendoj.last()
        assertTrue(mp4.fluo.endsWith(".mp4"), "La plej malnova devas esti MP4: ${mp4.fluo}")
        assertTrue(mp4.dato.startsWith("2021-"), "Malnova programo: ${mp4.dato}")
    }

    @Test
    fun priskriboKajBildoVenasDeLaKarto() {
        val elsendoj = parsilo.parsu(leguFiksaĵon(), kanalo)

        val tutmonda = elsendoj.find { it.id.contains("ARTI1789548552532187") }
        assertNotNull(tutmonda, "ARTI1789548552532187 devas ekzisti")
        assertTrue(
            tutmonda.priskribo!!.startsWith("Tra la vasta afrika kontinento"),
            "Priskribo el la kampo brief: ${tutmonda.priskribo}"
        )
        // photo.large estas malplena en la fiksaĵo — la emblemo de la kanalo estas la retroiro
        assertEquals("https://ekzemplo.eo/cri.png", tutmonda.bildoUrl)
    }

    @Test
    fun tolerasEraranSekcion() {
        val kombinita = "{\"rezulto\": nevalida" + CriParsilo.SEKCIO_APARTIGILON + leguFiksaĵon()
        val elsendoj = parsilo.parsu(kombinita, kanalo)
        assertEquals(9, elsendoj.size, "Erara sekcio ne devas paneigi la ceterajn (regulo 4)")
    }

    @Test
    fun formuDatonKonvertasEpokonAlUtc() {
        assertEquals("2026-09-22", parsilo.formuDaton(1790076173000L))
        assertEquals("1970-01-01", parsilo.formuDaton(0L))
    }
}
