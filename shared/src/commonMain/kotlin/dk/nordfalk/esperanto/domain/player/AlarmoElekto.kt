package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudataElsendo

/**
 * Elektas kiun elsendon vekhorloĝo ludu por podkasta kanalo (sen tuja elsendfluo).
 *
 * Reguloj:
 * 1. Nur elsendoj kun sono-URL.
 * 2. La plej nova (laŭ dato) kiu ne estas finaŭskultita nek erara.
 * 3. Se ĉiuj estas aŭskultitaj: la plej nova NE-erara (PR #95) — reludado
 *    pli bonas ol silento, sed erara fluo tuj paneas kaj lasus nur la
 *    vokosonon post 10 s.
 * 4. Se ĉiuj ceteraj estas eraraj: null (la ricevilo ludas la vokosonon).
 *
 * @return null se ne estas ludebla elsendo (tiam: rezerva vokosono)
 */
fun elektuAlarmElsendon(elsendoj: List<Elsendo>, ludatoj: Map<String, LudataElsendo>): Elsendo? {
    val ordigitaj = elsendoj.filter { it.fluo.isNotBlank() }.sortedByDescending { it.dato }
    // 1. La plej nova kiu ne estas finaŭskultita nek erara (aŭ pri kiu ni neniam ludis)
    val unua = ordigitaj.firstOrNull { e ->
        ludatoj[e.id]?.let { l -> !l.finita && !l.erara } ?: true
    }
    if (unua != null) return unua
    // 2. Falilo (PR #95): ĉiuj estas aŭskultitaj/erara — la plej nova NE-erara:
    // reludado pli bonas ol erara fluo (erara tuj paneas → nur vokosono post 10 s)
    return ordigitaj.firstOrNull { e -> ludatoj[e.id]?.erara != true }
    // 3. Nur eraraj restas → null → la ricevilo ludas la vokosonon
}
