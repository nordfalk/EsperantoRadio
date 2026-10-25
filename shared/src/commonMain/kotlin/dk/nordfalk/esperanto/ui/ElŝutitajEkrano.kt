package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElŝutitajEkrano(
    elŝutDeponejo: ElŝutDeponejo,
    ludilo: LudiloRegilo,
    onReen: () -> Unit,
    onLudi: (Sonfonto) -> Unit,
    onElsendo: (dk.nordfalk.esperanto.domain.model.Elsendo) -> Unit,
) {
    val elŝutoj by elŝutDeponejo.observiElŝutojn().collectAsState()
    val listo = elŝutoj.values.toList()
    val scope = rememberCoroutineScope()
    val ludantoInformo by ludilo.stato.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elŝutitaj (${listo.size})") },
                navigationIcon = { IconButton(onClick = { logi("Klako", "reen (ElŝutitajEkrano)"); onReen() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Reen") } }
            )
        }
    ) { padding ->
        if (listo.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Neniu elŝutita elsendo. Premu la elŝutbutonon sur elsendo por elŝuti.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(listo, key = { it.elsendo.id }) { elŝutita ->
                    val elsendoId = elŝutita.elsendo.id
                    val nunaFonto = ludantoInformo.nunaFonto
                    val ludiTiuĈi = when (nunaFonto) {
                        is Sonfonto.LokaElsendo -> nunaFonto.elsendo.id == elsendoId
                        is Sonfonto.ElsendoFonto -> nunaFonto.elsendo.id == elsendoId
                        else -> false
                    }
                    val ludas = ludiTiuĈi && ludantoInformo.stato is LudantoStato.Ludas

                    ElŝutitaEro(
                        elŝutita = elŝutita,
                        ludas = ludas,
                        onLudi = { onLudi(Sonfonto.LokaElsendo(elŝutita.elsendo, elŝutita.dosieroVojo)) },
                        onPaŭzigi = { scope.launch { ludilo.paŭzigi() } },
                        onElsendo = { onElsendo(elŝutita.elsendo) },
                        onForigi = {
                            logi("Klako", "forigi elŝuton ${elŝutita.elsendo.id}")
                            scope.launch { elŝutDeponejo.forigi(elŝutita.elsendo.id) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ElŝutitaEro(
    elŝutita: ElŝutitaElsendo,
    ludas: Boolean,
    onLudi: () -> Unit,
    onPaŭzigi: () -> Unit,
    onElsendo: () -> Unit,
    onForigi: () -> Unit,
) {
    val elsendo = elŝutita.elsendo
    val statoTeksto = when (elŝutita.stato) {
        is ElŝutStato.NeElŝutita -> "Ne elŝutita"
        is ElŝutStato.Elŝutanta -> "Elŝutas… ${((elŝutita.stato as ElŝutStato.Elŝutanta).progreso * 100).toInt()}%"
        is ElŝutStato.Preta -> "Preta"
        is ElŝutStato.Eraro -> "Eraro: ${(elŝutita.stato as ElŝutStato.Eraro).mesaĝo}"
        is ElŝutStato.Paŭzita -> "Paŭzita"
    }

    // Grandeco: dum elŝuto montru la fluantan grandon; se preta, la konservitan grandon
    val bitokoj = when (elŝutita.stato) {
        is ElŝutStato.Elŝutanta -> (elŝutita.stato as ElŝutStato.Elŝutanta).elŝutitajBitokoj
        else -> elŝutita.dosierGrando
    }
    val grandecoTeksto = formatiBitokojn(bitokoj)

    val daŭroTeksto = elsendo.daŭro?.let { formatiDaŭron(it) }

    ListItem(
        headlineContent = { Text(elsendo.titolo, maxLines = 2, modifier = Modifier.clickable { onElsendo() }) },
        supportingContent = { Text(statoTeksto) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (grandecoTeksto != null) {
                    Text(grandecoTeksto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (daŭroTeksto != null) {
                    Text(daŭroTeksto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    if (elŝutita.stato is ElŝutStato.Preta) {
                        IconButton(
                            onClick = {
                                if (ludas) {
                                    logi("Klako", "paŭzigi elŝutitan — ${elsendo.id}")
                                    onPaŭzigi()
                                } else {
                                    logi("Klako", "ludi elŝutitan — ${elsendo.id}")
                                    onLudi()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(if (ludas) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (ludas) "Paŭzigi" else "Ludi")
                        }
                    }
                    IconButton(onClick = onForigi, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Forigi")
                    }
                }
            }
        }
    )
}

/** Formatu bitokojn al homlegebla teksto (KB, MB, GB). */
private fun formatiBitokojn(bitokoj: Long): String? {
    if (bitokoj <= 0) return null
    val mb = bitokoj / (1024.0 * 1024.0)
    return when {
        mb >= 1024 -> "${(mb / 1024 * 10).toInt() / 10.0} GB"
        mb >= 1 -> "${(mb * 10).toInt() / 10.0} MB"
        bitokoj >= 1024 -> "${bitokoj / 1024} KB"
        else -> "$bitokoj B"
    }
}

/** Formatu daŭron en sekundoj al H:MM:SS aŭ M:SS. */
private fun formatiDaŭron(sekundoj: Long): String {
    val hor = sekundoj / 3600
    val min = (sekundoj % 3600) / 60
    val sek = sekundoj % 60
    return if (hor > 0) {
        "$hor:${min.toString().padStart(2, '0')}:${sek.toString().padStart(2, '0')}"
    } else {
        "$min:${sek.toString().padStart(2, '0')}"
    }
}

@Preview(name = "ElŝutitajEkrano", showBackground = true, heightDp = 250)
@Composable
fun ElŝutitajEkranoPreview() {
    pTemo { ElŝutitajEkrano(elŝutDeponejo = pElŝutDeponejo(), ludilo = PreviewLudiloRegilo(), onReen = {}, onLudi = {}, onElsendo = {}) }
}
