package dk.nordfalk.esperanto.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.domain.player.LudiloRegilo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        val ludilo: LudiloRegilo = ExoPlayerLudiloRegilo(this)
        setContent {
            EsperantoRadioApp(ludilo = ludilo)
        }
    }
}
