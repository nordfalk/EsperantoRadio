package dk.nordfalk.esperanto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.KanalDeponejoImpl
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.kreLudiloRegilo
import dk.nordfalk.esperanto.ui.ElsendoEkrano
import dk.nordfalk.esperanto.ui.KanalaroEkrano
import dk.nordfalk.esperanto.ui.KanalaroViewModel
import dk.nordfalk.esperanto.ui.KanalEkrano
import dk.nordfalk.esperanto.ui.MiniLudilbreto
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
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
        val ludilo = remember { kreLudiloRegilo() }
        val scope = rememberCoroutineScope()

        var ekrano by remember { mutableStateOf(Ekrano.KANALARO) }
        var elektitaKanal by remember { mutableStateOf<Kanal?>(null) }
        var elektitaElsendo by remember { mutableStateOf<Elsendo?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
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
                            onReen = { ekrano = Ekrano.KANALARO },
                            onElsendo = { elsendo ->
                                elektitaElsendo = elsendo
                                ekrano = Ekrano.ELSENDO
                            },
                            onLudi = { fonto ->
                                scope.launch {
                                    ludilo.fiksiFonton(fonto)
                                    ludilo.ludi()
                                }
                            }
                        )
                    }
                    Ekrano.ELSENDO -> {
                        val elsendo = elektitaElsendo!!
                        ElsendoEkrano(
                            elsendo = elsendo,
                            onReen = { ekrano = Ekrano.KANAL },
                            onLudi = {
                                scope.launch {
                                    ludilo.fiksiFonton(Sonfonto.ElsendoFonto(elsendo))
                                    ludilo.ludi()
                                }
                            }
                        )
                    }
                }
            }

            MiniLudilbreto(ludilo = ludilo)
        }
    }
}
