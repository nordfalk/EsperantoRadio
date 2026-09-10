package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ElsendoEkrano(
    elsendo: Elsendo,
    onReen: () -> Unit,
    onLudi: () -> Unit = {},
    onElshuti: () -> Unit = {},
    onHaltigiElshuton: () -> Unit = {},
    onForigiElshuton: () -> Unit = {},
    onKanalo: (Kanalo) -> Unit = {},
    onAldoniAlVico: () -> Unit = {},
    onMontriLudvicon: () -> Unit = {},
    kanalo: Kanalo? = null,
    elshutDeponejo: ElshutDeponejo? = null,
    ludatojDeponejo: LudatojDeponejo? = null,
    ludilo: LudiloRegilo? = null,
) {
    val scope = rememberCoroutineScope()
    val snackbarStato = remember { SnackbarHostState() }

    val elshutStato by (elshutDeponejo?.observiElshutStaton(elsendo.id)?.collectAsState() ?: remember { mutableStateOf<ElshutStato>(ElshutStato.NeElshutita) })
    val ludatojMapo by (ludatojDeponejo?.observiLudatojn()?.collectAsState() ?: remember { mutableStateOf(emptyMap<String, LudataElsendo>()) })
    val ludato = ludatojMapo[elsendo.id]
    val savitaPozicio = ludato?.pozicioMs?.takeIf { it > 0 && !ludato.finita }

    val ludantoStato by (ludilo?.stato?.collectAsState() ?: remember { mutableStateOf(LudantoInformo(stato = LudantoStato.Haltita)) })
    val nunaFonto = ludantoStato.nunaFonto
    val tiuElsendoLudas = when (nunaFonto) {
        is Sonfonto.ElsendoFonto -> nunaFonto.elsendo.id == elsendo.id && ludantoStato.stato is LudantoStato.Ludas
        is Sonfonto.LokaElsendo -> nunaFonto.elsendo.id == elsendo.id && ludantoStato.stato is LudantoStato.Ludas
        else -> false
    }
    val tiuElsendoPauxzita = when (nunaFonto) {
        is Sonfonto.ElsendoFonto -> nunaFonto.elsendo.id == elsendo.id && ludantoStato.stato is LudantoStato.Haltita
        is Sonfonto.LokaElsendo -> nunaFonto.elsendo.id == elsendo.id && ludantoStato.stato is LudantoStato.Haltita
        else -> false
    }

    fun montruMesaĝon(teksto: String) {
        scope.launch { snackbarStato.showSnackbar(teksto, duration = SnackbarDuration.Short) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(elsendo.titolo, maxLines = 1) },
                navigationIcon = {
                    TextButton(onClick = { logi("Klako", "reen (ElsendoEkrano)"); onReen() }) { Text("← Reen") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarStato) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === Bildo kun ikonoj super ===
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (elsendo.bildoUrl != null) {
                    AsyncImage(
                        model = elsendo.bildoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("♪", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Ikonoj sur la bildo: Elŝuti maldekstre, Ludi/paŭzi meze, Aldoni al ludvico dekstre
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IkonoButono(
                        ikono = when (elshutStato) {
                            is ElshutStato.Preta -> "✓"
                            is ElshutStato.Elshutanta -> "■"
                            else -> "⬇"
                        },
                        label = when (elshutStato) {
                            is ElshutStato.Elshutanta -> "Haltigi elŝuton"
                            else -> "Elŝuti"
                        },
                        onClick = {
                            when (elshutStato) {
                                is ElshutStato.NeElshutita -> { logi("Klako", "elŝuti — ${elsendo.id}"); onElshuti(); montruMesaĝon("Elŝutanta...") }
                                is ElshutStato.Elshutanta -> { logi("Klako", "haltigi elŝuton — ${elsendo.id}"); onHaltigiElshuton(); montruMesaĝon("Elŝuto haltigita") }
                                is ElshutStato.Preta -> { logi("Klako", "forigi elŝuton — ${elsendo.id}"); onForigiElshuton(); montruMesaĝon("Elŝuto forigita") }
                                is ElshutStato.Eraro -> { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti(); montruMesaĝon("Reprovas elŝuti") }
                                is ElshutStato.Pauxzita -> { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti(); montruMesaĝon("Reprovas elŝuti") }
                                else -> {}
                            }
                        },
                        onLongClick = { montruMesaĝon("Elŝuti") }
                    )

                    IkonoButono(
                        ikono = if (tiuElsendoLudas) "⏸" else "▶",
                        label = if (tiuElsendoLudas) "Paŭzigi" else "Ludi",
                        onClick = {
                            when {
                                tiuElsendoLudas -> { logi("Klako", "paŭzigi — ${elsendo.id}"); ludilo?.pauxzigi(); montruMesaĝon("Paŭzigita") }
                                tiuElsendoPauxzita -> { logi("Klako", "daŭrigi — ${elsendo.id}"); ludilo?.ludi(); montruMesaĝon("Daŭriganta") }
                                else -> { logi("Klako", "aŭskulti — ${elsendo.id}"); onLudi(); montruMesaĝon("Ludanta: ${elsendo.titolo}") }
                            }
                        },
                        onLongClick = { montruMesaĝon(if (tiuElsendoLudas) "Paŭzigi" else "Ludi") }
                    )

                    IkonoButono(
                        ikono = "📋",
                        label = "Aldoni al ludvico",
                        onClick = { logi("Klako", "aldoni al ludvico — ${elsendo.id}"); onAldoniAlVico(); montruMesaĝon("Aldonita al ludvico") },
                        onLongClick = { montruMesaĝon("Aldoni al ludvico") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Titolo
            Text(
                text = elsendo.titolo,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))

            // Dato kaj dauro
            val dauro = elsendo.dauro
            val dauroTeksto = if (dauro != null && dauro > 0) {
                val hor = dauro / 3600
                val min = (dauro % 3600) / 60
                "${elsendo.dato} · ${hor}:${min.toString().padStart(2, '0')}"
            } else {
                elsendo.dato
            }
            Text(
                text = dauroTeksto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Klakebla kanalnomo
            if (kanalo != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { logi("Klako", "kanalo-ligilo → ${kanalo.slug}"); onKanalo(kanalo) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = kanalo.nomo,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Savita pozicio
            if (savitaPozicio != null && savitaPozicio > 5000) {
                val min = savitaPozicio / 60000
                val sek = (savitaPozicio % 60000) / 1000
                val pozicioTeksto = "${min}:${sek.toString().padStart(2, '0')}"
                val dauroMs = (elsendo.dauro ?: 0L) * 1000
                val progreso = if (dauroMs > 0) (savitaPozicio.toFloat() / dauroMs).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Daŭrigi de $pozicioTeksto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
            }

            // Priskribo
            if (!elsendo.priskribo.isNullOrBlank()) {
                Text(
                    text = elsendo.priskribo!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))

            // === Butonoj kun tekstoj en 2 kolumnoj (malsupre) ===
            // Vico 1: Ludi/paŭzi | Elŝuti
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    tiuElsendoLudas -> Button(
                        onClick = { logi("Klako", "paŭzigi — ${elsendo.id}"); ludilo?.pauxzigi(); montruMesaĝon("Paŭzigita") },
                        modifier = Modifier.weight(1f)
                    ) { Text("⏸ Paŭzigi") }

                    tiuElsendoPauxzita -> Button(
                        onClick = { logi("Klako", "daŭrigi — ${elsendo.id}"); ludilo?.ludi(); montruMesaĝon("Daŭriganta") },
                        modifier = Modifier.weight(1f)
                    ) { Text("▶ Daŭrigi") }

                    else -> Button(
                        onClick = { logi("Klako", "aŭskulti — ${elsendo.id}"); onLudi(); montruMesaĝon("Ludanta: ${elsendo.titolo}") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (savitaPozicio != null && savitaPozicio > 5000) "▶ Daŭrigi" else "▶ Aŭskulti")
                    }
                }

                when (elshutStato) {
                    is ElshutStato.NeElshutita -> OutlinedButton(
                        onClick = { logi("Klako", "elŝuti — ${elsendo.id}"); onElshuti(); montruMesaĝon("Elŝutanta...") },
                        modifier = Modifier.weight(1f)
                    ) { Text("⬇ Elŝuti") }

                    is ElshutStato.Elshutanta -> {
                        val p = (elshutStato as ElshutStato.Elshutanta).progreso
                        OutlinedButton(
                            onClick = { logi("Klako", "haltigi elŝuton — ${elsendo.id}"); onHaltigiElshuton(); montruMesaĝon("Elŝuto haltigita") },
                            modifier = Modifier.weight(1f)
                        ) { Text("■ ${((p * 100).toInt())}%") }
                    }

                    is ElshutStato.Preta -> OutlinedButton(
                        onClick = { logi("Klako", "forigi elŝuton — ${elsendo.id}"); onForigiElshuton(); montruMesaĝon("Elŝuto forigita") },
                        modifier = Modifier.weight(1f)
                    ) { Text("✓ Elŝutita — Forigi") }

                    is ElshutStato.Eraro -> OutlinedButton(
                        onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti(); montruMesaĝon("Reprovas elŝuti") },
                        modifier = Modifier.weight(1f)
                    ) { Text("⚠ Eraro — reprovi") }

                    is ElshutStato.Pauxzita -> OutlinedButton(
                        onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti(); montruMesaĝon("Reprovas elŝuti") },
                        modifier = Modifier.weight(1f)
                    ) { Text("⏸ Paŭzita — reprovi") }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Vico 2: Aldoni al ludvico | Komenti
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        logi("Klako", "aldoni al ludvico — ${elsendo.id}"); onAldoniAlVico()
                        scope.launch {
                            val rezulto = snackbarStato.showSnackbar(
                                message = "Aldonita al ludvico",
                                actionLabel = "Montri",
                                duration = SnackbarDuration.Short,
                            )
                            if (rezulto == SnackbarResult.ActionPerformed) {
                                onMontriLudvicon()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("📋 Aldoni al ludvico") }

                if (kanalo?.retposhto != null) {
                    OutlinedButton(
                        onClick = {
                            logi("Klako", "retpoŝto el elsendo ${elsendo.id}")
                            malfermuRetposhton(
                                retposhto = kanalo.retposhto,
                                temo = "Pri ${kanalo.nomo}",
                                teksto = "Mi aŭskultas la elsendon (${elsendo.titolo} — ${elsendo.dato}) kaj havas komenton",
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("✉ Komenti") }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IkonoButono(
    ikono: String,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ikono,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
        )
    }
}

@Preview(name = "ElsendoEkrano", showBackground = true, heightDp = 500)
@Composable
fun ElsendoEkranoPreview() {
    pTemo { ElsendoEkrano(elsendo = pElsendo, onReen = {}, onLudi = {}, onElshuti = {}) }
}
