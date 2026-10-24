package dk.nordfalk.esperanto.domain.player

actual fun kreuDefaŭltanLudiloRegilon(): LudiloRegilo = WasmJsLudiloRegilo()

/** Web havas neniun rezervan alarm-sonon — senefika. */
actual fun haltuAlarmRingon() {}
