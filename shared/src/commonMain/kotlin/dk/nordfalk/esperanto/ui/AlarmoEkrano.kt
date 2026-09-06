package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.AlarmoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.data.repository.subtenasVekhorlogxn
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
    var redaktoModo by remember { mutableStateOf<Alarmo?>(null) } // null = listo, ne-null = redakti cxi tiun
    var kreiModo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏰ Vekhorloĝo (${alarmoj.size})") },
                navigationIcon = {
                    TextButton(onClick = {
                        logi("Klako", "reen (AlarmoEkrano)")
                        if (redaktoModo != null) redaktoModo = null
                        else if (kreiModo) kreiModo = false
                        else onReen()
                    }) { Text("← Reen") }
                },
                actions = {
                    if (redaktoModo == null && !kreiModo) {
                        TextButton(onClick = { logi("Klako", "nova alarmo"); kreiModo = true }) { Text("+") }
                    }
                }
            )
        }
    ) { padding ->
        when {
            redaktoModo != null -> {
                AlarmoRedaktilo(
                    kanaloj = kanaloj,
                    ekzistanta = redaktoModo!!,
                    onKonfirmi = { alarmo ->
                        logi("Klako", "konfirmu redaktadon de alarmo ${alarmo.id}")
                        scope.launch { alarmoDeponejo.ghisdatigi(alarmo) }
                        redaktoModo = null
                    },
                    onNuligi = { redaktoModo = null }
                )
            }
            kreiModo -> {
                AlarmoRedaktilo(
                    kanaloj = kanaloj,
                    ekzistanta = null,
                    onKonfirmi = { alarmo ->
                        logi("Klako", "konfirmu novan alarmon")
                        scope.launch { alarmoDeponejo.krei(alarmo) }
                        kreiModo = false
                    },
                    onNuligi = { kreiModo = false }
                )
            }
            alarmoj.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Neniu alarmo. Premu + por krei.")
                    if (!subtenasVekhorlogxn) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "ℹ Alarmoj funkcias nur sur Android-telefonoj.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (!subtenasVekhorlogxn) {
                        item {
                            Text(
                                "ℹ Alarmoj funkcias nur sur Android-telefonoj. Sur ĉi tiu platformo vi povas nur agordi ilin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    items(alarmoj) { alarmo ->
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
                            },
                            onRedakti = {
                                logi("Klako", "redakti alarmon ${alarmo.id}")
                                redaktoModo = alarmo
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
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
    onRedakti: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(alarmo.tempoTeksto, style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.clickable { onRedakti() })
        },
        supportingContent = {
            Column(modifier = Modifier.clickable { onRedakti() }) {
                Text(kanalNomo)
                if (!alarmo.etikedo.isNullOrBlank()) {
                    Text(alarmo.etikedo!!, style = MaterialTheme.typography.bodySmall)
                }
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

/**
 * Redaktilo por krei novan aü redakti ekzistantan alarmon.
 * Se ekzistanta != null, la kampoj estas antaüplenigitaj kaj la butono diras "Konservi".
 */
@Composable
private fun AlarmoRedaktilo(
    kanaloj: List<Kanal>,
    ekzistanta: Alarmo?,
    onKonfirmi: (Alarmo) -> Unit,
    onNuligi: () -> Unit,
) {
    var horo by remember { mutableStateOf(ekzistanta?.horo ?: 6) }
    var minuto by remember { mutableStateOf(ekzistanta?.minuto ?: 0) }
    var elektitaKanalSlug by remember { mutableStateOf(ekzistanta?.kanalSlug ?: kanaloj.firstOrNull()?.slug ?: "") }
    var ripeto by remember { mutableStateOf(ekzistanta?.ripeto ?: 0x7f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            if (ekzistanta != null) "Redakti alarmon" else "Nova alarmo",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))

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

        Text("Kanalo:")
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
        ) {
            items(kanaloj, key = { it.slug }) { kanal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = elektitaKanalSlug == kanal.slug,
                        onClick = { elektitaKanalSlug = kanal.slug }
                    )
                    Text(kanal.nomo)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

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

        Row {
            Button(onClick = {
                onKonfirmi(Alarmo(
                    id = ekzistanta?.id ?: 0,
                    horo = horo,
                    minuto = minuto,
                    ripeto = ripeto,
                    kanalSlug = elektitaKanalSlug,
                    aktiva = ekzistanta?.aktiva ?: true
                ))
            }) { Text(if (ekzistanta != null) "Konservi" else "Krei") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onNuligi) { Text("Nuligi") }
        }
    }
}
