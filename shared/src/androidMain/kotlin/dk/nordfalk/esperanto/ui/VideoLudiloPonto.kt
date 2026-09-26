package dk.nordfalk.esperanto.ui

import androidx.media3.common.Player

/**
 * Ponto inter la ludado-servo (androidApp: `EsperantoLudadoServo`, kiu
 * posedas la veran ExoPlayer) kaj la komuna UI ([VideoVido]).
 *
 * La servo skribas sian ludilon ĉi tie en onCreate() kaj forigas ĝin en
 * onDestroy(). Ambaŭ ruliĝas en la sama procezo, do rekte alirebla.
 */
object VideoLudiloPonto {
    /** La nuna ExoPlayer de la ludado-servo, aŭ null se la servo ne vivas. */
    @Volatile
    var ludilo: Player? = null
}
