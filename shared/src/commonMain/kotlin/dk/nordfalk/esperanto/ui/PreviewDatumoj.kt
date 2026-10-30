package dk.nordfalk.esperanto.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.SerĉoDeponejo
import dk.nordfalk.esperanto.domain.repository.SerĉoRezulto
import dk.nordfalk.esperanto.data.repository.MemorAlarmoDeponejo
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import io.ktor.client.HttpClient
import dk.nordfalk.esperanto.data.repository.httpMotoro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// === Fiksaj test-datumoj por antaŭvidoj ===

internal val pKanaloj = listOf(
    Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"),
    Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"),
    Kanalo(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://www.podkasto.net/feed/"),
)

internal val pElsendo = Elsendo(
    id = "kernpunkto:2024-01-01",
    kanaloSlug = "kernpunkto",
    titolo = "KP204 Pigmentoj kaj koloroj en la naturo",
    priskribo = "Hodiaŭ ni parolas pri pigmentoj kaj koloroj en la naturon.",
    fluo = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
    dato = "2024-01-01",
    daŭro = 6916,
)

internal fun pKanaloDeponejo() = object : KanaloDeponejo {
    private val f = MutableStateFlow(pKanaloj)
    override fun observiKanalojn() = f.asStateFlow()
    override suspend fun getKanalojn(fortoRefresigi: Boolean) = f.value
    override suspend fun getKanalo(slug: String) = f.value.find { it.slug == slug }
}

internal fun pSerĉoDeponejo() = object : SerĉoDeponejo {
    override suspend fun serĉi(teksto: String, limo: Int) =
        if (teksto.length >= 2) SerĉoRezulto(elsendojLaŭTitolo = listOf(pElsendo)) else SerĉoRezulto()
}

internal fun pPlejŝatatajDeponejo() = object : PlejŝatatajDeponejo {
    private val s = MutableStateFlow(setOf("muzaiko", "kernpunkto"))
    override fun observiPlejŝatatajn() = s.asStateFlow()
    override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
    override suspend fun estasPlejŝatata(kanaloSlug: String) = kanaloSlug in s.value
}

internal fun pElŝutDeponejo() = object : ElŝutDeponejo {
    private val e = MutableStateFlow(mapOf(pElsendo.id to ElŝutitaElsendo(pElsendo, "/tmp/test.mp3", ElŝutStato.Preta, dosierGrando = 55_300_000)))
    override fun observiElŝutojn() = e.asStateFlow()
    override fun observiElŝutStaton(elsendoId: String) = MutableStateFlow(ElŝutStato.Preta).asStateFlow()
    override suspend fun elŝuti(elsendo: Elsendo) {}
    override suspend fun haltigi(elsendoId: String) {}
    override suspend fun forigi(elsendoId: String) {}
    override suspend fun getLokaDosieroVojo(elsendoId: String) = "/tmp/test.mp3"
    override fun estasElŝutita(elsendoId: String) = true
}

internal fun pAlarmoDeponejo() = MemorAlarmoDeponejo(
    listOf(
        Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanaloSlug = "muzaiko", aktiva = true, etikedo = "Matene"),
        Alarmo(id = 2, horo = 22, minuto = 0, ripeto = 0, kanaloSlug = "kernpunkto", aktiva = false),
    )
)

internal fun pLudatojDeponejo(
    ludatoj: Map<String, LudataElsendo> = mapOf(
        pElsendo.id to LudataElsendo(
            elsendoId = pElsendo.id,
            kanaloSlug = "kernpunkto",
            pozicioMs = 3_000_000,
            daŭroMs = 6_916_000,
            finita = false,
            lasteLudita = 1_704_067_200_000, // 2024-01-01
        ),
    ),
) = object : dk.nordfalk.esperanto.domain.repository.LudatojDeponejo {
    private val f = MutableStateFlow(ludatoj)
    override fun observiLudatojn() = f.asStateFlow()
    override suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, daŭroMs: Long) {}
    override suspend fun markiLudita(elsendoId: String, kanaloSlug: String) {}
    override suspend fun markiFinita(elsendoId: String, kanaloSlug: String) {}
    override suspend fun markiErara(elsendoId: String, kanaloSlug: String) {}
    override suspend fun malmarkiFinita(elsendoId: String, kanaloSlug: String) {}
    override suspend fun getLudato(elsendoId: String) = f.value[elsendoId]
    override suspend fun estasFinita(elsendoId: String) = f.value[elsendoId]?.finita ?: false
    override suspend fun getPozicio(elsendoId: String) = f.value[elsendoId]?.pozicioMs
}

@Composable
internal fun pTemo(temo: TemoNomo = TemoNomo.ANTONIA, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = temuKolorskemo(temo, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
        content()
    }
}

internal class PreviewLudiloRegilo(initial: LudantoInformo = LudantoInformo(stato = LudantoStato.Haltita)) : LudiloRegilo {
    private val s = MutableStateFlow(initial)
    override val stato: StateFlow<LudantoInformo> = s.asStateFlow()
    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        s.value = LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = fonto, pozicioMs = komencoPozicioMs, estasRekta = fonto is Sonfonto.RektaKanalo)
    }
    override fun ludi() { s.value = s.value.copy(stato = LudantoStato.Ludas) }
    override fun paŭzigi() { s.value = s.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { s.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { s.value = s.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLaŭtecon(volumeno: Float) {}
    override val laŭteco: StateFlow<Float> = MutableStateFlow(1f)
}

internal class PreviewElsendoDeponejo(
    private val elsendoj: List<Elsendo>,
) : ElsendoDeponejoImpl(HttpClient(httpMotoro)) {
    override suspend fun ŝargiElsendojn(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> {
        konservu(kanalo.slug, elsendoj)
        return elsendoj
    }
}

internal fun pElsendoDeponejo() = PreviewElsendoDeponejo(listOf(pElsendo))
