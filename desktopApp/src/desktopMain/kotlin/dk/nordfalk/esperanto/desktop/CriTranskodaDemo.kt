package dk.nordfalk.esperanto.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Demonstra programo por la estonta CRI-peranto (transkoda servo).
 * Vidu docs/nova/07_cri_esperanto_kanalo.md.
 *
 * La programo montras la tutan servan logikon en unu rulado:
 *
 * 1. Demandas la (nedokumentitan) CRI-API-on `POST /api/getData` por la
 *    sekcioj aktualajo, LuciaStudio kaj eklubo.
 * 2. Kolektas la artikolojn kun sono (`isPlay: "1"`), ordigas ilin laŭ
 *    publikiga dato kaj prenas la 20 plej novajn.
 * 3. Eltiras la HLS-URL-on (m3u8) kaj transkodas per ffmpeg al MP3 64k
 *    (`ffmpeg -i <m3u8> -vn -c:a libmp3lame -b:a 64k`).
 * 4. Generas `cri_demo.rss` — validan RSS-2.0-fluon, kiun la apo povus rekte
 *    konsumi per la ĝenerala parsregulo 6.1 (`elsendojRssUrl`).
 *
 * Unu erara elsendo ne haltigas la programon (regulo 4: toleremo al
 * putrantaj fontoj) — la elsendo simple mankas en la RSS.
 *
 * Rulu per: JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
 *            ./gradlew :desktopApp:criTranskodaDemo
 *            (bezonas ankaŭ ffmpeg en $PATH; JDK 17 ĉar la defaŭlta
 *            sistema Java estas JRE 21 sen javac)
 * Argumentoj: [eligaDosierujo] [bazoUrlPorEnclosure]
 *             defaŭlte: build/cri-demo  kaj  https://ekzemplo.eo/cri
 */
private const val CRI_API = "https://esperanto.cri.cn/api/getData"
private val CRI_SEKCIOJ = listOf(
    "https://esperanto.cri.cn/aktualajo/page.shtml",
    "https://esperanto.cri.cn/LuciaStudio/page.shtml",
    "https://esperanto.cri.cn/eklubo/page.shtml",
)
private const val NOMBRO_DA_ELSENDOJ = 20
private val RETUMILA_IDENTIGO = "EsperantoRadio/CriTranskodaDemo (podkasta peranto; GPL)"

/** Artikolkarto el sekcipaĝo, kun sono. */
private data class CriKarto(
    val id: String,
    val sekcio: String,
    val link: String,
    val titolo: String,
    val brief: String,
    val publishedMs: Long,
    val m3u8: String?,
    val dauroSekundoj: Long?,
)

/** Finpretigita elsendo por la RSS. */
private data class CriElsendo(
    val karto: CriKarto,
    val mp3Dosiero: File,
    val grandecoBajtoj: Long,
    val dauroSekundoj: Long?,
)

