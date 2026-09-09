package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import java.util.concurrent.Executors

/**
 * BroadcastReceiver kiu komencas ludi elsendon rekte de la sciigo.
 *
 * Uzas goAsync() por teni la Receivilon vivanta dum la nesinkrona MediaController-konekto.
 * Sen tio, la sistemo povas detrui la Receivilon antaŭ ol la konekto sukcesas.
 */
class LudiElsendoReceivilo : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NovajElsendojKontroloWorker.ACTION_LUDI_ELSENDON) return

        val titolo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_TITOLO)
        val fluo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_FLUO)
        if (fluo.isNullOrBlank()) { logw("LudiElsendo", "Neniu fluo-URL"); return }

        logi("LudiElsendo", "Ludi: $titolo")

        // goAsync() tenas la Receivilon vivanta dum la nesinkrona konekto
        val pendingResult = goAsync()

        val metadata = MediaMetadata.Builder().setTitle(titolo ?: "Nekonata elsendo").apply {
            intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_NOMO)?.let { setArtist(it) }
            intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_BILDO_URL)?.let {
                runCatching { setArtworkUri(android.net.Uri.parse(it)) }
            }
        }.build()

        val mediaItem = MediaItem.Builder().setUri(fluo).setMediaMetadata(metadata).build()
        val sessionToken = SessionToken(context, ComponentName(context, "dk.nordfalk.esperanto.android.EsperantoLudadoServo"))

        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            runCatching {
                future.get()?.apply {
                    setMediaItem(mediaItem); prepare(); play()
                    logi("LudiElsendo", "Komencis ludi: $titolo")
                } ?: logw("LudiElsendo", "MediaController estas null")
            }.onFailure { logw("LudiElsendo", "Eraro dum konektado", it) }
            pendingResult.finish()
        }, Executors.newSingleThreadExecutor())
    }
}
