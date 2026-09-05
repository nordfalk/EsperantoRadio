package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stato por la kanalaro-ekrano.
 */
class KanalaroViewModel(
    private val deponejo: dk.nordfalk.esperanto.domain.repository.KanalDeponejo,
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
    onKanal: (Kanal) -> Unit = {},
    onSercxo: () -> Unit = {},
    onPlejsatataj: () -> Unit = {},
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
                title = { Text("EsperantoRadio") },
                actions = {
                    TextButton(onClick = { logi("Klako", "serĉo-butono"); onSercxo() }) { Text("🔍") }
                    TextButton(onClick = { logi("Klako", "plejŝatataj-butono"); onPlejsatataj() }) { Text("★") }
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
                items(kanaloj, key = { it.slug }) { kanal ->
                    KanalEro(kanal = kanal, onClick = { logi("Klako", "kanal ${kanal.slug}"); onKanal(kanal) })
                }
            }
        }
    }
}

@Composable
private fun KanalEro(
    kanal: Kanal,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(kanal.nomo) },
        supportingContent = {
            Text(
                when {
                    kanal.estasRekta -> "Rekta elsendo"
                    kanal.havasPodkastojn -> "Podkasto"
                    else -> "Neniu fluo"
                }
            )
        },
        leadingContent = {
            if (kanal.emblemoUrl != null) {
                AsyncImage(
                    model = kanal.emblemoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(kanal.nomo.take(2))
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
