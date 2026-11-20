package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import java.io.File

/**
 * Skribas RSS 2.0-fluon kun iTunes-namespace. La priskriboj povas esti HTML
 * (AI-rezultoj). Fazo 1: simpla rekta XML-skribado (neniu ekstera biblioteko).
 */
object FluSkribilo {

    /**
     * Skribas la riĉigitan fluon por unu kanalo.
     * [priskriboj] estas HTML (la rezumoj estas konvertitaj per `analizuRezumon`)
     * kaj [titoloj] povas anstataŭigi la titolon de unuopa elsendo (laŭ id).
     */
    fun skribu(
        kanalo: Kanalo,
        elsendoj: List<Elsendo>,
        priskriboj: Map<String, String>,
        titoloj: Map<String, String>,
        celo: File,
    ) {
        celo.parentFile.mkdirs()
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("""<rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:podcast="https://podcastindex.org/namespace/1.0" xml:lang="eo">""").append('\n')
        val kanaloPriskribo = kanalo.priskribo
        val retejo = kanalo.retejoUrl ?: kanalo.podkastaRssUrl
        val emblemo = kanalo.emblemoUrl
        sb.append("  <channel>\n")
        xml(sb, "    ", "title", kanalo.nomo)
        if (kanaloPriskribo != null) xml(sb, "    ", "description", kanaloPriskribo)
        if (retejo != null) xml(sb, "    ", "link", retejo)
        if (emblemo != null) {
            sb.append("    <itunes:image href=").append(citaĵo(emblemo)).append("/>\n")
        }
        for (e in elsendoj) {
            val priskribo = priskriboj[e.id] ?: e.priskriboHtml ?: e.priskribo
            val retpaĝo = e.retpaĝoUrl
            val daŭro = e.daŭro
            sb.append("    <item>\n")
            xml(sb, "      ", "title", titoloj[e.id] ?: e.titolo)
            xml(sb, "      ", "guid", e.id)
            xml(sb, "      ", "pubDate", rfc822(e.dato))
            if (priskribo != null) xml(sb, "      ", "description", priskribo)
            if (retpaĝo != null) xml(sb, "      ", "link", retpaĝo)
            sb.append("      <enclosure url=").append(citaĵo(e.fluo)).append(" type=\"audio/mpeg\"/>\n")
            if (daŭro != null && daŭro > 0) {
                sb.append("      <itunes:duration>%02d:%02d:%02d</itunes:duration>\n".format(daŭro / 3600, (daŭro / 60) % 60, daŭro % 60))
            }
            sb.append("    </item>\n")
        }
        sb.append("  </channel>\n</rss>\n")
        celo.writeText(sb.toString())
    }

    private fun xml(sb: StringBuilder, krommarĝeno: String, etikedo: String, teksto: String) {
        sb.append(krommarĝeno).append('<').append(etikedo).append('>')
            .append("<![CDATA[").append(teksto.replace("]]>", "]] >")).append("]]>")
            .append("</").append(etikedo).append(">\n")
    }

    private fun citaĵo(s: String): String =
        "\"" + s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;") + "\""

    /** yyyy-MM-dd → RFC-822 proksimuma (tagmeze UTC; la preciza horo estas en la originala fluo). */
    private fun rfc822(dato: String): String {
        return try {
            val p = dato.split("-")
            val loka = java.time.LocalDate.of(p[0].toInt(), p[1].toInt(), p[2].toInt())
            // RFC-822 stilo: "Wed, 18 Nov 2026 …" (ne GRANDlitere — kelkaj parsiloj
            // distingas la monaton laŭ uskleco)
            val tago = loka.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                .replace(".", "")
            val monato = loka.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                .replace(".", "")
            "$tago, ${loka.dayOfMonth} $monato ${loka.year} 12:00:00 +0000"
        } catch (e: Exception) {
            "Thu, 01 Jan 1970 12:00:00 +0000"
        }
    }
}
