package dk.nordfalk.esperanto.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dk.nordfalk.esperanto.logi

/**
 * Kiu langeto estas aktiva en la malsupra naviga breto.
 * NENIO = neniu langeto estas aktiva (ekz. sur detalekranoj).
 */
enum class EkranoLangeto { HEJMO, KANALARO, PLEJŜATATAJ, SERCXO, NENIO }

/**
 * Malsupra naviga breto — 4 langetoj: Hejmo, Kanaloj, Plej ŝatataj, Serĉo.
 * Dinamike montras kiu langeto estas aktiva per [nunaTab].
 */
@Composable
fun MalsupraNavigaBreto(
    nunaTab: EkranoLangeto,
    onHejmo: () -> Unit,
    onKanalaro: () -> Unit,
    onPlejŝatataj: () -> Unit,
    onSercxo: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.HEJMO,
            onClick = { logi("Klako", "hejmo-tab"); onHejmo() },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Hejmo") }
        )
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.KANALARO,
            onClick = { logi("Klako", "kanalaro-tab"); onKanalaro() },
            icon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
            label = { Text("Kanaloj") }
        )
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.PLEJŜATATAJ,
            onClick = { logi("Klako", "plejŝatataj-tab"); onPlejŝatataj() },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text("Plej ŝatataj") }
        )
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.SERCXO,
            onClick = { logi("Klako", "sercxo-tab"); onSercxo() },
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Serĉi") }
        )
    }
}
