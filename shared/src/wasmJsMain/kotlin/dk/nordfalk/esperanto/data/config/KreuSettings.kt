package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

/**
 * wasmJs: persistas la agordojn en la localStorage de la retumilo
 * (StorageSettings de multiplatform-settings, defaŭlte window.localStorage).
 * La pseŭdonimo, plej ŝatataj kaj aliaj agordoj do transvivas reŝargon.
 */
actual fun kreuSettings(): Settings = StorageSettings()
