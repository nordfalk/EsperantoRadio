package dk.nordfalk.esperanto.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.logi

class MainActivity : ComponentActivity() {
    private lateinit var ludilo: ExoPlayerLudiloRegilo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        ludilo = ExoPlayerLudiloRegilo(this)
        setContent {
            EsperantoRadioApp(ludilo = ludilo)
        }
        traktuAlarmIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        traktuAlarmIntent(intent)
    }

    private fun traktuAlarmIntent(intent: Intent?) {
        if (intent?.action == "dk.nordfalk.esperanto.ALARMO_EKIGAS") {
            val kanalSlug = intent.getStringExtra("alarmo_kanal_slug")
            val etikedo = intent.getStringExtra("alarmo_etikedo")
            logi("MainActivity", "Alarmo ricevita: kanal=$kanalSlug etikedo=$etikedo")
            // La ludado komencos kiam la uzanto elektos la kanalon en la UI
            // Estonte: aŭtomate komenci ludi la kanalon
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ludilo.release()
    }
}
