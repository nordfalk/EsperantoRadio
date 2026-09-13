package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn

/**
 * Kalkulas kiom nova la elsendo estas, kiel homlegabla teksto.
 * Redonas null se la elsendo estas pli malnova ol 6 monatoj.
 *
 * Ekzemploj: "hodiaux", "1 tago", "3 tagoj", "1 semajno", "2 semajnoj", "1 monato", "4 monatoj"
 */
@OptIn(ExperimentalTime::class)
fun kalkuliNovectempon(
    dato: String,
    nunaDatumo: LocalDate = Clock.System.todayIn(TimeZone.UTC),
): String? {
    val parsita = runCatching { LocalDate.parse(dato) }.getOrNull() ?: return null
    val tagoj = maxOf(0, parsita.daysUntil(nunaDatumo))

    return when {
        tagoj == 0 -> "hodiaŭ"
        tagoj == 1 -> "hieraŭ"
        tagoj <= 14 -> "$tagoj tagoj"
        tagoj < 60 -> "${tagoj / 7} semajnoj"
        tagoj < 180 -> "${tagoj / 30} monatoj"
        else -> null
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
 * Stato por la hejmekrano. Dum starto ĝi ŝargas ĉiujn kanalojn kaj iliajn
 * RSS-fluojn, kolektas ĉiujn elsendojn, kaj disponigas:
 * - [novajElsendoj] — ĉiuj elsendoj ordigitaj laŭ dato (plej nova unue) por "Kio novas"
 * - [popularajElsendoj] — hazardaj elsendoj por "Kio popularas"
 * - [cxiujKanaloj] — unu karto po kanalo kun la plej nova elsendo
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

    /** Ĉiuj kanaloj — unu karto po kanalo kun la plej nova elsendo. */
    private val _cxiujKanaloj = MutableStateFlow<List<KanalKarto>>(emptyList())
    val cxiujKanaloj = _cxiujKanaloj.asStateFlow()

    private val _sxargxas = MutableStateFlow(false)
    val sxargxas = _sxargxas.asStateFlow()

    suspend fun sxargxi() {
        _sxargxas.value = true
        try {
            val kanaloj = kanaloDeponejo.getKanalojn()
            logi("HejmoViewModel", "Ŝargas elsendojn por ${kanaloj.size} kanaloj")

            // Ŝargi ĉiujn RSS-fluojn samtempe (po unu async per kanalo kun podkasta RSS)
            val ĉiujElsendoj = coroutineScope {
                kanaloj
                    .filter { it.havasPodkastojn }
                    .map { kanalo -> async { elsendoDeponejo.sxargxiElsendojnPorKanal(kanalo) } }
                    .awaitAll()
                    .flatten()
            }
            logi("HejmoViewModel", "Ŝargis ${ĉiujElsendoj.size} elsendojn entute")

            val nunaDatumo = Clock.System.todayIn(TimeZone.UTC)

            // "Kio novas" — nur pli novaj ol 6 monatoj, maks 7 per kanalo, maks 50 entute
            val novaj = ĉiujElsendoj
                .filter { kalkuliNovectempon(it.dato, nunaDatumo) != null }
                .groupBy { it.kanaloSlug }
                .flatMap { (_, grupo) -> grupo.sortedByDescending { it.dato }.take(7) }
                .sortedByDescending { it.dato }
                .take(50)
            _novajElsendoj.value = novaj
            logi("HejmoViewModel", "Kio novas: ${novaj.size} elsendoj (post filtrado)")

            // "Kio popularas" — hazardaj elsendoj (maksimume 20)
            _popularajElsendoj.value = ĉiujElsendoj.shuffled().take(20)

            // "Lastatempe ludata" — elsendoj kiuj estis luditaj, ordigitaj laŭ lasteLudita
            if (ludatojDeponejo != null) {
                val ludatoj = ludatojDeponejo.observiLudatojn().value
                val lastatempe = ludatoj.values
                    .filter { it.lasteLudita > 0 }
                    .sortedByDescending { it.lasteLudita }
                    .mapNotNull { ludato -> ĉiujElsendoj.find { it.id == ludato.elsendoId } }
                    .take(20)
                _lastatempeLudataj.value = lastatempe
                logi("HejmoViewModel", "Lastatempe ludata: ${lastatempe.size} elsendoj")
            }

            // "Ĉiuj kanaloj" — unu karto po kanalo kun la plej nova elsendo
            val cxiujKanalojKartoj = kanaloj
                .filter { it.havasPodkastojn }
                .map { kanalo ->
                    val plejNova = ĉiujElsendoj
                        .filter { it.kanaloSlug == kanalo.slug }
                        .sortedByDescending { it.dato }
                        .firstOrNull()
                    KanalKarto(kanalo, plejNova)
                }
                .filter { it.plejNovaElsendo != null }
            _cxiujKanaloj.value = cxiujKanalojKartoj
            logi("HejmoViewModel", "Ĉiuj kanaloj: ${cxiujKanalojKartoj.size} kanaloj")
        } catch (e: Exception) {
            loge("HejmoViewModel", "Malsukcesis ŝargi hejmon", e)
        } finally {
            _sxargxas.value = false
        }
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
    onElshuti: (Elsendo) -> Unit = {},
    onAldoniAlVico: (Elsendo) -> Unit = {},
    ludatojDeponejo: LudatojDeponejo? = null,
    ludilo: LudiloRegilo? = null,
) {
    val viewModel = remember { HejmoViewModel(kanaloDeponejo, elsendoDeponejo, ludatojDeponejo) }
    val kanaloj by viewModel.kanaloj.collectAsState()
    val novajElsendoj by viewModel.novajElsendoj.collectAsState()
    val popularajElsendoj by viewModel.popularajElsendoj.collectAsState()
    val lastatempeLudataj by viewModel.lastatempeLudataj.collectAsState()
    val cxiujKanaloj by viewModel.cxiujKanaloj.collectAsState()
    val sxargxas by viewModel.sxargxas.collectAsState()
    val scope = rememberCoroutineScope()

    val ludantoStato by (ludilo?.stato?.collectAsState() ?: remember { mutableStateOf(LudantoInformo(stato = LudantoStato.Haltita)) })
    val ludatojMapo by (ludatojDeponejo?.observiLudatojn()?.collectAsState() ?: remember { mutableStateOf(emptyMap()) })

    LaunchedEffect(Unit) {
        scope.launch { viewModel.sxargxi() }
    }

    /** Ludata teksto (se ludita) aŭ novectempo (se ne ludita). */
    fun montruTekston(elsendo: Elsendo, ludata: LudataElsendo?): String? =
        ludata?.let { ludataTeksto(it) } ?: kalkuliNovectempon(elsendo.dato)


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EsperantoRadio", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { logi("Klako", "elŝutoj-butono"); onElshutoj() }) { Text("⬇") }
                    TextButton(onClick = { logi("Klako", "alarmoj-butono"); onAlarmoj() }) { Text("⏰") }
                    TextButton(onClick = { logi("Klako", "agordoj-butono"); onAgordoj() }) { Text("⚙") }
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
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
                                    ludantoStato = ludantoStato,
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
                                    ludantoStato = ludantoStato,
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
                    item { SekcioTitolo("Kio popularas") }
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
                                    ludantoStato = ludantoStato,
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

                // "Ĉiuj kanaloj" — unu karto po kanalo kun la plej nova elsendo
                if (cxiujKanaloj.isNotEmpty()) {
                    item { SekcioTitolo("Ĉiuj kanaloj") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cxiujKanaloj) { karto ->
                                val elsendo = karto.plejNovaElsendo ?: return@items
                                val kanalo = karto.kanalo
                                ElsendoKarto(
                                    elsendo = elsendo,
                                    kanalo = kanalo,
                                    montruTekston = montruTekston(elsendo, ludatojMapo[elsendo.id]),
                                    ludantoStato = ludantoStato,
                                    ludilo = ludilo,
                                    onLudi = onLudi,
                                    onElshuti = { onElshuti(elsendo) },
                                    onAldoniAlVico = { onAldoniAlVico(elsendo) },
                                    onClick = { logi("Klako", "kanalo ${kanalo.slug}"); onKanalo(kanalo) },
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
    ludantoStato: LudantoInformo = LudantoInformo(stato = LudantoStato.Haltita),
    ludilo: LudiloRegilo? = null,
    onLudi: (Elsendo) -> Unit = {},
    onElshuti: () -> Unit = {},
    onAldoniAlVico: () -> Unit = {},
) {
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
                            Text("♪", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                // Insigno: montruTekston (novectempo, ludata procento, ktp.)
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
                            Text("⋮", color = Color.White, style = MaterialTheme.typography.bodyMedium)
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
                        Text(
                            text = if (ludas) "⏸" else "▶",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
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
