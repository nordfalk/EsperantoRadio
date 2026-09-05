package dk.nordfalk.esperanto.data.config

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Android: uzas SharedPreferences per appContext.
 */
actual fun kreSettings(): Settings {
    val prefs = appContext.getSharedPreferences("esperantoradio", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}
