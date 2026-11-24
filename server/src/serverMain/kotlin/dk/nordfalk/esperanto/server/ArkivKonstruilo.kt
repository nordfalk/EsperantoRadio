package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.parser.HtmlFontoParsilo
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.data.parser.analizuRezumon
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant

/**
 * Arkiv-konstruilo (fazo 1): ruliĝas regule (cron/systemd), elŝutas la podkastajn
 * fluojn, poste parsas ilin per la SAMA RssParsilo kiel la apo (unu loko por la
 * parsreguloj, regulo A2), riĉigas erojn sen priskribo per rezumo el kaŝita
 * transskribo (STT-kaŝo en server/data/privata/transkriptoj/ (privata, ne publikigita) kaj skribas la riĉigitajn
 * RSS-dosierojn sur disko: server/data/publika/kanaloj/<slug>.xml
 *
 * Uzo:
 *   ./gradlew :server:arkivKonstruilo                      — ĉiuj kanaloj
 *   ./gradlew :server:arkivKonstruilo --args="radiovatikana" — nur unu kanalo
 *   ./gradlew :server:arkivKonstruilo --args="--kaŝo radiovatikana" — el kaŝo (sen reto)
 */
object ArkivKonstruilo {

    private val RADIKO = File("server/data")
    private val PUBLIKA = File(RADIKO, "publika")
    private val PRIVATA = File(RADIKO, "privata")
    private val KONFIGO = File("shared/src/commonMain/resources/esperantoradio_konfiguro.json")
    // Sufiĉas por la arkivo: krome la konstruilo ĉesas frue kiam ĝi atingas la plej
    // novan elsendon jam en la arkivo (vidu kolektuElsendojn) — pli grandaj fluoj nur
    // ŝarĝus la dosierojn (kaj la app-klientojn) sen nova enhavo.
    private const val MAKS_PAĜOJ = 10
    // HTML-fontoj (mediatheko sen RSS, ekz. radiofrei): pli da paĝoj kaj eroj ol RSS
    private const val MAKS_HTML_PAĜOJ = 30
    private const val MAKS_HTML_EROJ = 300

    private val rezumMotoro: RezumMotoro = EkstraktaRezumMotoro()

