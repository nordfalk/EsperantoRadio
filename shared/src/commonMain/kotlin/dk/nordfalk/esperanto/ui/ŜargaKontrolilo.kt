package dk.nordfalk.esperanto.ui

import dk.nordfalk.esperanto.logd
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Dedupigo kaj aĝ-limo (TTL) por ŝargado.
 *
 * La ekranoj vokas `ŝargi()` je ĉiu eniro (LaunchedEffect) — sen kontrolo tio
 * reŝargas ĉion je ĉiu navigado (KMP-erar-esplorado 2026-10-20: ĝis 7 plenaj
 * ŝarĝoj en 90 sekundoj ĉe unu uzanto). Tiu ĉi kontrolilo:
 * - seriigas samtempajn vokojn (la dua atendas ĝis la unua finas kaj tiam
 *   preterlasas sin per la TTL-kontrolo);
 * - preterlasas la ŝargadon se la lasta rezulto estas pli freŝa ol [ttl];
 * - lasas [fortoRefresigi] (malsupren-tiro) ĉiam ruli.
 *
 * La lasta-fino-tempo estas protokolita nur post sukcesa fino de [ŝargado] —
 * fiaskinta ŝargado reproveblas je la sekva eniro.
 */
@OptIn(ExperimentalTime::class)
class ŜargaKontrolilo(
    private val ttl: Duration = 5.minutes,
    private val horloĝo: () -> Instant = { Clock.System.now() },
) {
    private val mutex = Mutex()
    private var lastasFino: Instant? = null

    suspend fun rulu(tag: String, fortoRefresigi: Boolean, ŝargado: suspend () -> Unit) {
        mutex.withLock {
            val lasta = lastasFino
            if (!fortoRefresigi && lasta != null && horloĝo() - lasta < ttl) {
                logd(tag, "Ŝargado preterlasita — lasta rezulto aĝas malpli ol $ttl")
                return
            }
            ŝargado()
            lastasFino = horloĝo()
        }
    }
}
