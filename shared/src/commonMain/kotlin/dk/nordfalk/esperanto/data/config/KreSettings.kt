package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.Settings

/**
 * Kreas platform-specifan Settings-instalon.
 * Android: SharedPreferences, Desktop: java.util.prefs.Preferences, ktp.
 */
expect fun kreSettings(): Settings
