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
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stato por la kanalaro-ekrano.
 */
class KanalaroViewModel(
    private val deponejo: dk.nordfalk.esperanto.domain.repository.KanaloDeponejo,
) {
    val kanaloj = deponejo.observiKanalojn()

    private val _sxargxas = MutableStateFlow(false)
    val sxargxas = _sxargxas.asStateFlow()

    suspend fun sxargxi() {
        _sxargxas.value = true
        try {
            deponejo.getKanalojn()
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
                    KanaloEro(
                        kanalo = kanalo,
                        onClick = { logi("Klako", "kanalo ${kanalo.slug}"); onKanalo(kanalo) },
                        onLudi = if (kanalo.havasPodkastojn || kanalo.estasRekta) {
                            { logi("Klako", "ludi ${kanalo.slug}"); onLudi(Sonfonto.RektaKanalo(kanalo)) }
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
            Text(
                when {
                    kanalo.estasRekta -> "Rekta elsendo"
                    kanalo.havasPodkastojn -> "Podkasto"
                    else -> "Neniu fluo"
                },
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
