package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.LastAŭskultitajDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.repository.SerĉoDeponejo
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.protokoloDetala
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PlejŝatatajDeponejoImpl : PlejŝatatajDeponejo {
    private val _plejŝatataj = MutableStateFlow<Set<String>>(emptySet())
    override fun observiPlejŝatatajn(): StateFlow<Set<String>> = _plejŝatataj.asStateFlow()

    override suspend fun baskuliPlejŝaton(kanaloSlug: String) {
        val nuna = _plejŝatataj.value.toMutableSet()
        if (kanaloSlug in nuna) nuna.remove(kanaloSlug) else nuna.add(kanaloSlug)
        _plejŝatataj.value = nuna
        logi("Plejŝatataj", "Baskulas: $kanaloSlug → ${if (kanaloSlug in nuna) "aldonita" else "forigita"} (total ${nuna.size})")
    }

    override suspend fun estasPlejŝatata(kanaloSlug: String): Boolean = kanaloSlug in _plejŝatataj.value
}

class LastAŭskultitajDeponejoImpl : LastAŭskultitajDeponejo {
    private val _listo = MutableStateFlow<List<Elsendo>>(emptyList())
    private val pozicioj = mutableMapOf<String, Long>()

    override fun observiLastAŭskultitajn(): StateFlow<List<Elsendo>> = _listo.asStateFlow()

    override suspend fun registri(elsendo: Elsendo) {
        val nuna = _listo.value.toMutableList()
        nuna.removeAll { it.id == elsendo.id }
        nuna.add(0, elsendo)
        _listo.value = nuna.take(50) // LRU-maks 50
        logd("LastAŭskultitaj", "Registras: ${elsendo.id} — ${elsendo.titolo}")
    }

    override suspend fun getPozicio(elsendoId: String): Long? = pozicioj[elsendoId]
}

/**
 * Memora LudatojDeponejo — sen persistaj flankaj efikoj.
 * Uzu nur dum Preview kaj testoj. Por produktado uzu PersistaLudatojDeponejo.
 */
@OptIn(ExperimentalTime::class)
class LudatojDeponejoMaketo : LudatojDeponejo {
    private val _ludatoj = MutableStateFlow<Map<String, LudataElsendo>>(emptyMap())
    override fun observiLudatojn(): StateFlow<Map<String, LudataElsendo>> = _ludatoj.asStateFlow()

    /** Test-helpo: rekte enmetu ludaton (ekz. kun difinita lasteLudita — C59a-testoj). */
    fun enmetuLudaton(ludata: LudataElsendo) {
        _ludatoj.value = _ludatoj.value + (ludata.elsendoId to ludata)
    }

    override suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, daŭroMs: Long) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudataElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = pozicioMs,
            daŭroMs = if (daŭroMs > 0) daŭroMs else (ekzista?.daŭroMs ?: 0),
            finita = ekzista?.finita ?: false,
            erara = ekzista?.erara ?: false,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        _ludatoj.value = nuna
    }

    override suspend fun markiLudita(elsendoId: String, kanaloSlug: String) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId] ?: LudataElsendo(elsendoId = elsendoId, kanaloSlug = kanaloSlug)
        nuna[elsendoId] = ekzista.copy(lasteLudita = Clock.System.now().toEpochMilliseconds())
        _ludatoj.value = nuna
    }

    override suspend fun markiFinita(elsendoId: String, kanaloSlug: String) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudataElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = ekzista?.pozicioMs ?: 0,
            daŭroMs = ekzista?.daŭroMs ?: 0,
            finita = true,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        _ludatoj.value = nuna
    }

    override suspend fun markiErara(elsendoId: String, kanaloSlug: String) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudataElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = ekzista?.pozicioMs ?: 0,
            daŭroMs = ekzista?.daŭroMs ?: 0,
            finita = true,
            erara = true,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        _ludatoj.value = nuna
    }

    override suspend fun malmarkiFinita(elsendoId: String, kanaloSlug: String) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId] ?: return
        nuna[elsendoId] = ekzista.copy(finita = false, erara = false, pozicioMs = 0)
        _ludatoj.value = nuna
    }

    override suspend fun getLudato(elsendoId: String): LudataElsendo? = _ludatoj.value[elsendoId]
    override suspend fun estasFinita(elsendoId: String): Boolean = _ludatoj.value[elsendoId]?.finita ?: false
    override suspend fun getPozicio(elsendoId: String): Long? = _ludatoj.value[elsendoId]?.pozicioMs?.takeIf { it > 0 }
}

class SerĉoDeponejoImpl(
    private val elsendoDeponejo: ElsendoDeponejo,
) : SerĉoDeponejo {
    override suspend fun serĉi(teksto: String, limo: Int): List<Elsendo> {
        val rezulto = elsendoDeponejo.serĉiElsendojn(teksto, limo)
        logd("Serĉo", "Serĉas '$teksto' (limo=$limo) — ${rezulto.size} trovoj")
        return rezulto
    }
}

