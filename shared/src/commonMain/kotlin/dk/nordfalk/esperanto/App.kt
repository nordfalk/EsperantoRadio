package dk.nordfalk.esperanto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.config.parsuSugestojnPorAlarmoj
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl
import dk.nordfalk.esperanto.data.repository.PersistantaPlejsatatajDeponejo
import dk.nordfalk.esperanto.data.repository.SercxoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.kreuElshutDeponejo
import dk.nordfalk.esperanto.data.repository.PersistantaAlarmoDeponejo
import dk.nordfalk.esperanto.data.repository.kreuAlarmoSkedilo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.kreuDefauxltanLudiloRegilon
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.navigation.Vojo
import dk.nordfalk.esperanto.ui.*
import dk.nordfalk.esperanto.ui.MuzaikoTiparo
import dk.nordfalk.esperanto.ui.MuzaikoFormoj
import dk.nordfalk.esperanto.ui.temuKolorskemo
import dk.nordfalk.esperanto.ui.TemoNomo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Vojo.Hejmo::class, Vojo.Hejmo.serializer())
            subclass(Vojo.Kanalaro::class, Vojo.Kanalaro.serializer())
            subclass(Vojo.Plejsatataj::class, Vojo.Plejsatataj.serializer())
            subclass(Vojo.Sercxo::class, Vojo.Sercxo.serializer())
            subclass(Vojo.Elshutoj::class, Vojo.Elshutoj.serializer())
            subclass(Vojo.Alarmoj::class, Vojo.Alarmoj.serializer())
            subclass(Vojo.Agordoj::class, Vojo.Agordoj.serializer())
            subclass(Vojo.KanaloDetalo::class, Vojo.KanaloDetalo.serializer())
            subclass(Vojo.ElsendoDetalo::class, Vojo.ElsendoDetalo.serializer())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsperantoRadioApp(
    ludilo: LudiloRegilo = kreuDefauxltanLudiloRegilon(),
) {
    val malhela = androidx.compose.foundation.isSystemInDarkTheme()
    val agordojDeponejo = remember { AgordojDeponejoImpl() }
    val temoNomo by agordojDeponejo.temo.collectAsState()
    val temo = runCatching { TemoNomo.valueOf(temoNomo) }.getOrDefault(TemoNomo.ANTONIA)

    MaterialTheme(
        colorScheme = temuKolorskemo(temo, malhela),
        typography = MuzaikoTiparo,
        shapes = MuzaikoFormoj,
    ) {
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

        val kanaloDeponejo = remember {
            KanaloDeponejoImpl(
                leganto = KanalAgordoLeganto(),
                bundledTeksto = ::leguBundledKanalkonfiguron
            )
        }
        val elsendoDeponejo = remember { ElsendoDeponejoImpl(httpKliento) }
        val kanalaroViewModel = remember { KanalaroViewModel(kanaloDeponejo) }
        val settings = remember { kreuSettings() }
        val plejsatatajDeponejo = remember { PersistantaPlejsatatajDeponejo(settings) }
        val sercxoDeponejo = remember { SercxoDeponejoImpl(elsendoDeponejo) }
        val elshutDeponejo = remember { kreuElshutDeponejo(httpKliento) }
        val alarmoDeponejo = remember {
            val agordo = KanalAgordoLeganto().legu(leguBundledKanalkonfiguron())
            val sugestoj = agordo.sugestoj_por_alarmoj?.let { parsuSugestojnPorAlarmoj(it) } ?: emptyList()
            PersistantaAlarmoDeponejo(settings, sugestoj, kreuAlarmoSkedilo())
        }
        val scope = rememberCoroutineScope()

        val backStack = rememberNavBackStack(navConfig, Vojo.Hejmo)
        val kanaloj by kanaloDeponejo.observiKanalojn().collectAsState()
        val ludantoStato by ludilo.stato.collectAsState()

        val nunaVojo = backStack.lastOrNull()

        fun switchTab(vojo: Vojo) {
            logi("Nav", "→ tab: $vojo")
            backStack.clear()
            backStack.add(Vojo.Hejmo)
            if (vojo !is Vojo.Hejmo) backStack.add(vojo)
        }

        fun push(vojo: Vojo) {
            logi("Nav", "→ push: $vojo")
            backStack.add(vojo)
        }

        fun reen() {
            logi("Nav", "← reen")
            if (backStack.size > 1) backStack.removeLastOrNull()
        }

        val montruSubanBreton = nunaVojo !is Vojo.Agordoj && nunaVojo !is Vojo.Alarmoj

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { reen() },
                    entryProvider = entryProvider {
                        entry<Vojo.Hejmo> {
                            HejmoEkrano(
                                kanaloDeponejo = kanaloDeponejo,
                                elsendoDeponejo = elsendoDeponejo,
                                onKanalo = { kanalo -> push(Vojo.KanaloDetalo(kanalo)) },
                                onElsendo = { elsendo -> push(Vojo.ElsendoDetalo(elsendo)) },
                                onAgordoj = { push(Vojo.Agordoj) },
                                onElshutoj = { push(Vojo.Elshutoj) },
                                onAlarmoj = { push(Vojo.Alarmoj) },
                            )
                        }
                        entry<Vojo.Kanalaro> {
                            KanalaroEkrano(
                                viewModel = kanalaroViewModel,
                                onKanalo = { kanalo -> push(Vojo.KanaloDetalo(kanalo)) },
                                onLudi = { fonto ->
                                    logi("Nav", "Ludas rekte: $fonto")
                                    scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                                },
                                onElshutoj = { push(Vojo.Elshutoj) },
                                onAlarmoj = { push(Vojo.Alarmoj) },
                                onAgordoj = { push(Vojo.Agordoj) },
                            )
                        }
                        entry<Vojo.Plejsatataj> {
                            PlejsatatajEkrano(
                                plejsatatajDeponejo = plejsatatajDeponejo,
                                kanaloDeponejo = kanaloDeponejo,
                                onKanalo = { kanalo -> push(Vojo.KanaloDetalo(kanalo)) },
                            )
                        }
                        entry<Vojo.Sercxo> {
                            SercxoEkrano(
                                sercxoDeponejo = sercxoDeponejo,
                                onElsendo = { elsendo -> push(Vojo.ElsendoDetalo(elsendo)) },
                            )
                        }
                        entry<Vojo.Elshutoj> {
                            ElshutitajEkrano(
                                elshutDeponejo = elshutDeponejo,
                                onReen = { reen() },
                                onLudi = { fonto ->
                                    logi("Nav", "Ludas elŝutitan: $fonto")
                                    scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                                },
                                onElsendo = { elsendo -> push(Vojo.ElsendoDetalo(elsendo)) },
                            )
                        }
                        entry<Vojo.Alarmoj> {
                            AlarmoEkrano(
                                alarmoDeponejo = alarmoDeponejo,
                                kanaloDeponejo = kanaloDeponejo,
                                onReen = { reen() },
                            )
                        }
                        entry<Vojo.Agordoj> {
                            AgordojEkrano(
                                agordojDeponejo = agordojDeponejo,
                                onReen = { reen() },
                            )
                        }
                        entry<Vojo.KanaloDetalo> { vojo ->
                            KanaloEkrano(
                                kanalo = vojo.kanalo,
                                elsendoDeponejo = elsendoDeponejo,
                                onReen = { reen() },
                                onElsendo = { elsendo -> push(Vojo.ElsendoDetalo(elsendo)) },
                                onLudi = { fonto ->
                                    logi("Nav", "Ludas rekte: ${vojo.kanalo.slug}")
                                    scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                                },
                            )
                        }
                        entry<Vojo.ElsendoDetalo> { vojo ->
                            val elsendo = vojo.elsendo
                            val kanalo = kanaloj.find { it.slug == elsendo.kanaloSlug }
                            ElsendoEkrano(
                                elsendo = elsendo,
                                kanalo = kanalo,
                                onKanalo = { k -> push(Vojo.KanaloDetalo(k)) },
                                onReen = { reen() },
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
                                elshutDeponejo = elshutDeponejo,
                            )
                        }
                    },
                )
            }

            if (montruSubanBreton) {
                MiniLudilbreto(
                    ludilo = ludilo,
                    onClick = {
                        val fonto = ludantoStato.nunaFonto
                        when (fonto) {
                            is Sonfonto.ElsendoFonto -> push(Vojo.ElsendoDetalo(fonto.elsendo))
                            is Sonfonto.LokaElsendo -> push(Vojo.ElsendoDetalo(fonto.elsendo))
                            is Sonfonto.RektaKanalo -> push(Vojo.KanaloDetalo(fonto.kanalo))
                            null -> {}
                        }
                    },
                )
                MalsupraNavigaBreto(
                    nunaTab = when (nunaVojo) {
                        is Vojo.Hejmo -> EkranoLangeto.HEJMO
                        is Vojo.Kanalaro -> EkranoLangeto.KANALARO
                        is Vojo.Plejsatataj -> EkranoLangeto.PLEJSATATAJ
                        is Vojo.Sercxo -> EkranoLangeto.SERCXO
                        else -> EkranoLangeto.NENIO
                    },
                    onHejmo = { switchTab(Vojo.Hejmo) },
                    onKanalaro = { switchTab(Vojo.Kanalaro) },
                    onPlejsatataj = { switchTab(Vojo.Plejsatataj) },
                    onSercxo = { switchTab(Vojo.Sercxo) },
                )
            }
        }
    }
}
