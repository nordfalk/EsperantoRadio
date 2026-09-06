package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Nova hejmekrano laux Figma-dizajno "Muzaiko — Antonia".
 *
 * Horizontalaj rulantaj sekcioj:
 * - "Kio novas" — novaj elsendoj (horizontala LazyRow de kartoj)
 * - "Lastatempe ludata" — laste luditaj (horizontala LazyRow de kartoj)
 * - "Kio popularas" — popularaj kanaloj (horizontala LazyRow de kartoj)
 *
 * Malsupra naviga breto (NavigationBar) kun 4 langetoj.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HejmoEkrano(
    kanalDeponejo: KanalDeponejo,
    elsendoDeponejo: ElsendoDeponejo,
    onKanal: (Kanal) -> Unit = {},
    onElsendo: (Elsendo) -> Unit = {},
    onLudi: (Elsendo) -> Unit = {},
    onSercxo: () -> Unit = {},
    onPlejsatataj: () -> Unit = {},
    onKanalaro: () -> Unit = {},
    onAgordoj: () -> Unit = {},
    onElshutoj: () -> Unit = {},
    onAlarmoj: () -> Unit = {},
) {
    val kanaloj by kanalDeponejo.observiKanalojn().collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EsperantoRadio", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { logi("Klako", "agordoj-butono"); onAgordoj() }) { Text("⚙") }
                }
            )
        },
        bottomBar = {
            MalsupraNavigaBreto(
                onHejmo = {},
                onKanalaro = { logi("Nav", "→ KANALARO"); onKanalaro() },
                onPlejsatataj = { logi("Nav", "→ PLEJSATATAJ"); onPlejsatataj() },
                onSercxo = { logi("Nav", "→ SERCXO"); onSercxo() },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // "Kio novas"
            item {
                SekcioTitolo("Kio novas")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(kanaloj.take(6)) { kanal ->
                        KanalKarto(
                            kanal = kanal,
                            onClick = { logi("Klako", "kanal ${kanal.slug}"); onKanal(kanal) }
                        )
                    }
                }
            }

            // "Lastatempe ludata"
            item {
                SekcioTitolo("Lastatempe ludata")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(kanaloj.take(6)) { kanal ->
                        KanalKarto(
                            kanal = kanal,
                            onClick = { logi("Klako", "kanal ${kanal.slug}"); onKanal(kanal) }
                        )
                    }
                }
            }

            // "Kio popularas"
            item {
                SekcioTitolo("Kio popularas")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(kanaloj.take(6)) { kanal ->
                        KanalKarto(
                            kanal = kanal,
                            onClick = { logi("Klako", "kanal ${kanal.slug}"); onKanal(kanal) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SekcioTitolo(titolo: String) {
    Text(
        text = titolo,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun KanalKarto(
    kanal: Kanal,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .width(150.dp)
            .height(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Emblemo (130x130 aŭ 59x59 dependanta de varianto)
            if (kanal.emblemoUrl != null) {
                AsyncImage(
                    model = kanal.emblemoUrl,
                    contentDescription = "Emblemo de ${kanal.nomo}",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            kanal.nomo.take(2),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Kanalnomo
            Text(
                text = kanal.nomo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subteksto
            Text(
                text = when {
                    kanal.estasRekta -> "Rekta elsendo"
                    kanal.havasPodkastojn -> "Podkasto"
                    else -> "Neniu fluo"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MalsupraNavigaBreto(
    onHejmo: () -> Unit,
    onKanalaro: () -> Unit,
    onPlejsatataj: () -> Unit,
    onSercxo: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { logi("Klako", "hejmo-tab"); onHejmo() },
            icon = { Text("🏠") },
            label = { Text("Hejmo") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { logi("Klako", "kanalaro-tab"); onKanalaro() },
            icon = { Text("🎵") },
            label = { Text("Kanaloj") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { logi("Klako", "plejsatataj-tab"); onPlejsatataj() },
            icon = { Text("★") },
            label = { Text("Plej ŝatataj") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { logi("Klako", "sercxo-tab"); onSercxo() },
            icon = { Text("🔍") },
            label = { Text("Serĉi") }
        )
    }
}
