package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.compose.material3.DrawerState
import dk.nordfalk.compose.material3.ModalNavigationDrawer
import dk.nordfalk.compose.material3.rememberDrawerState
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stato por la kanalaro-ekrano.
 */
class KanalaroViewModel(
    private val deponejo: dk.nordfalk.esperanto.domain.repository.KanaloDeponejo,
    private val elsendoDeponejo: ElsendoDeponejo? = null,
) {
    val kanaloj = deponejo.observiKanalojn()

    private val _ŝargas = MutableStateFlow(false)
    val ŝargas = _ŝargas.asStateFlow()

    /** Mapo: kanaloSlug → nombro da elsendoj */
    private val _elsendoKontoj = MutableStateFlow<Map<String, Int>>(emptyMap())
    val elsendoKontoj = _elsendoKontoj.asStateFlow()

    /** Mapo: kanaloSlug → plej nova elsendo (por ludi per la ludo-butono) */
    private val _lastajElsendoj = MutableStateFlow<Map<String, Elsendo>>(emptyMap())
    val lastajElsendoj = _lastajElsendoj.asStateFlow()

    /**
     * Fona skopo en la ViewModel — transvivas navigadon (KMP-X/KMP-W:
     * rememberCoroutineScope nuligus la ŝargadon ĉe ekranforiro).
     */
    private val skopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Dedupigo + TTL: re-eniro al la ekrano ne reŝargas ĵus ŝargitan datenon. */
    private val ŝargilo = ŜargaKontrolilo()

    /** @param fortoRefresigi true = ignoru la memoran kaŝmemoron kaj elŝutu ĉiujn fluojn denove (malsupren-tiro). */
    fun ŝargi(fortoRefresigi: Boolean = false) {
        skopo.launch { ŝargilo.rulu("KanalaroViewModel", fortoRefresigi) { ŝargiInterna(fortoRefresigi) } }
    }

    private suspend fun ŝargiInterna(fortoRefresigi: Boolean) {
        val kanaloj = deponejo.getKanalojn()

        // Paŝo 1: Legu diskan kaŝmemoron (rapida — loka dosier-I/O + re-parsado)
        if (elsendoDeponejo != null) {
            val kaŝitaj = (elsendoDeponejo as? ElsendoDeponejoImpl)?.leguĈiujnKaŝitajnElsendojn(kanaloj) ?: emptyList()
            if (kaŝitaj.isNotEmpty()) {
                val kontoj = mutableMapOf<String, Int>()
                val lastaj = mutableMapOf<String, Elsendo>()
                for (kanalo in kanaloj.filter { it.havasPodkastojn }) {
                    val els = kaŝitaj.filter { it.kanaloSlug == kanalo.slug }
                    kontoj[kanalo.slug] = els.size
                    els.maxByOrNull { it.dato }?.let { lastaj[kanalo.slug] = it }
                }
                _elsendoKontoj.value = kontoj
                _lastajElsendoj.value = lastaj
                logi("KanalaroViewModel", "Diska kaŝmemoro: ${kontoj.values.sum()} elsendoj por ${kontoj.size} kanaloj")
            }
        }

        // Paŝo 2: Reto-elŝuto (malrapida)
        _ŝargas.value = true
        try {
            if (elsendoDeponejo != null) {
                val rezultoj = coroutineScope {
                    kanaloj
                        .filter { it.havasPodkastojn }
                        .map { kanalo -> async { kanalo to elsendoDeponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi) } }
                        .awaitAll()
                }
                val kontoj = mutableMapOf<String, Int>()
                val lastaj = mutableMapOf<String, Elsendo>()
                for ((kanalo, elsendoj) in rezultoj) {
                    kontoj[kanalo.slug] = elsendoj.size
                    elsendoj.maxByOrNull { it.dato }?.let { lastaj[kanalo.slug] = it }
                }
                _elsendoKontoj.value = kontoj
                _lastajElsendoj.value = lastaj
                logi("KanalaroViewModel", "Ŝargis ${kontoj.values.sum()} elsendojn por ${kontoj.size} kanaloj")
            }
        } catch (e: Exception) {
            loge("KanalaroViewModel", "Malsukcesis ŝargi kanalojn", e)
        } finally {
            _ŝargas.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanalaroEkrano(
    viewModel: KanalaroViewModel,
    onKanalo: (Kanalo) -> Unit = {},
    tirkestStato: DrawerState = rememberDrawerState(DrawerValue.Closed),
    tirkestEnhavo: @Composable () -> Unit = {},
    onLudi: (Sonfonto) -> Unit = {},
    onElŝutoj: () -> Unit = {},
    onAlarmoj: () -> Unit = {},
    onAgordoj: () -> Unit = {},
) {
    val kanaloj by viewModel.kanaloj.collectAsState()
    val ŝargas by viewModel.ŝargas.collectAsState()
    val elsendoKontoj by viewModel.elsendoKontoj.collectAsState()
    val lastajElsendoj by viewModel.lastajElsendoj.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.ŝargi()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { TirkestButono(tirkestStato) },
                title = { Text("Kanaloj") },
                actions = {
                    IconButton(onClick = { logi("Klako", "elŝutoj-butono"); onElŝutoj() }) { Icon(Icons.Filled.Download, contentDescription = "Elŝutoj") }
                    IconButton(onClick = { logi("Klako", "alarmoj-butono"); onAlarmoj() }) { Icon(Icons.Filled.Alarm, contentDescription = "Vekhorloĝo") }
                    IconButton(onClick = { logi("Klako", "agordoj-butono"); onAgordoj() }) { Icon(Icons.Filled.Settings, contentDescription = "Agordoj") }
                }
            )
        }
    ) { padding ->
        // La tirkesto vivas SUB la supra breto: la titolo kaj la hamburgero
        // restas videblaj kaj la tirkesto ne kovras ilin (kiel en la malnova apo)
        ModalNavigationDrawer(
            drawerState = tirkestStato,
            drawerContent = { tirkestEnhavo() },
            // La tirkesto kaj la ombreco vivas SUB la supra breto
            modifier = Modifier.padding(padding),
        ) {
        if (ŝargas && kanaloj.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else PullToRefreshBox(
            isRefreshing = ŝargas,
            onRefresh = { logi("Klako", "malsupren-tiro (Kanalaro)"); viewModel.ŝargi(fortoRefresigi = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(kanaloj, key = { it.slug }) { kanalo ->
                    val lastaElsendo = lastajElsendoj[kanalo.slug]
                    KanaloEro(
                        kanalo = kanalo,
                        elsendoKonto = elsendoKontoj[kanalo.slug],
                        onClick = { logi("Klako", "kanalo ${kanalo.slug}"); onKanalo(kanalo) },
                        onLudi = if (kanalo.estasRekta) {
                            { logi("Klako", "ludi rekte ${kanalo.slug}"); onLudi(Sonfonto.RektaKanalo(kanalo)) }
                        } else if (lastaElsendo != null) {
                            { logi("Klako", "ludi lastan elsendon de ${kanalo.slug}"); onLudi(Sonfonto.ElsendoFonto(lastaElsendo)) }
                        } else null
                    )
                }
            }
        }
        }
    }
}

@Composable
internal fun KanaloEro(
    kanalo: Kanalo,
    onClick: () -> Unit,
    onLudi: (() -> Unit)? = null,
    elsendoKonto: Int? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                kanalo.nomo,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val tipo = when {
                kanalo.estasRekta -> "Rekta elsendo"
                kanalo.havasPodkastojn -> "Podkasto"
                else -> "Neniu fluo"
            }
            val teksto = if (elsendoKonto != null && elsendoKonto > 0) {
                "$tipo · $elsendoKonto elsendoj"
            } else {
                tipo
            }
            Column {
                // Mallonga priskribo el la kanalkonfiguro (nedeviga)
                if (!kanalo.priskribo.isNullOrBlank()) {
                    Text(
                        kanalo.priskribo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    teksto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            if (kanalo.emblemoUrl != null) {
                AsyncImage(
                    model = kanalo.emblemoUrl,
                    contentDescription = "Emblemo de ${kanalo.nomo}",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            kanalo.nomo.take(2),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (onLudi != null) {
                Surface(
                    onClick = onLudi,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Ludi", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview(name = "Kanalaro", showBackground = true, heightDp = 250)
@Composable
fun KanalaroEkranoPreview() {
    pTemo { KanalaroEkrano(viewModel = KanalaroViewModel(pKanaloDeponejo())) }
}
