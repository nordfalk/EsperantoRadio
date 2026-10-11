package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.domain.model.Alarmo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testoj por la korupta-JSON-rezervo de PersistantaAlarmoDeponejo (C4).
 */
class AlarmoRezervoTest {

    /** Eta memor-bazita Settings por testoj. */
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

    private val sugestoj = listOf(
        Alarmo(id = 900, horo = 7, minuto = 0, kanaloSlug = "muzaiko", aktiva = false, etikedo = "Sugesto"),
    )

    @Test
    fun legasKajPersistasAlarmojn() = runTest {
        val settings = MapSettings()
        val deponejo = PersistantaAlarmoDeponejo(settings, sugestoj)

        assertEquals(1, deponejo.observiAlarmojn().value.size, "Komenca stato montras la sugestojn")
        deponejo.krei(Alarmo(id = 0, horo = 16, minuto = 10, kanaloSlug = "peranto"))
        val persistita = settings.getString(SettingsKeys.ALARMOJ, "")
        assertTrue(persistita.contains("\"peranto\""), "La nova alarmo devas esti persistita: $persistita")

        // Nova deponejo-lego devas vidi la persistitajn alarmojn
        val deponejo2 = PersistantaAlarmoDeponejo(settings, sugestoj)
        assertTrue(
            deponejo2.observiAlarmojn().value.any { it.kanaloSlug == "peranto" },
            "Persistita alarmo devas supervivi rekreon de la deponejo",
        )
    }

    @Test // (C4) korupta JSON devas esti REZERVATA antaŭ ol ĝi estos anstataŭigita
    fun koruptaJsonKreigasRezervon() = runTest {
        val settings = MapSettings()
        settings.putString(SettingsKeys.ALARMOJ, "{\"NE VALIDAS")

        val deponejo = PersistantaAlarmoDeponejo(settings, sugestoj)

        assertEquals(1, deponejo.observiAlarmojn().value.size, "Falilo al la sugestoj")
        assertEquals(
            "{\"NE VALIDAS",
            settings.getString(SettingsKeys.ALARMOJ_REZERVO, ""),
            "La kruda korupta valoro devas esti konservita kiel rezervo — la uzantaj " +
                "alarmoj ne estas definitive perditaj kiam persistu() anstataŭigas la ĉefan ŝlosilon",
        )
    }
}
