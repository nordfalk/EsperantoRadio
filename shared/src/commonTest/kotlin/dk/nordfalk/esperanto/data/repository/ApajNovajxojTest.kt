package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Testoj por [ApajNovajxojDeponejo] — la apaj novaĵoj: montrataj
 * unufoje por ĉiu nova enhavo, haŝaĵo persistas je konfirmo.
 */
class ApajNovajxojTest {

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
    fun novaMesagxoMontrigxas() {
        val deponejo = ApajNovajxojDeponejo(MapSettings(), "Versio 3.0.2 eldoniĝis")
        assertEquals("Versio 3.0.2 eldoniĝis", deponejo.observuMontratan().value)
    }

    @Test
    fun malplenaMesagxoNeniamMontrigxas() {
        val deponejo = ApajNovajxojDeponejo(MapSettings(), "")
        assertNull(deponejo.observuMontratan().value)

        val blanka = ApajNovajxojDeponejo(MapSettings(), "   ")
        assertNull(blanka.observuMontratan().value, "Nur blanksignoj devas esti traktataj kiel malplena mesaĝo")
    }

    @Test
    fun konfirmoPersistasHeshonKajNeRemontras() {
        val settings = MapSettings()
        val mesagxo = "Majo 2022: Muzaiko ne plu funkcias"
        val deponejo = ApajNovajxojDeponejo(settings, mesagxo)
        assertEquals(mesagxo, deponejo.observuMontratan().value)

        deponejo.konfirmu()
        assertNull(deponejo.observuMontratan().value)
        assertEquals(mesagxo.hashCode(), settings.getInt(SettingsKeys.APAJ_NOVAJXOJ_HESH, -1))

        // Nova lego (ekz. restarto) kun la sama mesaĝo ne plu montras
        val restarto = ApajNovajxojDeponejo(settings, mesagxo)
        assertNull(restarto.observuMontratan().value)
    }

    @Test
    fun sxangxitaMesagxoReaperas() {
        val settings = MapSettings()
        ApajNovajxojDeponejo(settings, "Unua mesaĝo").let { it.konfirmu() }

        val dua = ApajNovajxojDeponejo(settings, "Dua mesaĝo")
        assertEquals("Dua mesaĝo", dua.observuMontratan().value)
    }

    @Test
    fun decidlogikoEstasPuraKajKongruaKunMalnovaApo() {
        // Malplena mesaĝo neniam montriĝas — eĉ sen konservita haŝaĵo
        assertNull(ApajNovajxojDeponejo.deciduMontratan("", ApajNovajxojDeponejo.MALPLENA_HESH))

        // Ne-malplena mesaĝo kun ne-kongrua haŝaĵo montriĝas
        assertEquals("nova", ApajNovajxojDeponejo.deciduMontratan("nova", 12345))

        // Kongrua haŝaĵo (jam konfirmita) ne montriĝas
        assertNull(ApajNovajxojDeponejo.deciduMontratan("nova", "nova".hashCode()))

        // La haŝaĵo de malplena ĉeno estas MALPLENA_HESH (komenca stato de novaj instalaĵoj)
        assertEquals(ApajNovajxojDeponejo.MALPLENA_HESH, "".hashCode())
    }

    @Test
    fun traktasNulanMesagxonElKonfiguro() {
        val deponejo = ApajNovajxojDeponejo(MapSettings(), null)
        assertNull(deponejo.observuMontratan().value)
    }

    @Test
    fun enEvoluaMedoNeniamMontrigxas() {
        // Eĉ freŝa instalaĵo (neniu konservita haŝaĵo) kun nova mesaĝo
        // ne montras la dialogon dum evoluo (debug-konstruo/emulilo/Desktop/Web)
        val deponejo = ApajNovajxojDeponejo(MapSettings(), "Versio 3.0.2 eldoniĝis", montruDumEvoluo = false)
        assertNull(deponejo.observuMontratan().value)

        // La pura decidlogiko same: evolua medio → neniam montri
        assertNull(ApajNovajxojDeponejo.deciduMontratan("nova", 12345, montruDumEvoluo = false))

        // Kun montruDumEvoluo = true (produktado) la normala konduto restas
        assertEquals("nova", ApajNovajxojDeponejo.deciduMontratan("nova", 12345, montruDumEvoluo = true))
    }
}
