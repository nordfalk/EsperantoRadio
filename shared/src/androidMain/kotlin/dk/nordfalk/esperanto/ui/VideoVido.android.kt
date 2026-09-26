package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dk.nordfalk.esperanto.shared.R
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.logd

/**
 * Android-aktualigo: ligas [PlayerView] al la ExoPlayer de la ludado-servo
 * per [VideoLudiloPonto]. Se la servo ne vivas aŭ ludas alian fluon,
 * neniu ludilo estas ligita — la komuna UI tiam montras la statikan bildon.
 *
 * La PlayerView uzas TextureView (ne SurfaceView): la bildo do iras tra la
 * normala vido-hierarkio kaj bildiĝas ankaŭ post surfac-ŝanĝo (malgranda ↔
 * plenekrana) kaj en ajna fenestro — SurfaceView en nova tavolo post
 * ŝanĝo meze de ludado ne bildiĝis fidinde (aparte sur emuliloj).
 */
@Composable
actual fun VideoVido(elsendo: Elsendo, modifier: Modifier, montru: Boolean) {
    if (!montru) return

    val ludilo = VideoLudiloPonto.ludilo ?: run {
        logd("VideoVido", "preterlasas: neniu ludilo en la ponto")
        return
    }
    // Ligu nur se la servo efektive ludas ĉi tiun fluon (ne ekz. alian elsendon)
    val nunaFluo = ludilo.currentMediaItem?.localConfiguration?.uri?.toString()
    if (nunaFluo != elsendo.fluo) {
        logd("VideoVido", "preterlasas: alia fluo — $nunaFluo ≠ ${elsendo.fluo}")
        return
    }

    AndroidView(
        factory = { ctx ->
            // surface_type="texture_view" estas agordebla nur per XML-atributoj;
            // View-inflado bezonas veran rimedon (ne sinteza XmlPullParser)
            val vido = LayoutInflater.from(ctx).inflate(R.layout.videovido, null) as PlayerView
            vido.apply {
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
