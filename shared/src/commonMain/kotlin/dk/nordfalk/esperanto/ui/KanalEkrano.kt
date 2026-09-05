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
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KanalViewModel(
    private val kanal: Kanal,
    private val elsendoDeponejo: ElsendoDeponejoImpl,
) {
    private val _elsendoj = MutableStateFlow<List<Elsendo>>(emptyList())
    val elsendoj = _elsendoj.asStateFlow()

    private val _sxargxas = MutableStateFlow(false)
    val sxargxas = _sxargxas.asStateFlow()

    suspend fun sxargxi() {
        _sxargxas.value = true
        try {
            val rezulto = elsendoDeponejo.sxargxiElsendojn(kanal)
            _elsendoj.value = rezulto
        } finally {
            _sxargxas.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanalEkrano(
    kanal: Kanal,
    elsendoDeponejo: ElsendoDeponejoImpl,
    onReen: () -> Unit,
    onElsendo: (Elsendo) -> Unit = {},
    onLudi: (Sonfonto) -> Unit = {},
) {
    val viewModel = remember(kanal.slug) { KanalViewModel(kanal, elsendoDeponejo) }
    val elsendoj by viewModel.elsendoj.collectAsState()
    val sxargxas by viewModel.sxargxas.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(kanal.slug) {
        scope.launch { viewModel.sxargxi() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kanal.nomo) },
                navigationIcon = {
                    TextButton(onClick = onReen) { Text("← Reen") }
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
                // Rekta elsendo-butono se la kanal havas livestream
                if (kanal.estasRekta) {
                    item {
                        ListItem(
                            headlineContent = { Text("Aŭskulti rekte") },
                            leadingContent = { Text("▶", style = MaterialTheme.typography.headlineMedium) },
                            modifier = Modifier.clickable { onLudi(Sonfonto.RektaKanalo(kanal)) }
                        )
                        HorizontalDivider()
                    }
                }

                // Grupigi la elsendojn laŭ dato
                val grupigitaj = elsendoj.groupBy { it.dato }
                for ((dato, grupo) in grupigitaj) {
                    item {
                        Text(
                            text = dato,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(grupo, key = { it.id }) { elsendo ->
                        ElsendoEro(elsendo = elsendo, onClick = { onElsendo(elsendo) })
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
        headlineContent = { Text(elsendo.titolo, maxLines = 2) },
        supportingContent = {
            val dauro = elsendo.dauro
            if (dauro != null && dauro > 0) {
                val min = dauro / 60
                val sek = dauro % 60
                Text("$min:${sek.toString().padStart(2, '0')}")
            }
        },
        leadingContent = {
            if (elsendo.bildUrl != null) {
                AsyncImage(
                    model = elsendo.bildUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
