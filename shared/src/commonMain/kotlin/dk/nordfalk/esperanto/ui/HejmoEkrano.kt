package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Severeco
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/**
 * Ĉu la dato estas ene de la donita nombro da tagoj?
 * Hodiaŭaj kaj estontaj datoj validas; neparseblaj datoj ne.
 */
@OptIn(ExperimentalTime::class)
fun estasEneDe(
    dato: String,
    eneDeTagoj: Int,
    nunaDatumo: LocalDate = Clock.System.todayIn(TimeZone.UTC),
): Boolean {
    val tagoj = runCatching { LocalDate.parse(dato) }.getOrNull()?.daysUntil(nunaDatumo) ?: return false
    return tagoj < eneDeTagoj
}

/**
 * Ĉu la dato estas ene de la pasintaj 6 monatoj (180 tagoj)?
 * Regas kiuj elsendoj aperas en "Kio novas".
 */
@OptIn(ExperimentalTime::class)
fun estasEneDeSesMonatoj(
    dato: String,
    nunaDatumo: LocalDate = Clock.System.todayIn(TimeZone.UTC),
): Boolean = estasEneDe(dato, 180, nunaDatumo)

/**
 * Ĉu la dato estas ene de la pasinta jaro (365 tagoj)?
 * Regas kiuj kanaloj estas "aktivaj" (kontraŭe: arkivaj, pli aĝaj ol unu jaro).
 */
@OptIn(ExperimentalTime::class)
fun estasEneDeUnuJaro(
    dato: String,
    nunaDatumo: LocalDate = Clock.System.todayIn(TimeZone.UTC),
): Boolean = estasEneDe(dato, 365, nunaDatumo)

/**
 * Kalkulas la aĝon de elsendo kiel homlegebla teksto — ĉiam redonas tekston,
 * ankaŭ por elsendoj pli malnovaj ol 6 monatoj ("6 monatoj", "1 jaro", ktp.).
 * Redonas null nur se la dato ne parseblas.
 *
 * Uzata por la flava markilo sur la kartoj: ĝi ĉiam montras la aĝon de la
 * elsendo, neniam kiom multe oni aŭskultis.
 */
@OptIn(ExperimentalTime::class)
fun kalkuliAĝon(
    dato: String,
    nunaDatumo: LocalDate = Clock.System.todayIn(TimeZone.UTC),
): String? {
    val parsita = runCatching { LocalDate.parse(dato) }.getOrNull() ?: return null
    val tagoj = maxOf(0, parsita.daysUntil(nunaDatumo))
    val monatoj = tagoj / 30
    val jaroj = tagoj / 365
    return when {
        tagoj == 0 -> "hodiaŭ"
        tagoj == 1 -> "hieraŭ"
        tagoj <= 14 -> "$tagoj tagoj"
        tagoj < 60 -> "${tagoj / 7} semajnoj"
        monatoj < 12 -> "$monatoj monatoj"
        jaroj <= 1 -> "1 jaro"
        else -> "$jaroj jaroj"
    }
}

/**
 * Teksto pri ludata elsendo por montri sur karto.
 *
 * - Se finludita: "aŭdis"
 * - Se parte ludita kun konata daŭro: "aŭdis XX%"
 * - Alie: null (ne ludita aŭ mankas informo)
 */
private fun ludataTeksto(ludata: LudataElsendo): String? {
    if (ludata.lasteLudita <= 0) return null
    return when {
        ludata.finita -> "aŭdis"
        ludata.pozicioMs > 0 && ludata.dauroMs > 0 -> {
            val procento = (ludata.pozicioMs.toFloat() / ludata.dauroMs * 100).toInt().coerceIn(0, 99)
            "aŭdis $procento%"
        }
        else -> null
    }
}

/**
 * Datumo por unu karto en "Ĉiuj kanaloj" — kanalo + ĝia plej nova elsendo.
 */
data class KanalKarto(val kanalo: Kanalo, val plejNovaElsendo: Elsendo?)

/**
 * Dividas kanalkartojn en aktivaj kaj arkivaj.
 * Aktiva = la kanalo havas elsendon dum la pasinta jaro
 * (la plej nova elsendo estas malpli ol 365 tagojn for);
 * arkiva = la plej nova elsendo estas pli aĝa ol unu jaro.
 */
