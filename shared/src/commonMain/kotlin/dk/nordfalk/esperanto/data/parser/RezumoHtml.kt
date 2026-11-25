package dk.nordfalk.esperanto.data.parser

/**
 * Konvertas platajn AI-rezumojn al HTML, same kiel la priskriboj de aliaj
 * elsendoj kutime estas — tiel la rezulto estas legebla en la detala ekrano
 * (kiu montras `priskriboHtml`; plata teksto kun `\n` kolapsus al unu bloko).
 *
 * Enigo (la rezumo-kaŝo de la servilo, plata teksto):
 * - nedeviga unua linio `Titolo: …` — titolo de la elsendo (forigita el la priskribo);
 * - poste unua linio = la temo (resumo de la sekvaj punktoj), sekvaj linioj = punktoj;
 *   ĉe `unuaLinioTitolo=true` la unua linio iĝas `<p><strong>…</strong></p>` (titolo-stila)
 *   kaj la punktoj `<ul><li>` (linioj kun `•`/`-`/`*`-markoj simple iĝas `<li>`);
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
fun analizuRezumon(
    rezumoKruda: String,
    originalaTitolo: String,
    avertaFrazo: String,
    /** true por strukturitaj rezumoj el la rezumo-kaŝo (unua linio = temo, poste punktoj). */
    unuaLinioTitolo: Boolean = false,
): RezumoAnalizita {
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
    return RezumoAnalizita(titolo, rezumoAlHtml(korpo, avertaFrazo, unuaLinioTitolo))
}

/**
 * Plata rezumo (linioj, blokoj apartigitaj per malplenaj linioj) → HTML:
 * - la averta frazo sola → `<p><em>…</em></p>`;
 * - bloko nur el bulmarkitaj linioj → `<ul><li>…</li></ul>`;
 * - se [unuaLinioTitolo] kaj la bloko havas almenaŭ 2 liniojn: unua linio →
 *   `<p><strong>…</strong></p>` (titolo-stila), la ceteraj → `<ul><li>`;
 * - alie → `<p>` kun linioj kunigitaj per `<br/>`.
 * HTML-specialaj signoj estas eskapitaj.
 */
fun rezumoAlHtml(linioj: List<String>, avertaFrazo: String, unuaLinioTitolo: Boolean = false): String {
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
            unuaLinioTitolo && blok.size >= 2 ->
                "<p><strong>${esc(blok.first())}</strong></p>\n<ul>\n" +
                    blok.drop(1).joinToString("\n") { "<li>${esc(it.replace(BULETA, ""))}</li>" } + "\n</ul>"
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

/**
 * Rekonas VERAN HTML-etikedon (konatan etikednomon) — ne nur ajnan «<litero»-on:
 * pura teksto kiel «5<n» (matematiko) tial ne erare traktiĝas kiel HTML,
 * dum «<p>…</p>» aŭ «<br/>» jes. RSS-priskriboj ne portas eksplicitan
 * format-informon, do rekono laŭ etiked-blanklisto estas la plej profunde
 * diegebla proksimumaĵo (la apo mem same distingas per sia parsilo).
 */
private val HTML_ETIKEDO = Regex(
    """</?(p|br|em|strong|b|i|u|s\b|ul|ol|li|a|div|span|h[1-6]|img|blockquote|code|pre|hr|table|thead|tbody|tr|td|th|sub|sup|strike)\b""",
    RegexOption.IGNORE_CASE,
)

/**
 * Unu parto de priskribo al HTML: pura teksto estas eskapata kaj envolvata en
 * `<p>` (liniosaltoj iĝas `<br/>`); teksto kiu jam estas HTML restas nedifektita
 * (la kanalo mem difinis la formatadon — ekz. `<em>`-a kursiva teksto).
 */
fun priskriboPartoAlHtml(teksto: String): String {
    val t = teksto.trim()
    if (t.isEmpty()) return ""
    return if (HTML_ETIKEDO.containsMatchIn(t)) t
    else "<p>" + esc(t).replace("\n", "<br/>") + "</p>"
}

/**
 * Kunigas la priskribon de elsendo kun AI-rezumo: unue la (eventuala) originala
 * priskribo, poste la kursivigita averto pri la AI-fonto, poste la rezumo-HTML.
 * La originala priskribo restas ĉiam videbla (la rezumo aldoniĝas, ne anstataŭas).
 */
fun kuniguPriskribonKunRezumo(
    originalaPriskribo: String?,
    avertaFrazo: String,
    rezumoHtml: String,
): String {
    val partoj = mutableListOf<String>()
    priskriboPartoAlHtml(originalaPriskribo.orEmpty()).takeIf { it.isNotEmpty() }?.let { partoj.add(it) }
    partoj.add("<p><em>${esc(avertaFrazo)}</em></p>")
    partoj.add(rezumoHtml)
    return partoj.joinToString("\n")
}
