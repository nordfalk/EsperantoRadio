package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
 * - Titolo de la nuna elsendo
 * - Ludi/paŭzi-butono
 * - Pozicio-breto (serĉbreto) por podkastoj (ne por rekta)
 * - Volvigi-butono (→ elsendodetalo) — estonte
 */
@Composable
fun MiniLudilbreto(
    ludilo: LudiloRegilo,
    modifier: Modifier = Modifier,
) {
    val stato by ludilo.stato.collectAsState()
    val info = stato
    val fonto = info.nunaFonto

    if (fonto == null) return // Nenio ludiĝas — ne montru la breton

    val titolo = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.nomo
        is Sonfonto.ElsendoFonto -> fonto.elsendo.titolo
    }

    val bildUrl = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.emblemoUrl
        is Sonfonto.ElsendoFonto -> fonto.elsendo.bildUrl
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
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bildeto
            if (bildUrl != null) {
                AsyncImage(
                    model = bildUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪", style = MaterialTheme.typography.headlineSmall)
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