    /**
     * Rezulto de elŝutuEmblemojn: la URL-mapigo por la resskribo plus la nova
     * dosiernomo po kanalo-slug. La purigado de arkaikaj speguloj bezonas ĝin
     * (ĝi okazas POSTE, en la ĉefa ciklo — vidu puriguArkaikajnEmblemojn).
     */
    private data class EmblemaRezulto(
        val mapigo: Map<String, String>,
        val novaNomoLaŭSlug: Map<String, String>,
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val nurTiuj = args.filter { !it.startsWith("--") }.toSet()
        val uzuKaŝon = "--kaŝo" in args

        val agordo = KanalAgordoLeganto().legu(KONFIGO.readText())
        val ĉiujKanaloj = agordo.kanaloj.map { it.alKanalo() }
        val kanaloj = ĉiujKanaloj
            .filter { it.havasPodkastojn }
            .filter { nurTiuj.isEmpty() || it.slug in nurTiuj }

        println("Arkiv-konstruilo: ${kanaloj.size} podkastaj kanaloj (${Instant.now()})")

        val http = HttpClient(CIO)
        val parsilo = RssParsilo()
        var stato = StatoLeganto.legu(File(PRIVATA, "stato.json"))
        val novajStatoj = stato.kanaloj.toMutableMap()

        // FAROTA C56: spegulu la emblemojn al publika/bildoj/ kaj resskribu iliajn URL-ojn
        // en la publikigota konfiguro (kaj en la fluoj) al la arkivaj URL-oj — la
        // retversio gastigas samdevene kaj ne povas ŝargi bildojn el la originalaj gastigantoj (CORS).
        val emblemoj = elŝutuEmblemojn(ĉiujKanaloj, http, agordo.arkivoBazoUrl)

        for (kanalo in kanaloj) {
            try {
                val elsendoj = if (kanalo.podkastaRssUrl.isNullOrBlank() && !kanalo.htmlFontoUrl.isNullOrBlank()) {
                    kolektuElHtmlFonto(kanalo, parsilo, http, uzuKaŝon)
                } else {
                    kolektuElsendojn(kanalo, parsilo, http, uzuKaŝon)
                }
                val (priskriboj, titoloj) = riĉigu(kanalo, elsendoj)
                // La fluo ankaŭ portu la spegulitan emblemo-URL-on (eksteraj legitimiloj)
                val skriboKanalo = kanalo.emblemoUrl
                    ?.let { malnova -> emblemoj.mapigo[malnova]?.let { kanalo.copy(emblemoUrl = it) } }
                    ?: kanalo
                FluSkribilo.skribu(
                    skriboKanalo, elsendoj, priskriboj, titoloj,
                    File(PUBLIKA, "kanaloj/${kanalo.slug}.xml"),
                )
                // Nur NUN la arkiva fluo de tiu kanalo referencas la novan spegulon —
                // sekure forigi arkaikajn (aliajn sufiksojn). Parta rulo (--args=slug)
                // ne tuŝas la dosierojn de aliaj kanaloj.
                puriguArkaikajnEmblemojn(kanalo.slug, emblemoj.novaNomoLaŭSlug[kanalo.slug])
                novajStatoj[kanalo.slug] = KanalStato(
                    lastaSukceso = Instant.now().toString(),
                    nombroDaElsendoj = elsendoj.size,
                    lastaEraro = "",
                )
                println("  ${kanalo.slug}: ${elsendoj.size} elsendoj, ${priskriboj.size} novaj rezultoj -> kanaloj/${kanalo.slug}.xml")
            } catch (e: Exception) {
                // Regulo A4: unu kanalo ne haltigas la aliajn; regulo A8: neniam silente engluti
                logw("ArkivKonstruilo", "kanalo ${kanalo.slug} fiaskis", e)
                novajStatoj[kanalo.slug] = KanalStato(
                    lastaSukceso = stato.kanaloj[kanalo.slug]?.lastaSukceso ?: "",
                    nombroDaElsendoj = stato.kanaloj[kanalo.slug]?.nombroDaElsendoj ?: 0,
                    lastaEraro = "${e::class.simpleName}: ${e.message}",
                )
                println("  ${kanalo.slug}: ERARO — ${e::class.simpleName}: ${e.message}")
            }
        }

        // la kanalkonfiguro estas publika: la apo elŝutas ĝin de la sama servilo.
        // Publikiĝas PURIGITA versio (sen JSONC-komentoj) — tiel striktaj JSON-legiloj
        // (ankaŭ eksteraj) povas legi ĝin rekte. La emblemo-URL-oj montras al la
        // spegulitaj bildoj sur ĉi tiu servilo (C56) — la retversio gastigas samdevene.
        val leganto = KanalAgordoLeganto()
        val puraKonfiguro = leganto.striptiguKomentojn(KONFIGO.readText())
        File(PUBLIKA, "esperantoradio_konfiguro.json").writeText(
            Emblemoj.resskribuEmblemojn(puraKonfiguro, emblemoj.mapigo)
        )
        stato = stato.copy(lastaRulo = Instant.now().toString(), kanaloj = novajStatoj)
        StatoLeganto.skribu(File(PRIVATA, "stato.json"), stato)
        http.close()
        println(" preta. Stato: server/data/privata/stato.json")
    }