@OptIn(ExperimentalTime::class)
fun dividuKanalojn(
    kartaro: List<KanalKarto>,
    nunaDatumo: LocalDate = Clock.System.todayIn(TimeZone.UTC),
): Pair<List<KanalKarto>, List<KanalKarto>> =
    kartaro.partition { karto ->
        karto.plejNovaElsendo?.let { estasEneDeUnuJaro(it.dato, nunaDatumo) } == true
    }

/**
 * Stato por la hejmekrano. Dum starto ĝi ŝargas ĉiujn kanalojn kaj iliajn
 * RSS-fluojn, kolektas ĉiujn elsendojn, kaj disponigas:
 * - [novajElsendoj] — ĉiuj elsendoj ordigitaj laŭ dato (plej nova unue) por "Kio novas"
 * - [popularajElsendoj] — hazardaj elsendoj por "Kio popularas"
 * - [aktivajKanaloj] / [arkivajKanaloj] — unu karto po kanalo kun la plej nova elsendo,
 *   dividitaj laŭ ĉu la kanalo aktivas (elsendo dum la pasinta jaro; arkivo = pli aĝa ol unu jaro)
 */
@OptIn(ExperimentalTime::class)
class HejmoViewModel(
    private val kanaloDeponejo: KanaloDeponejo,
    private val elsendoDeponejo: ElsendoDeponejo,
    private val ludatojDeponejo: LudatojDeponejo? = null,
) {
    val kanaloj: StateFlow<List<Kanalo>> = kanaloDeponejo.observiKanalojn()

    private val _novajElsendoj = MutableStateFlow<List<Elsendo>>(emptyList())
    val novajElsendoj = _novajElsendoj.asStateFlow()

    private val _popularajElsendoj = MutableStateFlow<List<Elsendo>>(emptyList())
    val popularajElsendoj = _popularajElsendoj.asStateFlow()

    /** Lastatempe ludataj elsendoj — ordigitaj laŭ lasteLudita (plej freŝa unue). */
    private val _lastatempeLudataj = MutableStateFlow<List<Elsendo>>(emptyList())
    val lastatempeLudataj = _lastatempeLudataj.asStateFlow()

    /** Kruda listo de ĉiuj elsendoj — uzata por reaktualigi kiam ludatoj ŝanĝiĝas. */
    private var cxiujElsendoj: List<Elsendo> = emptyList()

    /** Aktivaj kanaloj — kun elsendo dum la pasinta jaro (montrataj unue). */
    private val _aktivajKanaloj = MutableStateFlow<List<KanalKarto>>(emptyList())
    val aktivajKanaloj = _aktivajKanaloj.asStateFlow()

    /** Arkivaj kanaloj — kies plej nova elsendo estas pli aĝa ol unu jaro (post la dividilo). */
    private val _arkivajKanaloj = MutableStateFlow<List<KanalKarto>>(emptyList())
    val arkivajKanaloj = _arkivajKanaloj.asStateFlow()

    private val _sxargxas = MutableStateFlow(false)
    val sxargxas = _sxargxas.asStateFlow()

    /** @param fortoRefresigi true = ignoru la memoran kaŝmemoron kaj elŝutu ĉiujn fluojn denove (malsupren-tiro). */
    suspend fun sxargxi(fortoRefresigi: Boolean = false) {
        val kanaloj = kanaloDeponejo.getKanalojn()
        logi("HejmoViewModel", "Ŝargas elsendojn por ${kanaloj.size} kanaloj (fortoRefresigi=$fortoRefresigi)")

        // Paŝo 1: Legu diskkaŝmemoron (rapida — loka dosier-I/O + re-parsado)
        val kashitaj = (elsendoDeponejo as? ElsendoDeponejoImpl)?.leguĈiujnKashitajnElsendojn(kanaloj) ?: emptyList()
        if (kashitaj.isNotEmpty()) {
            plenigu(kanaloj, kashitaj)
            logi("HejmoViewModel", "Diskkaŝmemoro: ${kashitaj.size} elsendoj — tuj montras")
        }

        // Paŝo 2: Reto-elŝuto (malrapida)
        _sxargxas.value = true
        try {
            val ĉiujElsendoj = coroutineScope {
                kanaloj
                    .filter { it.havasPodkastojn }
                    .map { kanalo -> async { elsendoDeponejo.sxargxiElsendojnPorKanal(kanalo, fortoRefresigi) } }
                    .awaitAll()
                    .flatten()
            }
            logi("HejmoViewModel", "Ŝargis ${ĉiujElsendoj.size} elsendojn entute")
            plenigu(kanaloj, ĉiujElsendoj)
        } catch (e: Exception) {
            loge("HejmoViewModel", "Malsukcesis ŝargi hejmon", e)
        } finally {
            _sxargxas.value = false
        }
    }

    /**
     * Rekte plenigas la fluojn sen asinkrona ŝargado — uzata por antaŭvidoj.
     */
    @OptIn(ExperimentalTime::class)
    fun plenigu(kanaloj: List<Kanalo>, elsendoj: List<Elsendo>) {
        cxiujElsendoj = elsendoj
        val nunaDatumo = Clock.System.todayIn(TimeZone.UTC)

        val cxiujKanalojKartog = kanaloj
            .filter { it.havasPodkastojn }
            .map { kanalo ->
                KanalKarto(kanalo, elsendoj.filter { it.kanaloSlug == kanalo.slug }.maxByOrNull { it.dato })
            }
            .filter { it.plejNovaElsendo != null }
        val (aktivaj, arkivaj) = dividuKanalojn(cxiujKanalojKartog, nunaDatumo)
        _aktivajKanaloj.value = aktivaj
        _arkivajKanaloj.value = arkivaj

        aktualigiuLudatajn()
    }

    /**
     * Reaktualigas "Kio novas", "Lastatempe ludata" kaj "Kio popularas" surbaze
     * de la nunaj ludatoj. Vokata post [plenigu] kaj kiam ludatoj ŝanĝiĝas.
     *
     * - Luditaj elsendoj malaperas de "Kio novas" kaj aperas en "Lastatempe ludata"
     * - Maksimume 5 plej novaj elsendoj po kanalo en "Kio novas"
     */
    @OptIn(ExperimentalTime::class)
    fun aktualigiuLudatajn() {
        val elsendoj = cxiujElsendoj
        if (elsendoj.isEmpty()) return
        val nunaDatumo = Clock.System.todayIn(TimeZone.UTC)

        val ludatoj = ludatojDeponejo?.observiLudatojn()?.value ?: emptyMap()
        val luditajIdj = ludatoj.values
            .filter { it.lasteLudita > 0 }
            .map { it.elsendoId }
            .toSet()

        // "Kio novas" — ekskludu luditajn, maks 5 po kanalo
        val novaj = elsendoj
            .filter { estasEneDeSesMonatoj(it.dato, nunaDatumo) && it.id !in luditajIdj }
            .groupBy { it.kanaloSlug }
            .flatMap { (_, grupo) -> grupo.sortedByDescending { it.dato }.take(5) }
            .sortedByDescending { it.dato }
            .take(50)
        _novajElsendoj.value = novaj

        // "Lastatempe ludata" — elsendoj kiuj estis luditaj, ordigitaj laŭ lasteLudita
        val lastatempe = ludatoj.values
            .filter { it.lasteLudita > 0 }
            .sortedByDescending { it.lasteLudita }
            .mapNotNull { ludato -> elsendoj.find { it.id == ludato.elsendoId } }
            .take(20)
        _lastatempeLudataj.value = lastatempe

        // "Kio popularas" — ekskludu novajn, kanalajn kaj luditajn
        val kanalajIdj = (aktivajKanaloj.value + arkivajKanaloj.value)
            .mapNotNull { it.plejNovaElsendo?.id }
            .toSet()
        val ekskluditaj = (novaj.take(20).map { it.id } + kanalajIdj + luditajIdj).toSet()
        _popularajElsendoj.value = elsendoj
            .filter { it.id !in ekskluditaj }
            .shuffled()
            .take(20)
    }
}

