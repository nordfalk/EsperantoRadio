package dk.nordfalk.esperanto.ui

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
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.SerĉoDeponejo
import dk.nordfalk.esperanto.domain.repository.SerĉoRezulto
import dk.nordfalk.compose.material3.DrawerState
import dk.nordfalk.compose.material3.ModalNavigationDrawer
import dk.nordfalk.compose.material3.rememberDrawerState
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerĉoEkrano(
    serĉoDeponejo: SerĉoDeponejo,
    onElsendo: (Elsendo) -> Unit,
    onKanalo: (Kanalo) -> Unit = {},
    tirkestStato: DrawerState = rememberDrawerState(DrawerValue.Closed),
    tirkestEnhavo: @Composable () -> Unit = {},
) {
    // (PR #93) la serĉteksto pluvivas ekranturnon (rememberSaveable — la rezultoj
    // estas DERIVITAJ statoj: post restarigo la serĉo aŭtomate re-ruliĝas malsupre)
    var teksto by rememberSaveable { mutableStateOf("") }
    var rezultoj by remember { mutableStateOf(SerĉoRezulto()) }
    var ŝargas by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // (PR #93) debounce + nuligebla tasko — antaŭe ĉiu klavpremo lanĉis novan
    // korutinon; ili konkuris kaj malrapida rezulto povis veni post pli rapida
    // (malĝusta ordo), kaj escepto en serĉi paneigus la korutinon.
    var serĉaJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(teksto) {
        serĉaJob?.cancel()
        if (teksto.length < 2) {
            rezultoj = SerĉoRezulto()
            ŝargas = false
            return@LaunchedEffect
        }
        val job = scope.launch {
            delay(300) // debounce — atends pluajn klavpremojn antaŭ ol serĉi
            ŝargas = true
            try {
                val trovitaj = serĉoDeponejo.serĉi(teksto)
                // La tasko eble estas nuligita intertempe (nova teksto) — ne superskribu
                if (isActive) {
                    rezultoj = trovitaj
                    ŝargas = false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Throwable, ne Exception: regulo A8 + la Js-motoro ĵetas kotlin.Error
                logw("Serĉo", "Serĉado malsukcesis por '$teksto'", e)
                rezultoj = SerĉoRezulto()
                ŝargas = false
            }
        }
        serĉaJob = job
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
                label = { Text("Trovu kanalon aŭ elsendon…") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                singleLine = true
            )

            if (ŝargas) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (teksto.length >= 2 && rezultoj.estasVacia) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Nenion trovitan")
                }
            } else {
                // C60: la listoj jam estas ordigitaj laŭ graveco — kanaloj unue
                // (titolo-matĉoj antaŭ priskribo-matĉoj), poste elsendoj (same)
                LazyColumn {
                    val kanaloj = rezultoj.kanaloj
                    val elsendoj = rezultoj.elsendoj
                    if (kanaloj.isNotEmpty()) {
                        item { SekcioTitolo("Kanaloj") }
                        items(kanaloj, key = { "kanalo:${it.slug}" }) { kanalo ->
                            // La sama listero kiel en la kanalary-paĝo
                            KanaloEro(
                                kanalo = kanalo,
                                onClick = { logi("Klako", "serĉrezulto kanalo ${kanalo.slug}"); onKanalo(kanalo) }
                            )
                        }
                    }
                    if (elsendoj.isNotEmpty()) {
                        item { SekcioTitolo("Elsendoj") }
                        items(elsendoj, key = { "elsendo:${it.id}" }) { elsendo ->
                            // La sama listero kiel en la kanalo-paĝo
                            ElsendoEro(
                                elsendo = elsendo,
                                onClick = { logi("Klako", "serĉrezulto ${elsendo.id}"); onElsendo(elsendo) }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Preview(name = "SerĉoEkrano", showBackground = true, heightDp = 350)
@Composable
fun SerĉoEkranoPreview() {
    pTemo { SerĉoEkrano(serĉoDeponejo = pSerĉoDeponejo(), onElsendo = {}) }
}

/** Sekci-titolo en la rezult-listo (kiel en HejmoEkrano, sed sen klako). */
@Composable
private fun SekcioTitolo(titolo: String) {
    Text(
        titolo,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}
