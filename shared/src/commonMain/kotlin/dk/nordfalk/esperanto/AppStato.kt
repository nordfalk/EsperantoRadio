package dk.nordfalk.esperanto

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguPakititanKanalkonfiguron
import dk.nordfalk.esperanto.data.config.parsuSugestojnPorAlarmoj
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.ArchiveOrgDosiernomoKaŝo
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.ApajNovaĵojDeponejo
import dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl
import dk.nordfalk.esperanto.data.repository.PersistaAlarmoDeponejo
import dk.nordfalk.esperanto.data.repository.PersistaPlejŝatatajDeponejo
import dk.nordfalk.esperanto.data.repository.PersistaLudatojDeponejo
import dk.nordfalk.esperanto.data.repository.SerĉoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.kreuAlarmoSkedilo
import dk.nordfalk.esperanto.data.repository.kreuElŝutDeponejo
import dk.nordfalk.esperanto.domain.DiagnozoRegilo
import dk.nordfalk.esperanto.domain.kreuDiagnozoRegilon
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.LudvicoRegilo
import dk.nordfalk.esperanto.domain.player.elektuAlarmElsendon
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
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
    /**
     * Ĉu la apo ruliĝas en evolua medio (debug-konstruo aŭ emulilo sur
     * Android; Desktop kaj Web ĉiam). La platforma enirpunkto ŝaltas ĝin
     * ĉe starto, antaŭ la unua kunmetaĵo. En evolua medio la
     * "Apaj novaĵoj"-dialogo ne montriĝas — ĝi aperus ĉe ĉiu freŝa
     * instalaĵo (agordoj malaperas) kaj ĝenus dum programado.
     */
    var estasEvoluaMedo: Boolean = false

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
    var plejŝatatajDeponejo: PersistaPlejŝatatajDeponejo? = null
        private set
    var serĉoDeponejo: SerĉoDeponejoImpl? = null
        private set
    var elŝutDeponejo: ElŝutDeponejo? = null
        private set
    var alarmoDeponejo: PersistaAlarmoDeponejo? = null
        private set
    var ludatojDeponejo: PersistaLudatojDeponejo? = null
        private set
    var ludvicoRegilo: LudvicoRegilo? = null
        private set
    var diagnozoRegilo: DiagnozoRegilo? = null
        private set
    var apajNovaĵojDeponejo: ApajNovaĵojDeponejo? = null
        private set

    /** KanaloViewModel-oj, ŝlositaj laŭ slug. */
    val kanaloViewModelj = mutableMapOf<String, KanaloViewModel>()

    fun inicialigita(): Boolean = httpKliento != null

    /**
     * Trovas la elsendon kiun alarmo ludu por podkasta kanalo (komuna por la
     * alarmo-ricevilo kaj la ĉefaktivon). Provas laŭvice:
     * reton (freŝa RSS), diskan kaŝmemoron, kaj elŝutitajn elsendojn — ĉar ĉe vekiĝo
     * la reto ofte ankoraŭ ne pretas. Poste elektas per [elektuAlarmElsendon].
     *
     * Revenas null se AppStato ne estas inicialigita aŭ neniu elsendo troveblas.
     */
    suspend fun trovuAlarmElsendon(kanalo: Kanalo): Elsendo? {
        val elsendoDeponejo = elsendoDeponejo
            ?: return null.also { logw("AppStato", "Ne inicialigita — ne povas trovi elsendon por alarmo") }
        val elsendoj = elsendoDeponejo.ŝargiElsendojn(kanalo, fortoRefresigi = true)
            .ifEmpty { elsendoDeponejo.leguKaŝitajnElsendojn(kanalo) ?: emptyList() }
            .ifEmpty {
                elŝutDeponejo?.observiElŝutojn()?.value?.values
                    ?.filter { it.elsendo.kanaloSlug == kanalo.slug && it.stato is ElŝutStato.Preta }
                    ?.map { it.elsendo } ?: emptyList()
            }
        val ludatoj = ludatojDeponejo?.observiLudatojn()?.value ?: emptyMap()
        return elektuAlarmElsendon(elsendoj, ludatoj)
    }

    /**
     * Kreas ĉiujn deponejojn kaj ViewModel-ojn. Idempotenta — se jam inicialigita, faras nenion.
     *
     * @param ludilo la ludilo-regilo (platform-specifa, transdonita de la enirpunkto)
     * @param settings platform-specifaj agordoj (SharedPreferences / Preferences)
     * @param agordojDeponejo la agordoj-deponejo (necesas por aŭtomataDaŭrigo-fluo)
     */
    fun inicialigu(
        ludilo: LudiloRegilo,
        settings: Settings,
        agordojDeponejo: AgordojDeponejoImpl,
    ) {
        if (httpKliento != null) return

        logGrava("AppStato", "Apo malfermita (versio ${ApoVersio.VERSION})")
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
            paktitaTeksto = ::leguPakititanKanalkonfiguron
        )

        elsendoDeponejo = ElsendoDeponejoImpl(httpKliento!!, RssParsilo(ArchiveOrgDosiernomoKaŝo(settings)))

        kanalaroViewModel = KanalaroViewModel(kanaloDeponejo!!, elsendoDeponejo!!)

        // Legu kanalkonfiguron unufoje — por defaŭltaj ŝatataj kaj alarm-sugestoj
        val agordo = KanalAgordoLeganto().legu(leguPakititanKanalkonfiguron())
        val defaŭltajŜatataj = agordo.kanaloj
            .filter { it.defaŭltŜatata }
            .map { it.kodo }
            .toSet()
        plejŝatatajDeponejo = PersistaPlejŝatatajDeponejo(settings, defaŭltajŜatataj)

        serĉoDeponejo = SerĉoDeponejoImpl(elsendoDeponejo!!, kanaloDeponejo!!)

        elŝutDeponejo = kreuElŝutDeponejo(httpKliento!!)

        val sugestoj = agordo.sugestoj_por_alarmoj?.let { parsuSugestojnPorAlarmoj(it) } ?: emptyList()
        alarmoDeponejo = PersistaAlarmoDeponejo(settings, sugestoj, kreuAlarmoSkedilo())

        // Apaj novaĵoj (novaĵo-mesaĝo de la apo) el la android-sekcio de la konfiguro
        apajNovaĵojDeponejo = ApajNovaĵojDeponejo(settings, agordo.android?.apajNovaĵoj)

        ludatojDeponejo = PersistaLudatojDeponejo(settings)

        hejmoViewModel = HejmoViewModel(kanaloDeponejo!!, elsendoDeponejo!!, ludatojDeponejo!!)

        ludvicoRegilo = LudvicoRegilo(
            ludilo = ludilo,
            elsendoDeponejo = elsendoDeponejo!!,
            kanaloDeponejo = kanaloDeponejo!!,
            plejŝatatajDeponejo = plejŝatatajDeponejo!!,
            ludatojDeponejo = ludatojDeponejo!!,
            getLokaDosieroVojo = { id -> elŝutDeponejo!!.getLokaDosieroVojo(id) },
            aŭtomataDaŭrigo = agordojDeponejo.aŭtomataDaŭrigo,
        )

        diagnozoRegilo = kreuDiagnozoRegilon()

        // Aŭtomata rapida kontrolo en la fono — por montri avertosignon sur la
        // ĉefekrano. Dispatchers.Default, ne IO: IO ne ekzistas sur wasmJs (la
        // DNS-testo de la Android-implemento rulas en propra fadeno interne).
        // La rezulto ankaŭ purigas la kaŝojn de problemoj kiuj ne plu ekzistas:
        // se problemo malaperas (ekz. la uzanto riparis ĝin), ĝia kaŝo
        // forgesiĝas — se ĝi poste revenas, la avertokarto reaperos.
        // ATENTU: purigu nur per la kontrol-REZULTO, neniam per elsendo de
        // `problemoj` — la komenca elsendo de tiu StateFlow estas la malplena
        // listo ANTAŬ la unua kontrolo, kaj purigi per ĝi forviŝus la
        // persistitan kaŝon je ĉiu apstarto (2026-10-22: la averto-karto
        // reaperis post ĉiu relanĉo).
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                val rezultoj = diagnozoRegilo?.kontroliRapide() ?: return@launch
                agordojDeponejo.puriguKaŝitajnAvertojn(rezultoj.map { it.tipo }.toSet())
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
        serĉoDeponejo = null
        elŝutDeponejo = null
        alarmoDeponejo = null
        ludatojDeponejo = null
        ludvicoRegilo = null
        diagnozoRegilo = null
        apajNovaĵojDeponejo = null
        kanaloViewModelj.clear()
    }
}
