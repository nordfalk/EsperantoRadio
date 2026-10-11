package dk.nordfalk.esperanto

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.config.parsuSugestojnPorAlarmoj
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.ArchiveOrgDosiernomoKasho
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl
import dk.nordfalk.esperanto.data.repository.PersistantaAlarmoDeponejo
import dk.nordfalk.esperanto.data.repository.PersistantaPlejŝatatajDeponejo
import dk.nordfalk.esperanto.data.repository.PersistaLudatojDeponejo
import dk.nordfalk.esperanto.data.repository.SercxoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.kreuAlarmoSkedilo
import dk.nordfalk.esperanto.data.repository.kreuElshutDeponejo
import dk.nordfalk.esperanto.domain.DiagnozoRegilo
import dk.nordfalk.esperanto.domain.kreuDiagnozoRegilon
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.LudvicoRegilo
import dk.nordfalk.esperanto.domain.player.elektuAlarmElsendon
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.ui.HejmoViewModel
import dk.nordfalk.esperanto.ui.KanalaroViewModel
import dk.nordfalk.esperanto.ui.KanaloViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import dk.nordfalk.esperanto.data.repository.httpMotoro
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Proceznivela unuopulo por deponejoj kaj ViewModel-oj.
 * Pluvivas dum la tuta procezo-vivdaŭro — je Activity-rekreo (ekranturno),
 * la ekzistantaj instancoj estas reuzataj anstataŭ krei novajn.
 */
object AppStato {
    var httpKliento: HttpClient? = null
        private set
    var kanaloDeponejo: KanaloDeponejoImpl? = null
        private set
    var elsendoDeponejo: ElsendoDeponejoImpl? = null
        private set
    var kanalaroViewModel: KanalaroViewModel? = null
        private set
    var hejmoViewModel: HejmoViewModel? = null
        private set
    var plejŝatatajDeponejo: PersistantaPlejŝatatajDeponejo? = null
        private set
    var sercxoDeponejo: SercxoDeponejoImpl? = null
        private set
    var elshutDeponejo: ElshutDeponejo? = null
        private set
    var alarmoDeponejo: PersistantaAlarmoDeponejo? = null
        private set
    var ludatojDeponejo: PersistaLudatojDeponejo? = null
        private set
    var ludvicoRegilo: LudvicoRegilo? = null
        private set
    var diagnozoRegilo: DiagnozoRegilo? = null
        private set

    /** KanaloViewModel-oj, ŝlositaj laŭ slug. */
    val kanaloViewModelj = mutableMapOf<String, KanaloViewModel>()

    fun inicialigita(): Boolean = httpKliento != null

    /**
     * Trovas la elsendon kiun alarmo ludu por podkasta kanalo (komuna por la
     * alarmo-ricevilo kaj la ĉefaktivon). Provas laŭvice:
     * reton (freŝa RSS), diskkaŝmemoron, kaj elŝutitajn elsendojn — ĉar ĉe vekiĝo
     * la reto ofte ankoraŭ ne pretas. Poste elektas per [elektuAlarmElsendon].
     *
     * Revenas null se AppStato ne estas inicialigita aŭ neniu elsendo troveblas.
     */
    suspend fun trovuAlarmElsendon(kanalo: Kanalo): Elsendo? {
        val elsendoDeponejo = elsendoDeponejo
            ?: return null.also { logw("AppStato", "Ne inicialigita — ne povas trovi elsendon por alarmo") }
        val elsendoj = elsendoDeponejo.sxargxiElsendojn(kanalo, fortoRefresigi = true)
            .ifEmpty { elsendoDeponejo.leguKashitajnElsendojn(kanalo) ?: emptyList() }
            .ifEmpty {
                elshutDeponejo?.observiElshutojn()?.value?.values
                    ?.filter { it.elsendo.kanaloSlug == kanalo.slug && it.stato is ElshutStato.Preta }
                    ?.map { it.elsendo } ?: emptyList()
            }
        val ludatoj = ludatojDeponejo?.observiLudatojn()?.value ?: emptyMap()
        return elektuAlarmElsendon(elsendoj, ludatoj)
    }

