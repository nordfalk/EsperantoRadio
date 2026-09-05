package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.AlarmoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmoEkrano(
    alarmoDeponejo: AlarmoDeponejo,
    kanalDeponejo: KanalDeponejo,
    onReen: () -> Unit,
) {
    val alarmoj by alarmoDeponejo.observiAlarmojn().collectAsState()
    val kanaloj by kanalDeponejo.observiKanalojn().collectAsState()
    var montriKreilon by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏰ Vekhorloĝo (${alarmoj.size})") },
                navigationIcon = { TextButton(onClick = { logi("Klako", "reen (AlarmoEkrano)"); onReen() }) { Text("← Reen") } },
                actions = {
                    TextButton(onClick = { logi("Klako", "nova alarmo"); montriKreilon = true }) { Text("+") }
                }
            )
        }
    ) { padding ->
        if (montriKreilon) {
            AlarmoKreilo(
                kanaloj = kanaloj,
                onKrei = { alarmo ->
                    logi("Klako", "konfirmu novan alarmon")
                    scope.launch { alarmoDeponejo.krei(alarmo) }
                    montriKreilon = false
                },
                onNuligi = { montriKreilon = false }
            )
        } else if (alarmoj.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Neniu alarmo. Premu + por krei.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(alarmoj, key = { it.id }) { alarmo ->
                    AlarmoEro(
                        alarmo = alarmo,
                        kanalNomo = kanaloj.find { it.slug == alarmo.kanalSlug }?.nomo ?: alarmo.kanalSlug,
                        onBaskuli = {
                            logi("Klako", "baskuligu alarmon ${alarmo.id}")
                            scope.launch { alarmoDeponejo.baskuliAktivon(alarmo.id) }
                        },
                        onForigi = {
                            logi("Klako", "forigu alarmon ${alarmo.id}")
                            scope.launch { alarmoDeponejo.forigi(alarmo.id) }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AlarmoEro(
    alarmo: Alarmo,
    kanalNomo: String,
    onBaskuli: () -> Unit,
    onForigi: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(alarmo.tempoTeksto, style = MaterialTheme.typography.headlineMedium) },
        supportingContent = {
            Column {
                Text(kanalNomo)
                Text(alarmo.ripetoTeksto, style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = alarmo.aktiva, onCheckedChange = { onBaskuli() })
                TextButton(onClick = onForigi) { Text("🗑") }
            }
        }
    )
}

@Composable
private fun AlarmoKreilo(
    kanaloj: List<Kanal>,
    onKrei: (Alarmo) -> Unit,
    onNuligi: () -> Unit,
) {
    var horo by remember { mutableStateOf(6) }
    var minuto by remember { mutableStateOf(0) }
    var elektitaKanalSlug by remember { mutableStateOf(kanaloj.firstOrNull()?.slug ?: "") }
    var ripeto by remember { mutableStateOf(0x7f) } // cxiutage

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nova alarmo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        // Tempo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tempo: ")
            OutlinedTextField(
                value = horo.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> horo = v.coerceIn(0, 23) } },
                modifier = Modifier.width(80.dp),
                singleLine = true
            )
            Text(":")
            OutlinedTextField(
                value = minuto.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> minuto = v.coerceIn(0, 59) } },
                modifier = Modifier.width(80.dp),
                singleLine = true
            )
        }
        Spacer(Modifier.height(16.dp))

        // Kanal
        Text("Kanalo:")
        kanaloj.forEach { kanal ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = elektitaKanalSlug == kanal.slug,
                    onClick = { elektitaKanalSlug = kanal.slug }
                )
                Text(kanal.nomo)
            }
        }
        Spacer(Modifier.height(16.dp))

        // Ripeto
        Text("Ripeto:")
        Row {
            val tagoj = listOf("Lu" to 0x01, "Ma" to 0x02, "Me" to 0x04, "Ja" to 0x08, "Ve" to 0x10, "Sa" to 0x20, "Di" to 0x40)
            tagoj.forEach { (nomo, bito) ->
                FilterChip(
                    selected = ripeto and bito != 0,
                    onClick = { ripeto = ripeto xor bito },
                    label = { Text(nomo) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // Butonoj
        Row {
            Button(onClick = {
                onKrei(Alarmo(
                    id = 0,
                    horo = horo,
                    minuto = minuto,
                    ripeto = ripeto,
                    kanalSlug = elektitaKanalSlug,
                    aktiva = true
                ))
            }) { Text("Krei") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onNuligi) { Text("Nuligi") }
        }
    }
}