    /**
     * Elŝutas la emblemojn de ĈIUJ kanaloj (ankaŭ la ne-podkastaj — la kanalaro
     * montras ĉiujn) kaj spegulas ilin al `publika/bildoj/<slug>.<sufikso>`.
     * Redonas mapigon malnova URL → arkiva URL por la resskribo en la publikigota
     * konfiguro kaj la fluoj (EmblemaRezulto.mapigo) plus la nova dosiernomo po
     * slug (EmblemaRezulto.novaNomoLaŭSlug) por la prokrastita purigado.
     * Fiaskinta emblemo simple restas sen mapigo (A4: ne haltigas la konstruadon;
     * sur Android/Desktop la originala URL plu funkcias). La forigo de arkaikaj
     * speguloj okazas NE ĉi tie sed nur post sukcesa FluSkribilo.skribu (vidu
     * puriguArkaikajnEmblemojn) — alie parta rulo (--args=slug) forigus dosierojn
     * kiujn la ne-rekonstruitaj fluoj ankoraŭ referencas.
     */
    private fun elŝutuEmblemojn(
        kanaloj: List<dk.nordfalk.esperanto.domain.model.Kanalo>,
        http: HttpClient,
        arkivoBazoUrl: String?,
    ): EmblemaRezulto {
        if (arkivoBazoUrl.isNullOrBlank()) return EmblemaRezulto(emptyMap(), emptyMap())
        val bazo = Emblemoj.bazaBildaUrl(arkivoBazoUrl)
        val bildujo = File(PUBLIKA, "bildoj").apply { mkdirs() }
        val mapigo = mutableMapOf<String, String>()
        val novaNomoLaŭSlug = mutableMapOf<String, String>()
        for (kanalo in kanaloj) {
            val url = kanalo.emblemoUrl ?: continue
            try {
                val nova = runBlocking {
                    val respondo = http.get(url)
                    // Plena paĝo (ekz. 404-HTML) ne iĝu emblemo — Ktor defaŭlte ne
                    // ĵetas ĉe erarstatusoj (expectSuccess=false).
                    check(respondo.status.value in 200..299) { "HTTP ${respondo.status.value}" }
                    val sufikso = Emblemoj.sufikso(respondo.headers[HttpHeaders.ContentType], url)
                    // SVG-on ni NE spegulas (CWE-79/XSS): samdevene servata SVG-dosiero
                    // povas enteni skripton kaj ruliĝi sub la origino de la retversio.
                    // La emblemo restas ĉe la originala URL (A4) kaj estas purigata
                    // el la arkivo (vidu puriguArkaikajnEmblemojn).
                    check(sufikso != "svg") { "SVG-emblemo ne spegulebla (XSS-risko, PR #210)" }
                    val dosiero = File(bildujo, "${kanalo.slug}.$sufikso")
                    dosiero.writeBytes(respondo.bodyAsBytes())
                    "$bazo/bildoj/${dosiero.name}"
                }
                mapigo[url] = nova
                novaNomoLaŭSlug[kanalo.slug] = nova.substringAfterLast('/')
                println("  emblemo ${kanalo.slug} -> bildoj/${nova.substringAfterLast('/')}")
            } catch (e: Exception) {
                // A4: unu fiaskinta emblemo ne haltigas la konstruadon; A8: protokolu kun stacktrace
                logw("ArkivKonstruilo", "emblemo de ${kanalo.slug} ($url) ne elŝuteblis — la konfiguro tenas la originalan URL-on", e)
            }
        }
        println("  emblemoj: ${mapigo.size}/${kanaloj.count { it.emblemoUrl != null }} spegulitaj al publika/bildoj/")
        return EmblemaRezulto(mapigo, novaNomoLaŭSlug)
    }

    /**
     * Forigas arkaikajn emblemo-spegulojn de unu kanalo (sama slug, alia dosiernomo
     * – ekz. malnova .jpg post ŝanĝo al .png). Vokata NUR post sukcesa skribo de la
     * arkiva fluo de tiu kanalo, por ke neniu publika fluo/konfiguro plu referencas
     * la forigotan dosieron (partaj ruloj --args=slug lasas aliajn kanalojn netuŝitaj).
     * novaNomo == null (emblemo fiaskis aŭ estis rifuzita, ekz. SVG) forigas ĈIUJN
     * spegulojn de la slug.
     */
    private fun puriguArkaikajnEmblemojn(slug: String, novaNomo: String?) {
        val bildujo = File(PUBLIKA, "bildoj")
        // novaNomo == null → la emblemo ne mapigis (fiasko aŭ rifuzita SVG):
        // la eventuala spegulo de tiu slug ne plu estas referencata de la
        // ĵus skribita fluo — forigu ĝin (malhelpas samdevenan SVG-reston).
        bildujo.listFiles()
            ?.filter { it.isFile && it.nameWithoutExtension == slug && (novaNomo == null || it.name != novaNomo) }
            ?.forEach { it.delete() }
    }

