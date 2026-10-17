package dk.nordfalk.esperanto.domain.player

actual fun kreuDefauxltanLudiloRegilon(): LudiloRegilo = DesktopLudiloRegilo()

/** Desktop havas neniun rezervan alarm-sonon — senefika. */
actual fun haltuAlarmRingon() {}