class AgordojDeponejoImpl(
    private val settings: Settings? = null,
) : AgordojDeponejo {
    private val _temo = MutableStateFlow("ANTONIA")
    override val temo: StateFlow<String> = _temo.asStateFlow()

    private val _sciigoj = MutableStateFlow(settings?.getBoolean(SettingsKeys.SCIIGOJ, true) ?: true)
    override val sciigoj: StateFlow<Boolean> = _sciigoj.asStateFlow()

    private val _aŭtomataDaŭrigo = MutableStateFlow(settings?.leguMigritanBuleon(SettingsKeys.AŬTOMATA_DAŬRIGO, SettingsKeys.AŬTOMATA_DAŬRIGO_MALNOVA, true) ?: true)
    override val aŭtomataDaŭrigo: StateFlow<Boolean> = _aŭtomataDaŭrigo.asStateFlow()

    private val _transsaltuEnkondukojn = MutableStateFlow(settings?.getBoolean(SettingsKeys.TRANSSALTU_ENKONDUKOJN, true) ?: true)
    override val transsaltuEnkondukojn: StateFlow<Boolean> = _transsaltuEnkondukojn.asStateFlow()

    private val _montruTranssaltanSciigon = MutableStateFlow(settings?.getBoolean(SettingsKeys.MONTRU_TRANSSALTAN_SCIIGON, true) ?: true)
    override val montruTranssaltanSciigon: StateFlow<Boolean> = _montruTranssaltanSciigon.asStateFlow()

    private val _evoluo = MutableStateFlow(settings?.getBoolean(SettingsKeys.EVOLO, false) ?: false)
    override val evoluo: StateFlow<Boolean> = _evoluo.asStateFlow()

    private val _detalaProtokolado = MutableStateFlow(settings?.getBoolean(SettingsKeys.DETALA_PROTOKOLADO, true) ?: true)
    override val detalaProtokolado: StateFlow<Boolean> = _detalaProtokolado.asStateFlow()

    private val _kaŝitajAvertoj = MutableStateFlow(
        settings?.leguMigritanĈenon(SettingsKeys.KAŜITAJ_AVERTOJ, SettingsKeys.KAŜITAJ_AVERTOJ_MALNOVA, "")?.split(",")?.mapNotNull { tipoDeNomo(it) }?.toSet() ?: emptySet()
    )
    override val kaŝitajAvertoj: StateFlow<Set<DiagnozoTipo>> = _kaŝitajAvertoj.asStateFlow()

    init {
        // Sinkronigu la tutmondan protokol-flagon (legata de logi en ProtokoloUtil)
        protokoloDetala = _detalaProtokolado.value
    }

    /** Persistas la aron kiel kom-separated nomoj (nekonataj nomoj estas ignorataj). */
    private fun persistu(tipoj: Set<DiagnozoTipo>) {
        settings?.skribuMigritanĈenon(SettingsKeys.KAŜITAJ_AVERTOJ, SettingsKeys.KAŜITAJ_AVERTOJ_MALNOVA, tipoj.joinToString(",") { it.name })
    }

    private fun tipoDeNomo(nomo: String): DiagnozoTipo? =
        nomo.takeIf { it.isNotBlank() }?.let { n -> DiagnozoTipo.entries.firstOrNull { it.name == n } }

    override fun fiksiTemon(temo: String) {
        _temo.value = temo
        logi("Agordoj", "Temo → $temo")
    }
    override fun fiksiSciigojn(ŝaltita: Boolean) {
        _sciigoj.value = ŝaltita
        settings?.putBoolean(SettingsKeys.SCIIGOJ, ŝaltita)
        logi("Agordoj", "Sciigoj → $ŝaltita")
    }
    override fun fiksiAŭtomatanDaŭrigon(ŝaltita: Boolean) {
        _aŭtomataDaŭrigo.value = ŝaltita
        settings?.skribuMigritanBuleon(SettingsKeys.AŬTOMATA_DAŬRIGO, SettingsKeys.AŬTOMATA_DAŬRIGO_MALNOVA, ŝaltita)
        logi("Agordoj", "AŭtomataDaŭrigo → $ŝaltita")
    }
    override fun fiksiTranssaltuEnkondukojn(ŝaltita: Boolean) {
        _transsaltuEnkondukojn.value = ŝaltita
        settings?.putBoolean(SettingsKeys.TRANSSALTU_ENKONDUKOJN, ŝaltita)
        logi("Agordoj", "TranssaltuEnkondukojn → $ŝaltita")
    }
    override fun fiksiMontruTranssaltanSciigon(ŝaltita: Boolean) {
        _montruTranssaltanSciigon.value = ŝaltita
        settings?.putBoolean(SettingsKeys.MONTRU_TRANSSALTAN_SCIIGON, ŝaltita)
        logi("Agordoj", "MontruTranssaltanSciigon → $ŝaltita")
    }
    override fun fiksiEvoluon(ŝaltita: Boolean) {
        _evoluo.value = ŝaltita
        settings?.putBoolean(SettingsKeys.EVOLO, ŝaltita)
        logi("Agordoj", "Evoluo → $ŝaltita")
    }
    override fun fiksiDetalanProtokoladon(ŝaltita: Boolean) {
        _detalaProtokolado.value = ŝaltita
        protokoloDetala = ŝaltita
        settings?.putBoolean(SettingsKeys.DETALA_PROTOKOLADO, ŝaltita)
        logi("Agordoj", "DetalaProtokolado → $ŝaltita")
    }
    override fun kaŝuAvertojn(tipoj: Set<DiagnozoTipo>) {
        _kaŝitajAvertoj.value = tipoj
        persistu(tipoj)
        logi("Agordoj", "KaŝitajAvertoj → $tipoj")
    }
    override fun puriguKaŝitajnAvertojn(ekzistantaj: Set<DiagnozoTipo>) {
        val purigita = _kaŝitajAvertoj.value intersect ekzistantaj
        if (purigita != _kaŝitajAvertoj.value) {
            _kaŝitajAvertoj.value = purigita
            persistu(purigita)
            logi("Agordoj", "Purigis kaŝitajn avertojn → $purigita")
        }
    }
}
