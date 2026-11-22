package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por [KanaloDeponejoImpl] — elŝuto de la kanalkonfiguro el la reto,
 * konservado en Settings kaj uzado anstataŭ la pakita version (kiel la malnova
 * apo: `App.initData` legas la konservitan, `hentEvtNyeGrunddata` elŝutas la novan
 * kaj konservas nur post sukcesa parsado).
 *
 * Rulas en desktopTest ĉar ĝi bezonas `ktor-client-mock` (MockEngine).
 */
class KanaloDeponejoImplTest {

    /** Eta memor-bazita Settings por testoj (kiel en ApajNovaĵojTest). */
    private class MapSettings : Settings {
        private val map = mutableMapOf<String, Any>()
        override val keys: Set<String> get() = map.keys.toSet()
        override val size: Int get() = map.size
        override fun clear() = map.clear()
        override fun remove(key: String) { map.remove(key) }
        override fun hasKey(key: String): Boolean = map.containsKey(key)
        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = map[key] as? Int
        override fun putLong(key: String, value: Long) { map[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = map[key] as? Long
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
        override fun getStringOrNull(key: String): String? = map[key] as? String
        override fun putFloat(key: String, value: Float) { map[key] = value }
        override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = map[key] as? Float
        override fun putDouble(key: String, value: Double) { map[key] = value }
        override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
        override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
        override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
    }

    /** La "pakita" konfiguro (en la testo: konstanta teksto anstataŭ resurco). */
    private val paktitaA = """{ "kanaloj": [ { "kodo": "aaa", "nomo": "Kanalo A" } ] }"""
    /** La "reta" konfiguro, kiun la servilo redonas (kun alia kampo por kontroli getAgordo). */
    private val retaB = """{ "arkivoBazoUrl": "https://ekzemplo.dk/arkivo/", "kanaloj": [ { "kodo": "bbb", "nomo": "Kanalo B" } ] }"""

    private fun deponejo(settings: Settings, motoro: MockEngine? = null) = KanaloDeponejoImpl(
        leganto = KanalAgordoLeganto(),
        paktitaTeksto = { paktitaA },
        httpKliento = motoro?.let { HttpClient(it) { install(HttpTimeout) } },
        settings = settings,
    )

    @Test
    fun senKaŝo_uzasPakitan() = runTest {
        val d = deponejo(MapSettings())
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
        assertEquals("aaa", d.getKanalo("aaa")?.slug)
        assertNull(d.getKanalo("bbb"))
    }

    @Test
    fun kaŝitaKonfiguro_venkas_super_pakita() = runTest {
        val st = MapSettings()
        st.putString(SettingsKeys.KANALKONFIGURO_TEKSTO, retaB)
        val d = deponejo(st)
        assertEquals(listOf("bbb"), d.getKanalojn(devige = false).map { it.slug })
        // getAgordo() liveras la kaŝitan agordon kun ĉiuj kampoj (ekz. arkivoBazoUrl)
        assertEquals("https://ekzemplo.dk/arkivo/", d.getAgordo().arkivoBazoUrl)
    }

    @Test
    fun rompitaKaŝo_rezervas_al_pakita() = runTest {
        val st = MapSettings()
        st.putString(SettingsKeys.KANALKONFIGURO_TEKSTO, "{ ne estas json")
        val d = deponejo(st)
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun retaNovaKonfiguro_konserviĝas_kaj_uziĝas() = runTest {
        val st = MapSettings()
        val motoro = MockEngine { respond(retaB, HttpStatusCode.OK) }
        val d = deponejo(st, motoro)

        d.refreŝiguElRete(devige = true)

        assertEquals(retaB, st.getStringOrNull(SettingsKeys.KANALKONFIGURO_TEKSTO))
        assertEquals(listOf("bbb"), d.getKanalojn(devige = false).map { it.slug })
        assertEquals("https://ekzemplo.dk/arkivo/", d.getAgordo().arkivoBazoUrl)
        assertEquals(listOf("bbb"), d.observiKanalojn().value.map { it.slug })
    }

    @Test
    fun retaEraro_ne_anstataŭas_la_lokan() = runTest {
        val st = MapSettings()
        st.putString(SettingsKeys.KANALKONFIGURO_TEKSTO, paktitaA) // konservita = A
        val motoro = MockEngine { respond("Eraro", HttpStatusCode.InternalServerError) }
        val d = deponejo(st, motoro)

        d.refreŝiguElRete(devige = true)

        assertEquals(paktitaA, st.getStringOrNull(SettingsKeys.KANALKONFIGURO_TEKSTO))
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun retaRompitaJson_ne_konserviĝas() = runTest {
        val st = MapSettings()
        // Fiaskpaĝo kun HTTP 200 (kaptiva portalo) — ne parseblas kiel JSON
        val motoro = MockEngine { respond("<html><body>404</body></html>", HttpStatusCode.OK) }
        val d = deponejo(st, motoro)

        d.refreŝiguElRete(devige = true)

        assertNull(st.getStringOrNull(SettingsKeys.KANALKONFIGURO_TEKSTO))
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun retaMalplenaKanalolisto_ne_akceptiĝas() = runTest {
        val st = MapSettings()
        val motoro = MockEngine { respond("""{ "kanaloj": [] }""", HttpStatusCode.OK) }
        val d = deponejo(st, motoro)

        d.refreŝiguElRete(devige = true)

        assertNull(st.getStringOrNull(SettingsKeys.KANALKONFIGURO_TEKSTO))
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
    }

    @Test
    fun ttl_evitas_tro_oftajn_elŝutojn() = runTest {
        val st = MapSettings()
        var petoj = 0
        val motoro = MockEngine { petoj++; respond(retaB, HttpStatusCode.OK) }
        val d = deponejo(st, motoro)

        d.refreŝiguElRete(devige = true)   // unua: elŝutas
        assertEquals(1, petoj)
        d.refreŝiguElRete(devige = false)  // ene de la TTL: ne elŝutas
        assertEquals(1, petoj)
        d.refreŝiguElRete(devige = true)   // devigita (malsupren-tiro): elŝutas denove
        assertEquals(2, petoj)
    }

    @Test
    fun malvarmaStartigo_refreŝigas_el_rete_en_fono() = runBlocking {
        val st = MapSettings()
        var petoj = 0
        val motoro = MockEngine { petoj++; respond(retaB, HttpStatusCode.OK) }
        val d = deponejo(st, motoro)

        // Malvarma ŝargo redonas tuj la lokan (pakitan) liston...
        assertEquals(listOf("aaa"), d.getKanalojn(devige = false).map { it.slug })
        // ...kaj en fono elŝutas la novan konfiguron kaj uzas ĝin
        val atendu = withTimeoutOrNull(5000) {
            while (petoj == 0 || d.observiKanalojn().value.map { it.slug } != listOf("bbb")) delay(20)
        }
        assertTrue(atendu != null, "La fona refreŝo ne alvenis ene de 5 s")
        assertEquals(retaB, st.getStringOrNull(SettingsKeys.KANALKONFIGURO_TEKSTO))
        assertEquals(listOf("bbb"), d.getKanalojn(devige = false).map { it.slug })
    }
}
