package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import dk.nordfalk.esperanto.logw

/**
 * wasmJs: persistas la agordojn en la localStorage de la retumilo
 * (StorageSettings de multiplatform-settings, defaŭlte window.localStorage).
 * La pseŭdonimo, plej ŝatataj kaj aliaj agordoj do transvivas reŝargon.
 *
 * Se la retumilo rifuzas aliron al la konservado (SecurityError ĉe blokitaj
 * retejaj datumoj aŭ sekurigita enkadrigo), la apo tamen lanĉiĝas —
 * kun ne-persistanta rezerva agordaro (A4: unu medio ne paneigu la apon).
 */
actual fun kreuSettings(): Settings = try {
    StorageSettings()
} catch (e: Throwable) {
    logw("KreuSettings", "localStorage ne alirebla — uzas ne-persistantan rezervan agordaron", e)
    NoOpSettings()
}

/**
 * Ne-persistanta rezerva agordaro por retumiloj, kiuj rifuzas localStorage-on.
 * Konservu la konduton de la antaŭa versio (neniu persisto), por ke la apo
 * funkcias same kiel antaŭ ĉi tiu PR ankaŭ en tiu flanka kazo.
 */
private class NoOpSettings : Settings {
    override val keys: Set<String> get() = emptySet()
    override val size: Int get() = 0
    override fun clear() {}
    override fun remove(key: String) {}
    override fun hasKey(key: String): Boolean = false
    override fun putInt(key: String, value: Int) {}
    override fun getInt(key: String, defaultValue: Int): Int = defaultValue
    override fun getIntOrNull(key: String): Int? = null
    override fun putLong(key: String, value: Long) {}
    override fun getLong(key: String, defaultValue: Long): Long = defaultValue
    override fun getLongOrNull(key: String): Long? = null
    override fun putString(key: String, value: String) {}
    override fun getString(key: String, defaultValue: String): String = defaultValue
    override fun getStringOrNull(key: String): String? = null
    override fun putFloat(key: String, value: Float) {}
    override fun getFloat(key: String, defaultValue: Float): Float = defaultValue
    override fun getFloatOrNull(key: String): Float? = null
    override fun putDouble(key: String, value: Double) {}
    override fun getDouble(key: String, defaultValue: Double): Double = defaultValue
    override fun getDoubleOrNull(key: String): Double? = null
    override fun putBoolean(key: String, value: Boolean) {}
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = null
}
