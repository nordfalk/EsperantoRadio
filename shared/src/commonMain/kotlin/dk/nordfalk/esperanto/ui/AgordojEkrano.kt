package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgordojEkrano(
    agordojDeponejo: AgordojDeponejo,
    onReen: () -> Unit,
) {
    val lingvo by agordojDeponejo.lingvo.collectAsState()
    val nurWifi by agordojDeponejo.nurWifi.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agordoj") },
                navigationIcon = { TextButton(onClick = onReen) { Text("← Reen") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Lingvo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = lingvo == "eo",
                    onClick = { agordojDeponejo.fiksiLingvon("eo") },
                    label = { Text("Esperanto") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = lingvo == "da",
                    onClick = { agordojDeponejo.fiksiLingvon("da") },
                    label = { Text("Dana") }
                )
            }
            Spacer(Modifier.height(24.dp))

            Text("Reto", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Nur per WiFi") },
                trailingContent = {
                    Switch(
                        checked = nurWifi,
                        onCheckedChange = { agordojDeponejo.fiksiNurWifi(it) }
                    )
                }
            )
            Divider()
        }
    }
}
