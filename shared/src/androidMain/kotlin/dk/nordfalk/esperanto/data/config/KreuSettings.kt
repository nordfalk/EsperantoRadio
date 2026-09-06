package dk.nordfalk.esperanto.data.config

import android.content.Context
import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.logw

/**
 * Android: uzas SharedPreferences rekte.
 * Se appContext ne estas agordita (ekz. en testoj), reeniras al no-op.
 */
actual fun kreuSettings(): Settings {
    return try {
        val prefs = appContext.getSharedPreferences("esperantoradio", Context.MODE_PRIVATE)
        SharedPreferencesSettings(prefs)
    } catch (e: UninitializedPropertyAccessException) {
        logw("KreuSettings", "appContext ne inicialigita — uzas NoOpSettings", e)
        NoOpSettings()
    }
}

private class SharedPreferencesSettings(private val prefs: android.content.SharedPreferences) : Settings {
    override val keys: Set<String> get() = prefs.all.keys
    override val size: Int get() = prefs.all.size
    override fun clear() { prefs.edit().clear().apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
    override fun hasKey(key: String): Boolean = prefs.contains(key)
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun getIntOrNull(key: String): Int? = if (prefs.contains(key)) prefs.getInt(key, 0) else null
    override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    override fun getLongOrNull(key: String): Long? = if (prefs.contains(key)) prefs.getLong(key, 0) else null
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
    override fun getStringOrNull(key: String): String? = prefs.getString(key, null)
    override fun putFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }
    override fun getFloat(key: String, defaultValue: Float): Float = prefs.getFloat(key, defaultValue)
    override fun getFloatOrNull(key: String): Float? = if (prefs.contains(key)) prefs.getFloat(key, 0f) else null
    override fun putDouble(key: String, value: Double) { prefs.edit().putString(key, value.toString()).apply() }
    override fun getDouble(key: String, defaultValue: Double): Double = prefs.getString(key, null)?.toDoubleOrNull() ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = prefs.getString(key, null)?.toDoubleOrNull()
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun getBooleanOrNull(key: String): Boolean? = if (prefs.contains(key)) prefs.getBoolean(key, false) else null
}

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
