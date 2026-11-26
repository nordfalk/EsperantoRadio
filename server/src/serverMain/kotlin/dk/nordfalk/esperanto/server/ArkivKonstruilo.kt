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
import io.ktor.client.engine.okhttp.OkHttp
import dk.nordfalk.esperanto.data.repository.RespondoTroGrandaException
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
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
     * Kliento por BILDO-elŝutoj (emblemoj kaj elsendlaj bildetoj):
     * - SENVOLA — alidirektoj (3xx) estas „nenormalaj" kaj rifuzataj
     *   (posedanto-decido 2026-11-25);
     * - OkHttp-motoro kun [Emblemoj.SekuraBildaDns]: la konekto povas atingi NUR
     *   publikajn adresojn — la DNS-rebinding-fenestro (CWE-918) estas fermita
     *   konstruite, sen propra TLS-kodo (SNI-kapo kaj atestokontrolo restas de
     *   OkHttp laŭ la originala gastiganto-nomo).
     */
    private val bildaHttp: HttpClient = HttpClient(OkHttp) {
        followRedirects = false
        engine {
            // config-locale ricevas la OkHttpClient.Builder kiel this
            config {
                dns(Emblemoj.SekuraBildaDns)
                // NENIU prokurilo: kun HTTP-prokurilo OkHttp solvas la prokurilon
                // per la Dns-hoko kaj la prokurilo mem solvas la celon — la
                // valida rezolvo tiel ne plu kontrolias la vojon al la celo.
                proxy(java.net.Proxy.NO_PROXY)
            }
        }
    }

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
        val emblemoj = elŝutuEmblemojn(ĉiujKanaloj, agordo.arkivoBazoUrl, retumado = !uzuKaŝon)

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
                val spegulitajElsendoj = speguluElsendajnBildetojn(elsendoj, http, agordo.arkivoBazoUrl, parsilo, retumado = !uzuKaŝon)
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
     * Skriba transakcio por spegulata bildo: unue al <nomo>.<PID>.part, poste
     * alinumo al la fina dosiero — interrompita skribo ne lasu koruptitan
     * dosieron servotan, kaj la PID-sufikso malebligas ke du paralelaj ruloj
     * (permana + cron) interfrapu la saman provizoran dosieron.
     */
    private fun skribuTransakcie(bildujo: File, enhavo: ByteArray, dosiero: File) {
        val provizora = File(bildujo, "${dosiero.name}.${ProcessHandle.current().pid()}.part")
        try {
            provizora.writeBytes(enhavo)
            if (!provizora.renameTo(dosiero)) {
                throw IllegalStateException("ne eblis alinumi ${provizora.name} al ${dosiero.name}")
            }
        } finally {
            // post sukcesa renomo la .part ne plu ekzistas (delete senefika);
            // post fiaskinta skribo aŭ renomo la restinta .part foriĝas
            provizora.delete()
        }
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
        retumado: Boolean,
    ): EmblemaRezulto {
        if (arkivoBazoUrl.isNullOrBlank()) return EmblemaRezulto(emptyMap(), emptyMap())
        val bazo = Emblemoj.bazaBildaUrl(arkivoBazoUrl)
        val bildujo = File(PUBLIKA, "bildoj").apply { mkdirs() }
        val mapigo = mutableMapOf<String, String>()
        val novaNomoLaŭSlug = mutableMapOf<String, String>()
        for (kanalo in kanaloj) {
            val url = kanalo.emblemoUrl ?: continue
            if (!retumado) {
                // Kaŝreĝimo (--kaŝo, sen reto): reuzu spegulon jam sur disko;
                // mankanta emblemo restas sencela — la konfiguro/fluo tenas la
                // originalan URL-on ĝis la venonta reta rulo (neniu lanco en la reto).
                bildujo.listFiles()
                    ?.firstOrNull { it.isFile && it.nameWithoutExtension == kanalo.slug }
                    ?.let {
                        mapigo[url] = "$bazo/bildoj/${it.name}"
                        novaNomoLaŭSlug[kanalo.slug] = it.name
                    }
                continue
            }
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
                    skribuTransakcie(bildujo, enhavo, dosiero)
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
    internal fun speguluElsendajnBildetojn(
        elsendoj: List<dk.nordfalk.esperanto.domain.model.Elsendo>,
        http: HttpClient,
        arkivoBazoUrl: String?,
        parsilo: RssParsilo,
        retumado: Boolean,
        // injektebla nur por testoj; la produktada vojo uzas la publikan dosierujon
        bildujo: File = File(PUBLIKA, "bildoj/elsendoj").apply { mkdirs() },
    ): List<dk.nordfalk.esperanto.domain.model.Elsendo> {
        if (arkivoBazoUrl.isNullOrBlank()) return elsendoj
        val bazo = Emblemoj.bazaBildaUrl(arkivoBazoUrl)
        val mapigo = mutableMapOf<String, String>()
        fun novaUrl(dosiero: java.io.File): String = "$bazo/bildoj/elsendoj/${dosiero.name}"

        fun spegulu(url: String): String {
            mapigo[url]?.let { return it }
            val radiko = Emblemoj.bildetaRadiko(url)
            val ekzistanta = bildujo.listFiles()?.firstOrNull { it.isFile && it.nameWithoutExtension == radiko }
            val nova = if (ekzistanta != null) {
                novaUrl(ekzistanta) // jam spegulita en pli frua rulo
            } else if (!retumado) {
                // Kaŝreĝimo (sen reto): ekzistantaj speguloj reuziĝas supre;
                // mankanta bildeto ne elŝutiĝas — la originala URL restas
                // ĝis la venonta reta rulo (la fluo tiam ne nomas mortintan spegulan ligilon).
                logd("ArkivKonstruilo", "bildeto ne spegulita (kaŝreĝimo): $url")
                url
            } else {
                try {
                    runBlocking {
                        val enhavo = elŝutuBildonKunŜirmo(url)
                        val formato = Emblemoj.formatoSeLegebla(enhavo)
                            ?: throw IllegalStateException("ne malkodebla PNG/JPEG/GIF (eble SVG/HTML)")
                        val dosiero = File(bildujo, "$radiko.$formato")
                        skribuTransakcie(bildujo, enhavo, dosiero)
                        novaUrl(dosiero)
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
            if (bildo.startsWith("$bazo/")) {
                // ALIA samdevena URL (ekz. kanal-emblemo kiel <itunes:image>, aŭ
                // bildo gastigata ĉe nia servilo) restas SENŜANĜE — la retversio
                // ŝargas ĝin sen CORS; re-spegulado estus nenecesa kaj povus anstataŭigi
                // ĝin per alia bildo.
                if (!bildo.startsWith("$bazo/bildoj/elsendoj/")) return@map e
                // Jam samdevena (pli frua rulo) — sed KONTROLU la dosieron: se la
                // spegulo perdiĝis, la fluo portas mortintan ligilon kaj la normala
                // prefikso-kontrolo eterne preterlasus ĝin (la originalo ne plu estas
                // eltrovebla el la fluo). Resanigu per la originala <img> el la
                // priskribo (RssParsilo.elektuBildeton — A2: la elekto-logiko en unu loko).
                // LIMIGO (akceptita de la posedanto 2026-11-26): por bildetoj kies
                // fonto estis NUR <itunes:image> de la originala fluo, la fonta URL
                // ne konserviĝas en la arkivo — la resanigo povas uzi nur priskriban
                // <img>, kiu por tiaj eroj povas esti alia bildo (aŭ neniu).
                if (File(bildujo, bildo.substringAfterLast('/')).isFile) return@map e
                val originalo = parsilo.elektuBildeton(e.priskriboHtml ?: e.priskribo.orEmpty())
                if (originalo == null || originalo.startsWith("$bazo/")) {
                    // A8: protokolu — la priskribo ne plu portas uzigeblan originalon
                    logw("ArkivKonstruilo", "spegulita bildeto perdiĝis kaj la priskribo ne portas originalon: $bildo")
                    return@map e
                }
                e.copy(bildoUrl = spegulu(originalo))
            }
            else {
                spegulitaj++
                e.copy(bildoUrl = spegulu(bildo))
            }
        }
        if (spegulitaj > 0) logd("ArkivKonstruilo", "elsendaj bildetoj: ${mapigo.size} unikaj, $spegulitaj spegulotaj")
        return rezulto
    }

    /**
     * Elŝutas la bildo-korpon kun permes-lista ŝirmo kaj fluanta grandec-limo.
     * Regulo de la posedanto (2026-11-25): ĉio nenormala → rifuzu; la bildo
     * restas ĉe la originala URL (regulo A4 — la fluo/kanalo daŭras):
     * - frua rifuzo de nenormalaj celoj (skemo, haveno, IP-literalo —
     *   [Emblemoj.ĉuAkcepteblaBildaCelo]) kaj DECIDA filtrado de la konekta
     *   rezolvo al publikaj adresoj ([Emblemoj.SekuraBildaDns] — la Dns-hoko
     *   de la OkHttp-motoro; SSRF-ŝirmo CWE-918, inkluzive DNS-rebinding);
     * - NENIU alidirekto: [bildaHttp] estas senvola kaj ĉiu 3xx estas rifuzata
     *   kiel nenormala — bildoj gastigitaj malantaŭ alidirekto ne speguliĝas;
     * - la korpo legiĝas FLUE per prepareGet-execute (HttpClient.get enmemorigus
     *   la tutan korpon antaŭ kontrolo — Ktor 3.6, HttpStatement) kaj la legado
     *   ĉesas tuj ĉe transpaso de la limo (leguBitokojnLimigite).
     */
    private suspend fun elŝutuBildonKunŜirmo(url: String): ByteArray {
        check(Emblemoj.ĉuAkcepteblaBildaCelo(url)) { "rifuzita bildo-celo (permes-listo): $url" }
        return bildaHttp.prepareGet(url).execute { respondo ->
            check(respondo.status.value in 200..299) { "HTTP ${respondo.status.value}: $url" }
            leguBitokojnLimigite(respondo, MAKS_BILDA_GRANDECO_BITOKOJ)
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
