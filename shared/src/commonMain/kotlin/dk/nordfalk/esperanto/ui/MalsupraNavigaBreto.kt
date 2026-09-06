package dk.nordfalk.esperanto.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import dk.nordfalk.esperanto.logi

/**
 * Kiu langeto estas aktiva en la malsupra naviga breto.
 * NENIO = neniu langeto estas aktiva (ekz. sur detalekranoj).
 */
enum class EkranoLangeto { HEJMO, KANALARO, PLEJSATATAJ, SERCXO, NENIO }

/**
 * Malsupra naviga breto — 4 langetoj: Hejmo, Kanaloj, Plej ŝatataj, Serĉo.
 * Dinamike montras kiu langeto estas aktiva per [nunaTab].
 */
@Composable
fun MalsupraNavigaBreto(
    nunaTab: EkranoLangeto,
    onHejmo: () -> Unit,
    onKanalaro: () -> Unit,
    onPlejsatataj: () -> Unit,
    onSercxo: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.HEJMO,
            onClick = { logi("Klako", "hejmo-tab"); onHejmo() },
            icon = { Text("🏠") },
            label = { Text("Hejmo") }
        )
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.KANALARO,
            onClick = { logi("Klako", "kanalaro-tab"); onKanalaro() },
            icon = { Text("🎵") },
            label = { Text("Kanaloj") }
        )
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.PLEJSATATAJ,
            onClick = { logi("Klako", "plejsatataj-tab"); onPlejsatataj() },
            icon = { Text("★") },
            label = { Text("Plej ŝatataj") }
        )
        NavigationBarItem(
            selected = nunaTab == EkranoLangeto.SERCXO,
            onClick = { logi("Klako", "sercxo-tab"); onSercxo() },
            icon = { Text("🔍") },
            label = { Text("Serĉi") }
        )
    }
}
