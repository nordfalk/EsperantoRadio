package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ElsendoEkrano(
    elsendo: Elsendo,
    onReen: () -> Unit,
    onLudi: () -> Unit = {},
    onElŝuti: () -> Unit = {},
    onHaltigiElŝuton: () -> Unit = {},
    onForigiElŝuton: () -> Unit = {},
    onKanalo: (Kanalo) -> Unit = {},
    onAldoniAlVico: () -> Unit = {},
    onMontriLudvicon: () -> Unit = {},
    kanalo: Kanalo? = null,
    elŝutDeponejo: ElŝutDeponejo? = null,
    ludatojDeponejo: LudatojDeponejo? = null,
    ludilo: LudiloRegilo? = null,
    agordojDeponejo: AgordojDeponejo? = null,
    /**
     * Paŭzigi/daŭrigi tra [dk.nordfalk.esperanto.domain.player.LudvicoRegilo] (PR #92):
     * rekta `ludilo.paŭzigi()` ne nuligas planitan reprovon, kiu rekomencigus la ludadon.
     */
    onPaŭzigi: () -> Unit = { ludilo?.paŭzigi() },
    onDaŭrigi: () -> Unit = { ludilo?.ludi() },
) {
    val scope = rememberCoroutineScope()
    val snackbarStato = remember { SnackbarHostState() }

    val elŝutStato by (elŝutDeponejo?.observiElŝutStaton(elsendo.id)?.collectAsState() ?: remember { mutableStateOf<ElŝutStato>(ElŝutStato.NeElŝutita) })
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
    val tiuElsendoPaŭzita = when (nunaFonto) {
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
                    IconButton(onClick = { logi("Klako", "reen (ElsendoEkrano)"); onReen() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Reen") }
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
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
                        ikono = when (elŝutStato) {
                            is ElŝutStato.Preta -> Icons.Filled.Check
                            is ElŝutStato.Elŝutanta -> Icons.Filled.Stop
                            else -> Icons.Filled.Download
                        },
                        label = when (elŝutStato) {
                            is ElŝutStato.Elŝutanta -> "Haltigi elŝuton"
                            else -> "Elŝuti"
                        },
                        onClick = {
                            when (elŝutStato) {
                                is ElŝutStato.NeElŝutita -> { logi("Klako", "elŝuti — ${elsendo.id}"); onElŝuti(); montruMesaĝon("Elŝutanta…") }
                                is ElŝutStato.Elŝutanta -> { logi("Klako", "haltigi elŝuton — ${elsendo.id}"); onHaltigiElŝuton(); montruMesaĝon("Elŝuto haltigita") }
                                is ElŝutStato.Preta -> { logi("Klako", "forigi elŝuton — ${elsendo.id}"); onForigiElŝuton(); montruMesaĝon("Elŝuto forigita") }
                                is ElŝutStato.Eraro -> { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElŝuti(); montruMesaĝon("Reprovas elŝuti") }
                                is ElŝutStato.Paŭzita -> { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElŝuti(); montruMesaĝon("Reprovas elŝuti") }
                                else -> {}
                            }
                        },
                        onLongClick = { montruMesaĝon("Elŝuti") }
                    )

                    IkonoButono(
                        ikono = if (tiuElsendoLudas) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        label = if (tiuElsendoLudas) "Paŭzigi" else "Ludi",
                        onClick = {
                            when {
                                tiuElsendoLudas -> { logi("Klako", "paŭzigi — ${elsendo.id}"); onPaŭzigi(); montruMesaĝon("Paŭzigita") }
                                tiuElsendoPaŭzita -> { logi("Klako", "daŭrigi — ${elsendo.id}"); onDaŭrigi(); montruMesaĝon("Daŭriganta") }
                                else -> { logi("Klako", "aŭskulti — ${elsendo.id}"); onLudi(); montruMesaĝon("Ludanta: ${elsendo.titolo}") }
                            }
                        },
                        onLongClick = { montruMesaĝon(if (tiuElsendoLudas) "Paŭzigi" else "Ludi") }
                    )

                    IkonoButono(
                        ikono = Icons.Filled.QueueMusic,
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

            // Dato kaj daŭro
            val daŭro = elsendo.daŭro
            val daŭroTeksto = if (daŭro != null && daŭro > 0) {
                val hor = daŭro / 3600
                val min = (daŭro % 3600) / 60
                "${elsendo.dato} · ${hor}:${min.toString().padStart(2, '0')}"
            } else {
                elsendo.dato
            }
            Text(
                text = daŭroTeksto,
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
                val daŭroMs = (elsendo.daŭro ?: 0L) * 1000
                val progreso = if (daŭroMs > 0) (savitaPozicio.toFloat() / daŭroMs).coerceIn(0f, 1f) else 0f
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
            val evoluo = agordojDeponejo?.evoluo?.collectAsState()?.value ?: false
            val defaŭltaUzuWebView = kanalo?.uzuWebViewPorElsendo == true

            // Programista reĝimo: elektiloj por fonto kaj vidmaniero
            if (evoluo) {
                var fontoHtml by remember(elsendo.id) { mutableStateOf(true) }
                var vidmaniero by remember(elsendo.id) { mutableStateOf(if (defaŭltaUzuWebView) 0 else 1) }

                Surface(
                    tonalElevation = 2.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Programisto — priskribo-fonto", style = MaterialTheme.typography.labelMedium)
                        Row {
                            FilterChip(
                                selected = fontoHtml,
                                onClick = { fontoHtml = true },
                                label = { Text("priskriboHtml") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = !fontoHtml,
                                onClick = { fontoHtml = false },
                                label = { Text("priskribo") },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Programisto — vidmaniero", style = MaterialTheme.typography.labelMedium)
                        Row {
                            FilterChip(
                                selected = vidmaniero == 0,
                                onClick = { vidmaniero = 0 },
                                label = { Text("WebView") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = vidmaniero == 1,
                                onClick = { vidmaniero = 1 },
                                label = { Text("HtmlVido") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = vidmaniero == 2,
                                onClick = { vidmaniero = 2 },
                                label = { Text("Teksto (kruda)") },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                val evoluaHtml = if (fontoHtml) elsendo.priskriboHtml else elsendo.priskribo
                if (!evoluaHtml.isNullOrBlank()) {
                    when (vidmaniero) {
                        0 -> HtmlVido(html = evoluaHtml, uzuWebView = true, modifier = Modifier.fillMaxWidth())
                        1 -> HtmlVido(html = evoluaHtml, uzuWebView = false, modifier = Modifier.fillMaxWidth())
                        2 -> Text(text = evoluaHtml, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                val html = elsendo.priskriboHtml ?: elsendo.priskribo
                if (!html.isNullOrBlank()) {
                    HtmlVido(
                        html = html,
                        uzuWebView = defaŭltaUzuWebView,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // === Butonoj kun tekstoj en 2 kolumnoj (malsupre) ===
            // Vico 1: Ludi/paŭzi | Elŝuti
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    tiuElsendoLudas -> Button(
                        onClick = { logi("Klako", "paŭzigi — ${elsendo.id}"); onPaŭzigi(); montruMesaĝon("Paŭzigita") },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Filled.Pause, contentDescription = null); Text(" Paŭzigi") }

                    tiuElsendoPaŭzita -> Button(
                        onClick = { logi("Klako", "daŭrigi — ${elsendo.id}"); onDaŭrigi(); montruMesaĝon("Daŭriganta") },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Filled.PlayArrow, contentDescription = null); Text(" Daŭrigi") }

                    else -> Button(
                        onClick = { logi("Klako", "aŭskulti — ${elsendo.id}"); onLudi(); montruMesaĝon("Ludanta: ${elsendo.titolo}") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(if (savitaPozicio != null && savitaPozicio > 5000) " Daŭrigi" else " Aŭskulti")
                    }
                }

                when (elŝutStato) {
                    is ElŝutStato.NeElŝutita -> OutlinedButton(
                        onClick = { logi("Klako", "elŝuti — ${elsendo.id}"); onElŝuti(); montruMesaĝon("Elŝutanta…") },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Filled.Download, contentDescription = null); Text(" Elŝuti") }

                    is ElŝutStato.Elŝutanta -> {
                        val p = (elŝutStato as ElŝutStato.Elŝutanta).progreso
                        OutlinedButton(
                            onClick = { logi("Klako", "haltigi elŝuton — ${elsendo.id}"); onHaltigiElŝuton(); montruMesaĝon("Elŝuto haltigita") },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Filled.Stop, contentDescription = null); Text(" ${((p * 100).toInt())}%") }
                    }

                    is ElŝutStato.Preta -> OutlinedButton(
                        onClick = { logi("Klako", "forigi elŝuton — ${elsendo.id}"); onForigiElŝuton(); montruMesaĝon("Elŝuto forigita") },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Filled.Check, contentDescription = null); Text(" Elŝutita — Forigi") }

                    is ElŝutStato.Eraro -> OutlinedButton(
                        onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElŝuti(); montruMesaĝon("Reprovas elŝuti") },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Filled.Warning, contentDescription = null); Text(" Eraro — reprovi") }

                    is ElŝutStato.Paŭzita -> OutlinedButton(
                        onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElŝuti(); montruMesaĝon("Reprovas elŝuti") },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Filled.Pause, contentDescription = null); Text(" Paŭzita — reprovi") }
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
                ) { Icon(Icons.Filled.QueueMusic, contentDescription = null); Text(" Aldoni al ludvico") }

                if (kanalo?.retpoŝto != null) {
                    OutlinedButton(
                        onClick = {
                            logi("Klako", "retpoŝto el elsendo ${elsendo.id}")
                            malfermuRetpoŝton(
                                retpoŝto = kanalo.retpoŝto,
                                temo = "Pri ${kanalo.nomo}",
                                teksto = "Mi aŭskultas la elsendon (${elsendo.titolo} — ${elsendo.dato}) kaj havas komenton",
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Filled.Email, contentDescription = null); Text(" Komenti") }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Envolvas [ElsendoEkrano] en [HorizontalPager] por ebligi horizontalan svipon
 * inter elsendoj de la sama kanalo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElsendoEkranoKunSvipo(
    komencaElsendo: Elsendo,
    elsendoj: List<Elsendo>,
    onReen: () -> Unit,
    /**
     * Raportas la nune VIDATAN elsendon (sekvas svipojn) — uzata de la mini-ludilbreto
     * por proponi ĝin kiam nenio ludas.
     */
    onVidataElsendo: (Elsendo) -> Unit = {},
    onLudi: (Elsendo) -> Unit = {},
    onElŝuti: (Elsendo) -> Unit = {},
    onHaltigiElŝuton: (Elsendo) -> Unit = {},
    onForigiElŝuton: (Elsendo) -> Unit = {},
    onKanalo: (Kanalo) -> Unit = {},
    onAldoniAlVico: (Elsendo) -> Unit = {},
    onMontriLudvicon: () -> Unit = {},
    kanalo: Kanalo? = null,
    elŝutDeponejo: ElŝutDeponejo? = null,
    ludatojDeponejo: LudatojDeponejo? = null,
    ludilo: LudiloRegilo? = null,
    agordojDeponejo: AgordojDeponejo? = null,
    onPaŭzigi: () -> Unit = { ludilo?.paŭzigi() },
    onDaŭrigi: () -> Unit = { ludilo?.ludi() },
) {
    if (elsendoj.size <= 1) {
        // Ne eblas svipi — montru nur la unuan elsendon
        val e = elsendoj.firstOrNull() ?: komencaElsendo
        ElsendoEkrano(
            elsendo = e,
            onReen = onReen,
            onLudi = { onLudi(e) },
            onElŝuti = { onElŝuti(e) },
            onHaltigiElŝuton = { onHaltigiElŝuton(e) },
            onForigiElŝuton = { onForigiElŝuton(e) },
            onKanalo = onKanalo,
            onAldoniAlVico = { onAldoniAlVico(e) },
            onMontriLudvicon = onMontriLudvicon,
            kanalo = kanalo,
            elŝutDeponejo = elŝutDeponejo,
            ludatojDeponejo = ludatojDeponejo,
            ludilo = ludilo,
            agordojDeponejo = agordojDeponejo,
            onPaŭzigi = onPaŭzigi,
            onDaŭrigi = onDaŭrigi,
        )
        LaunchedEffect(e) { onVidataElsendo(e) }
        return
    }

    val komencaPaĝo = elsendoj.indexOfFirst { it.id == komencaElsendo.id }.let { if (it < 0) 0 else it }
    val paĝoStato = rememberPagerState(initialPage = komencaPaĝo) { elsendoj.size }

    // Sekvu svipojn: la vidata elsendo ŝanĝiĝas kiam paĝo ekvidiĝas (settledPage)
    LaunchedEffect(paĝoStato.settledPage, elsendoj) {
        elsendoj.getOrNull(paĝoStato.settledPage)?.let(onVidataElsendo)
    }

    HorizontalPager(state = paĝoStato) { paĝo ->
        val e = elsendoj[paĝo]
        ElsendoEkrano(
            elsendo = e,
            onReen = onReen,
            onLudi = { onLudi(e) },
            onElŝuti = { onElŝuti(e) },
            onHaltigiElŝuton = { onHaltigiElŝuton(e) },
            onForigiElŝuton = { onForigiElŝuton(e) },
            onKanalo = onKanalo,
            onAldoniAlVico = { onAldoniAlVico(e) },
            onMontriLudvicon = onMontriLudvicon,
            kanalo = kanalo,
            elŝutDeponejo = elŝutDeponejo,
            ludatojDeponejo = ludatojDeponejo,
            ludilo = ludilo,
            agordojDeponejo = agordojDeponejo,
            onPaŭzigi = onPaŭzigi,
            onDaŭrigi = onDaŭrigi,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IkonoButono(
    ikono: ImageVector,
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
        Icon(
            imageVector = ikono,
            contentDescription = label,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
        )
    }
}

@Preview(name = "ElsendoEkrano", showBackground = true, heightDp = 500)
@Composable
fun ElsendoEkranoPreview() {
    pTemo { ElsendoEkrano(elsendo = pElsendo, onReen = {}, onLudi = {}, onElŝuti = {}) }
}
