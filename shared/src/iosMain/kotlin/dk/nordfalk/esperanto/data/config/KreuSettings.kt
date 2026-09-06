package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.Settings
import com.russhwolf.settings.nssecure.NSSettings

/**
 * iOS: uzas NSUserDefaults.
 */
actual fun kreuSettings(): Settings {
    return NSSettings("esperantoradio")
}
