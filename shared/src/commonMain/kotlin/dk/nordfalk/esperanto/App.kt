package dk.nordfalk.esperanto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.kreSettings
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.KanalDeponejoImpl
import dk.nordfalk.esperanto.data.repository.PersistantaPlejsatatajDeponejo
import dk.nordfalk.esperanto.data.repository.SercxoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.kreDefauxltanLudiloRegilon
import dk.nordfalk.esperanto.ui.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private enum class Ekrano { KANALARO, KANAL, ELSENDO, SERCXO, PLEJSATATAJ, AGORDOJ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsperantoRadioApp(
    ludilo: LudiloRegilo = kreDefauxltanLudiloRegilon(),
) {
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
        val settings = remember { kreSettings() }
        val plejsatatajDeponejo = remember { PersistantaPlejsatatajDeponejo(settings) }
        val sercxoDeponejo = remember { SercxoDeponejoImpl(elsendoDeponejo) }
        val agordojDeponejo = remember { AgordojDeponejoImpl() }
        val scope = rememberCoroutineScope()

        var ekrano by remember { mutableStateOf(Ekrano.KANALARO) }
        var elektitaKanal by remember { mutableStateOf<Kanal?>(null) }
        var elektitaElsendo by remember { mutableStateOf<Elsendo?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (ekrano) {
                    Ekrano.KANALARO -> {
                        KanalaroEkrano(
                            viewModel = kanalaroViewModel,
                            onKanal = { kanal -> elektitaKanal = kanal; ekrano = Ekrano.KANAL },
                            onSercxo = { ekrano = Ekrano.SERCXO },
                            onPlejsatataj = { ekrano = Ekrano.PLEJSATATAJ },
                            onAgordoj = { ekrano = Ekrano.AGORDOJ }
                        )
                    }
                    Ekrano.KANAL -> {
                        val kanal = elektitaKanal!!
                        KanalEkrano(
                            kanal = kanal,
                            elsendoDeponejo = elsendoDeponejo,
                            onReen = { ekrano = Ekrano.KANALARO },
                            onElsendo = { elsendo -> elektitaElsendo = elsendo; ekrano = Ekrano.ELSENDO },
                            onLudi = { fonto -> scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() } }
                        )
                    }
                    Ekrano.ELSENDO -> {
                        val elsendo = elektitaElsendo!!
                        ElsendoEkrano(
                            elsendo = elsendo,
                            onReen = { ekrano = Ekrano.KANAL },
                            onLudi = { scope.launch { ludilo.fiksiFonton(Sonfonto.ElsendoFonto(elsendo)); ludilo.ludi() } }
                        )
                    }
                    Ekrano.SERCXO -> {
                        SercxoEkrano(
                            sercxoDeponejo = sercxoDeponejo,
                            onReen = { ekrano = Ekrano.KANALARO },
                            onElsendo = { elsendo -> elektitaElsendo = elsendo; ekrano = Ekrano.ELSENDO }
                        )
                    }
                    Ekrano.PLEJSATATAJ -> {
                        PlejsatatajEkrano(
                            plejsatatajDeponejo = plejsatatajDeponejo,
                            kanalDeponejo = kanalDeponejo,
                            onReen = { ekrano = Ekrano.KANALARO },
                            onKanal = { kanal -> elektitaKanal = kanal; ekrano = Ekrano.KANAL }
                        )
                    }
                    Ekrano.AGORDOJ -> {
                        AgordojEkrano(
                            agordojDeponejo = agordojDeponejo,
                            onReen = { ekrano = Ekrano.KANALARO }
                        )
                    }
                }
            }

            MiniLudilbreto(ludilo = ludilo)
        }
    }
}
