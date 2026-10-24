package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por la migrado de la historiaj x-sistemaj agord-ŝlosiloj al la novaj
 * (veraj supersignoj): la malnova ŝlosilo uziĝas se la nova ne ekzistas;
 * skribo ĉiam iras al la nova kaj forigas la malnovan.
 */
class AgordoMigradoTest {

    /** Eta memor-bazita Settings por testoj (sama ŝajlo kiel ApajNovaĵojTest). */
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

    @Test
    fun ĉenoMalnovaŜlosiloUziĝasKiamNovaMankas() {
        val settings = MapSettings()
        settings.putString("malnova", "AVALO")
        assertEquals("AVALO", settings.leguMigritanĈenon("nova", "malnova", "defaŭlto"))
    }

    @Test
    fun ĉenoNovaŜlosiloVenkas() {
        val settings = MapSettings()
        settings.putString("malnova", "AVALO")
        settings.putString("nova", "NOVA")
        assertEquals("NOVA", settings.leguMigritanĈenon("nova", "malnova", "defaŭlto"))
    }

    @Test
    fun ĉenoDefaŭltoKiamAmbaŭMankas() {
        val settings = MapSettings()
        assertEquals("defaŭlto", settings.leguMigritanĈenon("nova", "malnova", "defaŭlto"))
    }

    @Test
    fun ĉenoSkriboMigrasKajForigasLaMalnovan() {
        val settings = MapSettings()
        settings.putString("malnova", "AVALO")
        settings.skribuMigritanĈenon("nova", "malnova", "SKRIBITA")
        assertEquals("SKRIBITA", settings.getString("nova", ""))
        assertFalse(settings.hasKey("malnova"), "La malnova ŝlosilo foriĝas post skribo")
    }

    @Test
    fun buleoMalnovaŜlosiloUziĝasKiamNovaMankas() {
        val settings = MapSettings()
        settings.putBoolean("malnova", false)
        assertFalse(settings.leguMigritanBuleon("nova", "malnova", true))
    }

    @Test
    fun buleoDefaŭltoKiamAmbaŭMankas() {
        val settings = MapSettings()
        assertTrue(settings.leguMigritanBuleon("nova", "malnova", true))
    }

    @Test
    fun buleoSkriboMigrasKajForigasLaMalnovan() {
        val settings = MapSettings()
        settings.putBoolean("malnova", false)
        settings.skribuMigritanBuleon("nova", "malnova", true)
        assertTrue(settings.getBoolean("nova", false))
        assertFalse(settings.hasKey("malnova"))
    }

    @Test
    fun entjeroMalnovaŜlosiloUziĝasKiamNovaMankas() {
        val settings = MapSettings()
        settings.putInt("malnova", 4711)
        assertEquals(4711, settings.leguMigritanEntjeroKunNulo("nova", "malnova"))
    }

    @Test
    fun entjeroNuloKiamAmbaŭMankas() {
        val settings = MapSettings()
        assertNull(settings.leguMigritanEntjeroKunNulo("nova", "malnova"))
    }

    @Test
    fun entjeroSkriboMigrasKajForigasLaMalnovan() {
        val settings = MapSettings()
        settings.putInt("malnova", 4711)
        settings.skribuMigritanEntjeron("nova", "malnova", 1000)
        assertEquals(1000, settings.getInt("nova", -1))
        assertFalse(settings.hasKey("malnova"))
    }
}