fun main(args: Array<String>) {
    val eligujo = File(args.getOrNull(0) ?: "build/cri-demo")
    val bazoUrl = (args.getOrNull(1) ?: "https://ekzemplo.eo/cri").trimEnd('/')
    val sonujo = File(eligujo, "sonoj")

    println("=== CRI Transkoda Demo ===")
    println("Eligujo: ${eligujo.absolutePath}")
    println("Bazo-URL por enclosure: $bazoUrl\n")

    // 1. Kolektu la kartojn el la sekcioj
    val kartoj = mutableMapOf<String, CriKarto>() // id → karto (dedupe)
    for (sekcio in CRI_SEKCIOJ) {
        val etikedo = sekcioEtikedo(sekcio)
        print("Demandas [$etikedo] $sekcio ... ")
        try {
            val json = httpPostJson(CRI_API, "{\"id\":\"$sekcio\"}")
            val trovitaj = kolektuKartojnKunSono(json, etikedo)
            println("${trovitaj.size} kun sono")
            // Dedupe: artikolo povas aperi en pluraj sekcioj — gardu la unuan
            // (ekz. ĉiutagaj novaĵoj aperas kaj en aktualajo kaj en LuciaStudio;
            // la etikedo de la unua sekcio estas la plej ĝusta)
            for (k in trovitaj) if (k.id !in kartoj) kartoj[k.id] = k
        } catch (e: Exception) {
            println("ERARO: ${e.message} — daŭrigas sen tiu sekcio")
        }
    }
    println("\nSume ${kartoj.size} unikaj elsendoj kun sono.")

    // 2. Prenu la 20 plej novajn
    val elektitaj = kartoj.values.sortedByDescending { it.publishedMs }.take(NOMBRO_DA_ELSENDOJ)

    // 3. Transkodigu ĉiun al MP3
    sonujo.mkdirs()
    val elsendoj = mutableListOf<CriElsendo>()
    for ((i, karto) in elektitaj.withIndex()) {
        print("[${i + 1}/${elektitaj.size}] ${karto.sekcio}: ${karto.titolo.take(46)} ... ")
        val mp3 = File(sonujo, "${karto.id}.mp3")
        try {
            if (!mp3.exists()) {
                val m3u8 = karto.m3u8 ?: leguM3u8DeArtikolo(karto.link)
                    ?: throw IllegalStateException("neniu m3u8 trovita")
                transkodigu(m3u8, mp3, karto.titolo)
            } else {
                print("(jam en kaŝmemoro) ")
            }
            val dauro = karto.dauroSekundoj ?: ffprobeDauro(mp3)
            elsendoj.add(CriElsendo(karto, mp3, mp3.length(), dauro))
            println("OK ${mp3.length() / 1024} KB, ${dauro ?: "?"} s")
        } catch (e: Exception) {
            println("MALSUKCESIS: ${e.message} — saltas tiun elsendon")
        }
    }

    // 4. Generu la RSS-fluon
    val rss = generuRss(elsendoj, bazoUrl)
    File(eligujo, "cri_demo.rss").writeText(rss)
    println("\nGeneris ${File(eligujo, "cri_demo.rss")} (${elsendoj.size} elsendoj)")

    // 5. Resumo: kiel la datumoj aspektus por la apo
    println("\n=== Resumo de la datumoj ===")
    for (e in elsendoj) {
        println(
            "  ${e.karto.sekcio.padEnd(12)} " +
                "${e.karto.link.takeLast(24).padEnd(24)} " +
                "${e.karto.titolo.take(45).padEnd(45)} " +
                "${formatuDaton(e.karto.publishedMs)} " +
                "${(e.dauroSekundoj ?: 0).toString().padStart(4)} s " +
                "${(e.grandecoBajtoj / 1024).toString().padStart(5)} KB"
        )
    }
    println(
        "\nKiel la apo konsumus tion: nova kanalo en esperantoradio_kanaloj_v9.json\n" +
            "kun \"elsendojRssUrl\": \"<servo>/cri_demo.rss\" — la ĝenerala parsregulo 6.1,\n" +
            "ludado, elŝutoj kaj ludvico tiam funkcias sen iu ajn koda ŝanĝo en la apo."
    )
}

// === 1-a paŝo: kolekti kartojn ===

/**
 * Rekurzive trairas la paĝ-JSON-on kaj redonas ĉiujn `card`-objektojn kiuj
 * reprezentas artikolon kun sono: isPlay=1 kaj ligilo al artikolo (/202jaro/...).
 */
