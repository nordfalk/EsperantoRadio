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

    companion object {
        private const val TAG = "LudiElsendoReceivilo"
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NovajElsendojKontroloWorker.ACTION_LUDI_ELSENDON) return

        val elsendoId = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_ID)
        val titolo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_TITOLO)
        val fluo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_FLUO)
        val kanaloNomo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_NOMO)
        val bildoUrl = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_BILDO_URL)

        logi(TAG, "Ludi elsendon: id=$elsendoId titolo=$titolo fluo=$fluo")

        if (fluo.isNullOrBlank()) {
            logw(TAG, "Neniu fluo-URL en la sciigo-intento")
            return
        }

        // Konektiĝu al la MediaSessionService kaj komencu ludi
        val sessionToken = SessionToken(
            context,
            ComponentName(context, "dk.nordfalk.esperanto.android.EsperantoLudadoServo")
        )

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(titolo ?: "Nekonata elsendo")
        if (kanaloNomo != null) metadataBuilder.setArtist(kanaloNomo)
        if (bildoUrl != null) {
            try { metadataBuilder.setArtworkUri(android.net.Uri.parse(bildoUrl)) } catch (_: Exception) {}
        }

        val mediaItem = MediaItem.Builder()
            .setUri(fluo)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                if (controller != null) {
                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                    logi(TAG, "Komencis ludi: $titolo")
                } else {
                    logw(TAG, "MediaController estas null")
                }
            } catch (e: Exception) {
                logw(TAG, "Eraro dum konektado al ludado-servo", e)
            }
        }, Executors.newSingleThreadExecutor())
    }
}
