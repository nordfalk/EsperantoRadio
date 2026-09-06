package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.SercxoDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SercxoEkrano(
    sercxoDeponejo: SercxoDeponejo,
    onReen: () -> Unit,
    onElsendo: (Elsendo) -> Unit,
) {
    var taxto by remember { mutableStateOf("") }
    val rezultoj = remember { mutableStateOf<List<Elsendo>>(emptyList()) }
    val sxargxas = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Serĉi") },
                navigationIcon = { TextButton(onClick = { logi("Klako", "reen (SercxoEkrano)"); onReen() }) { Text("← Reen") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = taxto,
                onValueChange = {
                    taxto = it
                    if (it.length >= 2) {
                        sxargxas.value = true
                        scope.launch {
                            rezultoj.value = sercxoDeponejo.sercxi(it)
                            sxargxas.value = false
                        }
                    } else {
                        rezultoj.value = emptyList()
                    }
                },
                label = { Text("Trovu elsendon...") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                singleLine = true
            )

            if (sxargxas.value) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(rezultoj.value, key = { it.id }) { elsendo ->
                        ListItem(
                            headlineContent = { Text(elsendo.titolo, maxLines = 2) },
                            supportingContent = { Text(elsendo.kanalSlug) },
                            modifier = Modifier.clickable { logi("Klako", "serĉrezulto ${elsendo.id}"); onElsendo(elsendo) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Preview(name = "SercxoEkrano", showBackground = true, heightDp = 350)
@Composable
fun SercxoEkranoPreview() {
    pTemo { SercxoEkrano(sercxoDeponejo = pSercxoDeponejo(), onReen = {}, onElsendo = {}) }
}
