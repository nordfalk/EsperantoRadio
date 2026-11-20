package dk.nordfalk.esperanto.data.parser

/**
 * Konvertas platajn AI-rezumojn al HTML, same kiel la priskriboj de aliaj
 * elsendoj kutime estas — tiel la rezulto estas legebla en la detala ekrano
 * (kiu montras `priskriboHtml`; plata teksto kun `\n` kolapsus al unu bloko).
 *
 * Enigo (la rezumo-kaŝo de la servilo, plata teksto):
 * - nedeviga unua linio `Titolo: …` — titolo de la elsendo (forigita el la priskribo);
 * - linioj = punktoj; linioj komenciĝantaj per `•`/`-`/`*` iĝas `<li>`-oj;
 * - la lasta bloko estu la AI-averta frazo (iĝas `<em>`).
 *
 * Se mankas `Titolo:`-linio kaj la originala titolo enhavas daton (do estas
 * ripetiĝanta, ekz. „Esperanto Programo 18.11.2026“), la titolo estas derivita
 * el la unua punkto de la rezumo.
 *
 * Uzo: la arkiv-servilo (server/) vokas [analizuRezumon] antaŭ ol skribi la
 * riĉigitan fluon; la apo mem nur legas la pretan HTML el la fluo.
 */

/** Analizita rezumo: nedeviga nova titolo + HTML-priskribo. */
data class RezumoAnalizita(
    val titolo: String?,
    val priskriboHtml: String,
)

private val BULETA = Regex("^([•*\\-–—])\\s+")
private val DATO_EN_TITOLO = Regex("\\d{1,2}[.\\s]\\d{1,2}[.\\s]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}")
private const val MAKS_TITOLO_LONGO = 70
private const val MIN_TITOLO_LONGO = 8

/**
 * Analizias platan rezumon: elprenas la titolon (`Titolo:`-linio, aŭ derivita el
 * la unua punkto se la originala titolo enhavas daton) kaj konvertas la korpon al HTML.
 */
fun analizuRezumon(rezumoKruda: String, originalaTitolo: String, avertaFrazo: String): RezumoAnalizita {
    val linioj = rezumoKruda.lines().map { it.trim() }
    val unua = linioj.indexOfFirst { it.isNotEmpty() }
    var titolo: String? = null
    var korpo = linioj
    if (unua >= 0 && linioj[unua].startsWith("Titolo:")) {
        titolo = linioj[unua].removePrefix("Titolo:").trim().ifEmpty { null }
        korpo = linioj.filterIndexed { i, _ -> i != unua }
    }
    val enhavo = korpo.filter { it.isNotEmpty() }
    if (titolo == null && enhavo.isNotEmpty() && DATO_EN_TITOLO.containsMatchIn(originalaTitolo)) {
        titolo = derivuTitolon(enhavo.first(), originalaTitolo)
    }
    return RezumoAnalizita(titolo, rezumoAlHtml(korpo, avertaFrazo))
}

/**
 * Plata rezumo (linioj, blokoj apartigitaj per malplenaj linioj) → HTML:
 * - bloko nur el bulmarkitaj linioj → `<ul><li>…</li></ul>`;
 * - la averta frazo sola → `<p><em>…</em></p>`;
 * - alie → `<p>` kun linioj kunigitaj per `<br/>`.
 * HTML-specialaj signoj estas eskapitaj.
 */
fun rezumoAlHtml(linioj: List<String>, avertaFrazo: String): String {
    val blokoj = mutableListOf<List<String>>()
    var nuna = mutableListOf<String>()
    for (l in linioj.map { it.trim() }) {
        if (l.isEmpty()) {
            if (nuna.isNotEmpty()) { blokoj.add(nuna); nuna = mutableListOf() }
        } else nuna.add(l)
    }
    if (nuna.isNotEmpty()) blokoj.add(nuna)
    return blokoj.joinToString("\n") { blok ->
        when {
            blok.size == 1 && blok.first() == avertaFrazo ->
                "<p><em>${esc(avertaFrazo)}</em></p>"
            blok.all { BULETA.containsMatchIn(it) } ->
                "<ul>\n" + blok.joinToString("\n") { "<li>${esc(it.replace(BULETA, ""))}</li>" } + "\n</ul>"
            else ->
                "<p>" + blok.joinToString("<br/>") { esc(it) } + "</p>"
        }
    }
}

/**
 * Derivas pli informan titolon el la unua punkto de la rezumo:
 * prefere la frazo ĝis „: “ (la temo), alie tranĉo ĉe vorta limo (maks. 70 signoj).
 * Redonas null se la rezulto estus tro mallonga aŭ identa al la originalo.
 */
fun derivuTitolon(unuaLinio: String, originalaTitolo: String): String? {
    var t = unuaLinio.replace(BULETA, "").trim()
    if (t.isEmpty()) return null
    val dupunkto = t.indexOf(": ")
    if (dupunkto in MIN_TITOLO_LONGO..MAKS_TITOLO_LONGO) {
        // „Temo: detaloj…“ — prenu la temon
        t = t.take(dupunkto)
    } else if (t.length > MAKS_TITOLO_LONGO) {
        // tranĉu ĉe la lasta vorto/limo (spaco, komo, streko) ene de la limo
        val tranĉo = t.take(MAKS_TITOLO_LONGO + 1)
        var lasta = -1
        for (i in tranĉo.indices) {
            val c = tranĉo[i]
            if (c == ' ' || c == ',' || c == ';' || c == '–' || c == '—') lasta = i
        }
        t = if (lasta >= MIN_TITOLO_LONGO) tranĉo.take(lasta) else tranĉo.trim()
    }
    t = t.trim().trimEnd('.', '!', '?', ':', ';', ',')
    if (t.length < MIN_TITOLO_LONGO) return null
    t = t.replaceFirstChar { it.uppercaseChar() }
    return if (t == originalaTitolo) null else t
}

private fun esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
