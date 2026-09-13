package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElshutitajEkrano(
    elshutDeponejo: ElshutDeponejo,
    ludilo: LudiloRegilo,
    onReen: () -> Unit,
    onLudi: (Sonfonto) -> Unit,
    onElsendo: (dk.nordfalk.esperanto.domain.model.Elsendo) -> Unit,
) {
    val elshutoj by elshutDeponejo.observiElshutojn().collectAsState()
    val listo = elshutoj.values.toList()
    val scope = rememberCoroutineScope()
    val ludantoInformo by ludilo.stato.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elŝutitaj (${listo.size})") },
                navigationIcon = { TextButton(onClick = { logi("Klako", "reen (ElshutitajEkrano)"); onReen() }) { Text("← Reen") } }
            )
        }
    ) { padding ->
        if (listo.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Neniu elŝutita elsendo. Premu ⬇ sur elsendo por elŝuti.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(listo, key = { it.elsendo.id }) { elshutita ->
                    val elsendoId = elshutita.elsendo.id
                    val nunaFonto = ludantoInformo.nunaFonto
                    val ludiTiuCxi = when (nunaFonto) {
                        is Sonfonto.LokaElsendo -> nunaFonto.elsendo.id == elsendoId
                        is Sonfonto.ElsendoFonto -> nunaFonto.elsendo.id == elsendoId
                        else -> false
                    }
                    val ludas = ludiTiuCxi && ludantoInformo.stato is LudantoStato.Ludas

                    ElshutitaEro(
                        elshutita = elshutita,
                        ludas = ludas,
                        onLudi = { onLudi(Sonfonto.LokaElsendo(elshutita.elsendo, elshutita.dosieroVojo)) },
                        onPauxzigi = { scope.launch { ludilo.pauxzigi() } },
                        onElsendo = { onElsendo(elshutita.elsendo) },
                        onForigi = {
                            logi("Klako", "forigi elŝuton ${elshutita.elsendo.id}")
                            scope.launch { elshutDeponejo.forigi(elshutita.elsendo.id) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ElshutitaEro(
    elshutita: ElshutitaElsendo,
    ludas: Boolean,
    onLudi: () -> Unit,
    onPauxzigi: () -> Unit,
    onElsendo: () -> Unit,
    onForigi: () -> Unit,
) {
    val elsendo = elshutita.elsendo
    val statoTeksto = when (elshutita.stato) {
        is ElshutStato.NeElshutita -> "Ne elŝutita"
        is ElshutStato.Elshutanta -> "Elŝutas... ${((elshutita.stato as ElshutStato.Elshutanta).progreso * 100).toInt()}%"
        is ElshutStato.Preta -> "Preta"
        is ElshutStato.Eraro -> "Eraro: ${(elshutita.stato as ElshutStato.Eraro).mesagho}"
        is ElshutStato.Pauxzita -> "Paŭzita"
    }

    // Grandeco: dum elŝuto montru la fluantan grandon; se preta, la konservitan grandon
    val bitokoj = when (elshutita.stato) {
        is ElshutStato.Elshutanta -> (elshutita.stato as ElshutStato.Elshutanta).elshutitajBitokoj
        else -> elshutita.dosierGrando
    }
    val grandecoTeksto = formatiBitokojn(bitokoj)

    val dauroTeksto = elsendo.dauro?.let { formatiDauron(it) }

    ListItem(
        headlineContent = { Text(elsendo.titolo, maxLines = 2, modifier = Modifier.clickable { onElsendo() }) },
        supportingContent = { Text(statoTeksto) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (grandecoTeksto != null) {
                    Text(grandecoTeksto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (dauroTeksto != null) {
                    Text(dauroTeksto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    if (elshutita.stato is ElshutStato.Preta) {
                        IconButton(
                            onClick = {
                                if (ludas) {
                                    logi("Klako", "paŭzigi elŝutitan — ${elsendo.id}")
                                    onPauxzigi()
                                } else {
                                    logi("Klako", "ludi elŝutitan — ${elsendo.id}")
                                    onLudi()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(if (ludas) "⏸" else "▶")
                        }
                    }
                    IconButton(onClick = onForigi, modifier = Modifier.size(36.dp)) {
                        Text("🗑")
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
private fun formatiDauron(sekundoj: Long): String {
    val hor = sekundoj / 3600
    val min = (sekundoj % 3600) / 60
    val sek = sekundoj % 60
    return if (hor > 0) {
        "$hor:${min.toString().padStart(2, '0')}:${sek.toString().padStart(2, '0')}"
    } else {
        "$min:${sek.toString().padStart(2, '0')}"
    }
}

@Preview(name = "ElshutitajEkrano", showBackground = true, heightDp = 250)
@Composable
fun ElshutitajEkranoPreview() {
    pTemo { ElshutitajEkrano(elshutDeponejo = pElshutDeponejo(), ludilo = PreviewLudiloRegilo(), onReen = {}, onLudi = {}, onElsendo = {}) }
}
