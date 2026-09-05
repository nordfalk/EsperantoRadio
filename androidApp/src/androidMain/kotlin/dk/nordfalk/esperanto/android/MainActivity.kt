package dk.nordfalk.esperanto.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.data.config.appContext

class MainActivity : ComponentActivity() {
    private lateinit var ludilo: ExoPlayerLudiloRegilo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        ludilo = ExoPlayerLudiloRegilo(this)
        setContent {
            EsperantoRadioApp(ludilo = ludilo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ludilo.release()
    }
}
