package dk.nordfalk.esperanto.domain.player

actual fun kreuDefaŭltanLudiloRegilon(): LudiloRegilo = NoOpLudiloRegilo()

/** iOS havas neniun rezervan alarm-sonon — senefika. */
actual fun haltuAlarmVokosonon() {}
