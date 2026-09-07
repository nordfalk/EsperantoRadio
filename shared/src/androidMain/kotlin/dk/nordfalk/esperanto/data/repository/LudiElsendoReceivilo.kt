package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import java.util.concurrent.Executors

/**
 * BroadcastReceiver kiu komencas ludi elsendon rekte de la sciigo.
 *
 * Ricevas la intencon kun ago LUDI_ELSENDON, konektiĝas al EsperantoLudadoServo
 * per MediaController, kaj komencas ludi la donitan elsendo-URL.
 */
class LudiElsendoReceivilo : BroadcastReceiver() {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NovajElsendojKontroloWorker.ACTION_LUDI_ELSENDON) return

        val titolo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_TITOLO)
        val fluo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_FLUO)
        if (fluo.isNullOrBlank()) { logw("LudiElsendo", "Neniu fluo-URL"); return }

        logi("LudiElsendo", "Ludi: $titolo")

        val metadata = MediaMetadata.Builder().setTitle(titolo ?: "Nekonata elsendo").apply {
            intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_NOMO)?.let { setArtist(it) }
            intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_BILDO_URL)?.let {
                runCatching { setArtworkUri(android.net.Uri.parse(it)) }
            }
        }.build()

        val mediaItem = MediaItem.Builder().setUri(fluo).setMediaMetadata(metadata).build()
        val sessionToken = SessionToken(context, ComponentName(context, "dk.nordfalk.esperanto.android.EsperantoLudadoServo"))

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            runCatching {
                controllerFuture?.get()?.apply {
                    setMediaItem(mediaItem); prepare(); play()
                    logi("LudiElsendo", "Komencis ludi: $titolo")
                }
            }.onFailure { logw("LudiElsendo", "Eraro dum konektado", it) }
        }, Executors.newSingleThreadExecutor())
    }
}
