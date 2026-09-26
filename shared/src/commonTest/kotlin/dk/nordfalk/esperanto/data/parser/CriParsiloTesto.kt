package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
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

    /** La kaŝmemora formato: ĉiu peco estas "<sekci-URL>\n<JSON>". */
    private val AKTUALAJO_URL = "https://esperanto.cri.cn/aktualajo/page.shtml"

    private fun leguFiksaĵon(): String {
        val fluo = CriParsiloTesto::class.java.classLoader
            ?.getResourceAsStream("feeds/cri_aktualajo.json")
            ?: error("Fiksaĵo cri_aktualajo.json ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    private fun kunSekcio(): String = AKTUALAJO_URL + "\n" + leguFiksaĵon()

    @Test
    fun parsasLudeblajnKartojnKunSekciEtikedo() {
        val elsendoj = parsilo.parsu(kunSekcio(), kanalo)

        // 9 unikaj ludeblaj kartoj; la fiksaĵo enhavas 2 duoblaĵojn kaj
        // kartojn kun isPlay=0 aŭ sen elsendo-ligilo (navigaj kartoj)
        assertEquals(9, elsendoj.size, "Duoblaĵoj devas esti deduplitaj: ${elsendoj.map { it.id }}")

        // Ordigitaj de la plej nova; la titolo portas la sekci-etikedon
        val unua = elsendoj.first()
        assertEquals("cri:2026-09-22:ARTI1790076169550487", unua.id)
        assertEquals("2026-09-22", unua.dato)
        assertTrue(
            unua.titolo.startsWith("Aktuala: Transpaso de montoj"),
            "La titolo devas porti la etikedon de la sekci-paĝo: ${unua.titolo}"
        )
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
    fun priskriboKAjBildoVenasDeLaKarto() {
        val elsendoj = parsilo.parsu(kunSekcio(), kanalo)

        val tutmonda = elsendoj.find { it.id.contains("ARTI1789548552532187") }
        assertNotNull(tutmonda, "ARTI1789548552532187 devas ekzisti")
        assertTrue(
            tutmonda.priskribo!!.startsWith("Tra la vasta afrika kontinento"),
            "Priskribo el la kampo brief: ${tutmonda.priskribo}"
        )
        // photo.large estas malplena — photo.thurm estas la bildo de la artikolo
        assertTrue(
            tutmonda.bildoUrl!!.endsWith("1789548533166_650.jpg"),
            "Bildo devas veni el photo.thurm, ne el la kanal-emblemo: ${tutmonda.bildoUrl}"
        )
    }

    @Test
    fun tolerasEraranSekcionKAjMalnovanKashon() {
        // Erara peco inter validaj — regulo 4
        val kunEraro = "{\"rezulto\": nevalida" + CriParsilo.SEKCIO_APARTIGILON + kunSekcio()
        assertEquals(9, parsilo.parsu(kunEraro, kanalo).size)

        // Malnova kaŝmemoro sen URL-prefikso (antaŭ la etikedoj) — akceptebla
        val malnova = leguFiksaĵon()
        val elsendoj = parsilo.parsu(malnova, kanalo)
        assertEquals(9, elsendoj.size)
        assertTrue(elsendoj.first().titolo.startsWith("CRI: "), "Etikedo sen URL estas 'CRI'")
    }

    @Test
    fun sekcioEtikedojMapigasPagxojnĜuste() {
        assertEquals("Aktuala", parsilo.sekcioEtikedo("https://esperanto.cri.cn/aktualajo/page.shtml"))
        assertEquals("LuciaStudio", parsilo.sekcioEtikedo("https://esperanto.cri.cn/LuciaStudio/page.shtml"))
        assertEquals("E-klubo", parsilo.sekcioEtikedo("https://esperanto.cri.cn/eklubo/page.shtml"))
        assertEquals("Mirinda", parsilo.sekcioEtikedo("https://esperanto.cri.cn/mirinda/page.shtml"))
        assertEquals("Novaĵo", parsilo.sekcioEtikedo("https://esperanto.cri.cn/news/page.shtml"))
        // Subpaĝoj de LuciaStudio same ricevas "LuciaStudio"
        assertEquals(
            "LuciaStudio",
            parsilo.sekcioEtikedo("https://esperanto.cri.cn/LuciaStudio/highlight/page.shtml")
        )
        assertEquals(
            "LuciaStudio",
            parsilo.sekcioEtikedo("https://esperanto.cri.cn/luciastudio/WhyChinaLoveChina/page.shtml")
        )
        // Nekonata sekci-paĝo: la vojo mem fariĝas la etikedo
        assertEquals("novasekcio", parsilo.sekcioEtikedo("https://esperanto.cri.cn/novasekcio/page.shtml"))
    }

    @Test
    fun formuDatonKonvertasEpokonAlUtc() {
        assertEquals("2026-09-22", parsilo.formuDaton(1790076173000L))
        assertEquals("1970-01-01", parsilo.formuDaton(0L))
    }

    @Test
    fun malnovaKashmemoroFormatoEstasRekonata() {
        val novaFormato =
            "https://esperanto.cri.cn/aktualajo/page.shtml\n{\"a\":1}" +
                CriParsilo.SEKCIO_APARTIGILON +
                "https://esperanto.cri.cn/LuciaStudio/page.shtml\n{\"a\":2}"
        assertFalse(parsilo.estasMalnovaFormato(novaFormato), "Nova formato kun URL-oj ne estas malnova")

        // Malnova formato: nura JSON sen sekci-URL (el la tempo antaŭ la etikedoj)
        assertTrue(parsilo.estasMalnovaFormato(leguFiksaĵon()), "Nura JSON = malnova formato")

        // Miksita (ekz. fiasko meze de ĝisdatigo) ankaŭ estas traktata kiel malnova
        val miksita =
            "https://esperanto.cri.cn/aktualajo/page.shtml\n{\"a\":1}" +
                CriParsilo.SEKCIO_APARTIGILON + "{\"a\":2}"
        assertTrue(parsilo.estasMalnovaFormato(miksita), "Neniu peco sen URL rajtas resti")

        // Malplena kaŝmemoro ne estu traktata kiel malnova (nur kiel malplena)
        assertFalse(parsilo.estasMalnovaFormato(""), "Malplena kaŝmemoro ne estas malnova formato")
    }
}
