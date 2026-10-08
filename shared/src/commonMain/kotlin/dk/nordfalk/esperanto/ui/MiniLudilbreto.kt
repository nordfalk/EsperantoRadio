package dk.nordfalk.esperanto.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.ReprovoLogiko
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

/** Kiom da la elsendo-daŭro la reen-/antaŭen-butonoj saltas — 5%, kiel en la malnova apo. */
const val SALTA_KVOCOTO = 0.05f

/**
 * Mini-ludilbreto — malsupera breto kiu montras la nunan elsendon kaj lud-regilojn.
 *
 * Kiel en la malnova apo ĝi havas du statojn, ŝalteblajn per sago dekstre:
 * - **Enfoldigita**: bildeto, titolo, ludi/paŭzi, halti, ludvico + maldika pozicio-linio.
 * - **Elfaldigita**: aldone serĉbreto (ŝovebla), reen-/antaŭen-5%-butonoj kaj laŭteco-regilo.
 *   Serĉbreto kaj saltbutonoj aperas nur por podkastoj, ne por rekta elsendo.
 */
@Composable
fun MiniLudilbreto(
    ludilo: LudiloRegilo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLudvico: () -> Unit = {},
    /** Nuna reprovo-numero el [dk.nordfalk.esperanto.domain.player.LudvicoRegilo.reprovo] (0 = neniu). */
    reprovo: Int = 0,
    /** Ĉu la ludilbreto estas elfaldigita (hoistita ŝtato — regata de App.kt). */
    elfaldita: Boolean = false,
    onElfalditaSxangxo: (Boolean) -> Unit = {},
    /** Sekvanta enhavo kiam nenio ludas — la breto sekvas la navigadon. */
    sekvantaTitolo: String? = null,
    sekvantaSubtitolo: String? = null,
    sekvantaBildoUrl: String? = null,
    onLudiSekvantan: () -> Unit = {},
) {
    val stato by ludilo.stato.collectAsState()
    val info = stato
    val fonto = info.nunaFonto

    // Se nenio ludas kaj ne estas sekvanta enhavo, ne montru la breton
    if (fonto == null && sekvantaTitolo == null) return

    val ludas = info.stato is LudantoStato.Ludas
    val scope = rememberCoroutineScope()

    // Se nenio ludas, montru la sekvantan enhavon (navigad-sekvado)
    if (fonto == null) {
        MiniLudilbretoSekvanta(
            titolo = sekvantaTitolo!!,
            subtitolo = sekvantaSubtitolo,
            bildoUrl = sekvantaBildoUrl,
            onLudi = onLudiSekvantan,
            onClick = onClick,
            modifier = modifier,
        )
        return
    }

    val titolo = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanalo.nomo
        is Sonfonto.ElsendoFonto -> fonto.elsendo.titolo
        is Sonfonto.LokaElsendo -> fonto.elsendo.titolo
    }

    val subtitolo = when (fonto) {
        is Sonfonto.RektaKanalo -> null
        is Sonfonto.ElsendoFonto -> fonto.elsendo.kanaloNomo
        is Sonfonto.LokaElsendo -> fonto.elsendo.kanaloNomo
    }

    val bildoUrl = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanalo.emblemoUrl
        is Sonfonto.ElsendoFonto -> fonto.elsendo.bildoUrl
        is Sonfonto.LokaElsendo -> fonto.elsendo.bildoUrl
    }

    val estasPodkasto = !info.estasRekta && info.dauroMs > 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Glata malfermo/firmo de la elfalda areo
                .animateContentSize(animationSpec = tween(durationMillis = 250))
                .testTag("ludilbreto")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { logi("Klako", "mini-ludilbreto → detalo"); onClick() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Bildeto
                if (bildoUrl != null) {
                    AsyncImage(
                        model = bildoUrl,
                        contentDescription = "Bildeto de nuna elsendo",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Titolo + stato
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = titolo,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val statTeksto = when {
                        reprovo > 0 && (info.stato is LudantoStato.Eraro || info.stato is LudantoStato.Konektas) ->
                            "Konektas… (provo $reprovo/${ReprovoLogiko.MAKS_PROVOJ})"
                        else -> when (info.stato) {
                            is LudantoStato.Ludas -> if (info.estasRekta) "Rekta elsendo" else "Ludas"
                            is LudantoStato.Konektas -> "Konektas…"
                            is LudantoStato.Haltita -> if (info.estasRekta) "Haltita" else "Paŭzita"
                            is LudantoStato.Finita -> "Finita"
                            is LudantoStato.Eraro -> "Ne eblas ludi — kontrolu la retkonekton"
                        }
                    }
                    val plenaTeksto = if (subtitolo != null) "$subtitolo · $statTeksto" else statTeksto
                    Text(
                        text = plenaTeksto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Ludi/paŭzi-butono
                IconButton(onClick = {
                    logi("Klako", if (ludas) "paŭzigi" else "ludi")
                    scope.launch {
                        if (ludas) ludilo.pauxzigi() else ludilo.ludi()
                    }
                }) {
                    val konektas = info.stato is LudantoStato.Konektas ||
                        (reprovo > 0 && info.stato is LudantoStato.Eraro)
                    if (konektas) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).semantics { contentDescription = "Konektas" },
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (ludas) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (ludas) "Paŭzigi" else "Ludi"
                        )
                    }
                }

                // Halti-butono
                IconButton(onClick = { logi("Klako", "halti"); ludilo.halti() }) {
                    Icon(Icons.Filled.Stop, contentDescription = "Halti")
                }

                // Ludvico-butono
                IconButton(onClick = { logi("Klako", "ludvico"); onLudvico() }) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = "Ludvico")
                }

                // Elfaldi/enfoldigi-sago (kiel en la malnova apo — turniĝas, ne ŝanĝas piktogramon)
                val sagoRotacio by animateFloatAsState(
                    targetValue = if (elfaldita) 180f else 0f,
                    animationSpec = tween(durationMillis = 250),
                )
                IconButton(onClick = {
                    logi("Klako", if (elfaldita) "enfoldigi la ludilon" else "elfaldi la ludilon")
                    onElfalditaSxangxo(!elfaldita)
                }) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (elfaldita) "Enfoldigi la ludilon" else "Elfaldigi la ludilon",
                        modifier = Modifier.rotate(sagoRotacio),
                    )
                }
            }

            if (elfaldita) {
                // Serĉbreto (nur por podkastoj) — ŝovebla se la ludilo subtenas saltadon
                if (estasPodkasto) {
                    val pozicioMs = info.pozicioMs.coerceIn(0L, info.dauroMs)
                    var sxtiraValoro by remember(info.nunaFonto) { mutableStateOf<Float?>(null) }
                    val valoro = sxtiraValoro ?: (pozicioMs.toFloat() / info.dauroMs)
                    val montrataPozicioMs = (valoro * info.dauroMs).toLong()

                    if (ludilo.subtenasSaltadon) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatuTempon(montrataPozicioMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = valoro,
                                onValueChange = { sxtiraValoro = it },
                                onValueChangeFinished = {
                                    val frakcio = sxtiraValoro ?: valoro
                                    val celo = (frakcio * info.dauroMs).toLong()
                                    logi("Klako", "salti al $celo ms (de ${info.pozicioMs} ms)")
                                    ludilo.saltiAl(celo)
                                    sxtiraValoro = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("sercxbreto"),
                            )
                            Text(
                                text = formatuTempon(info.dauroMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Reen-/antaŭen-butonoj — 5% de la daŭro, kiel en la malnova apo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            IconButton(onClick = { saltuDiference(ludilo, info, -1) }) {
                                Icon(Icons.Filled.FastRewind, contentDescription = "Reen ${(SALTA_KVOCOTO * 100).toInt()}%")
                            }
                            Spacer(Modifier.width(24.dp))
                            IconButton(onClick = { saltuDiference(ludilo, info, +1) }) {
                                Icon(Icons.Filled.FastForward, contentDescription = "Antaŭen ${(SALTA_KVOCOTO * 100).toInt()}%")
                            }
                        }
                    } else {
                        // La ludilo ne povas salti (ekz. Desktop/mp3spi) — nur pasiva indikilo
                        LinearProgressIndicator(
                            progress = { valoro },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }
                }

                // Laŭteco-regilo (funkcias kaj por rekta kaj por podkasto).
                // La fluo sekvas ankaŭ eksterajn ŝanĝojn (ekz. hardvaraj laŭteco-klavoj sur Android).
                val lauxtecoFluo by ludilo.lauxteco.collectAsState()
                var sxtiraLauxteco by remember(ludilo) { mutableStateOf<Float?>(null) }
                val lauxteco = (sxtiraLauxteco ?: lauxtecoFluo).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = lauxteco,
                        onValueChange = { sxtiraLauxteco = it },
                        onValueChangeFinished = {
                            val nova = sxtiraLauxteco ?: lauxteco
                            logi("Klako", "lauxteco → $nova")
                            ludilo.fiksiLauxtecon(nova)
                            sxtiraLauxteco = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("lauxteco"),
                    )
                }
            } else if (estasPodkasto) {
                // Enfoldigite: nur maldika, neinteraga pozicio-linio
                val progreso = (info.pozicioMs.toFloat() / info.dauroMs).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

/**
 * Mini-ludilbreto en sekvanta-reĝimo — montras la nunan navigan celon (kanalo/elsendo)
 * kun ludi-butono, kiam nenio aktive ludas.
 */
@Composable
private fun MiniLudilbretoSekvanta(
    titolo: String,
    subtitolo: String?,
    bildoUrl: String?,
    onLudi: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { logi("Klako", "mini-ludilbreto (sekvanta) → detalo"); onClick() }
                .padding(8.dp)
                .testTag("ludilbreto"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bildeto
            if (bildoUrl != null) {
                AsyncImage(
                    model = bildoUrl,
                    contentDescription = "Bildeto de nuna kanalo/elsendo",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(titolo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitolo != null) {
                    Text(subtitolo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.width(8.dp))

            // Ludi-butono — komencas ludi la nunan navigan celon
            IconButton(onClick = { logi("Klako", "ludi sekvantan"); onLudi() }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Ludi")
            }
        }
    }
}

/** Saltas [direkto]-foje 5% de la elsendo-daŭro (negativa = reen), kiel en la malnova apo. */
private fun saltuDiference(ludilo: LudiloRegilo, info: LudantoInformo, direkto: Int) {
    val diferenco = (info.dauroMs * SALTA_KVOCOTO).toLong() * direkto
    val celo = (info.pozicioMs + diferenco).coerceIn(0L, info.dauroMs)
    logi("Klako", "salti ${if (direkto < 0) "reen" else "antaŭen"} ${(SALTA_KVOCOTO * 100).toInt()}% al $celo ms (de ${info.pozicioMs} ms)")
    ludilo.saltiAl(celo)
}

/** Formatu pozicion/daŭron en ms al H:MM:SS aŭ M:SS. */
private fun formatuTempon(ms: Long): String {
    val sekundoj = ms / 1000
    val hor = sekundoj / 3600
    val min = (sekundoj % 3600) / 60
    val sek = sekundoj % 60
    return if (hor > 0) {
        "$hor:${min.toString().padStart(2, '0')}:${sek.toString().padStart(2, '0')}"
    } else {
        "$min:${sek.toString().padStart(2, '0')}"
    }
}

@Preview(name = "MiniLudilbreto — ludas", showBackground = true, heightDp = 80)
@Composable
fun MiniLudilbretoPreview() {
    val ludilo = PreviewLudiloRegilo(
        dk.nordfalk.esperanto.domain.model.LudantoInformo(
            stato = dk.nordfalk.esperanto.domain.model.LudantoStato.Ludas,
            nunaFonto = dk.nordfalk.esperanto.domain.model.Sonfonto.ElsendoFonto(pElsendo),
            pozicioMs = 30000, dauroMs = 6916000, estasRekta = false,
        )
    )
    pTemo { MiniLudilbreto(ludilo = ludilo) }
}

@Preview(name = "MiniLudilbreto — elfaldigita", showBackground = true, heightDp = 240)
@Composable
fun MiniLudilbretoElfalditaPreview() {
    val ludilo = PreviewLudiloRegilo(
        dk.nordfalk.esperanto.domain.model.LudantoInformo(
            stato = dk.nordfalk.esperanto.domain.model.LudantoStato.Ludas,
            nunaFonto = dk.nordfalk.esperanto.domain.model.Sonfonto.ElsendoFonto(pElsendo),
            pozicioMs = 30000, dauroMs = 6916000, estasRekta = false,
        )
    )
    pTemo {
        var elfaldita by remember { mutableStateOf(true) }
        MiniLudilbreto(ludilo = ludilo, elfaldita = elfaldita, onElfalditaSxangxo = { elfaldita = it })
    }
}

@Preview(name = "MiniLudilbreto — sekvanta (nenio ludas)", showBackground = true, heightDp = 80)
@Composable
fun MiniLudilbretoSekvantaPreview() {
    val ludilo = PreviewLudiloRegilo(
        dk.nordfalk.esperanto.domain.model.LudantoInformo(
            stato = dk.nordfalk.esperanto.domain.model.LudantoStato.Haltita,
            nunaFonto = null,
        )
    )
    pTemo {
        MiniLudilbreto(
            ludilo = ludilo,
            sekvantaTitolo = "Varsovia Vento",
            sekvantaSubtitolo = "Kanalo",
            sekvantaBildoUrl = null,
            onLudiSekvantan = {},
        )
    }
}
