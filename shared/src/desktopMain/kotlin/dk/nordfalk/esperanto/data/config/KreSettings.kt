package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

/**
 * Desktop (JVM): uzas java.util.prefs.Preferences.
 */
actual fun kreSettings(): Settings {
    return PreferencesSettings(Preferences.userRoot().node("esperantoradio"))
}
