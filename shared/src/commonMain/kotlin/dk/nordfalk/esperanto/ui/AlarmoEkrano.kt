package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.AlarmoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.data.repository.ekzaktajAlarmojPermesataj
import dk.nordfalk.esperanto.data.repository.subtenasVekhorlogxn
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmoEkrano(
    alarmoDeponejo: AlarmoDeponejo,
    kanaloDeponejo: KanaloDeponejo,
    onReen: () -> Unit,
) {
    val alarmoj by alarmoDeponejo.observiAlarmojn().collectAsState()
    val kanaloj by kanaloDeponejo.observiKanalojn().collectAsState()
    var redaktoModo by remember { mutableStateOf<Alarmo?>(null) } // null = listo, ne-null = redakti cxi tiun
    var kreiModo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Alarm, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Vekhorloĝo (${alarmoj.size})")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        logi("Klako", "reen (AlarmoEkrano)")
                        if (redaktoModo != null) redaktoModo = null
                        else if (kreiModo) kreiModo = false
                        else onReen()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Reen") }
                },
                actions = {
                    if (redaktoModo == null && !kreiModo) {
                        IconButton(onClick = { logi("Klako", "nova alarmo"); kreiModo = true }) { Icon(Icons.Filled.Add, contentDescription = "Nova alarmo") }
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
                    scaffoldPadding = padding,
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
                    scaffoldPadding = padding,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Alarmoj funkcias nur sur Android-telefonoj.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (subtenasVekhorlogxn && alarmoj.any { it.aktiva }) {
                        item { EkzaktajAlarmojAverto() }
                    }
                    if (!subtenasVekhorlogxn) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Alarmoj funkcias nur sur Android-telefonoj. Sur ĉi tiu platformo vi povas nur agordi ilin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(alarmoj) { alarmo ->
                        AlarmoEro(
                            alarmo = alarmo,
                            kanaloNomo = kanaloj.find { it.slug == alarmo.kanaloSlug }?.nomo ?: alarmo.kanaloSlug,
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

/**
 * Informa karto kiam la apo ne rajtas skedi ekzaktajn alarmojn (Android 12+).
 * La permeso SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM estas intence ne deklarita
 * (por eviti la Google Play-deklaron). La alarmoj tamen ekigas, sed eble
 * ĝis 10 minutoj malfrue. Sur Android 11 kaj sube tio ne validas.
 */
@Composable
private fun EkzaktajAlarmojAverto() {
    if (ekzaktajAlarmojPermesataj()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Alarmoj povas malfrui ĝis 10 minutoj (Android 12+). La sono ĉiam ludas — por ekzakta vekiĝo, uzu la sisteman vekhorloĝon.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun AlarmoEro(
    alarmo: Alarmo,
    kanaloNomo: String,
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
            // La teksto estas generata el la konfiguro de la alarmo (kanalo + ripeto) —
            // liberaj etikedoj el la konfiguro ne plu montriĝas (ili ne kongruis).
            Column(modifier = Modifier.clickable { onRedakti() }) {
                Text(kanaloNomo)
                Text(alarmo.ripetoTeksto, style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = alarmo.aktiva, onCheckedChange = { onBaskuli() })
                IconButton(onClick = onForigi) { Icon(Icons.Filled.Delete, contentDescription = "Forigi") }
            }
        }
    )
}

/**
 * Redaktilo por krei novan aü redakti ekzistantan alarmon.
 * Se ekzistanta != null, la kampoj estas antaüplenigitaj kaj la butono diras "Konservi".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlarmoRedaktilo(
    kanaloj: List<Kanalo>,
    ekzistanta: Alarmo?,
    scaffoldPadding: PaddingValues,
    onKonfirmi: (Alarmo) -> Unit,
    onNuligi: () -> Unit,
) {
    var horo by rememberSaveable { mutableStateOf(ekzistanta?.horo ?: 6) }
    var minuto by rememberSaveable { mutableStateOf(ekzistanta?.minuto ?: 0) }
    var elektitaKanaloSlug by rememberSaveable { mutableStateOf(ekzistanta?.kanaloSlug ?: kanaloj.firstOrNull()?.slug ?: "") }
    var ripeto by rememberSaveable { mutableStateOf(ekzistanta?.ripeto ?: 0x7f) }
    var tempoDialogoMalfermita by rememberSaveable { mutableStateOf(false) }
    var kanaloDialogoMalfermita by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            if (ekzistanta != null) "Redakti alarmon" else "Nova alarmo",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "Tempo",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${horo.toString().padStart(2, '0')}:${minuto.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { tempoDialogoMalfermita = true }
                .padding(vertical = 4.dp)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            "Kanalo",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val elektitaKanaloNomo = kanaloj.find { it.slug == elektitaKanaloSlug }?.nomo ?: elektitaKanaloSlug
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { kanaloDialogoMalfermita = true }
                .padding(vertical = 8.dp)
        ) {
            Text(elektitaKanaloNomo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Elekti kanalon")
        }
        Spacer(Modifier.height(16.dp))

        Text("Ripeto:")
        FlowRow {
            // 3-literaj nomoj — la 7 ĉipoj eniras en unu linio
            val tagoj = listOf("Lun" to 0x01, "Mar" to 0x02, "Mer" to 0x04, "Ĵaŭ" to 0x08, "Ven" to 0x10, "Sab" to 0x20, "Dim" to 0x40)
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
                    kanaloSlug = elektitaKanaloSlug,
                    aktiva = ekzistanta?.aktiva ?: true,
                    // La redaktilo ne havas kampon por la etikedo — konservu la ekzistantan
                    etikedo = ekzistanta?.etikedo,
                ))
            }) { Text(if (ekzistanta != null) "Konservi" else "Krei") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onNuligi) { Text("Nuligi") }
        }
    }

    if (tempoDialogoMalfermita) {
        TempoElektiloDialogo(
            komencaHoro = horo,
            komencaMinuto = minuto,
            onKonfirmi = { h, m ->
                horo = h
                minuto = m
                tempoDialogoMalfermita = false
            },
            onNuligi = { tempoDialogoMalfermita = false }
        )
    }
    if (kanaloDialogoMalfermita) {
        KanalElektiloDialogo(
            kanaloj = kanaloj,
            elektitaSlug = elektitaKanaloSlug,
            onElekti = { slug ->
                elektitaKanaloSlug = slug
                kanaloDialogoMalfermita = false
            },
            onNuligi = { kanaloDialogoMalfermita = false }
        )
    }
}

/**
 * Dialogo kun la ciferblida tempo-elektilo (kun eblo ŝalti al klavara enigo).
 * La elekto transiĝas al la alkutima ago nur per "Bone".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TempoElektiloDialogo(
    komencaHoro: Int,
    komencaMinuto: Int,
    onKonfirmi: (horo: Int, minuto: Int) -> Unit,
    onNuligi: () -> Unit,
) {
    val tempoStato = rememberTimePickerState(
        initialHour = komencaHoro,
        initialMinute = komencaMinuto,
        is24Hour = true,
    )
    var klavaraEnigo by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onNuligi,
        title = { Text(if (klavaraEnigo) "Enigi tempon" else "Elektu tempon") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (klavaraEnigo) {
                    TimeInput(state = tempoStato)
                } else {
                    TimePicker(state = tempoStato)
                }
                TextButton(onClick = { klavaraEnigo = !klavaraEnigo }) {
                    Text(if (klavaraEnigo) "Elekti per ciferblido" else "Enigi klavare")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onKonfirmi(tempoStato.hour, tempoStato.minute) }) { Text("Bone") }
        },
        dismissButton = {
            TextButton(onClick = onNuligi) { Text("Nuligi") }
        },
    )
}

/** Dialogo kun la elektilo de kanalo por la alarmo. Elektita kanalo tuj fermas la dialogon. */
@Composable
private fun KanalElektiloDialogo(
    kanaloj: List<Kanalo>,
    elektitaSlug: String,
    onElekti: (slug: String) -> Unit,
    onNuligi: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNuligi,
        title = { Text("Elektu kanalon") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                kanaloj.forEach { kanalo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onElekti(kanalo.slug) }
                    ) {
                        RadioButton(
                            selected = kanalo.slug == elektitaSlug,
                            onClick = { onElekti(kanalo.slug) }
                        )
                        Text(kanalo.nomo, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onNuligi) { Text("Nuligi") } },
    )
}

@Preview(name = "AlarmoEkrano", showBackground = true, heightDp = 350)
@Composable
fun AlarmoEkranoPreview() {
    pTemo { AlarmoEkrano(alarmoDeponejo = pAlarmoDeponejo(), kanaloDeponejo = pKanaloDeponejo(), onReen = {}) }
}
