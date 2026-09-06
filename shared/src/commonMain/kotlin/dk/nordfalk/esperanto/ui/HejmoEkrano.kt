package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
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
        tagoj == 1 -> "1 tago"
        tagoj <= 14 -> "$tagoj tagoj"
        tagoj < 60 -> "${tagoj / 7} semajnoj"
        tagoj < 180 -> "${tagoj / 30} monatoj"
        else -> null
    }
}

/**
 * Stato por la hejmekrano. Dum starto ĝi ŝargas ĉiujn kanalojn kaj iliajn
 * RSS-fluojn, kolektas ĉiujn elsendojn, kaj disponigas:
 * - [novajElsendoj] — ĉiuj elsendoj ordigitaj laŭ dato (plej nova unue) por "Kio novas"
 * - [popularajElsendoj] — hazardaj elsendoj por "Kio popularas"
 */
@OptIn(ExperimentalTime::class)
class HejmoViewModel(
    private val kanalDeponejo: KanalDeponejo,
    private val elsendoDeponejo: ElsendoDeponejo,
) {
    val kanaloj: StateFlow<List<Kanal>> = kanalDeponejo.observiKanalojn()

    private val _novajElsendoj = MutableStateFlow<List<Elsendo>>(emptyList())
    val novajElsendoj = _novajElsendoj.asStateFlow()

    private val _popularajElsendoj = MutableStateFlow<List<Elsendo>>(emptyList())
    val popularajElsendoj = _popularajElsendoj.asStateFlow()

    private val _sxargxas = MutableStateFlow(false)
    val sxargxas = _sxargxas.asStateFlow()

    suspend fun sxargxi() {
        _sxargxas.value = true
        try {
            val kanaloj = kanalDeponejo.getKanalojn()
            logi("HejmoViewModel", "Ŝargas elsendojn por ${kanaloj.size} kanaloj")

            // Ŝargi ĉiujn RSS-fluojn samtempe (po unu async per kanal kun podkasta RSS)
            val ĉiujElsendoj = coroutineScope {
                kanaloj
                    .filter { it.havasPodkastojn }
                    .map { kanal -> async { elsendoDeponejo.sxargxiElsendojnPorKanal(kanal) } }
                    .awaitAll()
                    .flatten()
            }
            logi("HejmoViewModel", "Ŝargis ${ĉiujElsendoj.size} elsendojn entute")

            val nunaDatumo = Clock.System.todayIn(TimeZone.UTC)

            // "Kio novas" — nur pli novaj ol 6 monatoj, maks 7 per kanal, maks 50 entute
            val novaj = ĉiujElsendoj
                .filter { kalkuliNovectempon(it.dato, nunaDatumo) != null }
                .groupBy { it.kanalSlug }
                .flatMap { (_, grupo) -> grupo.sortedByDescending { it.dato }.take(7) }
                .sortedByDescending { it.dato }
                .take(50)
            _novajElsendoj.value = novaj
            logi("HejmoViewModel", "Kio novas: ${novaj.size} elsendoj (post filtrado)")

            // "Kio popularas" — hazardaj elsendoj (maksimume 20)
            _popularajElsendoj.value = ĉiujElsendoj.shuffled().take(20)
        } catch (e: Exception) {
            loge("HejmoViewModel", "Malsukcesis ŝargi hejmon", e)
        } finally {
            _sxargxas.value = false
        }
    }
}