/**
 * Determinas ĉu la donita elsendo nun ludas aŭ paŭzas, surbaze de la ludanto-stato.
 * Redonas (ludas, pauxzita).
 */
private fun elsendoLudas(elsendoId: String, stato: LudantoInformo): Pair<Boolean, Boolean> {
    val fonto = stato.nunaFonto
    val kongruas = when (fonto) {
        is Sonfonto.ElsendoFonto -> fonto.elsendo.id == elsendoId
        is Sonfonto.LokaElsendo -> fonto.elsendo.id == elsendoId
        else -> false
    }
    val s = stato.stato
    return Pair(kongruas && s is LudantoStato.Ludas, kongruas && s is LudantoStato.Haltita)
}


/**
 * Nova hejmekrano laux Figma-dizajno "Muzaiko — Antonia".
 *
 * Horizontalaj rulantaj sekcioj:
 * - "Kio novas" — novaj elsendoj de ĉiuj kanaloj (horizontala LazyRow de kartoj)
 * - "Lastatempe ludata" — laste luditaj (nur se ekzistas)
 * - "Kio popularas" — hazardaj elsendoj (horizontala LazyRow de kartoj)
 * - "Ĉiuj kanaloj" — unu karto po kanalo kun la plej nova elsendo
 *
 * Malsupra naviga breto (NavigationBar) kun 4 langetoj.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HejmoEkrano(
    kanaloDeponejo: KanaloDeponejo,
    elsendoDeponejo: ElsendoDeponejo,
    onKanalo: (Kanalo) -> Unit = {},
    onElsendo: (Elsendo) -> Unit = {},
    onLudi: (Elsendo) -> Unit = {},
    onAgordoj: () -> Unit = {},
    onElshutoj: () -> Unit = {},
    onAlarmoj: () -> Unit = {},
    onDiagnozo: () -> Unit = {},
    diagnozoProblemoj: StateFlow<List<DiagnozoRezulto>> = MutableStateFlow(emptyList()),
    onElshuti: (Elsendo) -> Unit = {},
    onAldoniAlVico: (Elsendo) -> Unit = {},
    ludatojDeponejo: LudatojDeponejo? = null,
    ludilo: LudiloRegilo? = null,
    viewModel: HejmoViewModel? = null,
) {
    val vm = viewModel ?: AppStato.hejmoViewModel ?: remember { HejmoViewModel(kanaloDeponejo, elsendoDeponejo, ludatojDeponejo) }
    val kanaloj by vm.kanaloj.collectAsState()
    val novajElsendoj by vm.novajElsendoj.collectAsState()
    val popularajElsendoj by vm.popularajElsendoj.collectAsState()
    val lastatempeLudataj by vm.lastatempeLudataj.collectAsState()
    val aktivajKanaloj by vm.aktivajKanaloj.collectAsState()
    val arkivajKanaloj by vm.arkivajKanaloj.collectAsState()
    val sxargxas by vm.sxargxas.collectAsState()
    val problemoj by diagnozoProblemoj.collectAsState()
    val problemojGravaj = problemoj.filter { it.severeco != Severeco.INFO }
    val scope = rememberCoroutineScope()

    val ludatojMapo by (ludatojDeponejo?.observiLudatojn()?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })

    // Reaktualigu "Kio novas" kaj "Lastatempe ludata" kiam ludatoj ŝanĝiĝas
    LaunchedEffect(ludatojMapo) {
        vm.aktualigiuLudatajn()
    }

    if (viewModel == null) {
        LaunchedEffect(Unit) {
            scope.launch { vm.sxargxi() }
        }
    }

    /** Ludata teksto (se ludita) aŭ aĝo de la elsendo (se ne ludita). */
    fun montruTekston(elsendo: Elsendo, ludata: LudataElsendo?): String? =
        ludata?.let { ludataTeksto(it) } ?: kalkuliAĝon(elsendo.dato)

    /**
     * Karto de kanalo en la "Kanaloj"-vico. La flava markilo ĉiam montras la
     * tempon de la plej nova elsendo — neniam kiom multe oni aŭskultis ĝin.
     */
    @Composable
    fun kanaloKarto(kanto: KanalKarto) {
        val elsendo = kanto.plejNovaElsendo ?: return
        ElsendoKarto(
            elsendo = elsendo,
            kanalo = kanto.kanalo,
            montruTekston = kalkuliAĝon(elsendo.dato),
            ludilo = ludilo,
            onLudi = onLudi,
            onElshuti = { onElshuti(elsendo) },
            onAldoniAlVico = { onAldoniAlVico(elsendo) },
            onClick = { logi("Klako", "kanalo ${kanto.kanalo.slug}"); onKanalo(kanto.kanalo) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EsperantoRadio", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { logi("Klako", "elŝutoj-butono"); onElshutoj() }) { Icon(Icons.Filled.Download, contentDescription = "Elŝutoj") }
                    IconButton(onClick = { logi("Klako", "alarmoj-butono"); onAlarmoj() }) { Icon(Icons.Filled.Alarm, contentDescription = "Vekhorloĝo") }
                    IconButton(onClick = { logi("Klako", "agordoj-butono"); onAgordoj() }) { Icon(Icons.Filled.Settings, contentDescription = "Agordoj") }
                }
            )
        }
    ) { padding ->
        if (sxargxas && novajElsendoj.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Ŝarĝas elsendojn...", modifier = Modifier.padding(8.dp))
                }
            }
        } else PullToRefreshBox(
            isRefreshing = sxargxas,
            onRefresh = { logi("Klako", "malsupren-tiro (Hejmo)"); scope.launch { vm.sxargxi(fortoRefresigi = true) } },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {

                // Avertosigno pri detektitaj problemoj — supre, nur se estas
                if (problemojGravaj.isNotEmpty()) {
                    item {
                        DiagnozoAvertoKarto(problemoj = problemojGravaj, onDiagnozo = onDiagnozo)
                    }
                }

                // "Kanaloj" — unue la aktivaj kanaloj (elsendo en la pasinta jaro),
                // poste dividilo kun "Arkivo" kaj la arkivaj (pli ol jaron malnovaj)
                if (aktivajKanaloj.isNotEmpty() || arkivajKanaloj.isNotEmpty()) {
                    item { SekcioTitolo("Kanaloj") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(aktivajKanaloj) { karto -> kanaloKarto(kanto = karto) }
                            if (arkivajKanaloj.isNotEmpty()) {
                                item { ArkivoDividilo() }
                            }
                            items(arkivajKanaloj) { karto -> kanaloKarto(kanto = karto) }
                        }
                    }
                }

                // "Kio novas"
                if (novajElsendoj.isNotEmpty()) {
                    item { SekcioTitolo("Kio novas") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(novajElsendoj) { elsendo ->
                                val kanalo = kanaloj.find { it.slug == elsendo.kanaloSlug }
                                ElsendoKarto(
                                    elsendo = elsendo,
                                    kanalo = kanalo,
                                    montruTekston = montruTekston(elsendo, ludatojMapo[elsendo.id]),
                                    ludilo = ludilo,
                                    onLudi = onLudi,
                                    onElshuti = { onElshuti(elsendo) },
                                    onAldoniAlVico = { onAldoniAlVico(elsendo) },
                                    onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) },
                                )
                            }
                        }
                    }
                }

                // "Lastatempe ludata" — elsendoj kiuj estis luditaj
                if (lastatempeLudataj.isNotEmpty()) {
                    item { SekcioTitolo("Lastatempe ludata") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(lastatempeLudataj) { elsendo ->
                                val kanalo = kanaloj.find { it.slug == elsendo.kanaloSlug }
                                ElsendoKarto(
                                    elsendo = elsendo,
                                    kanalo = kanalo,
                                    montruTekston = montruTekston(elsendo, ludatojMapo[elsendo.id]),
                                    ludilo = ludilo,
                                    onLudi = onLudi,
                                    onElshuti = { onElshuti(elsendo) },
                                    onAldoniAlVico = { onAldoniAlVico(elsendo) },
                                    onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) },
                                )
                            }
                        }
                    }
                }

                // "Kio popularas" — hazardaj elsendoj
                if (popularajElsendoj.isNotEmpty()) {
                    item { SekcioTitolo("Aliaj elsendoj") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularajElsendoj) { elsendo ->
                                val kanalo = kanaloj.find { it.slug == elsendo.kanaloSlug }
                                ElsendoKarto(
                                    elsendo = elsendo,
                                    kanalo = kanalo,
                                    montruTekston = montruTekston(elsendo, ludatojMapo[elsendo.id]),
                                    ludilo = ludilo,
                                    onLudi = onLudi,
                                    onElshuti = { onElshuti(elsendo) },
                                    onAldoniAlVico = { onAldoniAlVico(elsendo) },
                                    onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) },
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SekcioTitolo(titolo: String) {
    Text(
        text = titolo,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

/**
 * Dividilo inter la aktivaj kaj arkivaj kanaloj en la "Kanaloj"-vico:
 * vertikala linio kun la etikedo "Arkivo" dekstre de ĝi.
 */
@Composable
private fun ArkivoDividilo() {
    Row(
        modifier = Modifier.height(200.dp).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(0.75f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "Arkivo",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

/** Ludo/paŭzo-logiko por specifa elsendo. */
private fun ludiAuxPauxzigi(
    elsendo: Elsendo, ludas: Boolean, pauxzita: Boolean,
    ludilo: LudiloRegilo?, onLudi: (Elsendo) -> Unit,
) {
    when {
        ludas -> { logi("Klako", "paŭzigi — ${elsendo.id}"); ludilo?.pauxzigi() }
        pauxzita -> { logi("Klako", "daŭrigi — ${elsendo.id}"); ludilo?.ludi() }
        else -> { logi("Klako", "ludi — ${elsendo.id}"); onLudi(elsendo) }
    }
}

@Composable
private fun ElsendoKarto(
    elsendo: Elsendo,
    kanalo: Kanalo?,
    onClick: () -> Unit,
    montruTekston: String? = null,
    ludilo: LudiloRegilo? = null,
    onLudi: (Elsendo) -> Unit = {},
    onElshuti: () -> Unit = {},
    onAldoniAlVico: () -> Unit = {},
) {
    val ludantoStato by (ludilo?.stato?.collectAsState() ?: remember { mutableStateOf(LudantoInformo(stato = LudantoStato.Haltita)) })
    val kanaloNomo = kanalo?.nomo ?: elsendo.kanaloSlug
    val bildoUrl = elsendo.bildoUrl ?: kanalo?.emblemoUrl
    val (ludas, pauxzita) = elsendoLudas(elsendo.id, ludantoStato)
    var menuMontrata by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(150.dp).height(200.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                if (bildoUrl != null) {
                    AsyncImage(
                        model = bildoUrl,
                        contentDescription = "Bildeto de ${elsendo.titolo}",
                        modifier = Modifier.size(130.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(130.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                // Insigno: montruTekston (aĝo, ludata procento, ktp.)
                if (montruTekston != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        color = Color(0xFFFFC107),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = montruTekston,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                // Ludo-butono kaj tripunkta menuo, malsupre dekstre
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tripunkta menuo
                    Box {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable {
                                    logi("Klako", "menuo — ${elsendo.id}")
                                    menuMontrata = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menuo", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = menuMontrata,
                            onDismissRequest = { menuMontrata = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Elŝuti") },
                                onClick = {
                                    menuMontrata = false
                                    logi("Klako", "elŝuti — ${elsendo.id}")
                                    onElshuti()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Aldoni al ludvico") },
                                onClick = {
                                    menuMontrata = false
                                    logi("Klako", "aldoni al ludvico — ${elsendo.id}")
                                    onAldoniAlVico()
                                }
                            )
                        }
                    }
                    // Ludo/paŭzo-butono
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable {
                                logi("Klako", "ludo-butono — ${elsendo.id}")
                                ludiAuxPauxzigi(elsendo, ludas, pauxzita, ludilo, onLudi)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (ludas) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (ludas) "Paŭzigi" else "Ludi",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = kanaloNomo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = elsendo.titolo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "ElsendoKarto — ne ludata", showBackground = true, heightDp = 220, widthDp = 170)
@Composable
private fun ElsendoKartoPreviewNeLudata() {
    pTemo {
        ElsendoKarto(
            elsendo = pElsendo,
            kanalo = pKanaloj[1],
            montruTekston = "hodiaŭ",
            onClick = {},
        )
    }
}

@Preview(name = "ElsendoKarto — ludata", showBackground = true, heightDp = 220, widthDp = 170)
@Composable
private fun ElsendoKartoPreviewLudata() {
    val ludilo = PreviewLudiloRegilo(
        LudantoInformo(
            stato = LudantoStato.Ludas,
            nunaFonto = Sonfonto.ElsendoFonto(pElsendo),
            pozicioMs = 30000, dauroMs = 6916000, estasRekta = false,
        )
    )
    pTemo {
        ElsendoKarto(
            elsendo = pElsendo,
            kanalo = pKanaloj[1],
            montruTekston = "aŭdis 42%",
            ludilo = ludilo,
            onClick = {},
        )
    }
}

@Preview(name = "ElsendoKarto — paŭzita", showBackground = true, heightDp = 220, widthDp = 170)
@Composable
private fun ElsendoKartoPreviewPauxzita() {
    val ludilo = PreviewLudiloRegilo(
        LudantoInformo(
            stato = LudantoStato.Haltita,
            nunaFonto = Sonfonto.ElsendoFonto(pElsendo),
            pozicioMs = 30000, dauroMs = 6916000, estasRekta = false,
        )
    )
    pTemo {
        ElsendoKarto(
            elsendo = pElsendo,
            kanalo = pKanaloj[1],
            montruTekston = "aŭdis 42%",
            ludilo = ludilo,
            onClick = {},
        )
    }
}

/**
 * Averto-karto pri detektitaj problemoj — montrata supre de la ĉefekrano.
 * Klako kondukas rekte al la diagnoza ekrano.
 */
@Composable
private fun DiagnozoAvertoKarto(
    problemoj: List<DiagnozoRezulto>,
    onDiagnozo: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { logi("Klako", "diagnozo-karto (Hejmo)"); onDiagnozo() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${problemoj.size} problemo(j) detektitaj",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    problemoj.first().titolo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Preview(name = "HejmoEkrano", showBackground = true, heightDp = 600)
@Composable
fun HejmoEkranoPreview() {
    val hodiaŭ = Clock.System.todayIn(TimeZone.UTC).toString()
    val hieraŭ = (Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 1)).toString()
    val antaŭ3tagoj = (Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 3)).toString()
    val antaŭ200tagoj = (Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 200)).toString()
    val antaŭJaro = (Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 400)).toString()

    // Meznova kanalo (lasta elsendo antaŭ 200 tagoj) — aktiva, ĉar ene de unu jaro
    val meznovaKanalo = Kanalo(slug = "meznova", nomo = "Meznova Radio", podkastaRssUrl = "https://x.com/m.rss")
    // Arkiva kanalo (lasta elsendo pli aĝa ol unu jaro) por montri la "Arkivo"-dividilon
    val arkivaKanalo = Kanalo(slug = "antikva", nomo = "Antikva Radio", podkastaRssUrl = "https://x.com/a.rss")

    val previewElsendoj = listOf(
        pElsendo.copy(id = "kernpunkto:nova1", kanaloSlug = "kernpunkto", dato = hodiaŭ, titolo = "KP300 Nova elsendo hodiaŭ"),
        pElsendo.copy(id = "kernpunkto:nova2", kanaloSlug = "kernpunkto", dato = hieraŭ, titolo = "KP299 Hieraŭa elsendo"),
        pElsendo.copy(id = "varsoviavento:nova1", kanaloSlug = "varsoviavento", dato = antaŭ3tagoj, titolo = "VV150 Antaŭ tri tagoj"),
        pElsendo.copy(id = "meznova:meznova", kanaloSlug = "meznova", dato = antaŭ200tagoj, titolo = "MR50 Antaŭ 200 tagoj"),
        pElsendo.copy(id = "antikva:malnova", kanaloSlug = "antikva", dato = antaŭJaro, titolo = "AR100 Longa paŭzo"),
    )

    val vm = HejmoViewModel(pKanaloDeponejo(), pElsendoDeponejo()).also {
        it.plenigu(pKanaloj + meznovaKanalo + arkivaKanalo, previewElsendoj)
    }

    pTemo {
        HejmoEkrano(
            kanaloDeponejo = pKanaloDeponejo(),
            elsendoDeponejo = pElsendoDeponejo(),
            onLudi = {},
            onElshuti = {},
            onAldoniAlVico = {},
            viewModel = vm,
        )
    }
}
