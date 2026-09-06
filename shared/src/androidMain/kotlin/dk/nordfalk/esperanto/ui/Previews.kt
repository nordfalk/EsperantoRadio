package dk.nordfalk.esperanto.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.SercxoDeponejo
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.MemorAlarmoDeponejo
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// === Fiksaj test-datumoj ===

private val pKanaloj = listOf(
    Kanal(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"),
    Kanal(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"),
    Kanal(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://www.podkasto.net/feed/"),
)

private val pElsendo = Elsendo(
    id = "kernpunkto:2024-01-01",
    kanalSlug = "kernpunkto",
    titolo = "KP204 Pigmentoj kaj koloroj en la naturo",
    priskribo = "Hodiaux ni parolas pri pigmentoj kaj koloroj en la naturon.",
    stream = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
    dato = "2024-01-01",
    dauro = 6916,
)

private fun pKanalDeponejo() = object : KanalDeponejo {
    private val f = MutableStateFlow(pKanaloj)
    override fun observiKanalojn() = f.asStateFlow()
    override suspend fun getKanalojn(fortoRefresigi: Boolean) = f.value
    override suspend fun getKanal(slug: String) = f.value.find { it.slug == slug }
}

@Composable
private fun pTemo(temo: TemoNomo = TemoNomo.ANTONIA, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = temuKolorskemo(temo, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
        content()
    }
}

// === Previews ===

@Preview(name = "Kanalaro — Antonia", showBackground = true)
@Composable
fun PreviewKanalaroAntonia() {
    pTemo(TemoNomo.ANTONIA) {
        KanalaroEkrano(viewModel = KanalaroViewModel(pKanalDeponejo()))
    }
}

@Preview(name = "Kanalaro — Rugxa", showBackground = true)
@Composable
fun PreviewKanalaroRugxa() {
    pTemo(TemoNomo.RUGXA) {
        KanalaroEkrano(viewModel = KanalaroViewModel(pKanalDeponejo()))
    }
}

@Preview(name = "Kanalaro — Verda", showBackground = true)
@Composable
fun PreviewKanalaroVerda() {
    pTemo(TemoNomo.VERDA) {
        KanalaroEkrano(viewModel = KanalaroViewModel(pKanalDeponejo()))
    }
}

@Preview(name = "Elsendo detalo", showBackground = true)
@Composable
fun PreviewElsendoDetalo() {
    pTemo() {
        ElsendoEkrano(elsendo = pElsendo, onReen = {}, onLudi = {}, onElshuti = {})
    }
}

@Preview(name = "Serchxo", showBackground = true)
@Composable
fun PreviewSerchxo() {
    val deponejo = object : SercxoDeponejo {
        override suspend fun sercxi(taxto: String, limo: Int) =
            if (taxto.length >= 2) listOf(pElsendo) else emptyList()
    }
    pTemo() {
        SercxoEkrano(sercxoDeponejo = deponejo, onReen = {}, onElsendo = {})
    }
}

@Preview(name = "Plejsatataj", showBackground = true)
@Composable
fun PreviewPlejsatataj() {
    val plej = object : PlejsatatajDeponejo {
        private val s = MutableStateFlow(setOf("muzaiko", "kernpunkto"))
        override fun observiPlejsatatajn() = s.asStateFlow()
        override suspend fun baskuliPlejsaton(kanalSlug: String) {}
        override suspend fun estasPlejsatata(kanalSlug: String) = kanalSlug in s.value
    }
    pTemo() {
        PlejsatatajEkrano(plejsatatajDeponejo = plej, kanalDeponejo = pKanalDeponejo(), onReen = {}, onKanal = {})
    }
}

@Preview(name = "Elshutitaj", showBackground = true)
@Composable
fun PreviewElshutitaj() {
    val deponejo = object : ElshutDeponejo {
        private val e = MutableStateFlow(mapOf(pElsendo.id to ElshutitaElsendo(pElsendo, "/tmp/test.mp3", ElshutStato.Preta)))
        override fun observiElshutojn() = e.asStateFlow()
        override fun observiElshutStaton(elsendoId: String) = MutableStateFlow(ElshutStato.Preta).asStateFlow()
        override suspend fun elshuti(elsendo: Elsendo) {}
        override suspend fun haltigi(elsendoId: String) {}
        override suspend fun forigi(elsendoId: String) {}
        override suspend fun getLokaDosieroVojo(elsendoId: String) = "/tmp/test.mp3"
        override fun estasElshutita(elsendoId: String) = true
    }
    pTemo() {
        ElshutitajEkrano(elshutDeponejo = deponejo, onReen = {}, onLudi = {}, onElsendo = {})
    }
}

@Preview(name = "Alarmoj", showBackground = true)
@Composable
fun PreviewAlarmoj() {
    val alarmoj = MemorAlarmoDeponejo(
        listOf(
            Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanalSlug = "muzaiko", aktiva = true, etikedo = "Matene"),
            Alarmo(id = 2, horo = 22, minuto = 0, ripeto = 0, kanalSlug = "kernpunkto", aktiva = false),
        )
    )
    pTemo() {
        AlarmoEkrano(alarmoDeponejo = alarmoj, kanalDeponejo = pKanalDeponejo(), onReen = {})
    }
}

@Preview(name = "Agordoj", showBackground = true)
@Composable
fun PreviewAgordoj() {
    pTemo() {
        AgordojEkrano(agordojDeponejo = AgordojDeponejoImpl(), onReen = {})
    }
}

@Preview(name = "Mini ludilbreto", showBackground = true)
@Composable
fun PreviewMiniLudilbreto() {
    val ludilo = PreviewLudiloRegilo(
        LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.ElsendoFonto(pElsendo), pozicioMs = 30000, dauroMs = 6916000, estasRekta = false)
    )
    pTemo() {
        MiniLudilbreto(ludilo = ludilo)
    }
}

@Preview(name = "Hejmo (nova)", showBackground = true, widthDp = 411, heightDp = 731)
@Composable
fun PreviewHejmoNova() {
    val elsendoDeponejo = object : dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo {
        override fun observiElsendojn(kanalSlug: String) = MutableStateFlow(emptyList<dk.nordfalk.esperanto.domain.model.Elsendo>()).asStateFlow()
        override suspend fun getElsendojn(kanalSlug: String, fortoRefresigi: Boolean) = emptyList<dk.nordfalk.esperanto.domain.model.Elsendo>()
        override suspend fun getElsendo(id: String) = null
        override suspend fun sercxiElsendojn(taxto: String, limo: Int) = emptyList<dk.nordfalk.esperanto.domain.model.Elsendo>()
    }
    pTemo() {
        HejmoEkrano(
            kanalDeponejo = pKanalDeponejo(),
            elsendoDeponejo = elsendoDeponejo,
        )
    }
}

private class PreviewLudiloRegilo(initial: LudantoInformo = LudantoInformo(stato = LudantoStato.Haltita)) : LudiloRegilo {
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
