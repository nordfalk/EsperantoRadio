package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.Settings

/**
 * wasmJs: provizore no-op (neniu persisto sur web).
 * TODO: Aldoni localStorage-bazitan Settings.
 */
actual fun kreuSettings(): Settings = NoOpSettings()

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
