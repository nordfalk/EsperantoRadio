package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.SercxoDeponejo
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SercxoEkrano(
    sercxoDeponejo: SercxoDeponejo,
    onElsendo: (Elsendo) -> Unit,
    tirkestStato: DrawerState = rememberDrawerState(DrawerValue.Closed),
    tirkestEnhavo: @Composable () -> Unit = {},
) {
    // (PR #93) la serĉteksto pluvivas ekranturnon (rememberSaveable — la rezultoj
    // estas DERIVITAJ statoj: post restarigo la serĉo aŭtomate re-ruliĝas malsupre)
    var teksto by rememberSaveable { mutableStateOf("") }
    var rezultoj by remember { mutableStateOf(emptyList<Elsendo>()) }
    var sxargxas by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // (PR #93) debounce + nuligebla tasko — antaŭe ĉiu klavpremo lanĉis novan
    // korutinon; ili konkuris kaj malrapida rezulto povis veni post pli rapida
    // (malĝusta ordo), kaj escepto en sercxi paneigus la korutinon.
    var sercxaJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(teksto) {
        sercxaJob?.cancel()
        if (teksto.length < 2) {
            rezultoj = emptyList()
            sxargxas = false
            return@LaunchedEffect
        }
        val job = scope.launch {
            delay(300) // debounce — atends pluajn klavpremojn antaŭ ol serĉi
            sxargxas = true
            try {
                val trovitaj = sercxoDeponejo.sercxi(teksto)
                // La tasko eble estas nuligita intertempe (nova teksto) — ne superskribu
                if (isActive) {
                    rezultoj = trovitaj
                    sxargxas = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Throwable, ne Exception: regulo 8 + la Js-motoro ĵetas kotlin.Error
                logw("Sercxo", "Serĉado malsukcesis por '$teksto'", e)
                rezultoj = emptyList()
                sxargxas = false
            }
        }
        sercxaJob = job
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { TirkestButono(tirkestStato) },
                title = { Text("Serĉi") }
            )
        }
    ) { padding ->
        // La tirkesto vivas SUB la supra breto: la titolo kaj la hamburgero
        // restas videblaj kaj la tirkesto ne kovras ilin (kiel en la malnova apo)
        ModalNavigationDrawer(
            drawerState = tirkestStato,
            drawerContent = { tirkestEnhavo() },
            // La tirkesto kaj la ombreco vivas SUB la supra breto
            modifier = Modifier.padding(padding),
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = teksto,
                onValueChange = { teksto = it },
                label = { Text("Trovu elsendon...") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                singleLine = true
            )

            if (sxargxas) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(rezultoj, key = { it.id }) { elsendo ->
                        ListItem(
                            headlineContent = { Text(elsendo.titolo, maxLines = 2) },
                            supportingContent = { Text(elsendo.kanaloSlug) },
                            modifier = Modifier.clickable { logi("Klako", "serĉrezulto ${elsendo.id}"); onElsendo(elsendo) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            }
        }
    }
}

@Preview(name = "SercxoEkrano", showBackground = true, heightDp = 350)
@Composable
fun SercxoEkranoPreview() {
    pTemo { SercxoEkrano(sercxoDeponejo = pSercxoDeponejo(), onElsendo = {}) }
}
