package dk.nordfalk.esperanto.domain.player

actual fun kreuDefaŭltanLudiloRegilon(): LudiloRegilo = DesktopLudiloRegilo()

/** Desktop havas neniun rezervan alarm-sonon — senefika. */
actual fun haltuAlarmRingon() {}
