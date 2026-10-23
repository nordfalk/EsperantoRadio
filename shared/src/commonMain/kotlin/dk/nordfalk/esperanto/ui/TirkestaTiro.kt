package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.launch

/**
 * Sojlo-kalkulo por la dekstra tro-tiro kiu malfermas la navigan tirkeston.
 *
 * Pure kalkula (sen Compose) por esti rekte testebla: [akceptu] ricevas la
 * rulo-deltojn de la uzanto kaj redonas `true` nur kiam la akumulita dekstra
 * tiro transpaŝas la sojlon DUM la vico ne povas ruli pli reen (ĝi jam estas
 * ĉe sia komenco).
 *
 * - Deltoj `<= 0` (maldekstren / senmovaj) nuligas la akumulon.
 * - Deltoj dum la vico ankoraŭ povas ruli reen estas ignorataj (nuligitaj) —
 *   unue la vico mem rulu.
 * - Post pafado la akumulo rekomenciĝas de nulo.
 */
class TirkestaTiroSojlo(private val sojloPx: Float) {
    private var akumulita = 0f

    /**
     * Registras rulo-delton kaj decidas ĉu la tirkesto malfermiĝu.
     *
     * @param deltaX pozitiva = la uzanto tiras dekstren
     * @param povasRuliReen ĉu la vico ankoraŭ povas ruli al sia komenco
     * @return true = la sojlo atingiĝis, malfermu la tirkeston
     */
    fun akceptu(deltaX: Float, povasRuliReen: Boolean): Boolean {
        if (deltaX <= 0f || povasRuliReen) {
            akumulita = 0f
            return false
        }
        akumulita += deltaX
        return if (akumulita >= sojloPx) {
            akumulita = 0f
            true
        } else false
    }
}

/**
 * Farias ke dekstra tro-tiro sur horizontala rulebla vico malfermas la navigan
 * tirkeston: kiam la vico estas ĉe sia komenco kaj ne povas ruli pli reen, la
 * resto de la dekstra tiro (trans [sojlo]) malfermas la tirkeston — kiel tiro
 * de la maldekstra rando.
 *
 * La modifilo devas esti sur la rulebla elemento mem (ekz. `LazyRow`): la
 * interna rul-mekanismo disponigas la deltojn al la primitiva `nestedScroll`-konekto.
 */
@Composable
fun Modifier.tirkestoJeTroRulo(
    tirkestStato: DrawerState,
    vicoStato: LazyListState,
    sojlo: Float = with(LocalDensity.current) { 40.dp.toPx() },
): Modifier {
    val kalkulo = remember { TirkestaTiroSojlo(sojlo) }
    val scope = rememberCoroutineScope()
    val konekto = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Nur kiam nenio ruleblas reen kaj la uzanto tiras dekstren
                if (vicoStato.canScrollBackward || available.x <= 0f) return Offset.Zero
                // Fermita tirkesto nur — malfermita ŝirmas la ekranon ajn
                if (tirkestStato.targetValue != DrawerValue.Closed) return Offset.Zero
                val malfermu = kalkulo.akceptu(available.x, vicoStato.canScrollBackward)
                if (malfermu) {
                    logi("Tiro", "dekstra tro-tiro sur vico malfermas la tirkeston")
                    scope.launch { tirkestStato.open() }
                }
                // Konsumu la delton: la vico ne povas ruli per ĝi ajn — ĝi iru al la tirkesto
                return Offset(available.x, 0f)
            }
        }
    }
    return this.nestedScroll(konekto)
}
