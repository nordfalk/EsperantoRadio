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
import androidx.compose.ui.layout.ContentScale
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(8.dp)
        ) {
            // Kanalinformoj: emblemo, nomo, retejo, retpoŝto
            item { KanalInformoj(kanalo) }

            // Ŝargado
            if (sxargxas && elsendoj.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Ŝarĝas elsendojn...", modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            } else if (elsendoj.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Neniu elsendo trovita")
                    }
                }
            } else {
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
private fun KanalInformoj(kanalo: Kanalo) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emblemo — tutpaĝa larĝo, adaptas aldon altecon
        if (kanalo.emblemoUrl != null) {
            AsyncImage(
                model = kanalo.emblemoUrl,
                contentDescription = "Emblemo de ${kanalo.nomo}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("♪", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(kanalo.nomo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Retejo kaj retpoŝto butonoj
        if (kanalo.retejoUrl != null || kanalo.retposhto != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (kanalo.retejoUrl != null) {
                    AssistChip(
                        onClick = { logi("Klako", "retejo ${kanalo.slug}"); malfermuLigon(kanalo.retejoUrl) },
                        label = { Text("Retejo") },
                        leadingIcon = { Text("🌐") }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (kanalo.retposhto != null) {
                    AssistChip(
                        onClick = {
                            logi("Klako", "retposhto ${kanalo.slug}")
                            malfermuRetposhton(
                                retposhto = kanalo.retposhto,
                                temo = "Pri ${kanalo.nomo}",
                                teksto = "Mi aŭskultas la elsendon kaj havas komenton",
                            )
                        },
                        label = { Text("Retpoŝto") },
                        leadingIcon = { Text("✉") }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
