package dk.nordfalk.esperanto.desktop

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ilo por kompari la kanalkonfiguron kun https://esperanto-radio.com/radio.txt.
 *
 * Respondas tri demandojn:
 * 1. Al kiuj kanaloj mankas informoj de radio.txt?
 * 2. Ĉu estas kanaloj kie radio.txt havas elsendojn kiujn ni ne havas?
 * 3. Por ĉiu kanalo, ĉu elsendoj mankas?
 *
 * Rulu per: ./gradlew :desktopApp:radioTxtKomparilo
 */
fun main() {
    val RADIO_TXT_URL = "https://esperanto-radio.com/radio.txt"

    // 1. Legu la kanalkonfiguron
    val configTeksto = object {}.javaClass.classLoader
        .getResourceAsStream("esperantoradio_kanaloj_v9.json")!!
        .bufferedReader().readText()
    val agordo = KanalAgordoLeganto().legu(configTeksto)
    val kanaloj = agordo.kanaloj.map { it.alKanalo() }

    println("=== Radio.txt Komparilo ===")
    println("Kanaloj en konfiguro: ${kanaloj.size}")

    // 2. Elŝutu radio.txt
    println("\nElŝutas radio.txt de $RADIO_TXT_URL ...")
    val radioTxt = httpGet(RADIO_TXT_URL)
    println("radio.txt: ${radioTxt.length} signoj, ${radioTxt.count { it == '\n' }} linioj")

    // 3. Parse radio.txt
    val radioTxtEroj = parseRadioTxt(radioTxt)
    println("Eroj en radio.txt: ${radioTxtEroj.size}")

    // 4. Group by channel name
    val radioTxtPerKanalo = radioTxtEroj.groupBy { it.kanalNomo }
    println("Kanaloj en radio.txt: ${radioTxtPerKanalo.size}")
    radioTxtPerKanalo.toSortedMap().forEach { (nomo, eroj) ->
        val datoj = eroj.map { it.dato }.toSet().sorted()
        println("  $nomo — ${eroj.size} elsendoj, datoj: ${datoj.first()}..${datoj.last()}")
    }

    // 5. Build matching map: normalized(nomo) → slug
    val normNomoAlSlug = mutableMapOf<String, String>()
    val slugAlKanalo = mutableMapOf<String, Kanalo>()
    for (k in kanaloj) {
        normNomoAlSlug[normaliguNomon(k.nomo)] = k.slug
        slugAlKanalo[k.slug] = k
    }

    // 6. Match radio.txt channels to config slugs
    val radioTxtAlSlug = mutableMapOf<String, String>()
    for (nomo in radioTxtPerKanalo.keys) {
        val slug = normNomoAlSlug[normaliguNomon(nomo)]
        if (slug != null) radioTxtAlSlug[nomo] = slug
    }

    // === Demando 1: Kanaloj en nia konfiguro kiuj NE estas en radio.txt ===
    println("\n" + "=".repeat(60))
    println("1. KANALJ SEN INFORMOJ DE radio.txt")
    println("=".repeat(60))
    val matchitajSlugs = radioTxtAlSlug.values.toSet()
    val mankantaj = kanaloj.filter { it.slug !in matchitajSlugs }
    mankantaj.forEach { k ->
        val fonto = k.podkastaRssUrl ?: "neniu RSS"
        println("  ${k.slug.padEnd(22)} ${k.nomo.padEnd(25)} $fonto")
    }
    println("Totalo: ${mankantaj.size} el ${kanaloj.size} kanaloj")

    // Kanaloj en radio.txt kiuj NE estas en nia konfiguro
    val neKonataj = radioTxtPerKanalo.keys.filter { it !in radioTxtAlSlug }
    if (neKonataj.isNotEmpty()) {
        println("\n  ATVPA: kanaloj en radio.txt ne en nia konfiguro:")
        neKonataj.forEach { println("    $it (${radioTxtPerKanalo[it]!!.size} elsendoj)") }
    }

    // === Demandoj 2 & 3: Komparo de elsendoj per kanalo ===
    println("\n" + "=".repeat(60))
    println("2-3. KOMPARO DE ELSENDOJ PER KANALO")
    println("=".repeat(60))

    var totalMankantaj = 0
    for ((radioTxtNomo, slug) in radioTxtAlSlug.toSortedMap()) {
        val kanalo = slugAlKanalo[slug]!!
        val rtEroj = radioTxtPerKanalo[radioTxtNomo]!!

        println("\n--- ${kanalo.slug} (${kanalo.nomo}) ---")
        println("  radio.txt: ${rtEroj.size} elsendoj")

        val rssUrl = kanalo.podkastaRssUrl
        if (rssUrl == null) {
            println("  NENIU RSS-URL en konfiguro")
            println("  >> radio.txt havas ${rtEroj.size} elsendojn, kiujn ni ne povas kompari")
            println("  >> CIUJ ${rtEroj.size} elsendoj eble mankas (neniu RSS-fonto)")
            totalMankantaj += rtEroj.size
            continue
        }

        print("  Elŝutas RSS de $rssUrl ... ")
        val rssTeksto = try {
            httpGet(rssUrl)
        } catch (e: Exception) {
            println("ERARO: ${e.message}")
            println("  >> Ne eblas kompari")
            continue
        }
        println("${rssTeksto.length} signoj")

        val rssElsendoj = runBlocking {
            RssParsilo().parsuRss(rssTeksto, kanalo) { url ->
                try { httpGet(url) } catch (e: Exception) { "" }
            }
        }
        println("  RSS-parsita: ${rssElsendoj.size} elsendoj")

        if (rssElsendoj.isEmpty()) {
            println("  >> RSS donis NENIUJN elsendojn — radio.txt havas ${rtEroj.size}")
            totalMankantaj += rtEroj.size
            continue
        }

        // Komparu: por ĉiu radio.txt-ero, ĉu ĝia dato ekzistas en RSS?
        val rssDatUrl = rssElsendoj.flatMap { listOf(it.dato, it.fluo) }.toSet()
        val rssDatMallongigita = rssElsendoj.map { it.dato }.toSet()

        val mankantajEroj = rtEroj.filter { ero ->
            // Manhavas se la dato NE estas en RSS kaj la URL NE estas en RSS
            ero.dato !in rssDatMallongigita && ero.url !in rssDatUrl
        }

        if (mankantajEroj.isEmpty()) {
            println("  >> CIUJ radio.txt-elsendoj trovitaj en RSS — NENIO MANKAS")
        } else {
            println("  >> MANKAS ${mankantajEroj.size} elsendoj:")
            mankantajEroj.forEach { ero ->
                println("     ${ero.dato} — ${ero.titolo.take(80)}")
                println("        ${ero.url}")
            }
            totalMankantaj += mankantajEroj.size
        }
    }

    println("\n" + "=".repeat(60))
    println("RESUMO")
    println("=".repeat(60))
    println("  Kanaloj en konfiguro:        ${kanaloj.size}")
    println("  Kanaloj en radio.txt:         ${radioTxtPerKanalo.size}")
    println("  Kanaloj matchitaj:            ${radioTxtAlSlug.size}")
    println("  Kanaloj ne en radio.txt:      ${mankantaj.size}")
    println("  Totalo mankantaj elsendoj:    $totalMankantaj")
}

