package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

/**
 * Mini-ludilbreto — malsupera breto kiu montras la nunan elsendon kaj lud-regilojn.
 *
 * Montroj:
 * - Emblemo/bildeto de la nuna elsendo
 * - Titolo de la nuna elsendo (klakebla → elsendodetalo aŭ kanalo)
 * - Ludi/paŭzi-butono
 * - Pozicio-breto (serĉbreto) por podkastoj (ne por rekta)
 */
@Composable
fun MiniLudilbreto(
    ludilo: LudiloRegilo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val stato by ludilo.stato.collectAsState()
    val info = stato
    val fonto = info.nunaFonto

    if (fonto == null) return // Nenio ludiĝas — ne montru la breton

    val titolo = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.nomo
        is Sonfonto.ElsendoFonto -> fonto.elsendo.titolo
        is Sonfonto.LokaElsendo -> fonto.elsendo.titolo
    }

    val bildUrl = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.emblemoUrl
        is Sonfonto.ElsendoFonto -> fonto.elsendo.bildUrl
        is Sonfonto.LokaElsendo -> fonto.elsendo.bildUrl
    }

    val ludas = info.stato is LudantoStato.Ludas
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { logi("Klako", "mini-ludilbreto → detalo"); onClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bildeto
            if (bildUrl != null) {
                AsyncImage(
                    model = bildUrl,
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
                        Text("♪", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                val statTeksto = when (info.stato) {
                    is LudantoStato.Ludas -> if (info.estasRekta) "Rekta elsendo" else "Ludas"
                    is LudantoStato.Konektas -> "Konektas..."
                    is LudantoStato.Haltita -> if (info.estasRekta) "Haltita" else "Paŭzita"
                    is LudantoStato.Eraro -> "Eraro: ${(info.stato as LudantoStato.Eraro).mesagho}"
                }
                Text(
                    text = statTeksto,
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
                Text(
                    text = if (ludas) "⏸" else "▶",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Halti-butono
            IconButton(onClick = { logi("Klako", "halti"); ludilo.halti() }) {
                Text("■", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Pozicio-breto por podkastoj (ne por rekta elsendo)
        if (!info.estasRekta && info.dauroMs > 0) {
            val progreso = if (info.dauroMs > 0) {
                (info.pozicioMs.toFloat() / info.dauroMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            )
        }
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
