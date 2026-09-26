package dk.nordfalk.esperanto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.ghisdatiguSciigSkedon
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.SciigoKontroloj
import dk.nordfalk.esperanto.domain.player.kreuDefauxltanLudiloRegilon
import dk.nordfalk.esperanto.logi
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import dk.nordfalk.esperanto.navigation.Vojo
import dk.nordfalk.esperanto.ui.*
import dk.nordfalk.esperanto.ui.MuzaikoTiparo
import dk.nordfalk.esperanto.ui.MuzaikoFormoj
import dk.nordfalk.esperanto.ui.temuKolorskemo
import dk.nordfalk.esperanto.ui.TemoNomo
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Vojo.Hejmo::class, Vojo.Hejmo.serializer())
            subclass(Vojo.Kanalaro::class, Vojo.Kanalaro.serializer())
            subclass(Vojo.Plejŝatataj::class, Vojo.Plejŝatataj.serializer())
            subclass(Vojo.Sercxo::class, Vojo.Sercxo.serializer())
            subclass(Vojo.Elshutoj::class, Vojo.Elshutoj.serializer())
            subclass(Vojo.Ludvico::class, Vojo.Ludvico.serializer())
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
    val settings = remember { kreuSettings() }
    val agordojDeponejo = remember { AgordojDeponejoImpl(settings) }
    val temoNomo by agordojDeponejo.temo.collectAsState()
    val temo = runCatching { TemoNomo.valueOf(temoNomo) }.getOrDefault(TemoNomo.ANTONIA)

    MaterialTheme(
        colorScheme = temuKolorskemo(temo, malhela),
        typography = MuzaikoTiparo,
        shapes = MuzaikoFormoj,
    ) {
        if (!AppStato.inicialigita()) {
            AppStato.inicialigu(ludilo, settings, agordojDeponejo)
        }
        val kanaloDeponejo = AppStato.kanaloDeponejo!!
        val elsendoDeponejo = AppStato.elsendoDeponejo!!
        val kanalaroViewModel = AppStato.kanalaroViewModel!!
        val plejŝatatajDeponejo = AppStato.plejŝatatajDeponejo!!
        val sercxoDeponejo = AppStato.sercxoDeponejo!!
        val elshutDeponejo = AppStato.elshutDeponejo!!
        val alarmoDeponejo = AppStato.alarmoDeponejo!!
        val ludatojDeponejo = AppStato.ludatojDeponejo!!
        val ludvicoRegilo = AppStato.ludvicoRegilo!!
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) { ludvicoRegilo.komenci() }

        // Konektu sciigo-kontrolon (Venonta) al LudvicoRegilo
        LaunchedEffect(Unit) {
            SciigoKontroloj.onVenonta = {
                val nunaElsendo = when (val fonto = ludvicoRegilo.stato.value.nunaFonto) {
                    is Sonfonto.ElsendoFonto -> fonto.elsendo
                    is Sonfonto.LokaElsendo -> fonto.elsendo
                    else -> null
                }
                ludvicoRegilo.ludiSekvan(nunaElsendo)
            }
        }

        // Savu pozicion kiam la komponanto detruiĝas (ekz. app fermo)
        DisposableEffect(ludvicoRegilo) {
            onDispose {
                scope.launch { ludvicoRegilo.savuPozicionNun() }
            }
        }

        val backStack = rememberNavBackStack(navConfig, Vojo.Hejmo)
        val kanaloj by kanaloDeponejo.observiKanalojn().collectAsState()
        val ludantoStato by ludilo.stato.collectAsState()

        val nunaVojo = backStack.lastOrNull()

        // Observi ŝanĝojn de ŝatoj kaj sciigoj por ĝisdatigi la fonan skedon
        val plejŝatataj by plejŝatatajDeponejo.observiPlejŝatatajn().collectAsState()
        val sciigoj by agordojDeponejo.sciigoj.collectAsState()
        LaunchedEffect(plejŝatataj, sciigoj) {
            ghisdatiguSciigSkedon(plejŝatataj, sciigoj)
        }

        // Observi pending elsendo-navigadon (de sciigo-klako)
        val pendingElsendo by dk.nordfalk.esperanto.data.repository.PendingElsendoNavigacio.elsendo.collectAsState()

        fun switchTab(vojo: Vojo) {
            val antauxa = nunaVojo?.javaClass?.simpleName ?: "nenio"
            logi("Nav", "→ tab: $vojo")
            Sentry.addBreadcrumb(Breadcrumb.navigation(antauxa, vojo.javaClass.simpleName))
            backStack.clear()
            backStack.add(Vojo.Hejmo)
            if (vojo !is Vojo.Hejmo) backStack.add(vojo)
        }

        fun push(vojo: Vojo) {
            val antauxa = nunaVojo?.javaClass?.simpleName ?: "nenio"
            logi("Nav", "→ push: $vojo")
            Sentry.addBreadcrumb(Breadcrumb.navigation(antauxa, vojo.javaClass.simpleName))
            backStack.add(vojo)
        }

        LaunchedEffect(pendingElsendo) {
            pendingElsendo?.let {
                logi("Nav", "→ sciigo: malfermas elsendon ${it.id}")
                push(Vojo.ElsendoDetalo(it))
                dk.nordfalk.esperanto.data.repository.PendingElsendoNavigacio.setu(null)
            }
        }

        fun reen() {
            logi("Nav", "← reen")
            if (backStack.size > 1) backStack.removeLastOrNull()
        }

        val montruSubanBreton = nunaVojo !is Vojo.Agordoj && nunaVojo !is Vojo.Alarmoj

        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
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
                                onLudi = { elsendo ->
                                    logi("Nav", "Hejmo: ludas elsendon ${elsendo.id}")
                                    scope.launch { ludvicoRegilo.ludiElsendon(elsendo) }
                                },
                                onAgordoj = { push(Vojo.Agordoj) },
                                onElshutoj = { push(Vojo.Elshutoj) },
                                onAlarmoj = { push(Vojo.Alarmoj) },
                                onElshuti = { elsendo ->
                                    logi("Nav", "Hejmo: elŝutas ${elsendo.id}")
                                    scope.launch { elshutDeponejo.elshuti(elsendo) }
                                },
                                onAldoniAlVico = { elsendo ->
                                    logi("Nav", "Hejmo: aldonas al ludvico ${elsendo.id}")
                                    scope.launch { ludvicoRegilo.aldoniAlVico(elsendo) }
                                },
                                ludatojDeponejo = ludatojDeponejo,
                                ludilo = ludilo,
                            )
                        }
                        entry<Vojo.Kanalaro> {
                            KanalaroEkrano(
                                viewModel = kanalaroViewModel,
                                onKanalo = { kanalo -> push(Vojo.KanaloDetalo(kanalo)) },
                                onLudi = { fonto ->
                                    logi("Nav", "Ludas: $fonto")
                                    scope.launch {
                                        when (fonto) {
                                            is Sonfonto.ElsendoFonto -> ludvicoRegilo.ludiElsendon(fonto.elsendo)
                                            is Sonfonto.LokaElsendo -> ludvicoRegilo.ludiElsendon(fonto.elsendo)
                                            is Sonfonto.RektaKanalo -> { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                                        }
                                    }
                                },
                                onElshutoj = { push(Vojo.Elshutoj) },
                                onAlarmoj = { push(Vojo.Alarmoj) },
                                onAgordoj = { push(Vojo.Agordoj) },
                            )
                        }
                        entry<Vojo.Plejŝatataj> {
                            PlejŝatatajEkrano(
                                plejŝatatajDeponejo = plejŝatatajDeponejo,
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
                                ludilo = ludilo,
                                onReen = { reen() },
                                onLudi = { fonto ->
                                    logi("Nav", "Ludas elŝutitan: $fonto")
                                    val elsendo = when (fonto) {
                                        is Sonfonto.ElsendoFonto -> fonto.elsendo
                                        is Sonfonto.LokaElsendo -> fonto.elsendo
                                        is Sonfonto.RektaKanalo -> null
                                    }
                                    if (elsendo != null) {
                                        scope.launch { ludvicoRegilo.ludiElsendon(elsendo) }
                                    } else {
                                        scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                                    }
                                },
                                onElsendo = { elsendo -> push(Vojo.ElsendoDetalo(elsendo)) },
                            )
                        }
                        entry<Vojo.Ludvico> {
                            LudvicoEkrano(
                                ludvicoRegilo = ludvicoRegilo,
                                onReen = { reen() },
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
                            KanalEkranoKunSvipo(
                                komencaKanalo = vojo.kanalo,
                                kanaloj = kanaloj,
                                elsendoDeponejo = elsendoDeponejo,
                                plejŝatatajDeponejo = plejŝatatajDeponejo,
                                agordojDeponejo = agordojDeponejo,
                                onReen = { reen() },
                                onElsendo = { elsendo -> push(Vojo.ElsendoDetalo(elsendo)) },
                                onLudi = { fonto ->
                                    logi("Nav", "Ludas rekte: ${vojo.kanalo.slug}")
                                    scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                                },
                                ludilo = ludilo,
                            )
                        }
                        entry<Vojo.ElsendoDetalo> { vojo ->
                            val elsendo = vojo.elsendo
                            val kanalo = kanaloj.find { it.slug == elsendo.kanaloSlug }
                            val elsendoj by elsendoDeponejo.observiElsendojn(elsendo.kanaloSlug).collectAsState()
                            LaunchedEffect(kanalo?.slug) {
                                if (kanalo != null && elsendoj.isEmpty()) {
                                    elsendoDeponejo.sxargxiElsendojnPorKanal(kanalo)
                                }
                            }
                            ElsendoEkranoKunSvipo(
                                komencaElsendo = elsendo,
                                elsendoj = elsendoj,
                                kanalo = kanalo,
                                onKanalo = { k -> push(Vojo.KanaloDetalo(k)) },
                                onReen = { reen() },
                                onLudi = { e ->
                                    logi("Nav", "Ludas elsendon: ${e.id}")
                                    scope.launch { ludvicoRegilo.ludiElsendon(e) }
                                },
                                onElshuti = { e ->
                                    logi("Nav", "Elŝutas elsendon: ${e.id}")
                                    scope.launch { elshutDeponejo.elshuti(e) }
                                },
                                onHaltigiElshuton = { e ->
                                    logi("Nav", "Haltigas elŝuton: ${e.id}")
                                    scope.launch { elshutDeponejo.haltigi(e.id) }
                                },
                                onForigiElshuton = { e ->
                                    logi("Nav", "Forigas elŝuton: ${e.id}")
                                    scope.launch { elshutDeponejo.forigi(e.id) }
                                },
                                onAldoniAlVico = { e ->
                                    logi("Nav", "Aldonas al ludvico: ${e.id}")
                                    scope.launch { ludvicoRegilo.aldoniAlVico(e) }
                                },
                                onMontriLudvicon = {
                                    logi("Nav", "Montri ludvicon")
                                    push(Vojo.Ludvico)
                                },
                                elshutDeponejo = elshutDeponejo,
                                ludatojDeponejo = ludatojDeponejo,
                                ludilo = ludilo,
                                agordojDeponejo = agordojDeponejo,
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
                    onLudvico = { push(Vojo.Ludvico) },
                )
                MalsupraNavigaBreto(
                    nunaTab = when (nunaVojo) {
                        is Vojo.Hejmo -> EkranoLangeto.HEJMO
                        is Vojo.Kanalaro -> EkranoLangeto.KANALARO
                        is Vojo.Plejŝatataj -> EkranoLangeto.PLEJŜATATAJ
                        is Vojo.Sercxo -> EkranoLangeto.SERCXO
                        else -> EkranoLangeto.NENIO
                    },
                    onHejmo = { switchTab(Vojo.Hejmo) },
                    onKanalaro = { switchTab(Vojo.Kanalaro) },
                    onPlejŝatataj = { switchTab(Vojo.Plejŝatataj) },
                    onSercxo = { switchTab(Vojo.Sercxo) },
                )
            }
        }

        // Plenekrana filmo (malfermita per klako sur la filmeto en
        // ElsendoEkrano): kovras la tutan ekranon, ankaŭ la mini-ludilon
        // kaj la navigan breton. Sama fenestro — ne Dialogo — ĉar la
        // videa SurfaceView ne bildiĝas fideble en subfenestroj.
        PlenekranaVido.elsendo?.let { plenaElsendo ->
            // Pinĉ-zomo (1×…5×) kaj trenado per du fingroj; duobla klako
            // restarigas 1×; unuobla klako fermas (nur je 1×)
            var skalo by remember { mutableStateOf(1f) }
            var ofseto by remember { mutableStateOf(Offset.Zero) }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zomo, _ ->
                            val novaSkalo = (skalo * zomo).coerceIn(1f, 5f)
                            skalo = novaSkalo
                            ofseto = if (novaSkalo > 1f) ofseto + pan else Offset.Zero
                        }
                    },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer {
                            scaleX = skalo
                            scaleY = skalo
                            translationX = ofseto.x
                            translationY = ofseto.y
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (skalo <= 1f) {
                                        logi("Klako", "fermu plenekranan — ${plenaElsendo.id}")
                                        PlenekranaVido.fermu()
                                    }
                                },
                                onDoubleTap = {
                                    logi("Klako", "zomo restarigita — ${plenaElsendo.id}")
                                    skalo = 1f
                                    ofseto = Offset.Zero
                                },
                            )
                        },
                ) {
                    VideoVido(
                        elsendo = plenaElsendo,
                        modifier = Modifier.fillMaxSize(),
                        montru = true,
                    )
                }
                IconButton(
                    onClick = { PlenekranaVido.fermu() },
                    // status-breta paddo: sen ĝi la butono falas sub la stata
                    // breto en horizontala reĝimo kaj ne reagas al klakoj
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding(),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermi plenekranan vidon", tint = Color.White)
                }
            }
        }
        }
    }
}