    /** Elŝutas la fluon (aŭ legas kaŝon) kaj sekvas rel="next" ĝis MAKS_PAĜOJ — sed ĉesas
     *  frue kiam oni atingas la plej novan elsendon jam en la arkivo (pli profunde nenio
     *  nova povas aperi, ĉar la fluoj estas ordigitaj plej-novaj-unue). */
    private fun kolektuElsendojn(
        kanalo: dk.nordfalk.esperanto.domain.model.Kanalo,
        parsilo: RssParsilo,
        http: HttpClient,
        uzuKaŝon: Boolean,
    ): List<dk.nordfalk.esperanto.domain.model.Elsendo> {
        val kaŝo = File(PRIVATA, "kaŝo/${kanalo.slug}.xml")
        var fluoUrl = kanalo.podkastaRssUrl!!
        val novaj = mutableListOf<dk.nordfalk.esperanto.domain.model.Elsendo>()
        // La malnova arkivo: ĝiaj eroj restas (kunfandataj sube), ĝiaj ID-oj markas la
        // punkton, kie la paĝigo ĉesu (la guid en la arkiva fluo estas la fina ID).
        val arkivaDosiero = File(PUBLIKA, "kanaloj/${kanalo.slug}.xml")
        val arkivaTeksto = if (arkivaDosiero.isFile) arkivaDosiero.readText() else null
        val arkivitaj = arkivaTeksto?.let { t ->
            Regex("<guid>(?:<!\\[CDATA\\[)?([^<\\]]+)").findAll(t).map { it.groupValues[1].trim() }.toSet()
        } ?: emptySet()
        val konataj = arkivitaj.toMutableSet()
        var paĝoj = 0
        var unuaPaĝoGrandeco = 0
        while (fluoUrl.isNotBlank() && paĝoj < MAKS_PAĜOJ) {
            paĝoj++
            val teksto = if (uzuKaŝon && kaŝo.isFile) {
                kaŝo.readText()
            } else {
                runBlocking {
                    val t = http.get(fluoUrl).bodyAsText()
                    if (paĝoj == 1) { kaŝo.parentFile.mkdirs(); kaŝo.writeText(t) }
                    t
                }
            }
            val novajElPaĝo = runBlocking {
                parsilo.parsuRss(teksto, kanalo) { url ->
                    runBlocking { http.get(url).bodyAsText() }
                }
            }
            if (paĝoj == 1) unuaPaĝoGrandeco = novajElPaĝo.size
            val nekonataj = novajElPaĝo.takeWhile { it.id !in konataj }
            // Ĉesu kiam la paĝo komenciĝas per jam-arkivitaj elsendoj (aŭ tute mankas novaj)
            if (nekonataj.isEmpty() && (paĝoj > 1 || arkivitaj.isNotEmpty())) break
            novaj.addAll(nekonataj)
            nekonataj.forEach { konataj.add(it.id) }
            fluoUrl = parsilo.leguNextLink(teksto) ?: ""
        }
        // Kunfandu kun la malnova arkivo kaj limigu la grandecon: maks. ~MAKS_PAĜOJ paĝoj da eroj
        val malnovaj = arkivaTeksto?.let { t ->
            runBlocking { parsilo.parsuRss(t, kanalo, deArkivo = true) }
        } ?: emptyList()
        val maksErjoj = MAKS_PAĜOJ * (if (unuaPaĝoGrandeco > 0) unuaPaĝoGrandeco else 10)
        return (novaj + malnovaj).distinctBy { it.id }.sortedByDescending { it.dato }.take(maksErjoj)
    }

