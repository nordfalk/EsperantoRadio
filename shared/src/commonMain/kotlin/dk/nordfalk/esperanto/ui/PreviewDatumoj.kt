package dk.nordfalk.esperanto.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.SercxoDeponejo
import dk.nordfalk.esperanto.data.repository.MemorAlarmoDeponejo
import dk.nordfalk.esperanto.data.repository.ElsendoDeponejoImpl
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
    priskribo = "Hodiaux ni parolas pri pigmentoj kaj koloroj en la naturon.",
    fluo = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
    dato = "2024-01-01",
    dauro = 6916,
)

internal fun pKanaloDeponejo() = object : KanaloDeponejo {
    private val f = MutableStateFlow(pKanaloj)
    override fun observiKanalojn() = f.asStateFlow()
    override suspend fun getKanalojn(fortoRefresigi: Boolean) = f.value
    override suspend fun getKanalo(slug: String) = f.value.find { it.slug == slug }
}

internal fun pSercxoDeponejo() = object : SercxoDeponejo {
    override suspend fun sercxi(teksto: String, limo: Int) =
        if (teksto.length >= 2) listOf(pElsendo) else emptyList()
}

internal fun pPlejsatatajDeponejo() = object : PlejsatatajDeponejo {
    private val s = MutableStateFlow(setOf("muzaiko", "kernpunkto"))
    override fun observiPlejsatatajn() = s.asStateFlow()
    override suspend fun baskuliPlejsaton(kanaloSlug: String) {}
    override suspend fun estasPlejsatata(kanaloSlug: String) = kanaloSlug in s.value
}

internal fun pElshutDeponejo() = object : ElshutDeponejo {
    private val e = MutableStateFlow(mapOf(pElsendo.id to ElshutitaElsendo(pElsendo, "/tmp/test.mp3", ElshutStato.Preta)))
    override fun observiElshutojn() = e.asStateFlow()
    override fun observiElshutStaton(elsendoId: String) = MutableStateFlow(ElshutStato.Preta).asStateFlow()
    override suspend fun elshuti(elsendo: Elsendo) {}
    override suspend fun haltigi(elsendoId: String) {}
    override suspend fun forigi(elsendoId: String) {}
    override suspend fun getLokaDosieroVojo(elsendoId: String) = "/tmp/test.mp3"
    override fun estasElshutita(elsendoId: String) = true
}

internal fun pAlarmoDeponejo() = MemorAlarmoDeponejo(
    listOf(
        Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanaloSlug = "muzaiko", aktiva = true, etikedo = "Matene"),
        Alarmo(id = 2, horo = 22, minuto = 0, ripeto = 0, kanaloSlug = "kernpunkto", aktiva = false),
    )
)

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
    override fun pauxzigi() { s.value = s.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { s.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { s.value = s.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLauxtecon(volumeno: Float) {}
}

internal class PreviewElsendoDeponejo(
    private val elsendoj: List<Elsendo>,
) : ElsendoDeponejoImpl(HttpClient(CIO)) {
    override suspend fun sxargxiElsendojn(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> {
        kaŝmemoro[kanalo.slug] = elsendoj
        fluoj.getOrPut(kanalo.slug) { MutableStateFlow(emptyList()) }.value = elsendoj
        return elsendoj
    }
}
