package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.SonfontoKodilo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import androidx.core.content.ContextCompat

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

        // Sonfonto en la ekstraĵoj — tiel la apo (ExoPlayerLudiloRegilo) rekonas la elsendon
        // kaj montras ĝin en la mini-ludilbreto, kvankam ĉi tiu Receivilo uzas propran MediaController
        val elsendo = Elsendo(
            id = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_ID) ?: fluo,
            kanaloSlug = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_SLUG) ?: "",
            kanaloNomo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_NOMO),
            titolo = titolo ?: "Nekonata elsendo",
            priskribo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_PRISKRIBO),
            bildoUrl = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_BILDO_URL),
            dato = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_DATO) ?: "",
            fluo = fluo,
        )
        val metadata = MediaMetadata.Builder().setTitle(elsendo.titolo).apply {
            elsendo.kanaloNomo?.let { setArtist(it) }
            elsendo.bildoUrl?.let {
                runCatching { setArtworkUri(android.net.Uri.parse(it)) }
            }
            setExtras(Bundle().apply {
                putString(SonfontoKodilo.SXLOSILO, SonfontoKodilo.kodigu(Sonfonto.ElsendoFonto(elsendo)))
            })
        }.build()

        val mediaItem = MediaItem.Builder().setUri(fluo).setMediaMetadata(metadata).build()
        // applicationContext, NE la kunteksto de la Receivilo: tiu estas ReceiverRestrictedContext kaj
        // bindService() ĵetas ReceiverCallNotAllowedException ("BroadcastReceiver components are not
        // allowed to bind to services") — la "Ludi"-butono en la sciigo kraŝigis la apon.
        val appKunteksto = context.applicationContext
        val sessionToken = SessionToken(appKunteksto, ComponentName(appKunteksto, "dk.nordfalk.esperanto.android.EsperantoLudadoServo"))

        val future = MediaController.Builder(appKunteksto, sessionToken).buildAsync()
        future.addListener({
            runCatching {
                future.get()?.apply {
                    setMediaItem(mediaItem); prepare(); play()
                    logi("LudiElsendo", "Komencis ludi: $titolo")
                } ?: logw("LudiElsendo", "MediaController estas null")
            }.onFailure { logw("LudiElsendo", "Eraro dum konektado", it) }
            pendingResult.finish()
            // Ĉefa fadeno: MediaController rajtas esti vokata nur el la fadeno de sia Looper
            // (verifyApplicationThread) — kun aparta Executor setMediaItem() ĵetis IllegalStateException.
        }, ContextCompat.getMainExecutor(appKunteksto))
    }
}