    /**
     * HTML-fonto (kanalo sen RSS, sed kun `elsendojHtmlUrl` — ekz. radiofrei ekde 2026-11-22):
     * tiras la filtritan list-paĝon (pPAGE=1…), poste por ĉiu nova artikolo ĝian paĝon kaj
     * el ĝi la unuan mp3-URL-on. Ĉesas frue ĉe jam arkivitaj id-oj (kiel kolektuElsendojn).
     * La kruda HTML restas en la kaŝo (privata/kaŝo/) por `--kaŝo`-ruloj kaj testoj.
     */
    private fun kolektuElHtmlFonto(
        kanalo: dk.nordfalk.esperanto.domain.model.Kanalo,
        parsilo: RssParsilo,
        http: HttpClient,
        uzuKaŝon: Boolean,
    ): List<dk.nordfalk.esperanto.domain.model.Elsendo> {
        val htmlFonto = HtmlFontoParsilo()
        val bazo = kanalo.htmlFontoUrl!!
        val kaŝoDir = File(PRIVATA, "kaŝo").apply { mkdirs() }
        val arkivaDosiero = File(PUBLIKA, "kanaloj/${kanalo.slug}.xml")
        val arkivaTeksto = if (arkivaDosiero.isFile) arkivaDosiero.readText() else null
        val konataj = arkivaTeksto?.let { t ->
            Regex("<guid>(?:<!\\[CDATA\\[)?([^<\\]]+)").findAll(t).map { it.groupValues[1].trim() }.toSet()
        }?.toMutableSet() ?: mutableSetOf()

        fun elŝutu(url: String, kaŝNomo: String): String {
            val kaŝo = File(kaŝoDir, kaŝNomo)
            if (uzuKaŝon && kaŝo.isFile) return kaŝo.readText()
            val bytes = runBlocking { http.get(url).bodyAsBytes() }
            kaŝo.writeBytes(bytes)
            return String(bytes, Charsets.UTF_8)
        }

        val novaj = mutableListOf<dk.nordfalk.esperanto.domain.model.Elsendo>()
        var paĝo = 1
        while (paĝo <= MAKS_HTML_PAĜOJ) {
            val url = if (paĝo == 1) bazo else "$bazo&pPAGE=$paĝo"
            val html = elŝutu(url, "${kanalo.slug}-listo-$paĝo.html")
            val eroj = htmlFonto.parsuListon(html, kanalo.slug, bazo)
            if (eroj.isEmpty()) break
            val novajEroj = eroj.takeWhile { it.id !in konataj }
            if (novajEroj.isEmpty()) break
            for (ero in novajEroj) {
                try {
                    val artikolo = elŝutu(ero.retpaĝoUrl, "${kanalo.slug}-artikolo-${ero.id.substringAfter(':')}.html")
                    val fluo = htmlFonto.eltiruFluon(artikolo)
                    if (fluo == null) {
                        // A8: neniam silente — la ero simple ne aperos en la fluo
                        logw("ArkivKonstruilo", "${kanalo.slug}: ${ero.id} (“${ero.titolo}”) — neniu mp3 trovita, preterlasita")
                        continue
                    }
                    novaj.add(
                        dk.nordfalk.esperanto.domain.model.Elsendo(
                            id = ero.id,
                            kanaloSlug = kanalo.slug,
                            kanaloNomo = kanalo.nomo,
                            titolo = ero.titolo,
                            priskribo = ero.priskribo,
                            dato = ero.dato ?: "1970-01-01",
                            fluo = fluo,
                            retpaĝoUrl = ero.retpaĝoUrl,
                        )
                    )
                    konataj.add(ero.id)
                } catch (e: Exception) {
                    // Regulo A4: unu artikolo ne haltigas la aliajn
                    logw("ArkivKonstruilo", "${kanalo.slug}: artikolo ${ero.id} fiaskis — daŭrigas kun la sekva", e)
                }
            }
            if (novajEroj.size < eroj.size) break // atingis jam arkivitajn erojn — pli frue nenio nova
            paĝo++
        }
        // Kunfandu kun la malnova arkivo (kiel kolektuElsendojn)
        val malnovaj = arkivaTeksto?.let { t ->
            runBlocking { parsilo.parsuRss(t, kanalo, deArkivo = true) }
        } ?: emptyList()
        return (novaj + malnovaj).distinctBy { it.id }.sortedByDescending { it.dato }.take(MAKS_HTML_EROJ)
    }

