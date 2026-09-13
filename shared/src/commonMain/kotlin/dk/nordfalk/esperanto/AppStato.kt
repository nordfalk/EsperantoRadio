package dk.nordfalk.esperanto

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.config.parsuSugestojnPorAlarmoj
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl
import dk.nordfalk.esperanto.data.repository.PersistantaAlarmoDeponejo
import dk.nordfalk.esperanto.data.repository.PersistantaPlejŝatatajDeponejo
import dk.nordfalk.esperanto.data.repository.PersistaLudatojDeponejo
import dk.nordfalk.esperanto.data.repository.SercxoDeponejoImpl
import dk.nordfalk.esperanto.data.repository.kreuAlarmoSkedilo
import dk.nordfalk.esperanto.data.repository.kreuElshutDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.LudvicoRegilo
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.ui.HejmoViewModel
import dk.nordfalk.esperanto.ui.KanalaroViewModel
import dk.nordfalk.esperanto.ui.KanaloViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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

    /** KanaloViewModel-oj, ŝlositaj laŭ slug. */
    val kanaloViewModelj = mutableMapOf<String, KanaloViewModel>()

    fun inicialigita(): Boolean = httpKliento != null

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

        httpKliento = HttpClient(CIO) {
            install(Logging) { level = LogLevel.INFO }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
        }

        kanaloDeponejo = KanaloDeponejoImpl(
            leganto = KanalAgordoLeganto(),
            bundledTeksto = ::leguBundledKanalkonfiguron
        )

        elsendoDeponejo = ElsendoDeponejoImpl(httpKliento!!)

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
        kanaloViewModelj.clear()
    }
}
