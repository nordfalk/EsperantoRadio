package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudataElsendo

/**
 * Elektas kiun elsendon vekhorloĝo ludu por podkasta kanalo (sen livestream).
 *
 * Reguloj:
 * 1. Nur elsendoj kun sono-URL.
 * 2. La plej nova (laŭ dato) kiu ne estas finaŭskultita nek erara.
 * 3. Se ĉiuj estas aŭskultitaj: la plej nova entute — pli bone ol nur ringtono.
 *
 * @return null se ne estas ludebla elsendo (tiam: fallback-ringtono)
 */
fun elektuAlarmElsendon(elsendoj: List<Elsendo>, ludatoj: Map<String, LudataElsendo>): Elsendo? {
    val ordigitaj = elsendoj.filter { it.fluo.isNotBlank() }.sortedByDescending { it.dato }
    return ordigitaj.firstOrNull { ludatoj[it.id]?.let { l -> !l.finita && !l.erara } ?: true }
        ?: ordigitaj.firstOrNull()
}
