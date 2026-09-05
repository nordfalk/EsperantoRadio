package dk.nordfalk.esperanto

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.KanalDeponejoImpl
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.ui.KanalaroEkrano
import dk.nordfalk.esperanto.ui.KanalaroViewModel
import dk.nordfalk.esperanto.ui.KanalEkrano
import dk.nordfalk.esperanto.ui.ElsendoEkrano
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private enum class Ekrano { KANALARO, KANAL, ELSENDO }

@Composable
fun EsperantoRadioApp() {
    MaterialTheme {
        val httpKliento = remember {
            HttpClient(CIO) {
                install(Logging) { level = LogLevel.INFO }
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 10_000
                }
            }
        }

        val kanalDeponejo = remember {
            KanalDeponejoImpl(
                leganto = KanalAgordoLeganto(),
                bundledTeksto = ::leguBundledKanalkonfiguron
            )
        }

        val elsendoDeponejo = remember { ElsendoDeponejoImpl(httpKliento) }
        val kanalaroViewModel = remember { KanalaroViewModel(kanalDeponejo) }

        var ekrano by remember { mutableStateOf(Ekrano.KANALARO) }
        var elektitaKanal by remember { mutableStateOf<Kanal?>(null) }
        var elektitaElsendo by remember { mutableStateOf<Elsendo?>(null) }

        when (ekrano) {
            Ekrano.KANALARO -> {
                KanalaroEkrano(viewModel = kanalaroViewModel) { kanal ->
                    elektitaKanal = kanal
                    ekrano = Ekrano.KANAL
                }
            }
            Ekrano.KANAL -> {
                val kanal = elektitaKanal!!
                KanalEkrano(
                    kanal = kanal,
                    elsendoDeponejo = elsendoDeponejo,
                    onReen = {
                        ekrano = Ekrano.KANALARO
                    },
                    onElsendo = { elsendo ->
                        elektitaElsendo = elsendo
                        ekrano = Ekrano.ELSENDO
                    }
                )
            }
            Ekrano.ELSENDO -> {
                val elsendo = elektitaElsendo!!
                ElsendoEkrano(
                    elsendo = elsendo,
                    onReen = {
                        ekrano = Ekrano.KANAL
                    }
                )
            }
        }
    }
}