private fun kolektuKartojnKunSono(jsonTeksto: String, sekcioEtikedo: String): List<CriKarto> {
    val radiko = Json.parseToJsonElement(jsonTeksto).jsonObject
    val kartoj = mutableListOf<JsonObject>()
    fun trairi(elemento: JsonElement) {
        when (elemento) {
            is JsonObject -> {
                val karto = elemento["card"]
                if (karto is JsonObject) kartoj.add(karto)
                for (filo in elemento.values) trairi(filo)
            }
            is JsonArray -> for (filo in elemento) trairi(filo)
            else -> {}
        }
    }
    trairi(radiko)

    return kartoj.mapNotNull { karto ->
        val link = karto["link"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val isPlay = karto["isPlay"]?.jsonPrimitive?.content
        if (isPlay != "1" || !Regex("""/20\d\d/\d\d/\d\d/""").containsMatchIn(link)) return@mapNotNull null

        val video = (karto["video"] as? JsonObject)
        CriKarto(
            id = karto["id"]?.jsonPrimitive?.content ?: link.substringAfterLast('/'),
            sekcio = sekcioEtikedo,
            link = link,
            titolo = karto["title"]?.jsonPrimitive?.content?.trim().orEmpty(),
            brief = karto["brief"]?.jsonPrimitive?.content?.trim().orEmpty(),
            publishedMs = karto["date"]?.jsonPrimitive?.content?.toLongOrNull()
                ?: karto["modifyDate"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            m3u8 = video?.get("url")?.jsonPrimitive?.content?.takeIf { it.endsWith(".m3u8") },
            dauroSekundoj = video?.get("duration")?.jsonPrimitive?.content
                ?.toDoubleOrNull()?.toLong()?.takeIf { it > 0 },
        )
    }
}

/** Legas la artikolon mem kaj eltiras la HLS-URL-on (necesas por kelkaj artikoloj). */
private fun leguM3u8DeArtikolo(artikolUrl: String): String? {
    val json = httpPostJson(CRI_API, "{\"id\":\"$artikolUrl\"}")
    val radiko = Json.parseToJsonElement(json).jsonObject
    val result = radiko["result"]?.jsonObject ?: return null

    // (a) el la video-objekto
    (result["video"] as? JsonObject)?.get("url")?.jsonPrimitive?.content
        ?.takeIf { it.endsWith(".m3u8") }?.let { return it }

    // (b) el la enhavo: <video src=…> aŭ rekte m3u8-URL en la HTML
    val enhavo = result["content"]?.jsonPrimitive?.content ?: return null
    return Regex("""https?://[^\s"'<>\\]+\.m3u8""").find(enhavo)?.value
}

// === 3-a paŝo: transkodigi ===

/**
 * Se la m3u8 estas mastra ludlisto kun pluraj daŭrkokurtaj variantoj,
 * elektas la varianton kun la plej malalta BANDWIDTH.
 *
 * Ni bezonas nur la sonon; ffmpeg defaŭlte elektus la plej altan varianton
 * (ekz. 2000 kbps ≈ 180 MB por 12-minuta programo), kio malrapidigas la
 * elŝuton sen ia sonkvalita gajno. La plej malalta varianto (600 kbps) portas
 * la saman AAC-sontrakon.
 */
private fun elektuPlejMalaltanVarianton(m3u8: String): String {
    val teksto = try { leguLudliston(m3u8) } catch (e: Exception) { return m3u8 }
    if (!teksto.contains("#EXT-X-STREAM-INF")) return m3u8

    var plejBona: Pair<Long, String>? = null // (bendlarĝo, URL)
    val linioj = teksto.lines()
    for ((i, linio) in linioj.withIndex()) {
        if (!linio.startsWith("#EXT-X-STREAM-INF")) continue
        val bendlargho = Regex("BANDWIDTH=(\\d+)").find(linio)?.groupValues?.get(1)?.toLong() ?: continue
        val variajtoUrl = linioj.getOrNull(i + 1)?.trim() ?: continue
        if (variajtoUrl.startsWith("#") || variajtoUrl.isEmpty()) continue
        if (plejBona == null || bendlargho < plejBona.first) {
            plejBona = bendlargho to variajtoUrl
        }
    }
    // La variant-URL estas relativa al la mastra ludlisto
    return plejBona?.second?.let { URL(m3u8).toURI().resolve(it).toString() } ?: m3u8
}

/** HTTP GET (por legi HLS-ludlistojn). Alinomita ĉar httpGet jam ekzistas en RadioTxtKomparilo.kt (sama pako). */
private fun leguLudliston(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 15000
    conn.readTimeout = 30000
    conn.setRequestProperty("User-Agent", RETUMILA_IDENTIGO)
    try {
        return conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

/**
 * Transkodas HLS-fluon al MP3 per ffmpeg.
 * `-vn` forĵetas la videon (ni bezonas nur la sontrakan AAC-on), do la
 * rekodado estas rapida (ĉ. 10–30× realtempe).
 */
private fun transkodigu(m3u8: String, eligo: File, titolo: String) {
    val fluo = elektuPlejMalaltanVarianton(m3u8)
    val proceso = ProcessBuilder(
        "ffmpeg", "-hide_banner", "-loglevel", "error",
        "-user_agent", RETUMILA_IDENTIGO,
        "-i", fluo,
        "-vn", "-c:a", "libmp3lame", "-b:a", "64k",
        "-metadata", "title=$titolo",
        "-y", eligo.absolutePath,
    ).redirectErrorStream(true).start()
    try {
        // 15 min anstataŭ 5: longaj programoj (~12 min) el la CRI-CDN povas
        // esti malrapidaj; reala servilo antaŭgenerus per cron sen tempolimo.
        if (!proceso.waitFor(15, TimeUnit.MINUTES)) {
            proceso.destroyForcibly()
            // Ne lasu partan dosieron en la kaŝmemoro — la kaŝmemoro estas
            // "se la dosiero ekzistas, ĝi estas kompleta".
            eligo.delete()
            throw IllegalStateException("ffmpeg tempolimis (5 min)")
        }
        if (proceso.exitValue() != 0) {
            eligo.delete()
            throw IllegalStateException("ffmpeg eliris kun kodo ${proceso.exitValue()}")
        }
    } finally {
        proceso.inputStream.close()
    }
}

/** Legas la daŭron de MP3-dosiero per ffprobe (en sekundoj). */
private fun ffprobeDauro(dosiero: File): Long? {
    val proceso = ProcessBuilder(
        "ffprobe", "-v", "error", "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1", dosiero.absolutePath,
    ).redirectErrorStream(true).start()
    return try {
        proceso.waitFor(30, TimeUnit.SECONDS)
        proceso.inputStream.bufferedReader().readText().trim().toDoubleOrNull()?.toLong()
    } finally {
        proceso.inputStream.close()
    }
}

// === 4-a paŝo: RSS-generado ===

/**
 * Generas RSS-2.0-fluon kongruan kun la ĝenerala parsregulo 6.1:
 * `item` kun title, description, pubDate, guid, enclosure, itunes:duration,
 * itunes:image. `pubDate` estas RFC-1123-formato, kiun `RssParsilo.normigiDaton`
 * komprenas ("25 Sep 2026" → "2026-09-25").
 */
private fun generuRss(elsendoj: List<CriElsendo>, bazoUrl: String): String {
    val eroj = elsendoj.joinToString("\n") { e ->
        val dauro = e.dauroSekundoj?.let { formatuDauren(it) } ?: ""
        """
        |<item>
        |  <title>${xmlEstigu(e.karto.titolo)}</title>
        |  <description>${xmlEstigu(e.karto.brief)}</description>
        |  <link>${xmlEstigu(e.karto.link)}</link>
        |  <guid isPermaLink="false">${xmlEstigu(e.karto.id)}</guid>
        |  <pubDate>${formatuDaton(e.karto.publishedMs)}</pubDate>
        |  <enclosure url="${xmlEstigu("$bazoUrl/sonoj/${e.karto.id}.mp3")}" length="${e.grandecoBajtoj}" type="audio/mpeg"/>
        |  <itunes:duration>$dauro</itunes:duration>
        |  <itunes:image href="${xmlEstigu("$bazoUrl/cri.png")}"/>
        |</item>""".trimMargin()
    }

    return """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:content="http://purl.org/rss/1.0/modules/content/">
<channel>
  <title>CRI — Ĉina Radio Internacia (Esperanto)</title>
  <link>https://esperanto.cri.cn/</link>
  <language>eo</language>
  <description>Ĉiutagaj Esperanto-elsendoj de Ĉina Radio Internacia, transkodataj el HLS al MP3 far la CRI-peranto.</description>
  <image><url>${xmlEstigu("$bazoUrl/cri.png")}</url><title>CRI</title><link>https://esperanto.cri.cn/</link></image>
$eroj
</channel>
</rss>
"""
}

// === Helpiloj ===

/**
 * Mallonga etikedo por sekci-paĝo — montras en la konzola eligo de kiu
 * paĝo ĉiu elsendo venas (ekz. "Aktuala", "LuciaStudio", "E-klubo").
 */
private fun sekcioEtikedo(sekcioUrl: String): String {
    val vojo = sekcioUrl.substringBefore("/page.shtml").substringAfterLast('/')
    return when (vojo.lowercase()) {
        "aktualajo" -> "Aktuala"
        "luciastudio" -> "LuciaStudio"
        "eklubo" -> "E-klubo"
        else -> vojo.ifEmpty { sekcioUrl }
    }
}

/** HTTP POST kun JSON-korpo (tiel funkcias la CRI-API). */
private fun httpPostJson(url: String, korpo: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 15000
    conn.readTimeout = 30000
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("User-Agent", RETUMILA_IDENTIGO)
    try {
        conn.outputStream.use { it.write(korpo.toByteArray(Charsets.UTF_8)) }
        val kodo = conn.responseCode
        if (kodo != 200) {
            throw IllegalStateException("HTTP $kodo: ${conn.responseMessage}")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

/** RFC-1123: ekz. "Fri, 25 Sep 2026 10:23:02 GMT" (kongrua kun RssParsilo.normigiDaton). */
private fun formatuDaton(epokoMs: Long): String =
    Instant.ofEpochMilli(epokoMs).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)

/** itunes:duration kiel HH:MM:SS (tio, kion RssParsilo.leguDauron komprenas). */
private fun formatuDauren(sekundoj: Long): String =
    "%d:%02d:%02d".format(sekundoj / 3600, sekundoj / 60 % 60, sekundoj % 60)

private fun xmlEstigu(teksto: String): String = teksto
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")
