package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.logd

actual class AlarmoSkedilo actual constructor() {
    actual fun skedi(alarmo: Alarmo) {
        logd("AlarmoSkedilo", "skedi (NoOp): ${alarmo.tempoTeksto} → ${alarmo.kanalSlug}")
    }

    actual fun malplani(alarmoId: Int) {
        logd("AlarmoSkedilo", "malplani (NoOp): $alarmoId")
    }

    actual fun reskediCxiujn(alarmoj: List<Alarmo>) {
        logd("AlarmoSkedilo", "reskediCxiujn (NoOp): ${alarmoj.size} alarmoj")
    }
}
actual val subtenasVekhorlogxn: Boolean = false
