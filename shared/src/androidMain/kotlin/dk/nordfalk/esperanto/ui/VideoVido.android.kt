package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dk.nordfalk.esperanto.domain.model.Elsendo

/**
 * Android-aktualigo: ligas [PlayerView] al la ExoPlayer de la ludado-servo
 * per [VideoLudiloPonto]. Se la servo ne vivas aŭ ludas alian fluon,
 * neniu ludilo estas ligita — la komuna UI tiam montras la statikan bildon.
 */
@Composable
actual fun VideoVido(elsendo: Elsendo, modifier: Modifier, montru: Boolean) {
    if (!montru) return

    val ludilo = VideoLudiloPonto.ludilo ?: return
    // Ligu nur se la servo efektive ludas ĉi tiun fluon (ne ekz. alian elsendon)
    val nunaFluo = ludilo.currentMediaItem?.localConfiguration?.uri?.toString()
    if (nunaFluo != elsendo.fluo) return

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                // Kontrolado restas ĉe la komunaj butonoj de ElsendoEkrano
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        modifier = modifier,
        update = { vido -> vido.player = ludilo },
        onRelease = { vido -> vido.player = null },
    )
}
