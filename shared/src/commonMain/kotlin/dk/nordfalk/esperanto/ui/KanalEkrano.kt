package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KanaloViewModel(
    private val kanalo: Kanalo,
    private val elsendoDeponejo: ElsendoDeponejoImpl,
) {
    private val _elsendoj = MutableStateFlow<List<Elsendo>>(emptyList())
    val elsendoj = _elsendoj.asStateFlow()

    private val _ŝargas = MutableStateFlow(false)
    val ŝargas = _ŝargas.asStateFlow()

    /**
     * Fona skopo en la ViewModel — transvivas navigadon (KMP-X/KMP-W:
     * rememberCoroutineScope nuligus la ŝargadon ĉe ekranforiro).
     */
    private val skopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Dedupigo + TTL: re-eniro al la ekrano ne reŝargas ĵus ŝargitan datenon. */
    private val ŝargilo = ŜargaKontrolilo()

    /** @param fortoRefresigi true = ignoru la memoran kaŝmemoron kaj elŝutu la fluon denove (malsupren-tiro). */
    fun ŝargi(fortoRefresigi: Boolean = false) {
        skopo.launch { ŝargilo.rulu("KanaloViewModel", fortoRefresigi) { ŝargiInterna(fortoRefresigi) } }
    }

    private suspend fun ŝargiInterna(fortoRefresigi: Boolean) {
        // Paŝo 1: Legu diskan kaŝmemoron (rapida — loka dosier-I/O + re-parsado)
        val kaŝitaj = elsendoDeponejo.leguKaŝitajnElsendojn(kanalo)
        if (kaŝitaj != null && kaŝitaj.isNotEmpty()) {
            _elsendoj.value = kaŝitaj
            logi("KanaloViewModel", "${kanalo.slug}: diska kaŝmemoro — ${kaŝitaj.size} elsendoj")
        }

        // Paŝo 2: Reto-elŝuto (malrapida)
        _ŝargas.value = true
        try {
            val rezulto = elsendoDeponejo.ŝargiElsendojn(kanalo, fortoRefresigi)
            _elsendoj.value = rezulto
        } catch (e: Exception) {
            loge("KanaloViewModel", "Malsukcesis ŝargi elsendojn por ${kanalo.slug}", e)
        } finally {
            _ŝargas.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaloEkrano(
    kanalo: Kanalo,
    elsendoDeponejo: ElsendoDeponejoImpl,
    plejŝatatajDeponejo: PlejŝatatajDeponejo,
    agordojDeponejo: AgordojDeponejo,
    onReen: () -> Unit,
    onElsendo: (Elsendo) -> Unit = {},
    onLudi: (Sonfonto) -> Unit = {},
    onAgordoj: () -> Unit = {},
    ludilo: LudiloRegilo? = null,
) {
    val viewModel = remember(kanalo.slug) {
        AppStato.kanaloViewModelj.getOrPut(kanalo.slug) { KanaloViewModel(kanalo, elsendoDeponejo) }
    }
    val elsendoj by viewModel.elsendoj.collectAsState()
    val ŝargas by viewModel.ŝargas.collectAsState()
    val scope = rememberCoroutineScope()
    val plejŝatataj by plejŝatatajDeponejo.observiPlejŝatatajn().collectAsState()
    val sciigoj by agordojDeponejo.sciigoj.collectAsState()
    val estasPlejŝatata = kanalo.slug in plejŝatataj
    val snackbarStato = remember { SnackbarHostState() }

    LaunchedEffect(kanalo.slug) {
        viewModel.ŝargi()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kanalo.nomo) },
                navigationIcon = {
                    IconButton(onClick = { logi("Klako", "reen (KanaloEkrano)"); onReen() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Reen") }
                },
                actions = {
                    IconButton(onClick = {
                        logi("Klako", "baskulas plejŝaton: ${kanalo.slug}")
                        val antaŭa = estasPlejŝatata
                        scope.launch {
                            plejŝatatajDeponejo.baskuliPlejŝaton(kanalo.slug)
                            // Montru sciigon nur kiam oni ALDONAS ŝaton (ne kiam oni forigas)
                            if (!antaŭa && sciigoj) {
                                snackbarStato.showSnackbar(
                                    message = "Vi ricevos sciigon kiam aperas nova elsendo el ${kanalo.nomo}.",
                                    actionLabel = "Malŝalti en la agordoj",
                                    duration = SnackbarDuration.Long,
                                )?.let { rezulto ->
                                    if (rezulto == SnackbarResult.ActionPerformed) {
                                        onAgordoj()
                                        logi("Klako", "Navigas al Agordoj de Snackbar")
                                    }
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (estasPlejŝatata) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (estasPlejŝatata) "Forigi ŝaton" else "Aldoni ŝaton",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarStato) }
    ) { padding -> PullToRefreshBox(
        // La granda spinilo en la listo montras la unuan ŝarĝon; ĉi tiu nur kiam jam estas elsendoj
        isRefreshing = ŝargas && elsendoj.isNotEmpty(),
        onRefresh = { logi("Klako", "malsupren-tiro (${kanalo.slug})"); viewModel.ŝargi(fortoRefresigi = true) },
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            // Kanalinformoj: emblemo, nomo, retejo, retpoŝto
            item { KanalInformoj(kanalo, ludilo) }

            // Ŝargado
            if (ŝargas && elsendoj.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Ŝarĝas elsendojn…", modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            } else if (elsendoj.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Neniu elsendo trovita")
                    }
                }
            } else {
                // Rekta elsendo-butono se la kanalo havas tujan elsendfluon
                if (kanalo.estasRekta) {
                    item {
                        Surface(
                            onClick = { logi("Klako", "ludi rekte — ${kanalo.slug}"); onLudi(Sonfonto.RektaKanalo(kanalo)) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Ludi", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Aŭskulti rekte", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // Plata listo — la dato nun aperas en la ero mem (maldekstre de la daŭro)
                items(elsendoj, key = { it.id }) { elsendo ->
                    ElsendoEro(elsendo = elsendo, onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) })
                }
            }
        }
    }
    }
}

/**
 * Envolvas [KanalEkrano] en [HorizontalPager] por ebligi horizontalan svipon
 * inter kanaloj.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanalEkranoKunSvipo(
    komencaKanalo: Kanalo,
    kanaloj: List<Kanalo>,
    elsendoDeponejo: ElsendoDeponejoImpl,
    plejŝatatajDeponejo: PlejŝatatajDeponejo,
    agordojDeponejo: AgordojDeponejo,
    onReen: () -> Unit,
    /** Raportas la nune VIDATAN kanalon (sekvas svipojn) — por la mini-ludilbreto kiam nenio ludas. */
    onVidataKanalo: (Kanalo) -> Unit = {},
    onElsendo: (Elsendo) -> Unit = {},
    onLudi: (Sonfonto) -> Unit = {},
    onAgordoj: () -> Unit = {},
    ludilo: LudiloRegilo? = null,
) {
    if (kanaloj.size <= 1) {
        val k = kanaloj.firstOrNull() ?: komencaKanalo
        KanaloEkrano(
            kanalo = k,
            elsendoDeponejo = elsendoDeponejo,
            plejŝatatajDeponejo = plejŝatatajDeponejo,
            agordojDeponejo = agordojDeponejo,
            onReen = onReen,
            onElsendo = onElsendo,
            onLudi = onLudi,
            onAgordoj = onAgordoj,
            ludilo = ludilo,
        )
        LaunchedEffect(k) { onVidataKanalo(k) }
        return
    }

    val komencaPaĝo = kanaloj.indexOfFirst { it.slug == komencaKanalo.slug }.let { if (it < 0) 0 else it }
    val paĝoStato = rememberPagerState(initialPage = komencaPaĝo) { kanaloj.size }

    // Sekvu svipojn: la vidata kanalo ŝanĝiĝas kiam paĝo ekvidiĝas (settledPage)
    LaunchedEffect(paĝoStato.settledPage, kanaloj) {
        kanaloj.getOrNull(paĝoStato.settledPage)?.let(onVidataKanalo)
    }

    HorizontalPager(state = paĝoStato) { paĝo ->
        KanaloEkrano(
            kanalo = kanaloj[paĝo],
            elsendoDeponejo = elsendoDeponejo,
            plejŝatatajDeponejo = plejŝatatajDeponejo,
            agordojDeponejo = agordojDeponejo,
            onReen = onReen,
            onElsendo = onElsendo,
            onLudi = onLudi,
            onAgordoj = onAgordoj,
            ludilo = ludilo,
        )
    }
}

@Composable
private fun KanalInformoj(kanalo: Kanalo, ludilo: LudiloRegilo?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emblemo — tutpaĝa larĝo, adaptas aldon altecon
        if (kanalo.emblemoUrl != null) {
            AsyncImage(
                model = kanalo.emblemoUrl,
                contentDescription = "Emblemo de ${kanalo.nomo}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(kanalo.nomo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Mallonga priskribo el la kanalkonfiguro (nedeviga)
        if (!kanalo.priskribo.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                kanalo.priskribo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Retejo kaj retpoŝto butonoj
        if (kanalo.retejoUrl != null || kanalo.retpoŝto != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (kanalo.retejoUrl != null) {
                    AssistChip(
                        onClick = { logi("Klako", "retejo ${kanalo.slug}"); malfermuLigon(kanalo.retejoUrl) },
                        label = { Text("Retejo") },
                        leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (kanalo.retpoŝto != null) {
                    AssistChip(
                        onClick = {
                            logi("Klako", "retpoŝto ${kanalo.slug}")
                            val stato = ludilo?.stato?.value
                            val nunaElsendo: Elsendo? = when (stato?.nunaFonto) {
                                is Sonfonto.ElsendoFonto -> (stato.nunaFonto as Sonfonto.ElsendoFonto).elsendo
                                is Sonfonto.LokaElsendo -> (stato.nunaFonto as Sonfonto.LokaElsendo).elsendo
                                else -> null
                            }?.takeIf { it.kanaloSlug == kanalo.slug }
                            val teksto = if (nunaElsendo != null) {
                                val verbTempo = if (stato?.stato is LudantoStato.Ludas || stato?.stato is LudantoStato.Konektas)
                                    "aŭskultas" else "aŭskultis"
                                "Mi $verbTempo la elsendon (${nunaElsendo.titolo} — ${nunaElsendo.dato}) kaj havas komenton"
                            } else {
                                "Mi aŭskultas la elsendon kaj havas komenton"
                            }
                            malfermuRetpoŝton(
                                retpoŝto = kanalo.retpoŝto,
                                temo = "Pri ${kanalo.nomo}",
                                teksto = teksto,
                            )
                        },
                        label = { Text("Retpoŝto") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun ElsendoEro(
    elsendo: Elsendo,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                elsendo.titolo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            // Dato maldekstre de la daŭro (anstataŭ aparta datum-titolo super la grupo)
            val partoj = mutableListOf<String>()
            if (elsendo.dato.isNotBlank()) partoj.add(elsendo.dato)
            val daŭro = elsendo.daŭro
            if (daŭro != null && daŭro > 0) {
                val min = daŭro / 60
                val sek = daŭro % 60
                partoj.add("$min:${sek.toString().padStart(2, '0')}")
            }
            if (partoj.isNotEmpty()) {
                Text(
                    partoj.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            if (elsendo.bildoUrl != null) {
                AsyncImage(
                    model = elsendo.bildoUrl,
                    contentDescription = "Bildeto de ${elsendo.titolo}",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview(name = "KanaloEkrano", showBackground = true, heightDp = 400)
@Composable
fun KanaloEkranoPreview() {
    pTemo {
        KanaloEkrano(
            kanalo = pKanaloj[1],
            elsendoDeponejo = PreviewElsendoDeponejo(listOf(pElsendo)),
            plejŝatatajDeponejo = pPlejŝatatajDeponejo(),
            agordojDeponejo = dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl(),
            onReen = {},
        )
    }
}
