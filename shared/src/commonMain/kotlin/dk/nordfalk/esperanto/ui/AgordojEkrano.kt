package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.ApoVersio
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.repository.subtenasSciigojn
import dk.nordfalk.esperanto.data.repository.sciigPermesoDonita
import dk.nordfalk.esperanto.data.repository.malfermuSciigAgordojn
import dk.nordfalk.esperanto.data.repository.generuPseŭdonimon
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgordojEkrano(
    agordojDeponejo: AgordojDeponejo,
    onReen: () -> Unit,
    onDiagnozo: () -> Unit = {},
    diagnozoProblemoj: StateFlow<List<DiagnozoRezulto>> = MutableStateFlow(emptyList()),
) {
    val temoNomo by agordojDeponejo.temo.collectAsState()
    val sciigoj by agordojDeponejo.sciigoj.collectAsState()
    val aŭtomataDaŭrigo by agordojDeponejo.aŭtomataDaŭrigo.collectAsState()
    val transsaltuEnkondukojn by agordojDeponejo.transsaltuEnkondukojn.collectAsState()
    val montruTranssaltanSciigon by agordojDeponejo.montruTranssaltanSciigon.collectAsState()
    val evoluo by agordojDeponejo.evoluo.collectAsState()
    val detalaProtokolado by agordojDeponejo.detalaProtokolado.collectAsState()
    val erarRaportado by agordojDeponejo.erarRaportado.collectAsState()
    val pseŭdonimo by agordojDeponejo.pseŭdonimo.collectAsState()
    val problemoj by diagnozoProblemoj.collectAsState()
    var montriPseŭdonimanDialogon by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agordoj") },
                navigationIcon = { IconButton(onClick = { logi("Klako", "reen (AgordojEkrano)"); onReen() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Reen") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Ludado", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ŜaltilaLinio(
                titolo = "Aŭtomate daŭrigu kun alia elsendo",
                priskribo = "Kiam elsendo finiĝas, aŭtomate ludu la sekvan",
                ŝaltita = aŭtomataDaŭrigo,
                onŜanĝo = { logi("Klako", "aŭtomataDaŭrigo → $it"); agordojDeponejo.fiksiAŭtomatanDaŭrigon(it) }
            )
            ŜaltilaLinio(
                titolo = "Transsalti enkondukon",
                priskribo = "Kanaloj kun ripetiĝanta enkonduko transsaltas ĝin, se vi aŭskultis la kanalon dum la pasinta semajno",
                ŝaltita = transsaltuEnkondukojn,
                onŜanĝo = { logi("Klako", "transsaltuEnkondukojn → $it"); agordojDeponejo.fiksiTranssaltuEnkondukojn(it) }
            )
            Spacer(Modifier.height(24.dp))
            Text("Sistemo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Diagnozo") },
                supportingContent = {
                    Text(
                        if (problemoj.isEmpty()) "Via aparato estas bone agordita por fona ludado kaj aŭtomata daŭrigo"
                        else "${problemoj.size} problemo(j) detektitaj — tuŝu por vidi la detalojn"
                    )
                },
                leadingContent = {
                    if (problemoj.isNotEmpty())
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    else
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, // verda — ĉio estas en ordo
                        )
                },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable { logi("Klako", "Diagnozo"); onDiagnozo() }
            )


            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Sciigoj — nur sur platformoj kiuj subtenas ĝin (Android)
            if (subtenasSciigojn) {
                Spacer(Modifier.height(24.dp))
                Text("Sciigoj", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ŜaltilaLinio(
                    titolo = "Ricevi sciigojn",
                    priskribo = "Sciigo kiam aperas nova elsendo el ŝatata kanalo",
                    ŝaltita = sciigoj,
                    onŜanĝo = { logi("Klako", "sciigoj → $it"); agordojDeponejo.fiksiSciigojn(it) }
                )
                // Se la uzanto enŝaltis sciigojn sed la permeso mankas — montru ligilon
                if (sciigoj && !sciigPermesoDonita()) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        logi("Klako", "Malfermas sistemajn sciigagordojn")
                        malfermuSciigAgordojn()
                    }) {
                        Text("Vi devas doni permeson al la apo montri sciigojn")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(Modifier.height(24.dp))
            Text("Programisto", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ŜaltilaLinio(
                titolo = "Programista reĝimo",
                priskribo = "Montru elektilojn por priskribo-fonto kaj vidmaniero en elsendo-ekrano",
                ŝaltita = evoluo,
                onŜanĝo = { logi("Klako", "programista reĝimo → $it"); agordojDeponejo.fiksiEvoluon(it) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(24.dp))
            Text("Privateco", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Pseŭdonimo") },
                supportingContent = { Text(pseŭdonimo) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable { logi("Klako", "Pseŭdonimo"); montriPseŭdonimanDialogon = true }
            )
            Spacer(Modifier.height(8.dp))
            ŜaltilaLinio(
                titolo = "Sendu erar-raportojn",
                priskribo = "Permesu al la apo sendi sennomajn erarojn kaj protokolojn al la erar-monitorado (Sentry) de la evoluigisto — tio helpas trovi kaj ripari problemojn. Ŝanĝoj de kraŝ-raportoj tute ekvalidas post restartigo de la apo",
                ŝaltita = erarRaportado,
                onŜanĝo = { logi("Klako", "erarRaportado → $it"); agordojDeponejo.fiksiErarRaportadon(it) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ŜaltilaLinio(
                titolo = "Ŝaltu detalan protokoladon",
                priskribo = "(Nur se erar-raportado estas ŝaltita) Protokolas anonimajn detalojn pri via uzado (navigado, klakoj, ŝargado). Malŝaltu por sendi nur erarojn kaj gravajn eventojn (malfermo, ekludo, vekiĝo)",
                ŝaltita = detalaProtokolado,
                onŜanĝo = { logi("Klako", "detalaProtokolado → $it"); agordojDeponejo.fiksiDetalanProtokoladon(it) }
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Versio de la apo — el ApoVersio, generata el `apoversio` en libs.versions.toml
            Spacer(Modifier.height(32.dp))
            Text(
                // Se konstruita el branĉo (ne master), la branĉnomo aperas post la versio
                "Versio ${ApoVersio.VERSION}${AppStato.branĉoNomo?.let { " ($it)" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            )
        }
    }

    if (montriPseŭdonimanDialogon) {
        var kampo by remember { mutableStateOf(pseŭdonimo) }
        AlertDialog(
            onDismissRequest = { montriPseŭdonimanDialogon = false },
            title = { Text("Pseŭdonimo") },
            text = {
                Column {
                    Text("Via unika nomo en la protokoloj de la apo (ekz. „blua suno“).")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = kampo,
                        onValueChange = { kampo = it },
                        label = { Text("Pseŭdonimo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { kampo = generuPseŭdonimon() }) {
                        Text("Generi novan hazardan nomon")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    agordojDeponejo.fiksiPseŭdonimon(kampo)
                    montriPseŭdonimanDialogon = false
                }) { Text("Bone") }
            },
            dismissButton = {
                TextButton(onClick = { montriPseŭdonimanDialogon = false }) { Text("Nuligi") }
            }
        )
    }
}

/**
 * Agorda linio kun ŝaltilo: la tuta linio estas klakebla kaj ŝaltas la agordon
 * (ne nur la ŝaltilo mem — ĝi estas neinteraga, do `onCheckedChange = null`).
 */
@Composable
private fun ŜaltilaLinio(
    titolo: String,
    priskribo: String,
    ŝaltita: Boolean,
    onŜanĝo: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(titolo) },
        supportingContent = { Text(priskribo) },
        trailingContent = { Switch(checked = ŝaltita, onCheckedChange = null) },
        modifier = Modifier.clickable { onŜanĝo(!ŝaltita) }
    )
}

@Preview(name = "AgordojEkrano", showBackground = true, heightDp = 400)
@Composable
fun AgordojEkranoPreview() {
    pTemo { AgordojEkrano(agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl(), onReen = {}) }
}
