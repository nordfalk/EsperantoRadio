package dk.nordfalk.esperanto.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Ludvico-butono por la supra menua linio (TopAppBar).
 *
 * Montriĝas nur se la ludvico ([vico]) enhavas elementojn — malplena vico
 * ne okupas lokon en la breto.
 */
@Composable
fun LudvicoButono(
    vico: StateFlow<List<Elsendo>> = MutableStateFlow(emptyList()),
    onLudvico: () -> Unit = {},
) {
    val eroj by vico.collectAsState()
    if (eroj.isNotEmpty()) {
        IconButton(onClick = { logi("Klako", "ludvico-butono (supra breto, ${eroj.size} eroj)"); onLudvico() }) {
            Icon(Icons.Filled.QueueMusic, contentDescription = "Ludvico")
        }
    }
}
