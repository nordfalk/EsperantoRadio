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
import dk.nordfalk.esperanto.data.config.parsuSugestojnPorAlarmoj
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.KanalDeponejoImpl
import dk.nordfalk.esperanto.data.repository.PersistantaPlejsatatajDeponejo
import dk.nordfalk.esperanto.data.repository.SercxoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.kreElshutDeponejo
import dk.nordfalk.esperanto.data.repository.MemorAlarmoDeponejo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.kreDefauxltanLudiloRegilon
import dk.nordfalk.esperanto.logi
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

private enum class Ekrano { KANALARO, KANAL, ELSENDO, SERCXO, PLEJSATATAJ, ELSHUTOJ, ALARMOJ, AGORDOJ }

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
        val elshutDeponejo = remember { kreElshutDeponejo(httpKliento) }
        val alarmoDeponejo = remember {
            val agordo = KanalAgordoLeganto().legu(leguBundledKanalkonfiguron())
            val sugestoj = agordo.sugestoj_por_alarmoj?.let { parsuSugestojnPorAlarmoj(it) } ?: emptyList()
            MemorAlarmoDeponejo(sugestoj)
        }
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
                            onKanal = { kanal -> logi("Nav", "→ KANAL: ${kanal.slug}"); elektitaKanal = kanal; ekrano = Ekrano.KANAL },
                            onSercxo = { logi("Nav", "→ SERCXO"); ekrano = Ekrano.SERCXO },
                            onPlejsatataj = { logi("Nav", "→ PLEJSATATAJ"); ekrano = Ekrano.PLEJSATATAJ },
                            onElshutoj = { logi("Nav", "→ ELSHUTOJ"); ekrano = Ekrano.ELSHUTOJ },
                            onAlarmoj = { logi("Nav", "→ ALARMOJ"); ekrano = Ekrano.ALARMOJ },
                            onAgordoj = { logi("Nav", "→ AGORDOJ"); ekrano = Ekrano.AGORDOJ }
                        )
                    }
                    Ekrano.KANAL -> {
                        val kanal = elektitaKanal!!
                        KanalEkrano(
                            kanal = kanal,
                            elsendoDeponejo = elsendoDeponejo,
                            onReen = { logi("Nav", "→ KANALARO (reen)"); ekrano = Ekrano.KANALARO },
                            onElsendo = { elsendo -> logi("Nav", "→ ELSENDO: ${elsendo.id}"); elektitaElsendo = elsendo; ekrano = Ekrano.ELSENDO },
                            onLudi = { fonto ->
                                logi("Nav", "Ludas rekte: ${kanal.slug}")
                                scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                            }
                        )
                    }
                    Ekrano.ELSENDO -> {
                        val elsendo = elektitaElsendo!!
                        ElsendoEkrano(
                            elsendo = elsendo,
                            onReen = { logi("Nav", "→ KANAL (reen)"); ekrano = Ekrano.KANAL },
                            onLudi = {
                                logi("Nav", "Ludas elsendon: ${elsendo.id}")
                                scope.launch {
                                    val lokaVojo = elshutDeponejo.getLokaDosieroVojo(elsendo.id)
                                    val fonto = if (lokaVojo != null) {
                                        logi("Nav", "Ludas elŝutitan: $lokaVojo")
                                        Sonfonto.LokaElsendo(elsendo, lokaVojo)
                                    } else {
                                        Sonfonto.ElsendoFonto(elsendo)
                                    }
                                    ludilo.fiksiFonton(fonto); ludilo.ludi()
                                }
                            },
                            onElshuti = {
                                logi("Nav", "Elŝutas elsendon: ${elsendo.id}")
                                scope.launch { elshutDeponejo.elshuti(elsendo) }
                            },
                            onForigiElshuton = {
                                logi("Nav", "Forigas elŝuton: ${elsendo.id}")
                                scope.launch { elshutDeponejo.forigi(elsendo.id) }
                            },
                            elshutDeponejo = elshutDeponejo
                        )
                    }
                    Ekrano.SERCXO -> {
                        SercxoEkrano(
                            sercxoDeponejo = sercxoDeponejo,
                            onReen = { logi("Nav", "→ KANALARO (reen)"); ekrano = Ekrano.KANALARO },
                            onElsendo = { elsendo -> logi("Nav", "→ ELSENDO el serĉo: ${elsendo.id}"); elektitaElsendo = elsendo; ekrano = Ekrano.ELSENDO }
                        )
                    }
                    Ekrano.PLEJSATATAJ -> {
                        PlejsatatajEkrano(
                            plejsatatajDeponejo = plejsatatajDeponejo,
                            kanalDeponejo = kanalDeponejo,
                            onReen = { logi("Nav", "→ KANALARO (reen)"); ekrano = Ekrano.KANALARO },
                            onKanal = { kanal -> logi("Nav", "→ KANAL el plejŝatataj: ${kanal.slug}"); elektitaKanal = kanal; ekrano = Ekrano.KANAL }
                        )
                    }
                    Ekrano.ELSHUTOJ -> {
                        ElshutitajEkrano(
                            elshutDeponejo = elshutDeponejo,
                            onReen = { logi("Nav", "→ KANALARO (reen)"); ekrano = Ekrano.KANALARO },
                            onLudi = { fonto ->
                                logi("Nav", "Ludas elŝutitan: ${fonto}")
                                scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                            },
                            onElsendo = { elsendo -> logi("Nav", "→ ELSENDO el elŝutoj: ${elsendo.id}"); elektitaElsendo = elsendo; ekrano = Ekrano.ELSENDO }
                        )
                    }
                    Ekrano.ALARMOJ -> {
                        AlarmoEkrano(
                            alarmoDeponejo = alarmoDeponejo,
                            kanalDeponejo = kanalDeponejo,
                            onReen = { logi("Nav", "→ KANALARO (reen)"); ekrano = Ekrano.KANALARO }
                        )
                    }
                    Ekrano.AGORDOJ -> {
                        AgordojEkrano(
                            agordojDeponejo = agordojDeponejo,
                            onReen = { logi("Nav", "→ KANALARO (reen)"); ekrano = Ekrano.KANALARO }
                        )
                    }
                }
            }

            MiniLudilbreto(ludilo = ludilo)
        }
    }
}
