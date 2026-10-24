package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import dk.nordfalk.compose.material3.DrawerState
import dk.nordfalk.esperanto.logi

/**
 * Farias ke dekstra tro-tiro sur horizontala rulebla vico GLITAS la navigan
 * tirkeston: kiam la vico estas ĉe sia komenco kaj ne povas ruli pli reen, la
 * resto de la tiro puŝas deltojn rekte en la `anchoredDraggableState` de la
 * tirkesto (publica nur en nia forkigo, dk.nordfalk.compose.material3) — ĝi do
 * sekvas la fingron en ambaŭ direktoj. Kiam la gesto finiĝas, la restanta
 * flug-rapido solvas la staton ĉe la cel-ankro — kun la samaj sojloj kiel la
 * propra gesto de la tirkesto (pozicia sojlo 50 %, rapida sojlo 400 dp/s).
 *
 * La modifilo devas esti sur la rulebla elemento mem (ekz. `LazyRow`): la interna
 * rul-mekanismo disponigas la deltojn al la `nestedScroll`-konekto.
 */
@Composable
fun Modifier.tirkestoJeTroRulo(
    tirkestStato: DrawerState,
    vicoStato: LazyListState,
): Modifier {
    val konekto = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val ŝovumo = tirkestStato.anchoredDraggableState
                val ofseto = ŝovumo.offset
                val fermitaAnkro = ŝovumo.anchors.positionOf(DrawerValue.Closed)
                if (ofseto.isNaN() || fermitaAnkro.isNaN()) return Offset.Zero

                // Jam tirita per ĉi tiu gesto: sekvu la fingron ankaŭ reen (maldekstren)
                val estasTirita = ofseto > fermitaAnkro + 0.5f
                // Alie: komencu nur kiam la vico ne povas ruli pli reen kaj la tiro iras dekstren
                if (!estasTirita && (vicoStato.canScrollBackward || available.x <= 0f)) return Offset.Zero

                val konsumita = ŝovumo.dispatchRawDelta(available.x)
                return Offset(konsumita, 0f)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val ŝovumo = tirkestStato.anchoredDraggableState
                val ofseto = ŝovumo.offset
                val fermitaAnkro = ŝovumo.anchors.positionOf(DrawerValue.Closed)
                if (ofseto.isNaN() || fermitaAnkro.isNaN()) return Velocity.Zero

                if (ofseto > fermitaAnkro + 0.5f) {
                    // La gesto tiris la tirkeston — solvu ĝin per la restanta flug-rapido,
                    // kun la sojloj de la vera tirkesta gesto
                    logi("Tiro", "tro-tiro finita: solvas la tirkeston (ofseto=$ofseto, rapido=${available.x})")
                    @Suppress("DEPRECATION") // rapido-konscia settle — kiel la tirkesta gesto mem
                    ŝovumo.settle(available.x)
                }
                return Velocity.Zero
            }
        }
    }
    return this.nestedScroll(konekto)
}
