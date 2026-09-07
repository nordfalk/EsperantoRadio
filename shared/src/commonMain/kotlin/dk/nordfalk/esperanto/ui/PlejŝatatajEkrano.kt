package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

/**
 * Ekrano por plej ŝatataj kanaloj.
 *
 * TODO (venonta PR): Sciigoj — kiam nova elsendo aperas el ŝatata kanalo,
 * la apo devas sendi sciigon. Ankaŭ aldonu klarigon en ĉi tiu ekrano:
 * "Vi ricevos sciigon kiam aperas nova elsendo el ŝatata kanalo."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlejŝatatajEkrano(
    plejŝatatajDeponejo: PlejŝatatajDeponejo,
    kanaloDeponejo: KanaloDeponejo,
    onKanalo: (Kanalo) -> Unit,
) {
    val plejŝatataj by plejŝatatajDeponejo.observiPlejŝatatajn().collectAsState()
    val ĉiujKanaloj by kanaloDeponejo.observiKanalojn().collectAsState()
    val plejKanaloj = ĉiujKanaloj.filter { it.slug in plejŝatataj }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plej ŝatataj") }
            )
        }
    ) { padding ->
        if (plejKanaloj.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Neniu plej ŝatata kanalo. Premu ★ sur kanalo por aldoni.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(plejKanaloj, key = { it.slug }) { kanalo ->
                    ListItem(
                        headlineContent = { Text(kanalo.nomo) },
                        supportingContent = { Text(if (kanalo.estasRekta) "Rekta elsendo" else "Podkasto") },
                        trailingContent = {
                            TextButton(onClick = {
                                logi("Klako", "★ forigas plejŝaton: ${kanalo.slug}")
                                scope.launch { plejŝatatajDeponejo.baskuliPlejŝaton(kanalo.slug) }
                            }) {
                                Text("★", style = MaterialTheme.typography.headlineSmall)
                            }
                        },
                        modifier = Modifier.clickable { logi("Klako", "plejŝatata kanalo ${kanalo.slug}"); onKanalo(kanalo) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Preview(name = "PlejŝatatajEkrano", showBackground = true, heightDp = 250)
@Composable
fun PlejŝatatajEkranoPreview() {
    pTemo { PlejŝatatajEkrano(plejŝatatajDeponejo = pPlejŝatatajDeponejo(), kanaloDeponejo = pKanaloDeponejo(), onKanalo = {}) }
}
