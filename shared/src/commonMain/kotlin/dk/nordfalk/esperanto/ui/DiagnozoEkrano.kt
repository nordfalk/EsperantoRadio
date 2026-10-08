package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.nordfalk.esperanto.domain.DiagnozoRegilo
import dk.nordfalk.esperanto.domain.malfermiSistemAgordon
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.model.Severeco
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

/**
 * Diagnoza ekrano — kontrolas la aparaton por oftaj problemoj kiuj malhelpas
 * fona ludado kaj aŭtomata daŭrigo (bateri-optimumado, sciig-permeso, DNS, ktp.)
 * kaj proponas butonojn kiuj malfermas la ĝustajn sistem-agordojn.
 *
 * Alireble el Agordoj → Sistemo → Diagnozo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnozoEkrano(
    diagnozoRegilo: DiagnozoRegilo,
    onReen: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rezultoj by remember { mutableStateOf<List<DiagnozoRezulto>?>(null) }
    var ŝargxata by remember { mutableStateOf(true) }

    fun reKontroli() {
        scope.launch {
            ŝargxata = true
            rezultoj = diagnozoRegilo.kontroli()
            ŝargxata = false
        }
    }

    LaunchedEffect(Unit) { reKontroli() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnozo") },
                navigationIcon = {
                    IconButton(onClick = { logi("Klako", "reen (DiagnozoEkrano)"); onReen() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Reen")
                    }
                },
                actions = {
                    IconButton(onClick = { logi("Klako", "re-kontroli"); reKontroli() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Re-kontroli")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Tiu ekrano kontrolas ĉu via aparato estas bone agordita por fona ludado kaj aŭtomata daŭrigo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            when {
                ŝargxata && rezultoj == null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Kontrolas la aparaton…")
                    }
                }

                rezultoj != null && rezultoj!!.isEmpty() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Ĉio en ordo", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Neniuj problemoj trovitaj. Fona ludado kaj aŭtomata daŭrigo devus funkcii.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    Text(
                        "${rezultoj!!.size} problemo(j) trovitaj:",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    rezultoj!!.forEach { rezulto ->
                        DiagnozoEro(rezulto)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { logi("Klako", "re-kontroli (butono)"); reKontroli() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !ŝargxata,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Re-kontroli")
            }
        }
    }
}

@Composable
private fun DiagnozoEro(rezulto: DiagnozoRezulto) {
    val (ikono, koloro) = when (rezulto.severeco) {
        Severeco.ERARO -> Icons.Filled.Error to MaterialTheme.colorScheme.error
        Severeco.AVERTO -> Icons.Filled.Warning to MaterialTheme.colorScheme.tertiary
        Severeco.INFO -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ikono, contentDescription = null, tint = koloro)
                Spacer(Modifier.width(12.dp))
                Text(rezulto.titolo, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(rezulto.priskribo, style = MaterialTheme.typography.bodyMedium)
            rezulto.ago?.let { ago ->
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        logi("Klako", "malfermi sistem-agordon: ${ago.intenco}")
                        malfermiSistemAgordon(ago.intenco)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(ago.etikedo)
                }
            }
        }
    }
}
