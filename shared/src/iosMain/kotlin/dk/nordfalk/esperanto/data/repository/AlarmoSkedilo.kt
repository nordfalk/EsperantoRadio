package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.logd

actual class AlarmoSkedilo actual constructor() {
    actual fun skedi(alarmo: Alarmo) {
        logd("AlarmoSkedilo", "skedi (NoOp): ${alarmo.tempoTeksto} → ${alarmo.kanaloSlug}")
    }

    actual fun malplani(alarmoId: Int) {
        logd("AlarmoSkedilo", "malplani (NoOp): $alarmoId")
    }

    actual fun reskediĈiujn(alarmoj: List<Alarmo>) {
        logd("AlarmoSkedilo", "reskediĈiujn (NoOp): ${alarmoj.size} alarmoj")
    }
}
actual val subtenasVekhorloĝon: Boolean = false
actual fun ekzaktajAlarmojPermesataj(): Boolean = true