    /**
     * Riĉigas erojn sen priskribo per AI-rezulto (4–9 linioj + averto).
     * Prioritato: preta rezumo el la rezumo-kaŝo (privata/rezumoj/ — LLM-prilaborita);
     * se mankas, ekstrakta rezumo el la kaŝita transskribo (privata/transkriptoj/).
     * Transskriboj kaj rezumoj estas PRIVATAJ — la publika fluo ricevas nur la pretan
     * rezumon, konvertitan al HTML (legebla kiel la priskriboj de aliaj elsendoj).
     * Redonas: priskriboj (HTML) kaj titoloj (anstataŭaĵoj laŭ id — ekz. se la
     * originala titolo estas nur „<kanalo> <dato>“, oni uzas la temon el la rezumo).
     */
    private fun riĉigu(
        kanalo: dk.nordfalk.esperanto.domain.model.Kanalo,
        elsendoj: List<dk.nordfalk.esperanto.domain.model.Elsendo>,
    ): Pair<Map<String, String>, Map<String, String>> {
        val priskriboj = mutableMapOf<String, String>()
        val titoloj = mutableMapOf<String, String>()
        for (e in elsendoj) {
            if (!e.priskribo.isNullOrBlank()) continue
            val nomo = transkriptoDosieroNomo(e.id)
            val rezumoDosiero = File(PRIVATA, "rezumoj/${kanalo.slug}/$nomo.txt")
            val transkriptoDosiero = File(PRIVATA, "transkriptoj/${kanalo.slug}/$nomo.txt")
            var elRezumoKaŝo = false
            val kruda = when {
                rezumoDosiero.isFile -> { elRezumoKaŝo = true; certiguAverton(rezumoDosiero.readText()) }
                transkriptoDosiero.isFile -> rezumMotoro.rezumu(transkriptoDosiero.readText(), e.titolo)
                else -> {
                    println("    ${e.id}: sen priskribo kaj sen transskribo — rezulto ne generita")
                    null
                }
            }
            if (kruda != null) {
                // el la rezumo-kaŝo la unua linio estas la temo (titolo-stila), el la ekstrakta
                // rezumo ĝi estas nur la unua frazo de fluanta teksto
                val analizita = analizuRezumon(kruda, e.titolo, AI_AVERTO, unuaLinioTitolo = elRezumoKaŝo)
                priskriboj[e.id] = analizita.priskriboHtml
                analizita.titolo?.let { titoloj[e.id] = it }
            }
        }
        return priskriboj to titoloj
    }

    /** certigas, ke la rezulto finiĝas per la AI-averta frazo */
    private fun certiguAverton(rezumo: String): String {
        val t = rezumo.trim()
        return if (t.endsWith(AI_AVERTO)) t else "$t\n\n$AI_AVERTO"
    }

    /**
     * Dosiernomo por transskribo: la id (`<slug>:<guid>`) enhavas `/` kaj `:`, do
     * sanitarigita + 8-signa haketo (md5) por unikeco. La STT-flanko (Python) uzas la saman regulon.
     */
    fun transkriptoDosieroNomo(id: String): String {
        val sanitarigita = id.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60)
        val haketo = java.security.MessageDigest.getInstance("MD5").digest(id.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(8)
        return "${sanitarigita}_$haketo"
    }
}
