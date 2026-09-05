package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dk.nordfalk.esperanto.domain.model.Elsendo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElsendoEkrano(
    elsendo: Elsendo,
    onReen: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(elsendo.titolo, maxLines = 1) },
                navigationIcon = {
                    TextButton(onClick = onReen) { Text("← Reen") }
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
            if (elsendo.bildUrl != null) {
                AsyncImage(
                    model = elsendo.bildUrl,
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
            Spacer(Modifier.height(24.dp))

            // Priskribo
            if (!elsendo.priskribo.isNullOrBlank()) {
                Text(
                    text = elsendo.priskribo!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))

            // Lud-butono (TODO: Fazo 2)
            Button(
                onClick = { /* TODO: Ludilo */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("▶ Aŭskulti")
            }
        }
    }
}