/**
 * Nova hejmekrano laux Figma-dizajno "Muzaiko — Antonia".
 *
 * Horizontalaj rulantaj sekcioj:
 * - "Kio novas" — novaj elsendoj de ĉiuj kanaloj (horizontala LazyRow de kartoj)
 * - "Lastatempe ludata" — laste luditaj (nur se ekzistas)
 * - "Kio popularas" — hazardaj elsendoj (horizontala LazyRow de kartoj)
 *
 * Malsupra naviga breto (NavigationBar) kun 4 langetoj.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HejmoEkrano(
    kanalDeponejo: KanalDeponejo,
    elsendoDeponejo: ElsendoDeponejo,
    onKanal: (Kanal) -> Unit = {},
    onElsendo: (Elsendo) -> Unit = {},
    onLudi: (Elsendo) -> Unit = {},
    onSercxo: () -> Unit = {},
    onPlejsatataj: () -> Unit = {},
    onKanalaro: () -> Unit = {},
    onAgordoj: () -> Unit = {},
    onElshutoj: () -> Unit = {},
    onAlarmoj: () -> Unit = {},
) {
    val viewModel = remember { HejmoViewModel(kanalDeponejo, elsendoDeponejo) }
    val kanaloj by viewModel.kanaloj.collectAsState()
    val novajElsendoj by viewModel.novajElsendoj.collectAsState()
    val popularajElsendoj by viewModel.popularajElsendoj.collectAsState()
    val sxargxas by viewModel.sxargxas.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { viewModel.sxargxi() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EsperantoRadio", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { logi("Klako", "agordoj-butono"); onAgordoj() }) { Text("⚙") }
                }
            )
        },
        bottomBar = {
            MalsupraNavigaBreto(
                onHejmo = {},
                onKanalaro = { logi("Nav", "→ KANALARO"); onKanalaro() },
                onPlejsatataj = { logi("Nav", "→ PLEJSATATAJ"); onPlejsatataj() },
                onSercxo = { logi("Nav", "→ SERCXO"); onSercxo() },
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
                                val kanal = kanaloj.find { it.slug == elsendo.kanalSlug }
                                ElsendoKarto(
                                    elsendo = elsendo,
                                    kanalNomo = kanal?.nomo ?: elsendo.kanalSlug,
                                    bildUrl = elsendo.bildUrl ?: kanal?.emblemoUrl,
                                    novectempo = kalkuliNovectempon(elsendo.dato),
                                    onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) }
                                )
                            }
                        }
                    }
                }

                // "Lastatempe ludata" — nur se ekzistas (estonte)
                // lastatempeLuditaj — malplena por nun

                // "Kio popularas" — hazardaj elsendoj
                if (popularajElsendoj.isNotEmpty()) {
                    item { SekcioTitolo("Kio popularas") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularajElsendoj) { elsendo ->
                                val kanal = kanaloj.find { it.slug == elsendo.kanalSlug }
                                ElsendoKarto(
                                    elsendo = elsendo,
                                    kanalNomo = kanal?.nomo ?: elsendo.kanalSlug,
                                    bildUrl = elsendo.bildUrl ?: kanal?.emblemoUrl,
                                    onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) }
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

@Composable
private fun ElsendoKarto(
    elsendo: Elsendo,
    kanalNomo: String,
    bildUrl: String?,
    onClick: () -> Unit,
    novectempo: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(150.dp).height(200.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                if (bildUrl != null) {
                    AsyncImage(
                        model = bildUrl,
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
                if (novectempo != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFFFC107),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = novectempo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = kanalNomo,
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

@Composable
private fun MalsupraNavigaBreto(
    onHejmo: () -> Unit,
    onKanalaro: () -> Unit,
    onPlejsatataj: () -> Unit,
    onSercxo: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = { logi("Klako", "hejmo-tab"); onHejmo() }, icon = { Text("🏠") }, label = { Text("Hejmo") })
        NavigationBarItem(selected = false, onClick = { logi("Klako", "kanalaro-tab"); onKanalaro() }, icon = { Text("🎵") }, label = { Text("Kanaloj") })
        NavigationBarItem(selected = false, onClick = { logi("Klako", "plejsatataj-tab"); onPlejsatataj() }, icon = { Text("★") }, label = { Text("Plej ŝatataj") })
        NavigationBarItem(selected = false, onClick = { logi("Klako", "sercxo-tab"); onSercxo() }, icon = { Text("🔍") }, label = { Text("Serĉi") })
    }
}
