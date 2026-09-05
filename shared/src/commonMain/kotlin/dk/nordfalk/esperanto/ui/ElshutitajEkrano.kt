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
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElshutitajEkrano(
    elshutDeponejo: ElshutDeponejo,
    onReen: () -> Unit,
    onLudi: (Sonfonto) -> Unit,
    onElsendo: (dk.nordfalk.esperanto.domain.model.Elsendo) -> Unit,
) {
    val elshutoj by elshutDeponejo.observiElshutojn().collectAsState()
    val listo = elshutoj.values.toList()
    val scope = rememberCoroutineScope()

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
                    ElshutitaEro(
                        elshutita = elshutita,
                        onLudi = { onLudi(Sonfonto.LokaElsendo(elshutita.elsendo, elshutita.dosieroVojo)) },
                        onElsendo = { onElsendo(elshutita.elsendo) },
                        onForigi = {
                            logi("Klako", "forigi elŝuton ${elshutita.elsendo.id}")
                            scope.launch { elshutDeponejo.forigi(elshutita.elsendo.id) }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ElshutitaEro(
    elshutita: ElshutitaElsendo,
    onLudi: () -> Unit,
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

    ListItem(
        headlineContent = { Text(elsendo.titolo, maxLines = 2, modifier = Modifier.clickable { onElsendo() }) },
        supportingContent = { Text(statoTeksto) },
        trailingContent = {
            Row {
                if (elshutita.stato is ElshutStato.Preta) {
                    TextButton(onClick = { logi("Klako", "ludi elŝutitan — ${elsendo.id}"); onLudi() }) { Text("▶") }
                }
                TextButton(onClick = onForigi) { Text("🗑") }
            }
        }
    )
}
