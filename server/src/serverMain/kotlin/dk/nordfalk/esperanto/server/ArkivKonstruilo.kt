package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.parser.HtmlFontoParsilo
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.data.parser.analizuRezumon
import dk.nordfalk.esperanto.data.parser.kuniguPriskribonKunRezumo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import dk.nordfalk.esperanto.data.repository.RespondoTroGrandaException
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import java.net.InetAddress
import java.net.URI
import io.ktor.client.statement.bodyAsText
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
    // Spegulata bildo (kanal-emblemo aŭ elsenda bildeto): sufiĉe por kovrilo aŭ
    // kontribuaĵo-foto, tro por erarpaĝo aŭ kaŝvestitaĵo (C56 — la bildoj estas
    // servataj samdevene por la retversio). La legado ĉesas fluante ĉe transpaso
    // (vidu leguBitokojnLimigite) — neniam senlima enmemorigo de la korpo.
    private const val MAKS_BILDA_GRANDECO_BITOKOJ = 5L * 1024 * 1024

    private val rezumMotoro: RezumMotoro = EkstraktaRezumMotoro()

    /**
     * Kliento por BILDO-elŝutoj (emblemoj kaj elsendlaj bildetoj): SENVOLA
     * alidirekto — ĉiun salton validigas `elŝutuBildonKunŜirmo` mem (SSRF-ŝirmo,
     * CWE-918: aŭtoro de ekstera fluo alie povus devigi la konstruilon peti
     * internajn adresojn).
     */
    private val bildaHttp: HttpClient = HttpClient(CIO) { followRedirects = false }

    /**
     * Priskriboj ĝis tiu longo ricevas AI-rezumon (aldonatan sub la originalo);
     * pli longaj priskriboj estas jam sufiĉe informaj kaj restas senŝanĝaj
     * (decido de la posedanto, 2026-11-25 — la limo konserveme super la analizo,
     * kie la originaj priskriboj ekportas aŭtorojn, gastojn kaj ligilojn).
     */
    private const val AI_PRISKRIBO_JE_LONGO_MALPLI_OL = 150

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
        val emblemoj = elŝutuEmblemojn(ĉiujKanaloj, agordo.arkivoBazoUrl)

        for (kanalo in kanaloj) {
            try {
                val elsendoj = if (kanalo.podkastaRssUrl.isNullOrBlank() && !kanalo.htmlFontoUrl.isNullOrBlank()) {
                    kolektuElHtmlFonto(kanalo, parsilo, http, uzuKaŝon)
                } else {
                    kolektuElsendojn(kanalo, parsilo, http, uzuKaŝon)
                }
                val (priskriboj, titoloj) = riĉigu(kanalo, elsendoj)
                // Elsendaj bildetoj speguliĝas antaŭ la skribo — la fluo ricevas
                // samdevenan URL-on kiel <itunes:image> (C56: CORS blokas eksterajn
                // bildojn en la retversio; FluSkribilo skribas ĝin po ero).
                val spegulitajElsendoj = speguluElsendajnBildetojn(elsendoj, http, agordo.arkivoBazoUrl)
                // La fluo ankaŭ portu la spegulitan emblemo-URL-on (eksteraj legitimiloj)
                val skriboKanalo = kanalo.emblemoUrl
                    ?.let { malnova -> emblemoj.mapigo[malnova]?.let { kanalo.copy(emblemoUrl = it) } }
                    ?: kanalo
                FluSkribilo.skribu(
                    skriboKanalo, spegulitajElsendoj, priskriboj, titoloj,
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
        bildaHttp.close()
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
                    val enhavo = elŝutuBildonKunŜirmo(url)
                    // XSS-ŝirmo (CWE-79, PR #210): la formato estas determinata laŭ
                    // la ENHAVO de la bildo (magiaj signoj), ne laŭ Content-Type aŭ
                    // dosiernomo kaj validigata per VERA malkodado (ImageIO). Nur malkodatablaj
                    // PNG/JPEG/GIF speguliĝas; SVG
                    // (skript-ruligebla samdevene), HTML kaj erar-paĝoj estas rifuzataj
                    // — la emblemo restas ĉe la originala URL (A4).
                    val formato = Emblemoj.formatoSeLegebla(enhavo)
                        ?: throw IllegalStateException("enhavo ne estas malkodebla PNG/JPEG/GIF (eble SVG/HTML/koruptita) — ne spegulata")
                    val dosiero = File(bildujo, "${kanalo.slug}.$formato")
                    // Transakcie kiel ĉe la elsendlaj bildetoj: .part + rename —
                    // interrompita skribo ne lasu koruptitan emblemon servotan.
                    val provizora = File(bildujo, "${dosiero.name}.part")
                    try {
                        provizora.writeBytes(enhavo)
                        if (!provizora.renameTo(dosiero)) {
                            throw IllegalStateException("ne eblis alinumi ${provizora.name} al ${dosiero.name}")
                        }
                    } finally {
                        provizora.delete()
                    }
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

    /**
     * Spegulas la elsendlajn bildetojn ([dk.nordfalk.esperanto.domain.model.Elsendo.bildoUrl]
     * : la `RssParsilo` elektas ĝin kiel la unuan `<img>` el la priskribo) al
     * `publika/bildoj/elsendoj/<sha1(url)>.<ekst>` kaj redonas la elsendojn kun
     * samdevena URL-o (C56: la retversio ne povas ŝargi eksterajn bildojn — CORS).
     * Jam spegulita bildeto (ekzistanta dosiero kun la sama radiko) ne re-elŝutiĝas.
     * Fiaskinta elŝuto (reta eraro, tro granda, ne malkodebla enhavo) lasas la
     * ORIGINALAN URL-on (regulo A4) — sur Android/Desktop ĝi plu funkcias; la
     * retversio tiam ne montras la bildeton.
     */
    private fun speguluElsendajnBildetojn(
        elsendoj: List<dk.nordfalk.esperanto.domain.model.Elsendo>,
        http: HttpClient,
        arkivoBazoUrl: String?,
    ): List<dk.nordfalk.esperanto.domain.model.Elsendo> {
        if (arkivoBazoUrl.isNullOrBlank()) return elsendoj
        val bazo = Emblemoj.bazaBildaUrl(arkivoBazoUrl)
        val bildujo = File(PUBLIKA, "bildoj/elsendoj").apply { mkdirs() }
        val mapigo = mutableMapOf<String, String>()
        fun novaUrl(url: String, dosiero: java.io.File): String = "$bazo/bildoj/elsendoj/${dosiero.name}"

        fun spegulu(url: String): String {
            mapigo[url]?.let { return it }
            val radiko = Emblemoj.bildetaRadiko(url)
            val ekzistanta = bildujo.listFiles()?.firstOrNull { it.isFile && it.nameWithoutExtension == radiko }
            val nova = if (ekzistanta != null) {
                novaUrl(url, ekzistanta) // jam spegulita en pli frua rulo
            } else {
                try {
                    runBlocking {
                        val enhavo = elŝutuBildonKunŜirmo(url)
                        val formato = Emblemoj.formatoSeLegebla(enhavo)
                            ?: throw IllegalStateException("ne malkodebla PNG/JPEG/GIF (eble SVG/HTML)")
                        val dosiero = File(bildujo, "$radiko.$formato")
                        // Transakcie: skribu al .part kaj alinomu nur post plena skribo —
                        // interrompita skribo ne kredigu al la sekva rulo koruptitan
                        // dosieron kiel jam-spegulitan (la ekzisto-kontrolo rigardas radikon).
                        val provizora = File(bildujo, "${dosiero.name}.part")
                        try {
                            provizora.writeBytes(enhavo)
                            if (!provizora.renameTo(dosiero)) {
                                throw IllegalStateException("ne eblis alinumi ${provizora.name} al ${dosiero.name}")
                            }
                        } finally {
                            // post sukcesa renomo la .part ne ekzistas (delete senefika);
                            // post fiaskinta SKRIBO aŭ renomo la restinta .part foriĝas
                            provizora.delete()
                        }
                        novaUrl(url, dosiero)
                    }
                } catch (e: Exception) {
                    // A4: unu fiaskinta bildeto ne haltigas la kanalon; A8: protokolu
                    logw("ArkivKonstruilo", "elsenda bildeto ne spegulebla: $url — la originala URL restas", e)
                    url
                }
            }
            mapigo[url] = nova
            return nova
        }

        var spegulitaj = 0
        val rezulto = elsendoj.map { e ->
            val bildo = e.bildoUrl ?: return@map e
            if (bildo.startsWith("$bazo/")) e // jam samdevena (pli frua rulo)
            else {
                spegulitaj++
                e.copy(bildoUrl = spegulu(bildo))
            }
        }
        if (spegulitaj > 0) logd("ArkivKonstruilo", "elsendaj bildetoj: ${mapigo.size} unikaj, $spegulitaj spegulotaj")
        return rezulto
    }

    /**
     * Elŝutas la bildo-korpon kun SSRF-ŝirmo kaj fluanta grandec-limo.
     *
     * La celo de ĉIU salto (ankaŭ alidirektoj) devas esti http(s) kaj publika
     * adreso (CWE-918) — [bildaHttp] sekvas NENIUJN saltojn, do ĉi tien ili estas
     * sekvataj MANE (ĝis 5), ĉiu validigata per [estasPublikaCelo] antaŭ la peto.
     * La korpo legiĝas FLUE per prepareGet-execute (HttpClient.get enmemorigus la
     * tutan korpon antaŭ kontrolo — Ktor 3.6, HttpStatement) kaj la legado ĉesas
     * tuj ĉe transpaso de la limo. Refuzoj traktiĝas kiel ordinara elŝuta eraro
     * (la alvokanto kaptas kaj daŭrigas, regulo A4).
     */
    private suspend fun elŝutuBildonKunŜirmo(url: String): ByteArray {
        var nuna = url
        var saltoj = 0
        while (true) {
            check(saltoj++ < 5) { "tro da alidirektoj (≥ 5) ekde $url" }
            check(estasPublikaCelo(nuna)) { "ne-publika/ne-http(s) celo (SSRF-ŝirmo): $nuna" }
            var alidirekto: String? = null
            val enhavo: ByteArray? = bildaHttp.prepareGet(nuna).execute { respondo ->
                if (respondo.status.value in 300..399) {
                    val loko = respondo.headers[HttpHeaders.Location]
                    check(!loko.isNullOrBlank()) { "HTTP ${respondo.status.value} sen Location" }
                    alidirekto = URI(nuna).resolve(loko).toString()
                    null
                } else {
                    check(respondo.status.value in 200..299) { "HTTP ${respondo.status.value}" }
                    leguBitokojnLimigite(respondo, MAKS_BILDA_GRANDECO_BITOKOJ)
                }
            }
            enhavo?.let { return it }
            nuna = alidirekto ?: throw IllegalStateException("alidirekto sen sekva celo: $nuna")
        }
    }

    /**
     * Ĉu la URL-celo estas publika: skemo http(s) kaj ĉiuj resolvitaj adresoj
     * estas neniu el: any-local, loopback, site-local (privata), link-local,
     * multicast. Neresolvebla gastiganto → false (la normala elŝuto same
     * fiaskus — rifuzu frue).
     */
    private fun estasPublikaCelo(url: String): Boolean {
        return try {
            val uri = URI(url)
            if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return false
            // ĈIU resolvita adreso devas esti publika laŭ la striktaj intervaloj
            // en Emblemoj.ĉuPublikaAdreso (kaj fd00::1 kaj 100.64.0.1 rifuzataj —
            // la InetAddress-oportunaj kontroloj ne sufiĉas).
            InetAddress.getAllByName(uri.host).all { Emblemoj.ĉuPublikaAdreso(it) }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fluanta korpo-legado kun grandec-limo (la G45-skemo el KorpaLegilo, bitokoj):
     * antaŭkontrolo de Content-Length KAJ ĉesigo dum la flu-legado — neniam
     * senlima enmemorigo (OutOfMemoryError ne estas kaptata de catch(Exception)).
     */
    internal suspend fun leguBitokojnLimigite(respondo: HttpResponse, limo: Long): ByteArray {
        val deklarita = respondo.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (deklarita != null && deklarita > limo) throw RespondoTroGrandaException(deklarita, limo)
        val kanalo = respondo.bodyAsChannel()
        val pecoj = mutableListOf<ByteArray>()
        val buf = ByteArray(64 * 1024)
        var totalo = 0L
        while (true) {
            val n = kanalo.readAvailable(buf)
            if (n <= 0) break
            totalo += n
            if (totalo > limo) throw RespondoTroGrandaException(totalo, limo)
            pecoj.add(buf.copyOf(n))
        }
        val ĉiuj = ByteArray(totalo.toInt())
        var pozicio = 0
        for (peco in pecoj) {
            peco.copyInto(ĉiuj, pozicio)
            pozicio += peco.size
        }
        return ĉiuj
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
     * Riĉigas erojn kies priskribo estas malplena aŭ maks. [AI_PRISKRIBO_JE_LONGO_MALPLI_OL]
     * signoj longa per AI-rezulto: la originala priskribo restas unue (gni ĉiam
     * videbla), poste la kursivigita averto pri la AI-fonto kaj la rezumo mem.
     * Prioritato: preta rezumo el la rezumo-kaŝo (privata/rezumoj/ — LLM-prilaborita);
     * se mankas, ekstrakta rezumo el la kaŝita transskribo (privata/transkriptoj/).
     * Transskriboj kaj rezumoj estas PRIVATAJ — la publika fluo ricevas nur la pretan
     * rezumon, konvertitan al HTML (legebla kiel la priskriboj de aliaj elsendoj).
     * Redonas: priskriboj (HTML) kaj titoloj (anstataŭaĵoj laŭ id — ekz. se la
     * originala titolo estas nur „<kanalo> <dato>“, oni uzas la temon el la rezumo).
     */
    internal fun riĉigu(
        kanalo: dk.nordfalk.esperanto.domain.model.Kanalo,
        elsendoj: List<dk.nordfalk.esperanto.domain.model.Elsendo>,
        privata: File = PRIVATA,
    ): Pair<Map<String, String>, Map<String, String>> {
        val priskriboj = mutableMapOf<String, String>()
        val titoloj = mutableMapOf<String, String>()
        for (e in elsendoj) {
            val krudaPriskribo = e.priskribo.orEmpty().trim()
            // Jam riĉigita priskribo (re-legita el la arkivo) ne riĉiĝu duan fojon
            if (krudaPriskribo.contains(AI_AVERTO)) continue
            if (krudaPriskribo.length > AI_PRISKRIBO_JE_LONGO_MALPLI_OL) continue
            val nomo = transkriptoDosieroNomo(e.id)
            val rezumoDosiero = File(privata, "rezumoj/${kanalo.slug}/$nomo.txt")
            val transkriptoDosiero = File(privata, "transkriptoj/${kanalo.slug}/$nomo.txt")
            var elRezumoKaŝo = false
            val kruda = when {
                rezumoDosiero.isFile -> { elRezumoKaŝo = true; rezumoDosiero.readText() }
                transkriptoDosiero.isFile -> rezumMotoro.rezumu(transkriptoDosiero.readText(), e.titolo)
                else -> {
                    if (krudaPriskribo.isEmpty()) println("    ${e.id}: sen priskribo kaj sen transskribo — rezulto ne generita")
                    null
                }
            }
            if (!kruda.isNullOrBlank()) {
                // el la rezumo-kaŝo la unua linio estas la temo (titolo-stila), el la ekstrakta
                // rezumo ĝi estas nur la unua frazo de fluanta teksto
                val analizita = analizuRezumon(kruda, e.titolo, AI_AVERTO, unuaLinioTitolo = elRezumoKaŝo)
                priskriboj[e.id] = kuniguPriskribonKunRezumo(e.priskriboHtml ?: e.priskribo, AI_AVERTO, analizita.priskriboHtml)
                analizita.titolo?.let { titoloj[e.id] = it }
            }
        }
        return priskriboj to titoloj
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
