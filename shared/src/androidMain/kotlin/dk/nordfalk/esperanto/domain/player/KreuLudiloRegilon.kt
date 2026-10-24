package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.data.repository.AlarmoReceivilo

actual fun kreuDefaŭltanLudiloRegilon(): LudiloRegilo = NoOpLudiloRegilo()

actual fun haltuAlarmVokosonon() {
    AlarmoReceivilo.haltuRezervanVokosonon()
}
