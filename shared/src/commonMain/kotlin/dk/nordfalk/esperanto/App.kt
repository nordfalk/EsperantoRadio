package dk.nordfalk.esperanto

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.ghisdatiguSciigSkedon
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
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
import kotlinx.coroutines.flow.MutableStateFlow
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
            subclass(Vojo.Diagnozo::class, Vojo.Diagnozo.serializer())
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
        val diagnozoRegilo = AppStato.diagnozoRegilo!!
        val apajNovajxojDeponejo = AppStato.apajNovajxojDeponejo!!
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

        // Hoistita elfaldita-ŝtato de la mini-ludilbreto — por ke reen-butono kaj
        // klako ekstere povu kolapsi ĝin
        var elfaldita by rememberSaveable { mutableStateOf(false) }

        // La nune VIDATA kanalo/elsendo sur la detalaj ekranoj — sekvata de svipoj
        // (HorizontalPager); uzata de la mini-ludilbreto kiam nenio ludas.
        var vidataKanalo by remember { mutableStateOf<Kanalo?>(null) }
        var vidataElsendo by remember { mutableStateOf<Elsendo?>(null) }

        // La laste ludata elsendo — por la mini-ludilbreto kiam nenio ludas sur
        // ekranoj sen specifa kanalo/elsendo (kiel en Spotify).
        var lasteLudataElsendo by remember { mutableStateOf<Elsendo?>(null) }
        LaunchedEffect(ludantoStato.nunaFonto) {
            when (val fonto = ludantoStato.nunaFonto) {
                is Sonfonto.ElsendoFonto -> lasteLudataElsendo = fonto.elsendo
                is Sonfonto.LokaElsendo -> lasteLudataElsendo = fonto.elsendo
                // Rekta kanalo ne havas elsendon; lasu la lastan podkaston
                else -> {}
            }
        }
        // Bazo-eltenaĵo post restarto: la lasta elsendo laŭ la persistaj ludatoj
        val lastatempeLudatajFluo = remember {
            AppStato.hejmoViewModel?.lastatempeLudataj ?: MutableStateFlow(emptyList<Elsendo>())
        }
        val lastatempeLudataj by lastatempeLudatajFluo.collectAsState()

        // Observi ŝanĝojn de ŝatoj kaj sciigoj por ĝisdatigi la fonan skedon
        val plejŝatataj by plejŝatatajDeponejo.observiPlejŝatatajn().collectAsState()
        val sciigoj by agordojDeponejo.sciigoj.collectAsState()
        LaunchedEffect(plejŝatataj, sciigoj) {
            ghisdatiguSciigSkedon(plejŝatataj, sciigoj)
        }

        // Observi pending elsendo-navigadon (de sciigo-klako)
        val pendingElsendo by dk.nordfalk.esperanto.data.repository.PendingElsendoNavigacio.elsendo.collectAsState()

        fun switchTab(vojo: Vojo) {
            val antauxa = nunaVojo?.let { it::class.simpleName } ?: "nenio"
            logi("Nav", "→ tab: $vojo")
            Sentry.addBreadcrumb(Breadcrumb.navigation(antauxa, vojo::class.simpleName ?: "?"))
            backStack.clear()
            backStack.add(Vojo.Hejmo)
            if (vojo !is Vojo.Hejmo) backStack.add(vojo)
        }

        fun push(vojo: Vojo) {
            // Evitu duplikatan ŝlosilon supre de la stako — ekz. klako sur la
            // mini-ludilbreto dum la elsendo jam estas malfermita ne puŝu duan kopion
            if (backStack.lastOrNull() == vojo) {
                logi("Nav", "→ push preterlasita (jam supre): $vojo")
                return
            }
            val antauxa = nunaVojo?.let { it::class.simpleName } ?: "nenio"
            logi("Nav", "→ push: $vojo")
            Sentry.addBreadcrumb(Breadcrumb.navigation(antauxa, vojo::class.simpleName ?: "?"))
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

        // Flankaj ekranoj (dua nivelo) estas tutekranoj sen mini-ludilbreto —
        // unueca modelo por Agordoj, Alarmoj, Elŝutoj, Ludvico kaj Diagnozo
        fun estasFlankaEkrano(vojo: Any?) = vojo is Vojo.Agordoj ||
            vojo is Vojo.Alarmoj || vojo is Vojo.Elshutoj ||
            vojo is Vojo.Ludvico || vojo is Vojo.Diagnozo

        val montruMiniLudilbreton = !estasFlankaEkrano(nunaVojo)

        fun estasCxefaEkrano(vojo: Any?) =
            vojo is Vojo.Hejmo || vojo is Vojo.Kanalaro ||
            vojo is Vojo.Plejŝatataj || vojo is Vojo.Sercxo

        // Ĉiuj tirkest-eroj kondutas kiel supranivelaj celoj: la stako rekomenciĝas
        // je [Hejmo, elektita vojo], reen-gesto revenas al Hejmo.
        val tirkestStato = rememberDrawerState(DrawerValue.Closed)
        fun elektuTirkeste(vojo: Vojo) {
            scope.launch { tirkestStato.close() }
            switchTab(vojo)
        }

        // Langet-ŝanĝoj fadas (fade-through); detalaj puŝoj/reenigoj glitas horizontale.
        // La implicitaj transiroj de NavDisplay estas platform-dependaj (sur Desktop kaj
        // Web ili tute mankas), do ni difinas ilin eksplicite por ĉiuj platformoj.

        // La tirkesto mem vivas interne de ĉiu ĉefekrano, sub la supra breto
        // (vidu HejmoEkrano ktp.); ĉi tie ni transdonas nur la staton kaj la enhavon.
        val tirkestEnhavo: @Composable () -> Unit = {
            NavigaTirkesto(tirkestStato = tirkestStato, nunaVojo = nunaVojo, onElektu = ::elektuTirkeste)
        }

        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { reen() },
                    transitionSpec = {
                        if (estasCxefaEkrano(initialState.key) && estasCxefaEkrano(targetState.key)) {
                            // langet-ŝanĝo: subtila fade-through
                            fadeIn(tween(200)) togetherWith fadeOut(tween(90))
                        } else {
                            // detala puŝo: nova ekrano glitas el dekstre
                            (slideInHorizontally(tween(240)) { it / 4 } + fadeIn(tween(240))) togetherWith
                                (slideOutHorizontally(tween(240)) { -it / 4 } + fadeOut(tween(240)))
                        }
                    },
                    popTransitionSpec = {
                        if (estasCxefaEkrano(initialState.key) && estasCxefaEkrano(targetState.key)) {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(90))
                        } else {
                            // reenigo: detala ekrano glitas reen dekstren
                            (slideInHorizontally(tween(240)) { -it / 4 } + fadeIn(tween(240))) togetherWith
                                (slideOutHorizontally(tween(240)) { it / 4 } + fadeOut(tween(240)))
                        }
                    },
                    entryProvider = entryProvider {
                        entry<Vojo.Hejmo> {
                            HejmoEkrano(
                                kanaloDeponejo = kanaloDeponejo,
                                elsendoDeponejo = elsendoDeponejo,
                                onKanalo = { kanalo -> push(Vojo.KanaloDetalo(kanalo)) },
                                onKanalaro = { switchTab(Vojo.Kanalaro) },
                                tirkestStato = tirkestStato,
                                tirkestEnhavo = tirkestEnhavo,
                                onElsendo = { elsendo -> push(Vojo.ElsendoDetalo(elsendo)) },
                                onLudi = { elsendo ->
                                    logi("Nav", "Hejmo: ludas elsendon ${elsendo.id}")
                                    scope.launch { ludvicoRegilo.ludiElsendon(elsendo) }
                                },
                                onAgordoj = { push(Vojo.Agordoj) },
                                onElshutoj = { push(Vojo.Elshutoj) },
                                onAlarmoj = { push(Vojo.Alarmoj) },
                                onKasxuHejmajnAvertojn = {
                                    // Kaŝu la NUNAJN problemo-tipojn; nova problemo poste reaperigos la karton
                                    agordojDeponejo.kasxuAvertojn(diagnozoRegilo.problemoj.value.map { it.tipo }.toSet())
                                },
                                diagnozoProblemoj = diagnozoRegilo.problemoj,
                                kasxitajAvertoj = agordojDeponejo.kasxitajAvertoj,
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
                                tirkestStato = tirkestStato,
                                tirkestEnhavo = tirkestEnhavo,
                                onElshutoj = { push(Vojo.Elshutoj) },
                                onAlarmoj = { push(Vojo.Alarmoj) },
                                onAgordoj = { push(Vojo.Agordoj) },
                                onLudi = { fonto ->
                                    logi("Nav", "Ludas: $fonto")
                                    scope.launch {
                                        when (fonto) {
                                            is Sonfonto.ElsendoFonto -> ludvicoRegilo.ludiElsendon(fonto.elsendo)
                                            is Sonfonto.LokaElsendo -> ludvicoRegilo.ludiElsendon(fonto.elsendo)
                                            // (PR #92) tra la regilo — reprovo/kalkuloj/dedup restas koheraj
                                            is Sonfonto.RektaKanalo -> ludvicoRegilo.ludiRektan(fonto.kanalo)
                                        }
                                    }
                                },
                            )
                        }
                        entry<Vojo.Plejŝatataj> {
                            PlejŝatatajEkrano(
                                plejŝatatajDeponejo = plejŝatatajDeponejo,
                                kanaloDeponejo = kanaloDeponejo,
                                onKanalo = { kanalo -> push(Vojo.KanaloDetalo(kanalo)) },
                                tirkestStato = tirkestStato,
                                tirkestEnhavo = tirkestEnhavo,
                            )
                        }
                        entry<Vojo.Sercxo> {
                            SercxoEkrano(
                                sercxoDeponejo = sercxoDeponejo,
                                tirkestStato = tirkestStato,
                                tirkestEnhavo = tirkestEnhavo,
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
                                onDiagnozo = { push(Vojo.Diagnozo) },
                                diagnozoProblemoj = diagnozoRegilo.problemoj,
                            )
                        }
                        entry<Vojo.Diagnozo> {
                            DiagnozoEkrano(
                                diagnozoRegilo = diagnozoRegilo,
                                onReen = { reen() },
                            )
                        }
                        entry<Vojo.KanaloDetalo> { vojo ->
                            // Restarigu la vidatan kanalon ĉe ĉiu eniro — svipoj poste transprenas
                            LaunchedEffect(vojo) { vidataKanalo = vojo.kanalo }
                            KanalEkranoKunSvipo(
                                komencaKanalo = vojo.kanalo,
                                kanaloj = kanaloj,
                                elsendoDeponejo = elsendoDeponejo,
                                plejŝatatajDeponejo = plejŝatatajDeponejo,
                                agordojDeponejo = agordojDeponejo,
                                onReen = { reen() },
                                onAgordoj = { push(Vojo.Agordoj) },
                                onVidataKanalo = { vidataKanalo = it },
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
                            // Restarigu la vidatan elsendon ĉe ĉiu eniro — svipoj poste transprenas
                            LaunchedEffect(vojo) { vidataElsendo = vojo.elsendo }
                            ElsendoEkranoKunSvipo(
                                komencaElsendo = elsendo,
                                elsendoj = elsendoj,
                                kanalo = kanalo,
                                onKanalo = { k -> push(Vojo.KanaloDetalo(k)) },
                                onReen = { reen() },
                                onVidataElsendo = { vidataElsendo = it },
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
                                // (PR #92) paŭzigi/daŭrigi tra LudvicoRegilo — rekta ludilo-voko
                                // ne nuligas planitan reprovon
                                onPauxzigi = { scope.launch { ludvicoRegilo.pauxziguLudadon() } },
                                onDaurigi = { scope.launch { ludvicoRegilo.dauriguLudadon() } },
                            )
                        }
                    },
                )

                // Scrim-overlay: klakoj ekstere kolapsas la elfalditan ludilbreton
                if (elfaldita) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                logi("Klako", "scrim → enfoldigi la ludilon")
                                elfaldita = false
                            }
                    )
                }

                // Kaptu reen-butonon por enfoldigi la ludilbreton — devas esti post
                // NavDisplay por havi prioritaton (laste komponita handler venkas)
                val kolapsoStato = rememberNavigationEventState(NavigationEventInfo.None)
                NavigationBackHandler(
                    state = kolapsoStato,
                    isBackEnabled = elfaldita,
                    onBackCompleted = {
                        logi("Nav", "← reen: enfoldigas ludilbreton (NavigationBackHandler)")
                        elfaldita = false
                    },
                )
            }

            if (montruMiniLudilbreton) {
                val reprovo by ludvicoRegilo.reprovo.collectAsState()

                // Sekvanta enhavo — kiam nenio ludas, la breto montras la nunan navigan
                // celon (kanalo/elsendo detalo, sekvante svipojn) aŭ — sur aliaj ekranoj —
                // la laste ludatan elsendon.
                val sekvantaTitolo: String?
                val sekvantaSubtitolo: String?
                val sekvantaBildoUrl: String?
                val onLudiSekvantan: () -> Unit
                // Klako sur la sekvanta breto navigas tien; null = jam sur tiu ekrano
                val sekvantaVojo: Vojo? = when (val vojo = nunaVojo) {
                    is Vojo.KanaloDetalo -> {
                        val k = vidataKanalo ?: vojo.kanalo
                        sekvantaTitolo = k.nomo
                        sekvantaSubtitolo = "Kanalo"
                        sekvantaBildoUrl = k.emblemoUrl
                        onLudiSekvantan = {
                            val fonto = k.rektaElsendaSonoUrl?.let { Sonfonto.RektaKanalo(k) }
                            if (fonto != null) {
                                logi("Nav", "Sekvanta: ludas rekte ${k.slug}")
                                scope.launch { ludilo.fiksiFonton(fonto); ludilo.ludi() }
                            }
                        }
                        null
                    }
                    is Vojo.ElsendoDetalo -> {
                        // Svipoj ŝanĝas la vidatan elsendon — uzu ĝin, ne la eniran
                        val e = vidataElsendo?.takeIf { it.kanaloSlug == vojo.elsendo.kanaloSlug } ?: vojo.elsendo
                        sekvantaTitolo = e.titolo
                        sekvantaSubtitolo = e.kanaloNomo
                        sekvantaBildoUrl = e.bildoUrl
                        onLudiSekvantan = {
                            logi("Nav", "Sekvanta: ludas elsendon ${e.id}")
                            scope.launch { ludvicoRegilo.ludiElsendon(e) }
                        }
                        null
                    }
                    else -> {
                        val e = lasteLudataElsendo ?: lastatempeLudataj.firstOrNull()
                        sekvantaTitolo = e?.titolo
                        sekvantaSubtitolo = e?.kanaloNomo
                        sekvantaBildoUrl = e?.bildoUrl
                        onLudiSekvantan = {
                            if (e != null) {
                                logi("Nav", "Sekvanta: daŭrigas laste ludatan elsendon ${e.id}")
                                scope.launch { ludvicoRegilo.ludiElsendon(e) }
                            }
                        }
                        e?.let { Vojo.ElsendoDetalo(it) }
                    }
                }

                MiniLudilbreto(
                    ludilo = ludilo,
                    reprovo = reprovo,
                    elfaldita = elfaldita,
                    onElfalditaSxangxo = { elfaldita = it },
                    sekvantaTitolo = sekvantaTitolo,
                    sekvantaSubtitolo = sekvantaSubtitolo,
                    sekvantaBildoUrl = sekvantaBildoUrl,
                    onLudiSekvantan = onLudiSekvantan,
                    onHaltigi = { ludvicoRegilo.haltuLudadon() },
                    onPauxzigi = { ludvicoRegilo.pauxziguLudadon() },
                    onDaurigi = { ludvicoRegilo.dauriguLudadon() },
                    onClick = {
                        val fonto = ludantoStato.nunaFonto
                        when (fonto) {
                            is Sonfonto.ElsendoFonto -> push(Vojo.ElsendoDetalo(fonto.elsendo))
                            is Sonfonto.LokaElsendo -> push(Vojo.ElsendoDetalo(fonto.elsendo))
                            is Sonfonto.RektaKanalo -> push(Vojo.KanaloDetalo(fonto.kanalo))
                            // Nenio ludas: klaku al la sekvan enhavon (laste ludata elsendo);
                            // sur detalaj ekranoj la breto jam montras tiun enhavon (null)
                            null -> sekvantaVojo?.let { push(it) }
                        }
                    },
                    onLudvico = { push(Vojo.Ludvico) },
                )
            }
        }

        // Apaj novaĵoj — novaĵo-mesaĝo el la kanalkonfiguro; dialogo unufoje
        // por ĉiu nova mesaĝo; "Bone" persistas la haŝaĵon en la agordoj.
        // En evolua medio ĝi ne montriĝas — ĉe freŝa instalaĵo la haŝaĵo
        // forestas kaj la dialogo aperus ĉiufoje, ĝene dum programado.
        val apajNovajxoj by apajNovajxojDeponejo.observuMontratan().collectAsState()
        apajNovajxoj?.takeIf { !AppStato.estasEvoluaMedo }?.let { mesagxo ->
            LaunchedEffect(mesagxo) {
                logi("ApajNovajxoj", "Montras novajn apajn novaĵojn (hesho=${mesagxo.hashCode()})")
            }
            AlertDialog(
                // Kiel en la malnova apo: nur la "Bone"-butono fermas la dialogon
                onDismissRequest = { },
                title = { Text("Novaĵoj") },
                // uzuWebView=false → AnnotatedString-vidigo (la elekto de la uzanto)
                text = { HtmlVido(html = mesagxo, uzuWebView = false, modifier = Modifier.fillMaxWidth()) },
                confirmButton = {
                    TextButton(onClick = {
                        logi("ApajNovajxoj", "Uzanto konfirmis la apajn novaĵojn")
                        apajNovajxojDeponejo.konfirmu()
                    }) { Text("Bone") }
                },
            )
        }
    }
}