    /**
     * Kreas ĉiujn deponejojn kaj ViewModel-ojn. Idempotent — se jam inicialigita, faras nenion.
     *
     * @param ludilo la ludilo-regilo (platform-specifa, transdonita de la enirpunkto)
     * @param settings platform-specifaj agordoj (SharedPreferences / Preferences)
     * @param agordojDeponejo la agordoj-deponejo (necesas por auxtomataDaurigo-fluo)
     */
    fun inicialigu(
        ludilo: LudiloRegilo,
        settings: Settings,
        agordojDeponejo: AgordojDeponejoImpl,
    ) {
        if (httpKliento != null) return

        logi("AppStato", "Inicialigas deponejojn kaj ViewModel-ojn")

        httpKliento = HttpClient(httpMotoro) {
            install(Logging) { level = LogLevel.INFO }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                // (PR #93) sen tio servilo kiu akceptas kaj poste silentegas povas
                // teni la peton viva pli longe ol la dezirata limo
                socketTimeoutMillis = 30_000
            }
        }

        kanaloDeponejo = KanaloDeponejoImpl(
            leganto = KanalAgordoLeganto(),
            bundledTeksto = ::leguBundledKanalkonfiguron
        )

        elsendoDeponejo = ElsendoDeponejoImpl(httpKliento!!, RssParsilo(ArchiveOrgDosiernomoKasho(settings)))

        kanalaroViewModel = KanalaroViewModel(kanaloDeponejo!!, elsendoDeponejo!!)

        plejŝatatajDeponejo = PersistantaPlejŝatatajDeponejo(settings)

        sercxoDeponejo = SercxoDeponejoImpl(elsendoDeponejo!!)

        elshutDeponejo = kreuElshutDeponejo(httpKliento!!)

        val agordo = KanalAgordoLeganto().legu(leguBundledKanalkonfiguron())
        val sugestoj = agordo.sugestoj_por_alarmoj?.let { parsuSugestojnPorAlarmoj(it) } ?: emptyList()
        alarmoDeponejo = PersistantaAlarmoDeponejo(settings, sugestoj, kreuAlarmoSkedilo())

        ludatojDeponejo = PersistaLudatojDeponejo(settings)

        hejmoViewModel = HejmoViewModel(kanaloDeponejo!!, elsendoDeponejo!!, ludatojDeponejo!!)

        ludvicoRegilo = LudvicoRegilo(
            ludilo = ludilo,
            elsendoDeponejo = elsendoDeponejo!!,
            kanaloDeponejo = kanaloDeponejo!!,
            plejŝatatajDeponejo = plejŝatatajDeponejo!!,
            ludatojDeponejo = ludatojDeponejo!!,
            getLokaDosieroVojo = { id -> elshutDeponejo!!.getLokaDosieroVojo(id) },
            auxtomataDaurigo = agordojDeponejo.auxtomataDaurigo,
        )

        diagnozoRegilo = kreuDiagnozoRegilon()

        // Aŭtomata rapida kontrolo en la fono — por montri avertosignon sur la ĉefekrano.
        // Dispatchers.Default, ne IO: IO ne ekzistas sur wasmJs (la DNS-testo de la
        // Android-implemento rulas en propra fadeno interne).
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                diagnozoRegilo?.kontroliRapide()
            } catch (e: Exception) {
                logw("AppStato", "Malsukcesis kontroli diagnozon", e)
            }
        }

        logi("AppStato", "Inicialigito completa")
    }

    /**
     * Forigas ĉiujn referencojn. Uzata en testoj por eviti stat-poluadon.
     */
    fun reset() {
        httpKliento = null
        kanaloDeponejo = null
        elsendoDeponejo = null
        kanalaroViewModel = null
        hejmoViewModel = null
        plejŝatatajDeponejo = null
        sercxoDeponejo = null
        elshutDeponejo = null
        alarmoDeponejo = null
        ludatojDeponejo = null
        ludvicoRegilo = null
        diagnozoRegilo = null
        kanaloViewModelj.clear()
    }
}