// === Helpiloj ===

data class RadioTxtEro(
    val kanalNomo: String,
    val dato: String,
    val url: String,
    val titolo: String,
)

/**
 * Parsas radio.txt-formaton.
 *
 * Format: unua linio estas tempmarko (A<numeroj>), poste blokoj apartigitaj
 * per malplenaj linioj. Ĉiu bloko:
 *   linio 1: kanalnomo
 *   linio 2: dato (yyyy-MM-dd)
 *   linio 3: MP3-URL
 *   linio 4+: priskribo/titolo (povas esti pluraj linioj)
 */
fun parseRadioTxt(teksto: String): List<RadioTxtEro> {
    val rezulto = mutableListOf<RadioTxtEro>()
    val blokoj = teksto.split(Regex("(\\r?\\n){2,}"))
    for (bloko in blokoj) {
        val linioj = bloko.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (linioj.size < 3) continue
        // Saltu kaplinion (tempmarko: A + ciferoj)
        if (linioj[0].matches(Regex("A\\d+"))) continue
        val kanalNomo = linioj[0]
        val dato = linioj[1]
        val url = linioj[2]
        val titolo = if (linioj.size > 3) linioj.subList(3, linioj.size).joinToString(" ") else ""
        if (!dato.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) continue
        if (!url.startsWith("http")) continue
        rezulto.add(RadioTxtEro(kanalNomo, dato, url, titolo))
    }
    return rezulto
}

/**
 * Normaligas kanalnomon por kongruo: minuskligas, forigas spacojn kaj
 * ne-ASCII-literojn, forigas komojn.
 */
fun normaliguNomon(nomo: String): String = nomo
    .lowercase()
    .replace(" ", "")
    .replace(",", "")
    .replace("ĉ", "c").replace("ĝ", "g").replace("ĵ", "j")
    .replace("ŝ", "s").replace("ŭ", "u")
    .replace("Ĉ", "c").replace("Ĝ", "g").replace("Ĵ", "j")
    .replace("Ŝ", "s").replace("Ŭ", "u")

/**
 * HTTP GET kun tempolimo (15s).
 */
fun httpGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 15000
    conn.readTimeout = 30000
    conn.setRequestProperty("User-Agent", "EsperantoRadio/RadioTxtKomparilo")
    try {
        return conn.inputStream.bufferedReader().readText()
    } finally {
        conn.disconnect()
    }
}
