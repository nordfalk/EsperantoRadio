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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KanaloViewModel(
    private val kanalo: Kanalo,
    private val elsendoDeponejo: ElsendoDeponejoImpl,
) {
    private val _elsendoj = MutableStateFlow<List<Elsendo>>(emptyList())
    val elsendoj = _elsendoj.asStateFlow()

    private val _sxargxas = MutableStateFlow(false)
    val sxargxas = _sxargxas.asStateFlow()

    suspend fun sxargxi() {
        _sxargxas.value = true
        try {
            val rezulto = elsendoDeponejo.sxargxiElsendojn(kanalo)
            _elsendoj.value = rezulto
        } catch (e: Exception) {
            loge("KanaloViewModel", "Malsukcesis sargi elsendojn por ${kanalo.slug}", e)
        } finally {
            _sxargxas.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaloEkrano(
    kanalo: Kanalo,
    elsendoDeponejo: ElsendoDeponejoImpl,
    onReen: () -> Unit,
    onElsendo: (Elsendo) -> Unit = {},
    onLudi: (Sonfonto) -> Unit = {},
) {
    val viewModel = remember(kanalo.slug) { KanaloViewModel(kanalo, elsendoDeponejo) }
    val elsendoj by viewModel.elsendoj.collectAsState()
    val sxargxas by viewModel.sxargxas.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(kanalo.slug) {
        scope.launch { viewModel.sxargxi() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kanalo.nomo) },
                navigationIcon = {
                    TextButton(onClick = { logi("Klako", "reen (KanaloEkrano)"); onReen() }) { Text("← Reen") }
                }
            )
        }
    ) { padding ->
        if (sxargxas && elsendoj.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Ŝarĝas elsendojn...", modifier = Modifier.padding(8.dp))
                }
            }
        } else if (elsendoj.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Neniu elsendo trovita")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(8.dp)
            ) {
                // Rekta elsendo-butono se la kanalo havas livestream
                if (kanalo.estasRekta) {
                    item {
                        Surface(
                            onClick = { logi("Klako", "ludi rekte — ${kanalo.slug}"); onLudi(Sonfonto.RektaKanalo(kanalo)) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("▶", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Aŭskulti rekte", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // Grupigi la elsendojn laŭ dato
                val grupigitaj = elsendoj.groupBy { it.dato }
                for ((dato, grupo) in grupigitaj) {
                    item {
                        Text(
                            text = dato,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(grupo, key = { it.id }) { elsendo ->
                        ElsendoEro(elsendo = elsendo, onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ElsendoEro(
    elsendo: Elsendo,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                elsendo.titolo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            val dauro = elsendo.dauro
            if (dauro != null && dauro > 0) {
                val min = dauro / 60
                val sek = dauro % 60
                Text(
                    "$min:${sek.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            if (elsendo.bildoUrl != null) {
                AsyncImage(
                    model = elsendo.bildoUrl,
                    contentDescription = "Bildeto de ${elsendo.titolo}",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("♪", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview(name = "KanaloEkrano", showBackground = true, heightDp = 400)
@Composable
fun KanaloEkranoPreview() {
    pTemo {
        KanaloEkrano(
            kanalo = pKanaloj[1],
            elsendoDeponejo = PreviewElsendoDeponejo(listOf(pElsendo)),
            onReen = {},
        )
    }
}
