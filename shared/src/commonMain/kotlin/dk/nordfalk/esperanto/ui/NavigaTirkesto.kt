package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.navigation3.runtime.NavKey
import dk.nordfalk.compose.material3.DrawerState
import dk.nordfalk.compose.material3.ModalDrawerSheet
import dk.nordfalk.compose.material3.rememberDrawerState
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.navigation.Vojo
import kotlinx.coroutines.launch

/**
 * Naviga tirkesto — anstataŭas la iaman malsupran navigan breton, laŭ la modelo
 * de la malnova apo: sen titolo, nur tiom larĝa kiel bezonata, kaj ĝi vivas SUB
 * la supra breto (ĝi do ne kovras la titolon nek la hamburgeron). Eroj: la 4
 * ĉefekranoj, poste dividilo kaj la flankaj ekranoj. Ĉiuj eroj kondutas kiel
 * supranivela celo: `onElektu` ricevas la vojon kaj la navigado restas en App.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigaTirkesto(
    tirkestStato: DrawerState = rememberDrawerState(DrawerValue.Closed),
    nunaVojo: NavKey?,
    onElektu: (Vojo) -> Unit,
    /** Retpoŝtadreso de la prizorganto — ricevita de la „Proponu kanalon"-butono (datum-movita, A5). */
    prizorgantaRetpoŝto: String,
) {
    ModalDrawerSheet(
        tirkestStato,
        // Nur tiom larĝa kiel bezonata por la eroj — sen tio la implicito
        // estas la maksimuma tirkest-larĝo (360 dp)
        modifier = Modifier.width(IntrinsicSize.Max),
        // La tirkesto vivas interne de la jam inset-padita enhavo de la
        // ekrano — ĝi ne bezonas proprajn sistemajn enmetaĵojn
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        tirkestEroj().forEach { (ikono, etikedo, vojo) ->
            if (vojo is Vojo.Elŝutoj) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            NavigationDrawerItem(
                icon = { Icon(ikono, contentDescription = null) },
                label = { Text(etikedo) },
                selected = nunaVojo?.let { it::class } == vojo::class,
                onClick = { logi("Klako", "tirkesto: $etikedo"); onElektu(vojo) },
            )
        }

        // "Proponi kanalon" — retpoŝta butono al la prizorganto (FAROTA C57)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        val tirkestScope = rememberCoroutineScope()
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Email, contentDescription = null) },
            label = { Text("Proponi kanalon") },
            selected = false,
            onClick = {
                logi("Klako", "tirkesto: proponi kanalon")
                // Kiel la aliaj eroj: la tirkesto fermiĝas post la ago
                tirkestScope.launch { tirkestStato.close() }
                malfermuRetpoŝton(
                    retpoŝto = prizorgantaRetpoŝto,
                    temo = "Kanalo-propono por EsperantoRadio",
                    teksto = "Saluton!\n\nMi proponas la kanalon <nomo>:\n\nRSS-fluo: <url>\nPriskribo: <…>\n\nDankon!",
                )
            },
        )
    }
}

/**
 * Hamburgero por la supra breto de ĉefekrano: malfermas la navigan tirkeston
 * kaj, dum ĝi estas malfermita, fermas ĝin ree. La piktogramo MORFADAS inter
 * hamburgero (3 strekoj) kaj reen-sago per printempa animacio — same kiel la
 * `ActionBarDrawerToggle` de la malnova apo; la tirkesto mem glitas enen kaj
 * elen per la animacio de la forkigita `ModalNavigationDrawer`
 * (dk.nordfalk.compose.material3 — la tro-rula gesto povas movi la tirkeston
 * kun la fingro).
 */
@Composable
fun TirkestButono(tirkestStato: DrawerState) {
    val malfermata = tirkestStato.targetValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val koloro = LocalContentColor.current
    // La morfado moviĝas printempe kune kun la glitanta tirkesto
    val progreso by animateFloatAsState(
        targetValue = if (malfermata) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 800f),
        label = "hamburgero-morfado",
    )
    IconButton(
        onClick = {
            logi("Klako", if (malfermata) "menuo → fermas tirkeston" else "menuo → malfermas tirkeston")
            scope.launch { if (malfermata) tirkestStato.close() else tirkestStato.open() }
        },
    ) {
        HamburgeroSagoIkono(
            progreso = progreso,
            koloro = koloro,
            contentDescription = if (malfermata) "Fermi navigan menuon" else "Naviga menuo",
        )
    }
}

/**
 * Morfebla piktogramo: 0 = hamburgero (fermita tirkesto), 1 = reen-sago
 * (malfermita). La supra kaj malsupra strekoj kliniĝas en la sagokapon,
 * la meza streko restas kiel la sagotigo.
 */
@Composable
private fun HamburgeroSagoIkono(progreso: Float, koloro: Color, contentDescription: String) {
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val dikeco = 2.2.dp.toPx()
        val cx = size.width / 2
        val cy = size.height / 2
        val longeco = size.width * 0.66f
        val interspaco = size.height * 0.26f
        val kapo = longeco * 0.5f
        val k45 = 0.7071f
        val p = progreso

        fun lerp(a: Float, b: Float) = a + (b - a) * p

        // Meza streko — restas ankaŭ kiel la sagotigo
        drawLine(koloro, Offset(cx - longeco / 2, cy), Offset(cx + longeco / 2, cy), dikeco, StrokeCap.Round)

        // Supra streko: horizontala → supra branĉo de la sagokapo
        drawLine(
            koloro,
            Offset(lerp(cx - longeco / 2, cx - longeco / 2 + kapo * k45), lerp(cy - interspaco, cy - kapo * k45)),
            Offset(lerp(cx + longeco / 2, cx - longeco / 2), lerp(cy - interspaco, cy)),
            dikeco, StrokeCap.Round,
        )

        // Malsupra streko: horizontala → malsupra branĉo de la sagokapo
        drawLine(
            koloro,
            Offset(lerp(cx - longeco / 2, cx - longeco / 2 + kapo * k45), lerp(cy + interspaco, cy + kapo * k45)),
            Offset(lerp(cx + longeco / 2, cx - longeco / 2), lerp(cy + interspaco, cy)),
            dikeco, StrokeCap.Round,
        )
    }
}

private data class TirkestEro(val ikono: ImageVector, val etikedo: String, val vojo: Vojo)

private fun tirkestEroj() = listOf(
    TirkestEro(Icons.Filled.Home, "Hejmo", Vojo.Hejmo),
    TirkestEro(Icons.Filled.MusicNote, "Kanaloj", Vojo.Kanalaro),
    TirkestEro(Icons.Filled.Favorite, "Plej ŝatataj", Vojo.Plejŝatataj),
    TirkestEro(Icons.Filled.Search, "Serĉi", Vojo.Serĉo),
    TirkestEro(Icons.Filled.Download, "Elŝutoj", Vojo.Elŝutoj),
    TirkestEro(Icons.Filled.Alarm, "Vekhorloĝo", Vojo.Alarmoj),
    TirkestEro(Icons.Filled.Settings, "Agordoj", Vojo.Agordoj),
)
