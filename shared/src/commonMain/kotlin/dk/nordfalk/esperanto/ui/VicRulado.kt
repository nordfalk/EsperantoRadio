package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Musa navigado de ruleblaj ujoj (retversio/Desktop, FAROTA C56-beta).
 *
 * La ruleblaj ujoj (`LazyColumn`/`LazyRow`) respondas al tuŝgestoj, sed en la
 * retumila konstruo de Compose la muso ne funkcias: la radeto havis neniun
 * efikon (neniu ligas ĝin al la ujoj) kaj la muso-treno ne movis ilin. Tiuj
 * adaptiloj aldonas ambaŭ — NUR por mus-montrilo (`PointerType.Mouse`), do la
 * tuŝa konduto de Android/poŝtelefona retumilo restas ĉe la denaskaj gestoj.
 *
 * Pas-intertrakto: la vicoj (`rulumuViconPerMuso`) konsumas la radet eventojn
 * en la Main-paso antaŭ ol la ĉirkaŭa kolumno (`rulumuKolumnonPerMuso`) vidu ilin
 * (Main-paso traktas la idojn unue) — la radeto super karta vico do movas la VICON
 * kaj la radeto inter la vicoj movas la kolumnon. La trenado nuligas la malleviĝon post vera
 * movo (> [TRENA_SOGLIMO]) por ke klakeblaj kartoj ne misprezenti kiel klakon.
 */
fun Modifier.rulumuViconPerMuso(stato: LazyListState): Modifier = this
    .pointerInput(stato) { musaTrenado(horizontal = true) { stato.scrollBy(it) } }
    .pointerInput(stato) { musaRadetado(horizontal = true) { stato.scrollBy(it) } }

/**
 * Musa navigado de la ĉefa VERTIKALA kolumno de ekrano (vidu [rulumuViconPerMuso]).
 * La radetoj venas al ĝi nur se neniom interna (ekz. karta vico) konsumis ilin.
 */
fun Modifier.rulumuKolumnonPerMuso(stato: LazyListState): Modifier = this
    .pointerInput(stato) { musaTrenado(horizontal = false) { stato.scrollBy(it) } }
    .pointerInput(stato) { musaRadetado(horizontal = false) { stato.scrollBy(it) } }

/** Maksimuma unuopa radet-rulumendo (px); ĉirkaŭ kvarono de kartolarĝo. */
private const val MAKS_RADA_DELTO = 120f

/** Suma movo (px) post kiu premado-malhalto validas kiel trenado, ne kiel klako. */
private const val TRENA_SOGLIMO = 12f

/**
 * Muso-treno (Initial-paso — antaŭ la idoj, por ke la fina konsumo haltu klakon).
 * [horizontal] elektas la movatan akson; nur la dominanta akso movas, por ke
 * horizontala trenado sur karta vico ne skuigu la kolumnon (kaj inverse).
 */
private suspend fun PointerInputScope.musaTrenado(
    horizontal: Boolean,
    rulumu: suspend (Float) -> Unit,
) {
    coroutineScope {
        val rulumendoj = Channel<Float>(Channel.UNLIMITED)
        val rulumanto = launch {
            for (kvanto in rulumendoj) {
                try {
                    // La enhavo sekvas la muson: treni antaŭen → rulumi reen.
                    rulumu(-kvanto)
                } catch (e: CancellationException) {
                    // Nuligo de la konkura scrollBy (MutatorMutex) ne mortigu la
                    // konsumanton — la kanalo alie kreskus sen leganto; nur la
                    // propra nuligo (forigo de la modifilo) disvastiĝu (#205).
                    coroutineContext.ensureActive()
                }
            }
        }
        try {
            awaitPointerEventScope {
                var premas = false
                var lastaX = 0f
                var lastaY = 0f
                var trenMezuro = 0f
                while (true) {
                    val evento = awaitPointerEvent(PointerEventPass.Initial)
                    val ŝanĝo = evento.changes.firstOrNull() ?: continue
                    // NUR muso — tuŝgestoj apartenas al la denaska traktilo (aliel la
                    // adaptilo duobligus ĉiun tuŝan trenadon sur Android).
                    if (ŝanĝo.type != PointerType.Mouse) continue
                    when (evento.type) {
                        PointerEventType.Press -> {
                            premas = true
                            lastaX = ŝanĝo.position.x
                            lastaY = ŝanĝo.position.y
                            trenMezuro = 0f
                        }
                        PointerEventType.Move -> if (premas && ŝanĝo.pressed) {
                            val deltoX = ŝanĝo.position.x - lastaX
                            val deltoY = ŝanĝo.position.y - lastaY
                            lastaX = ŝanĝo.position.x
                            lastaY = ŝanĝo.position.y
                            val delto = if (horizontal) {
                                if (abs(deltoX) > abs(deltoY)) deltoX else 0f
                            } else {
                                if (abs(deltoY) > abs(deltoX)) deltoY else 0f
                            }
                            trenMezuro += abs(delto)
                            if (abs(delto) >= 0.5f) rulumendoj.trySend(delto)
                        }
                        PointerEventType.Release, PointerEventType.Unknown -> {
                            if (premas && trenMezuro > TRENA_SOGLIMO) {
                                // Vera trenado — la elemento reagu NE kvazaŭ klakita.
                                evento.changes.forEach { it.consume() }
                            }
                            premas = false
                            trenMezuro = 0f
                        }
                        else -> Unit
                    }
                }
            }
        } finally {
            rulumendoj.close()
            rulumanto.cancel()
        }
    }
}

/**
 * Radeto (Main-paso — la idoj unue; nur nekonsumitaj eventoj pritraktiĝas).
 * La retumilo raportas rad-tiklojn kiel vertikalan delton (ĉ. ±100 px per
 * tiktako); horizontala radeto/tuŝplato venas per x-deltoj — la vico akceptas
 * ambaŭ, la kolumno nur vertikalan.
 */
private suspend fun PointerInputScope.musaRadetado(
    horizontal: Boolean,
    rulumu: suspend (Float) -> Unit,
) {
    coroutineScope {
        val rulumendoj = Channel<Float>(Channel.UNLIMITED)
        val rulumanto = launch {
            for (kvanto in rulumendoj) {
                try {
                    rulumu(kvanto)
                } catch (e: CancellationException) {
                    // kiel en musaTrenado — konkura anuligo ne mortigu la konsumanton
                    coroutineContext.ensureActive()
                }
            }
        }
        try {
            awaitPointerEventScope {
                while (true) {
                    val evento = awaitPointerEvent(PointerEventPass.Main)
                    val ŝanĝo = evento.changes.firstOrNull() ?: continue
                    if (evento.type != PointerEventType.Scroll || ŝanĝo.isConsumed) continue
                    val delto = ŝanĝo.scrollDelta.let {
                        if (horizontal) it.x + it.y else it.y
                    }
                    if (delto != 0f) {
                        evento.changes.forEach { it.consume() }
                        rulumendoj.trySend(delto.coerceIn(-MAKS_RADA_DELTO, MAKS_RADA_DELTO))
                    }
                }
            }
        } finally {
            rulumendoj.close()
            rulumanto.cancel()
        }
    }
}
