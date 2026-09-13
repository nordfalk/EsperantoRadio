package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
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

    private val _sxargxas = MutableStateFlow(false)
    val sxargxas = _sxargxas.asStateFlow()

    /** Mapo: kanaloSlug → nombro da elsendoj */
    private val _elsendoKontoj = MutableStateFlow<Map<String, Int>>(emptyMap())
    val elsendoKontoj = _elsendoKontoj.asStateFlow()

    /** Mapo: kanaloSlug → plej nova elsendo (por ludi per la ludo-butono) */
    private val _lastajElsendoj = MutableStateFlow<Map<String, Elsendo>>(emptyMap())
    val lastajElsendoj = _lastajElsendoj.asStateFlow()

    suspend fun sxargxi() {
        _sxargxas.value = true
        try {
            val kanaloj = deponejo.getKanalojn()
            // Ŝargi elsendojn por ĉiuj podkastaj kanaloj (se elsendoDeponejo haveblas)
            if (elsendoDeponejo != null) {
                val rezultoj = coroutineScope {
                    kanaloj
                        .filter { it.havasPodkastojn }
                        .map { kanalo -> async { kanalo to elsendoDeponejo.sxargxiElsendojnPorKanal(kanalo) } }
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
            loge("KanalaroViewModel", "Malsukcesis sargi kanalojn", e)
        } finally {
            _sxargxas.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanalaroEkrano(
    viewModel: KanalaroViewModel,
    onKanalo: (Kanalo) -> Unit = {},
    onLudi: (Sonfonto) -> Unit = {},
    onElshutoj: () -> Unit = {},
    onAlarmoj: () -> Unit = {},
    onAgordoj: () -> Unit = {},
) {
    val kanaloj by viewModel.kanaloj.collectAsState()
    val sxargxas by viewModel.sxargxas.collectAsState()
    val elsendoKontoj by viewModel.elsendoKontoj.collectAsState()
    val lastajElsendoj by viewModel.lastajElsendoj.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { viewModel.sxargxi() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanaloj") },
                actions = {
                    TextButton(onClick = { logi("Klako", "elŝutoj-butono"); onElshutoj() }) { Text("⬇") }
                    TextButton(onClick = { logi("Klako", "alarmoj-butono"); onAlarmoj() }) { Text("⏰") }
                    TextButton(onClick = { logi("Klako", "agordoj-butono"); onAgordoj() }) { Text("⚙") }
                }
            )
        }
    ) { padding ->
        if (sxargxas && kanaloj.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
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

@Composable
private fun KanaloEro(
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
            Text(
                teksto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        Text("▶", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
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
