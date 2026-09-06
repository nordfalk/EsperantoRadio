package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw

/**
 * BroadcastReceiver kiu ricevas la alarmon kiam ĝi ekigas.
 *
 * Ĝi akiras WakeLock, lanĉas la ĉefaktivon kaj metas la kanalo-slugon kiel
 * ekstran intencon por ke la apo povu komenci ludi la ĝustan kanalon.
 */
class AlarmoReceivilo : BroadcastReceiver() {

    companion object {
        private const val WAKELOCK_TAG = "EsperantoRadio::Alarmo"
        private const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L // 10 minutoj
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmoId = intent.getIntExtra("alarmo_id", -1)
        val kanaloSlug = intent.getStringExtra("kanal_slug")
        val etikedo = intent.getStringExtra("etikedo")

        logi("AlarmoReceivilo", "Alarmo ekigis! id=$alarmoId kanalo=$kanaloSlug etikedo=$etikedo")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply { acquire(WAKELOCK_TIMEOUT) }

        try {
            // Lanĉu la ĉefaktivon por ke la uzanto vidu la apot
            val launchIntent = Intent().apply {
                setClassName(context, "dk.nordfalk.esperanto.android.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = "dk.nordfalk.esperanto.ALARMO_EKIGAS"
                putExtra("alarmo_kanal_slug", kanaloSlug)
                putExtra("alarmo_etikedo", etikedo)
            }
            context.startActivity(launchIntent)
            logi("AlarmoReceivilo", "Lanĉis MainActivity kun kanalo=$kanaloSlug")
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro lanĉante aktivon", e)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
