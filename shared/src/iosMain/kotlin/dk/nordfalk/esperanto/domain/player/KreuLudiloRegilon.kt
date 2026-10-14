package dk.nordfalk.esperanto.domain.player

actual fun kreuDefauxltanLudiloRegilon(): LudiloRegilo = NoOpLudiloRegilo()

/** iOS havas neniun rezervan alarm-sonon — senefika. */
actual fun haltuAlarmRingon() {}
