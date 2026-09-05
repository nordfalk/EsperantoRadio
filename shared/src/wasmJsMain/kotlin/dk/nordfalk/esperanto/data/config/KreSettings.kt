package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.Settings
import com.russhwolf.settings.storage.StorageSettings
import com.russhwolf.settings.storage.set

/**
 * wasmJs: uzas window.localStorage (StorageSettings).
 */
actual fun kreSettings(): Settings {
    return StorageSettings()
}
