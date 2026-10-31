package dk.nordfalk.esperanto.domain.repository

import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlejŝatatajDeponejo {
    fun observiPlejŝatatajn(): StateFlow<Set<String>>  // kanalo-slugs
    suspend fun baskuliPlejŝaton(kanaloSlug: String)
    suspend fun estasPlejŝatata(kanaloSlug: String): Boolean
}

interface LastAŭskultitajDeponejo {
    fun observiLastAŭskultitajn(): StateFlow<List<Elsendo>>
    suspend fun registri(elsendo: Elsendo)
    suspend fun getPozicio(elsendoId: String): Long?
}

/**
 * Spuras ludstatuson de elsendoj — pozicio, ĉu finita, tempmarko.
 * Uzata de LudvicoRegilo por resumigo kaj aŭtomata sekva-ludado.
 */
interface LudatojDeponejo {
    fun observiLudatojn(): StateFlow<Map<String, LudataElsendo>>
    suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, daŭroMs: Long)

    /**
     * Metas la tempmarkon lasteLudita al nun — sen tuŝi pozicion aŭ daŭron.
     * Vokata kiam la ludado vere ekis (ne jam ĉe klako sur "Ludi"),
     * por ke la elsendo aperu en "Lastatempe ludata" nur tiam.
     */
    suspend fun markiLudita(elsendoId: String, kanaloSlug: String)
    suspend fun markiFinita(elsendoId: String, kanaloSlug: String)
    suspend fun markiErara(elsendoId: String, kanaloSlug: String)
    suspend fun malmarkiFinita(elsendoId: String, kanaloSlug: String)
    suspend fun getLudato(elsendoId: String): LudataElsendo?
    suspend fun estasFinita(elsendoId: String): Boolean
    suspend fun getPozicio(elsendoId: String): Long?
}

/**
 * Rezulto de serĉo (C60): `kanaloj` kaj `elsendoj`, ĉiu jam ordigita laŭ graveco
 * — titolo-trovoj unue, poste nur-priskribo-trovoj (dedup: ero aperas nur unufoje).
 * La tuta montra sinsekvo estas: ĉiuj kanaloj (titolo→priskribo), poste ĉiuj
 * elsendoj (titolo→priskribo).
 */
data class SerĉoRezulto(
    val kanaloj: List<Kanalo> = emptyList(),
    val elsendoj: List<Elsendo> = emptyList(),
) {
    val estasVacia: Boolean get() = kanaloj.isEmpty() && elsendoj.isEmpty()
}

interface SerĉoDeponejo {
    suspend fun serĉi(teksto: String, limo: Int = 50): SerĉoRezulto
}

interface AgordojDeponejo {
    val temo: StateFlow<String>
    val sciigoj: StateFlow<Boolean>
    val aŭtomataDaŭrigo: StateFlow<Boolean>
    val evoluo: StateFlow<Boolean>
    val detalaProtokolado: StateFlow<Boolean>

    /** Ĉu la ludilo transsaltas la ripetiĝantan enkondukon de kanaloj (C59a). */
    val transsaltuEnkondukojn: StateFlow<Boolean>

    /** Ĉu montri la snackbar-on pri transsaltita enkonduko. */
    val montruTranssaltanSciigon: StateFlow<Boolean>

    /**
     * Ĉu la uzanto konsentis al aŭtomata erar-raportado al Sentry
     * (opt-in, defaŭlte MALŜALTITA — F-Droid "Tracking"-kontraŭeco, C50).
     */
    val erarRaportado: StateFlow<Boolean>

    /** Ĉu la konsento-demando jam estis respondita (dialogo unufoje, ĉe la unua starto). */
    val erarRaportadoDemandita: StateFlow<Boolean>

    /**
     * Problemoj kiujn la uzanto kaŝis per la 'x'-butono de la avertokarto sur
     * la ĉefekrano. La avertoj ĉiam restas videblaj en Agordoj → Diagnozo.
     */
    val kaŝitajAvertoj: StateFlow<Set<DiagnozoTipo>>

    fun fiksiTemon(temo: String)
    fun fiksiSciigojn(ŝaltita: Boolean)
    fun fiksiAŭtomatanDaŭrigon(ŝaltita: Boolean)
    fun fiksiEvoluon(ŝaltita: Boolean)
    fun fiksiDetalanProtokoladon(ŝaltita: Boolean)
    fun fiksiTranssaltuEnkondukojn(ŝaltita: Boolean)
    fun fiksiMontruTranssaltanSciigon(ŝaltita: Boolean)

    /** Ŝaltas/malŝaltas la erar-raportadon kaj memorigas, ke la demando estas respondita. */
    fun fiksiErarRaportadon(ŝaltita: Boolean)

    /** Memorigas rifuzon de la konsento-demando (sen ŝalti la raportadon). */
    fun konfirmuErarRaportadoDemandon()

    /** Kaŝas la donitajn problemo-tipojn (anstataŭigas la tutan aron). */
    fun kaŝuAvertojn(tipoj: Set<DiagnozoTipo>)

    /** Forgesas la kaŝojn de problemo-tipoj kiuj ne plu ekzistas */
    fun puriguKaŝitajnAvertojn(ekzistantaj: Set<DiagnozoTipo>)
}

/**
 * Elŝut-deponejo. Platform-specifaj implementoj:
 * - Android/Desktop: Ktor → dosiero
 * - wasmJs/iOS: NoOp (ne haveblas)
 */
interface ElŝutDeponejo {
    fun observiElŝutojn(): StateFlow<Map<String, ElŝutitaElsendo>>  // elsendoId → stato
    fun observiElŝutStaton(elsendoId: String): StateFlow<ElŝutStato>
    suspend fun elŝuti(elsendo: Elsendo)
    suspend fun haltigi(elsendoId: String)
    suspend fun forigi(elsendoId: String)
    suspend fun getLokaDosieroVojo(elsendoId: String): String?
    fun estasElŝutita(elsendoId: String): Boolean
}

/**
 * Vekhorloĝo-deponejo. Platform-specifaj implementoj:
 * - Android: AlarmManager + BroadcastReceiver (estonte)
 * - Desktop/wasmJs/iOS: NoOp (nur UI, ne planas vere)
 */
interface AlarmoDeponejo {
    fun observiAlarmojn(): StateFlow<List<Alarmo>>
    suspend fun krei(alarmo: Alarmo)
    suspend fun ĝisdatigi(alarmo: Alarmo)
    suspend fun forigi(alarmoId: Int)
    suspend fun baskuliAktivon(alarmoId: Int)
}
