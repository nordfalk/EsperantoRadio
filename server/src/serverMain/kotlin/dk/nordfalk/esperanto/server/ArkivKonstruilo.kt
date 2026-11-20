package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.data.parser.analizuRezumon
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
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
    private const val MAKS_PAĜOJ = 30

    private val rezumMotoro: RezumMotoro = EkstraktaRezumMotoro()

    @JvmStatic
    fun main(args: Array<String>) {
        val nurTiuj = args.filter { !it.startsWith("--") }.toSet()
        val uzuKaŝon = "--kaŝo" in args

        val kanaloj = KanalAgordoLeganto().legu(KONFIGO.readText()).kanaloj
            .map { it.alKanalo() }
            .filter { it.havasPodkastojn }
            .filter { nurTiuj.isEmpty() || it.slug in nurTiuj }

        println("Arkiv-konstruilo: ${kanaloj.size} podkastaj kanaloj (${Instant.now()})")

        val http = HttpClient(CIO)
        val parsilo = RssParsilo()
        var stato = StatoLeganto.legu(File(PRIVATA, "stato.json"))
        val novajStatoj = stato.kanaloj.toMutableMap()

        for (kanalo in kanaloj) {
            try {
                val elsendoj = kolektuElsendojn(kanalo, parsilo, http, uzuKaŝon)
                val (priskriboj, titoloj) = riĉigu(kanalo, elsendoj)
                FluSkribilo.skribu(
                    kanalo, elsendoj, priskriboj, titoloj,
                    File(PUBLIKA, "kanaloj/${kanalo.slug}.xml"),
                )
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
        // (ankaŭ eksteraj) povas legi ĝin rekte.
        val leganto = KanalAgordoLeganto()
        File(PUBLIKA, "esperantoradio_konfiguro.json").writeText(leganto.striptiguKomentojn(KONFIGO.readText()))
        stato = stato.copy(lastaRulo = Instant.now().toString(), kanaloj = novajStatoj)
        StatoLeganto.skribu(File(PRIVATA, "stato.json"), stato)
        http.close()
        println(" preta. Stato: server/data/privata/stato.json")
    }

    /** Elŝutas la fluon (aŭ legas kaŝon) kaj sekvas rel="next" ĝis MAKS_PAĜOJ. */
    private fun kolektuElsendojn(
        kanalo: dk.nordfalk.esperanto.domain.model.Kanalo,
        parsilo: RssParsilo,
        http: HttpClient,
        uzuKaŝon: Boolean,
    ): List<dk.nordfalk.esperanto.domain.model.Elsendo> {
        val kaŝo = File(PRIVATA, "kaŝo/${kanalo.slug}.xml")
        var fluoUrl = kanalo.podkastaRssUrl!!
        val ĉiuj = mutableListOf<dk.nordfalk.esperanto.domain.model.Elsendo>()
        val konataj = mutableSetOf<String>()
        var paĝoj = 0
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
            val novaj = runBlocking {
                parsilo.parsuRss(teksto, kanalo) { url ->
                    runBlocking { http.get(url).bodyAsText() }
                }
            }
            val nekonataj = novaj.takeWhile { it.id !in konataj }
            if (nekonataj.isEmpty() && paĝoj > 1) break
            ĉiuj.addAll(nekonataj)
            nekonataj.forEach { konataj.add(it.id) }
            fluoUrl = parsilo.leguNextLink(teksto) ?: ""
        }
        return ĉiuj.distinctBy { it.id }.sortedByDescending { it.dato }
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
