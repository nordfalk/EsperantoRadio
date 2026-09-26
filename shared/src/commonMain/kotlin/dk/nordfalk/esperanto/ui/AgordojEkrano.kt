package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.ApoVersio
import dk.nordfalk.esperanto.data.repository.subtenasSciigojn
import dk.nordfalk.esperanto.data.repository.sciigPermesoDonita
import dk.nordfalk.esperanto.data.repository.malfermuSciigAgordojn
import dk.nordfalk.esperanto.logi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgordojEkrano(
    agordojDeponejo: AgordojDeponejo,
    onReen: () -> Unit,
) {
    val temoNomo by agordojDeponejo.temo.collectAsState()
    val sciigoj by agordojDeponejo.sciigoj.collectAsState()
    val auxtomataDaurigo by agordojDeponejo.auxtomataDaurigo.collectAsState()
    val evoluo by agordojDeponejo.evoluo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agordoj") },
                navigationIcon = { IconButton(onClick = { logi("Klako", "reen (AgordojEkrano)"); onReen() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Reen") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Ludado", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Aŭtomate daŭrigu kun alia elsendo") },
                supportingContent = { Text("Kiam elsendo finiĝas, aŭtomate ludu la sekvan") },
                trailingContent = {
                    Switch(
                        checked = auxtomataDaurigo,
                        onCheckedChange = { logi("Klako", "auxtomataDaurigo → $it"); agordojDeponejo.fiksiAuxtomatanDaurigon(it) }
                    )
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Sciigoj — nur sur platformoj kiuj subtenas ĝin (Android)
            if (subtenasSciigojn) {
                Spacer(Modifier.height(24.dp))
                Text("Sciigoj", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ListItem(
                    headlineContent = { Text("Ricevi sciigojn") },
                    supportingContent = { Text("Sciigo kiam aperas nova elsendo el ŝatata kanalo") },
                    trailingContent = {
                        Switch(
                            checked = sciigoj,
                            onCheckedChange = { logi("Klako", "sciigoj → $it"); agordojDeponejo.fiksiSciigojn(it) }
                        )
                    }
                )
                // Se la uzanto enŝaltis sciigojn sed la permeso mankas — montru ligilon
                if (sciigoj && !sciigPermesoDonita()) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        logi("Klako", "Malfermas sistemajn sciig-agordojn")
                        malfermuSciigAgordojn()
                    }) {
                        Text("Vi devas doni permeson al la apo montri sciigojn")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(Modifier.height(24.dp))
            Text("Evoluo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Evolua reĝimo") },
                supportingContent = { Text("Montru elektilojn por priskribo-fonto kaj vidmaniero en elsendo-ekrano") },
                trailingContent = {
                    Switch(
                        checked = evoluo,
                        onCheckedChange = { logi("Klako", "evoluo → $it"); agordojDeponejo.fiksiEvoluon(it) }
                    )
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(24.dp))
            Text("Temo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TemoNomo.entries.forEach { temo ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(
                        selected = temoNomo == temo.name,
                        onClick = { logi("Klako", "temo → ${temo.name}"); agordojDeponejo.fiksiTemon(temo.name) }
                    )
                    Text(temo.etikedo)
                }
            }

            // Versio de la apo — el ApoVersio, generata el `apoversio` en libs.versions.toml
            Spacer(Modifier.height(32.dp))
            Text(
                "Versio ${ApoVersio.VERSION}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(name = "AgordojEkrano", showBackground = true, heightDp = 400)
@Composable
fun AgordojEkranoPreview() {
    pTemo { AgordojEkrano(agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl(), onReen = {}) }
}
