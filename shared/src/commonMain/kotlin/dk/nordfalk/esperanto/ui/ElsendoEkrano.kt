package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantaElsendo
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.domain.repository.LudantojDeponejo
import dk.nordfalk.esperanto.logi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElsendoEkrano(
    elsendo: Elsendo,
    onReen: () -> Unit,
    onLudi: () -> Unit = {},
    onElshuti: () -> Unit = {},
    onForigiElshuton: () -> Unit = {},
    onKanalo: (Kanalo) -> Unit = {},
    onAldoniAlVico: () -> Unit = {},
    kanalo: Kanalo? = null,
    elshutDeponejo: ElshutDeponejo? = null,
    ludantojDeponejo: LudantojDeponejo? = null,
) {
    val elshutStato by (elshutDeponejo?.observiElshutStaton(elsendo.id)?.collectAsState() ?: remember { mutableStateOf<ElshutStato>(ElshutStato.NeElshutita) })
    val ludantojMapo by (ludantojDeponejo?.observiLudantojn()?.collectAsState() ?: remember { mutableStateOf(emptyMap<String, LudantaElsendo>()) })
    val ludanto = ludantojMapo[elsendo.id]
    val savitaPozicio = ludanto?.pozicioMs?.takeIf { it > 0 && !ludanto.finita }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(elsendo.titolo, maxLines = 1) },
                navigationIcon = {
                    TextButton(onClick = { logi("Klako", "reen (ElsendoEkrano)"); onReen() }) { Text("← Reen") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bildo
            if (elsendo.bildoUrl != null) {
                AsyncImage(
                    model = elsendo.bildoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            // Titolo
            Text(
                text = elsendo.titolo,
                style = MaterialTheme.typography.headlineSmall
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

            // Klakebla kanalnomo → iras al kanalo-ekrano
            if (kanalo != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { logi("Klako", "kanalo-ligilo → ${kanalo.slug}"); onKanalo(kanalo) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = kanalo.nomo,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Savita pozicio — progresbreto kaj "daŭrigi de" teksto
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

            // Lud-butono
            val ludButonoTeksto = if (savitaPozicio != null && savitaPozicio > 5000) "▶ Daŭrigi" else "▶ Aŭskulti"
            Button(
                onClick = { logi("Klako", "aŭskulti — ${elsendo.id}"); onLudi() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(ludButonoTeksto)
            }

            Spacer(Modifier.height(8.dp))

            // Aldoni al ludvico
            OutlinedButton(
                onClick = { logi("Klako", "aldoni al ludvico — ${elsendo.id}"); onAldoniAlVico() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📋 Aldoni al ludvico") }

            Spacer(Modifier.height(8.dp))

            // Elŝut-butono — statdependa
            when (elshutStato) {
                is ElshutStato.NeElshutita -> OutlinedButton(
                    onClick = { logi("Klako", "elŝuti — ${elsendo.id}"); onElshuti() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⬇ Elŝuti") }

                is ElshutStato.Elshutanta -> {
                    val p = (elshutStato as ElshutStato.Elshutanta).progreso
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("⏳ Elŝutas... ${((p * 100).toInt())}%") }
                }

                is ElshutStato.Preta -> OutlinedButton(
                    onClick = { logi("Klako", "forigi elŝuton — ${elsendo.id}"); onForigiElshuton() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✓ Elŝutita — Forigi") }

                is ElshutStato.Eraro -> OutlinedButton(
                    onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⚠ Eraro — reprovi") }

                is ElshutStato.Pauxzita -> OutlinedButton(
                    onClick = { logi("Klako", "reprovi elŝuti — ${elsendo.id}"); onElshuti() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("⏸ Paŭzita — reprovi") }
            }

            // Retpoŝto-butono — nur se la kanalo havas retpoŝtadreson
            if (kanalo?.retposhto != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        logi("Klako", "retpoŝto el elsendo ${elsendo.id}")
                        malfermuRetposhton(
                            retposhto = kanalo.retposhto,
                            temo = "Pri ${kanalo.nomo}",
                            teksto = "Mi aŭskultas la elsendon (${elsendo.titolo} — ${elsendo.dato}) kaj havas komenton",
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✉ Komenti") }
            }
        }
    }
}

@Preview(name = "ElsendoEkrano", showBackground = true, heightDp = 500)
@Composable
fun ElsendoEkranoPreview() {
    pTemo { ElsendoEkrano(elsendo = pElsendo, onReen = {}, onLudi = {}, onElshuti = {}) }
}
