package dk.nordfalk.esperanto.ui

import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * Referenco al la nuna ĉefa Activity — agordita de MainActivity (androidApp).
 * Bezona por ŝanĝi la orientiĝon kiam la plenekrana filmo malfermiĝas.
 */
object AktivecoPonto {
    @Volatile
    var aktiveco: Activity? = null
}

/** La orientiĝo antaŭ la plenekrana filmo — restarigata je fermo. */
private var antauxaOrientigo: Int? = null

internal actual fun platformaPlenekranaOrientigo(malfermita: Boolean) {
    val aktiveco = AktivecoPonto.aktiveco ?: return
    if (malfermita) {
        antauxaOrientigo = aktiveco.requestedOrientation
        aktiveco.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        antauxaOrientigo?.let { aktiveco.requestedOrientation = it }
        antauxaOrientigo = null
    }
}
