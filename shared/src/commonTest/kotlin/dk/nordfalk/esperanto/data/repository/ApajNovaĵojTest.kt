package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Testoj por [ApajNovaĵojDeponejo] — la apaj novaĵoj: montrataj
 * unufoje por ĉiu nova enhavo, haŝaĵo persistas je konfirmo.
 */
class ApajNovaĵojTest {

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

    @Test
    fun novaMesaĝoMontriĝas() {
        val deponejo = ApajNovaĵojDeponejo(MapSettings(), "Versio 3.0.2 eldoniĝis")
        assertEquals("Versio 3.0.2 eldoniĝis", deponejo.observuMontratan().value)
    }

    @Test
    fun malplenaMesaĝoNeniamMontriĝas() {
        val deponejo = ApajNovaĵojDeponejo(MapSettings(), "")
        assertNull(deponejo.observuMontratan().value)

        val blanka = ApajNovaĵojDeponejo(MapSettings(), "   ")
        assertNull(blanka.observuMontratan().value, "Nur blanksignoj devas esti traktataj kiel malplena mesaĝo")
    }

    @Test
    fun konfirmoPersistasHaŝonKajNeRemontras() {
        val settings = MapSettings()
        val mesaĝo = "Majo 2022: Muzaiko ne plu funkcias"
        val deponejo = ApajNovaĵojDeponejo(settings, mesaĝo)
        assertEquals(mesaĝo, deponejo.observuMontratan().value)

        deponejo.konfirmu()
        assertNull(deponejo.observuMontratan().value)
        assertEquals(mesaĝo.hashCode(), settings.getInt(SettingsKeys.APAJ_NOVAĴOJ_HEŜ, -1))

        // Nova lego (ekz. restarto) kun la sama mesaĝo ne plu montras
        val restarto = ApajNovaĵojDeponejo(settings, mesaĝo)
        assertNull(restarto.observuMontratan().value)
    }

    @Test
    fun ŝanĝitaMesaĝoReaperas() {
        val settings = MapSettings()
        ApajNovaĵojDeponejo(settings, "Unua mesaĝo").let { it.konfirmu() }

        val dua = ApajNovaĵojDeponejo(settings, "Dua mesaĝo")
        assertEquals("Dua mesaĝo", dua.observuMontratan().value)
    }

    @Test
    fun decidlogikoEstasPuraKajKongruaKunMalnovaApo() {
        // Malplena mesaĝo neniam montriĝas — eĉ sen konservita haŝaĵo
        assertNull(ApajNovaĵojDeponejo.deciduMontratan("", ApajNovaĵojDeponejo.MALPLENA_HESH))

        // Ne-malplena mesaĝo kun ne-kongrua haŝaĵo montriĝas
        assertEquals("nova", ApajNovaĵojDeponejo.deciduMontratan("nova", 12345))

        // Kongrua haŝaĵo (jam konfirmita) ne montriĝas
        assertNull(ApajNovaĵojDeponejo.deciduMontratan("nova", "nova".hashCode()))

        // La haŝaĵo de malplena ĉeno estas MALPLENA_HESH (komenca stato de novaj instalaĵoj)
        assertEquals(ApajNovaĵojDeponejo.MALPLENA_HESH, "".hashCode())
    }

    @Test
    fun traktasNulanMesaĝonElKonfiguro() {
        val deponejo = ApajNovaĵojDeponejo(MapSettings(), null)
        assertNull(deponejo.observuMontratan().value)
    }
}
