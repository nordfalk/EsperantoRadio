package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.config.FORA_KONFIGURO_URL
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testoj por la fora kanalkonfiguro (kiel en la malnova apo): elŝuto de la
 * servilo, konservado en la agordoj, uzado de la kaŝita versio ĉe relanĉo,
 * rezervo al la pakita versio ĉe fiasko.
 */
class KanaloDeponejoImplTest {

    /** Eta memor-bazita Settings por testoj (sama stilo kiel AgordoMigradoTest). */
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

    private fun konfiguro(vararg kodoj: String): String =
        """{ "kanaloj": [ ${kodoj.joinToString(",") { """{ "kodo": "$it", "nomo": "$it" }""" }} ] }"""

    private fun deponejo(
        settings: Settings? = MapSettings(),
        pakita: String = konfiguro("muzaiko"),
        elŝutTeksto: (suspend (String) -> String)? = null,
    ) = KanaloDeponejoImpl(
        leganto = KanalAgordoLeganto(),
        paktitaTeksto = { pakita },
        settings = settings,
        elŝutTeksto = elŝutTeksto,
    )

    @Test
    fun senForaKaŝo_legasPaktitanKonfiguron() = runTest {
        val d = deponejo(pakita = konfiguro("muzaiko", "peranto"))
        assertEquals(listOf("muzaiko", "peranto"), d.getKanalojn().map { it.slug })
    }

    @Test
    fun foraKaŝoEnAgordoj_venkasSuperPaktita() = runTest {
        val settings = MapSettings()
        settings.putString(SettingsKeys.FORA_KANALKONFIGURO, konfiguro("fornova"))
        val d = deponejo(settings = settings, pakita = konfiguro("muzaiko"))
        assertEquals(listOf("fornova"), d.getKanalojn().map { it.slug })
    }

    @Test
    fun fuŝitaForaKaŝo_rezervoAlPaktita() = runTest {
        val settings = MapSettings()
        settings.putString(SettingsKeys.FORA_KANALKONFIGURO, "{ rompita json")
        val d = deponejo(settings = settings, pakita = konfiguro("muzaiko"))
        assertEquals(listOf("muzaiko"), d.getKanalojn().map { it.slug })
    }

    @Test
    fun fortoRefreski_elŝutasKajKonservasKajNovanDeponejonLegasElKaŝo() = runTest {
        val settings = MapSettings()
        val d1 = deponejo(settings = settings, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { url ->
                assertEquals(FORA_KONFIGURO_URL, url)
                konfiguro("fornova")
            })
        // "relanĉo": elŝuto kaj ĝisdatigo
        assertEquals(listOf("fornova"), d1.getKanalojn(fortoRefresigi = true).map { it.slug })
        // konservita en la agordoj?
        assertEquals(konfiguro("fornova"), settings.getStringOrNull(SettingsKeys.FORA_KANALKONFIGURO))
        assertTrue(settings.getLong(SettingsKeys.FORA_KANALKONFIGURO_TEMPO, 0L) > 0L)
        // "nova relanĉo": nova deponejo super la sama settings legas la kaŝon SEN reto
        val d2 = deponejo(settings = settings, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { error("ne rajtas peti la reton") })
        assertEquals(listOf("fornova"), d2.getKanalojn().map { it.slug })
    }

    @Test
    fun fiaskoDeElŝuto_konservasLaMalnovanKajNeKraŝas() = runTest {
        val settings = MapSettings()
        val d = deponejo(settings = settings, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { error("reto mankas") })
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertFalse(settings.hasKey(SettingsKeys.FORA_KANALKONFIGURO))
    }

    @Test
    fun neParseblaElŝutitaTeksto_neKonserviĝas() = runTest {
        val settings = MapSettings()
        val d = deponejo(settings = settings, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { "{ ankoraŭ rompita" })
        assertEquals(listOf("muzaiko"), d.getKanalojn(fortoRefresigi = true).map { it.slug })
        assertFalse(settings.hasKey(SettingsKeys.FORA_KANALKONFIGURO))
    }

    @Test
    fun senFonaScope_neElŝutasEnFono() = runTest {
        val settings = MapSettings()
        val d = deponejo(settings = settings, pakita = konfiguro("muzaiko"),
            elŝutTeksto = { error("ne rajtas okazi — neniu fona amplekso") })
        // sen fonaScope kaj sen fortoRefresigi: neniu reta voko, nur loka ŝargo
        assertEquals(listOf("muzaiko"), d.getKanalojn().map { it.slug })
    }

    @Test
    fun getAgordon_donasLaEfektivanAgordon() = runTest {
        val settings = MapSettings()
        settings.putString(SettingsKeys.FORA_KANALKONFIGURO, """{ "arkivoBazoUrl": "https://ekzemplo.test/", "kanaloj": [ { "kodo": "x", "nomo": "X" } ] }""")
        val d = deponejo(settings = settings, pakita = konfiguro("muzaiko"))
        assertEquals("https://ekzemplo.test/", d.getAgordon().arkivoBazoUrl)
        assertEquals(listOf("x"), d.getKanalojn().map { it.slug })
        assertEquals("x", d.observiKanalojn().value.single().slug)
    }
}
