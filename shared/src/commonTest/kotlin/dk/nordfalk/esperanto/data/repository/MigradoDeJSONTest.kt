package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regres-testoj por la persistigitaj JSON-ŝlosiloj (ortografia renomo, 2026-10-24):
 * la nova apo devas LEGI datumon skribitan de malnova versio (historiaj
 * x-sistemaj ŝlosiloj, per @JsonNames) kaj SKRIBI la novajn, ĝustliterajn
 * ŝlosilojn (la posedanto decidis: malnova versio ne devas legi la novan).
 */
class MigradoDeJSONTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun kanaloLegasHistoriajnŜlosilojn() {
        val kanalo = json.decodeFromString<Kanalo>(
            """{"slug":"kernpunkto","nomo":"Kernpunkto","retposhto":"info@ekzemplo.org","defaltSxatata":true}"""
        )
        assertEquals("info@ekzemplo.org", kanalo.retpoŝto, "retposhto → retpoŝto")
        assertTrue(kanalo.defaŭltŜatata, "defaltSxatata → defaŭltŜatata")
    }

    @Test
    fun kanaloSkribasNovajnŜlosilojn() {
        val kanalo = Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", retpoŝto = "a@b.c", defaŭltŜatata = true)
        val teksto = json.encodeToString(Kanalo.serializer(), kanalo)
        assertTrue("\"retpoŝto\"" in teksto, "Skribado devas uzi la novan ŝlosilon retpoŝto: $teksto")
        assertTrue("\"defaŭltŜatata\"" in teksto, "Skribado devas uzi la novan ŝlosilon defaŭltŜatata: $teksto")
    }

    @Test
    fun elsendoLegasHistoriajnŜlosilojn() {
        val elsendo = json.decodeFromString<Elsendo>(
            """{"id":"kernpunkto:2026","kanaloSlug":"kernpunkto","titolo":"Titolo","dato":"2026-01-01","fluo":"https://x/a.mp3","dauro":123,"retpaghoUrl":"https://ekzemplo.org"}"""
        )
        assertEquals(123L, elsendo.daŭro, "dauro → daŭro")
        assertEquals("https://ekzemplo.org", elsendo.retpaĝoUrl, "retpaghoUrl → retpaĝoUrl")
    }

    @Test
    fun elsendoSkribasNovajnŜlosilojn() {
        val elsendo = Elsendo(
            id = "kernpunkto:2026", kanaloSlug = "kernpunkto", titolo = "T", dato = "2026-01-01", fluo = "https://x/a.mp3",
            daŭro = 123, retpaĝoUrl = "https://ekzemplo.org",
        )
        val teksto = json.encodeToString(Elsendo.serializer(), elsendo)
        assertTrue("\"daŭro\"" in teksto, "Skribado devas uzi la novan ŝlosilon daŭro: $teksto")
        assertTrue("\"retpaĝoUrl\"" in teksto, "Skribado devas uzi la novan ŝlosilon retpaĝoUrl: $teksto")
    }

    @Test
    fun ludataElsendoLegasKajSkribasDauroms() {
        val ludata = json.decodeFromString<LudataElsendo>(
            """{"elsendoId":"kernpunkto:2026","kanaloSlug":"kernpunkto","dauroMs":65000}"""
        )
        assertEquals(65000L, ludata.daŭroMs)
        val teksto = json.encodeToString(LudataElsendo.serializer(), ludata)
        assertTrue("\"daŭroMs\"" in teksto, "Skribado devas uzi la novan ŝlosilon daŭroMs: $teksto")
    }

    @Test
    fun vojoDiskriminilojRestasHistoriaj() {
        // La persistigita navig-stato uzas la klasan nomon kiel diskriminilon;
        // la renomitaj Vojo.Serĉo/Elŝutoj devas gardi la historian FQN-on.
        val serĉo = dk.nordfalk.esperanto.navigation.Vojo.Serĉo.serializer().descriptor.serialName
        assertEquals("dk.nordfalk.esperanto.navigation.Vojo.Sercxo", serĉo,
            "Vojo.Serĉo devas havi la historian diskriminilon")
        val elŝutoj = dk.nordfalk.esperanto.navigation.Vojo.Elŝutoj.serializer().descriptor.serialName
        assertEquals("dk.nordfalk.esperanto.navigation.Vojo.Elshutoj", elŝutoj,
            "Vojo.Elŝutoj devas havi la historian diskriminilon")
        // La nerennomitaj vojoj daŭre uzas sian klasan nomon
        assertEquals(
            "dk.nordfalk.esperanto.navigation.Vojo.Hejmo",
            dk.nordfalk.esperanto.navigation.Vojo.Hejmo.serializer().descriptor.serialName,
        )
        assertNotNull(dk.nordfalk.esperanto.navigation.Vojo.Ludvico)
    }
}
