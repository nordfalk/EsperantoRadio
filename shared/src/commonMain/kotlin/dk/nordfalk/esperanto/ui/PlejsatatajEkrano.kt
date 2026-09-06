package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.logi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlejsatatajEkrano(
    plejsatatajDeponejo: PlejsatatajDeponejo,
    kanalDeponejo: KanalDeponejo,
    onReen: () -> Unit,
    onKanal: (Kanal) -> Unit,
) {
    val plejsatataj by plejsatatajDeponejo.observiPlejsatatajn().collectAsState()
    val ĉiujKanaloj by kanalDeponejo.observiKanalojn().collectAsState()
    val plejKanaloj = ĉiujKanaloj.filter { it.slug in plejsatataj }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plej ŝatataj") },
                navigationIcon = { TextButton(onClick = { logi("Klako", "reen (PlejsatatajEkrano)"); onReen() }) { Text("← Reen") } }
            )
        }
    ) { padding ->
        if (plejKanaloj.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Neniu plej ŝatata kanalo. Premu ★ sur kanalo por aldoni.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(plejKanaloj, key = { it.slug }) { kanal ->
                    ListItem(
                        headlineContent = { Text(kanal.nomo) },
                        supportingContent = { Text(if (kanal.estasRekta) "Rekta elsendo" else "Podkasto") },
                        modifier = Modifier.clickable { logi("Klako", "plejŝatata kanal ${kanal.slug}"); onKanal(kanal) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Preview(name = "PlejsatatajEkrano", showBackground = true, heightDp = 250)
@Composable
fun PlejsatatajEkranoPreview() {
    pTemo { PlejsatatajEkrano(plejsatatajDeponejo = pPlejsatatajDeponejo(), kanalDeponejo = pKanalDeponejo(), onReen = {}, onKanal = {}) }
}
