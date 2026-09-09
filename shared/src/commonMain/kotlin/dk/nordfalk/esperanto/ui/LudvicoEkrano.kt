package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.player.LudvicoRegilo
import dk.nordfalk.esperanto.logi

/**
 * Ludvico-ekrano — montras la nunan ludvicon kun eble forigi/malplenigi.
 *
 * La unua ero en la vico estas tiu kiu ludos sekve post la nuna elsendo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudvicoEkrano(
    ludvicoRegilo: LudvicoRegilo,
    onReen: () -> Unit,
    onElsendo: (Elsendo) -> Unit = {},
) {
    val vico by ludvicoRegilo.vico.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ludvico (${vico.size})") },
                navigationIcon = {
                    TextButton(onClick = { logi("Klako", "reen (LudvicoEkrano)"); onReen() }) { Text("← Reen") }
                },
                actions = {
                    if (vico.isNotEmpty()) {
                        TextButton(onClick = { logi("Klako", "malplenigi vicon"); ludvicoRegilo.malplenigiVicon() }) {
                            TextMalplenigi()
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (vico.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Ludvico estas malplena",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(vico, key = { it.id }) { elsendo ->
                    VicoEro(
                        elsendo = elsendo,
                        onClick = { logi("Klako", "vico-elsendo ${elsendo.id}"); onElsendo(elsendo) },
                        onForigi = { logi("Klako", "forigi el vico ${elsendo.id}"); ludvicoRegilo.forigiElVico(elsendo.id) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun TextMalplenigi() {
    Text("Malplenigi", color = MaterialTheme.colorScheme.error)
}

@Composable
private fun VicoEro(
    elsendo: Elsendo,
    onClick: () -> Unit,
    onForigi: () -> Unit,
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
            Text(
                "${elsendo.kanaloNomo ?: elsendo.kanaloSlug} · ${elsendo.dato}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            TextButton(onClick = onForigi) {
                Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(name = "LudvicoEkrano — malplena", showBackground = true, heightDp = 300)
@Composable
fun LudvicoEkranoMalplenaPreview() {
    val regilo = LudvicoRegilo(
        ludilo = dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo(),
        elsendoDeponejo = object : dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo {
            override fun observiElsendojn(kanaloSlug: String) = kotlinx.coroutines.flow.MutableStateFlow(emptyList<dk.nordfalk.esperanto.domain.model.Elsendo>())
            override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean) = emptyList<dk.nordfalk.esperanto.domain.model.Elsendo>()
            override suspend fun getElsendo(id: String) = null
            override suspend fun sercxiElsendojn(teksto: String, limo: Int) = emptyList<dk.nordfalk.esperanto.domain.model.Elsendo>()
            override suspend fun sxargxiElsendojnPorKanal(kanalo: dk.nordfalk.esperanto.domain.model.Kanalo, fortoRefresigi: Boolean) = emptyList<dk.nordfalk.esperanto.domain.model.Elsendo>()
        },
        kanaloDeponejo = object : dk.nordfalk.esperanto.domain.repository.KanaloDeponejo {
            override fun observiKanalojn() = kotlinx.coroutines.flow.MutableStateFlow(emptyList<dk.nordfalk.esperanto.domain.model.Kanalo>())
            override suspend fun getKanalojn(fortoRefresigi: Boolean) = emptyList<dk.nordfalk.esperanto.domain.model.Kanalo>()
            override suspend fun getKanalo(slug: String) = null
        },
        plejsatatajDeponejo = dk.nordfalk.esperanto.data.repository.PlejsatatajDeponejoImpl(),
        ludantojDeponejo = object : dk.nordfalk.esperanto.domain.repository.LudantojDeponejo {
            private val st = kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, dk.nordfalk.esperanto.domain.model.LudantaElsendo>())
            override fun observiLudantojn() = st
            override suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, dauroMs: Long) {}
            override suspend fun markiFinita(elsendoId: String, kanaloSlug: String) {}
            override suspend fun getLudanto(elsendoId: String) = null
            override suspend fun estasFinita(elsendoId: String) = false
            override suspend fun getPozicio(elsendoId: String) = null
        },
    )
    pTemo { LudvicoEkrano(ludvicoRegilo = regilo, onReen = {}) }
}
