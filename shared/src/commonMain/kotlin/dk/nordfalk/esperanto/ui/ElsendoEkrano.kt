package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.logi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElsendoEkrano(
    elsendo: Elsendo,
    onReen: () -> Unit,
    onLudi: () -> Unit = {},
    onElshuti: () -> Unit = {},
    onForigiElshuton: () -> Unit = {},
    elshutDeponejo: ElshutDeponejo? = null,
) {
    val elshutStato by (elshutDeponejo?.observiElshutStaton(elsendo.id)?.collectAsState() ?: remember { mutableStateOf<ElshutStato>(ElshutStato.NeElshutita) })
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(elsendo.titolo, maxLines = 1) },
                navigationIcon = {
                    TextButton(onClick = { logi("Klako", "reen (ElsendoEkrano)"); onReen() }) { Text("← Reen") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bildo
            if (elsendo.bildUrl != null) {
                AsyncImage(
                    model = elsendo.bildUrl,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            // Titolo
            Text(
                text = elsendo.titolo,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))

            // Dato kaj dauro
            val dauro = elsendo.dauro
            val dauroTeksto = if (dauro != null && dauro > 0) {
                val hor = dauro / 3600
                val min = (dauro % 3600) / 60
                "${elsendo.dato} · ${hor}:${min.toString().padStart(2, '0')}"
            } else {
                elsendo.dato
            }
            Text(
                text = dauroTeksto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            // Priskribo
            if (!elsendo.priskribo.isNullOrBlank()) {
                Text(
                    text = elsendo.priskribo!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))

            // Lud-butono
            Button(
                onClick = { logi("Klako", "aŭskulti — ${elsendo.id}"); onLudi() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("▶ Aŭskulti")
            }

            Spacer(Modifier.height(8.dp))

            // Elŝut-butono — statdependa
            when (elshutStato) {
                is ElshutStato.NeElshutita -> OutlinedButton(
                    onClick = { logi("Klako", "elŝuti — ${elsendo.id}"); onElshuti() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⬇ Elŝuti") }

                is ElshutStato.Elshutanta -> {
                    val p = (elshutStato as ElshutStato.Elshutanta).progreso
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("⏳ Elŝutas... ${((p * 100).toInt())}%") }
                }

                is ElshutStato.Preta -> OutlinedButton(
                    onClick = { logi("Klako", "forigi elŝuton — ${elsendo.id}"); onForigiElshuton() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✓ Elŝutita — Forigi") }

                is ElshutStato.Eraro -> OutlinedButton(
                    onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⚠ Eraro — reprovi") }

                is ElshutStato.Pauxzita -> OutlinedButton(
                    onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⏸ Paŭzita — reprovi") }
            }
        }
    }
}

@Preview(name = "ElsendoEkrano", showBackground = true, heightDp = 500)
@Composable
fun ElsendoEkranoPreview() {
    pTemo { ElsendoEkrano(elsendo = pElsendo, onReen = {}, onLudi = {}, onElshuti = {}) }
}
