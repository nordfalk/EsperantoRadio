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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.logi

/**
 * Nova hejmekrano laux Figma-dizajno "Muzaiko — Antonia".
 *
 * Horizontalaj rulantaj sekcioj:
 * - "Kio novas" — novaj elsendoj (horizontala LazyRow de kartoj)
 * - "Lastatempe ludata" — laste luditaj (nur se ekzistas)
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

    // Generi test-elsendojn por "Kio novas" kaj "Kio popularas"
    // Lastatempe ludata komencigxe malplena
    val novajElsendoj = kanaloj.flatMap { kanal ->
        (1..2).map { i ->
            Elsendo(
                id = "${kanal.slug}:novo$i",
                kanalSlug = kanal.slug,
                titolo = "Elsendo de 2024-01-0$i",
                stream = "",
                dato = "2024-01-0$i",
                bildUrl = kanal.emblemoUrl,
            )
        }
    }
    val popularajKanaloj = kanaloj.take(8)
    val lastatempeLuditaj: List<Elsendo> = emptyList()

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
            if (novajElsendoj.isNotEmpty()) {
                item { SekcioTitolo("Kio novas") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(novajElsendoj) { elsendo ->
                            val kanal = kanaloj.find { it.slug == elsendo.kanalSlug }
                            ElsendoKarto(
                                elsendo = elsendo,
                                kanalNomo = kanal?.nomo ?: elsendo.kanalSlug,
                                bildUrl = elsendo.bildUrl ?: kanal?.emblemoUrl,
                                onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) }
                            )
                        }
                    }
                }
            }

            // "Lastatempe ludata" — nur se ekzistas
            if (lastatempeLuditaj.isNotEmpty()) {
                item { SekcioTitolo("Lastatempe ludata") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lastatempeLuditaj) { elsendo ->
                            val kanal = kanaloj.find { it.slug == elsendo.kanalSlug }
                            ElsendoKarto(
                                elsendo = elsendo,
                                kanalNomo = kanal?.nomo ?: elsendo.kanalSlug,
                                bildUrl = elsendo.bildUrl ?: kanal?.emblemoUrl,
                                onClick = { logi("Klako", "elsendo ${elsendo.id}"); onElsendo(elsendo) }
                            )
                        }
                    }
                }
            }

            // "Kio popularas"
            if (popularajKanaloj.isNotEmpty()) {
                item { SekcioTitolo("Kio popularas") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popularajKanaloj) { kanal ->
                            KanalKarto(
                                kanal = kanal,
                                onClick = { logi("Klako", "kanal ${kanal.slug}"); onKanal(kanal) }
                            )
                        }
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
private fun ElsendoKarto(
    elsendo: Elsendo,
    kanalNomo: String,
    bildUrl: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(150.dp).height(200.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (bildUrl != null) {
                AsyncImage(
                    model = bildUrl,
                    contentDescription = "Bildeto de ${elsendo.titolo}",
                    modifier = Modifier.size(130.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("♪", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = kanalNomo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = elsendo.titolo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
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
        modifier = Modifier.width(150.dp).height(200.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (kanal.emblemoUrl != null) {
                AsyncImage(
                    model = kanal.emblemoUrl,
                    contentDescription = "Emblemo de ${kanal.nomo}",
                    modifier = Modifier.size(130.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(130.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(kanal.nomo.take(2), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = kanal.nomo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    kanal.estasRekta -> "Rekta elsendo"
                    kanal.havasPodkastojn -> "Podkasto"
                    else -> "Neniu fluo"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
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
        NavigationBarItem(selected = true, onClick = { logi("Klako", "hejmo-tab"); onHejmo() }, icon = { Text("🏠") }, label = { Text("Hejmo") })
        NavigationBarItem(selected = false, onClick = { logi("Klako", "kanalaro-tab"); onKanalaro() }, icon = { Text("🎵") }, label = { Text("Kanaloj") })
        NavigationBarItem(selected = false, onClick = { logi("Klako", "plejsatataj-tab"); onPlejsatataj() }, icon = { Text("★") }, label = { Text("Plej ŝatataj") })
        NavigationBarItem(selected = false, onClick = { logi("Klako", "sercxo-tab"); onSercxo() }, icon = { Text("🔍") }, label = { Text("Serĉi") })
    }
}
