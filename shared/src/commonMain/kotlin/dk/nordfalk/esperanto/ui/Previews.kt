package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudvicoRegilo

// === Antaŭvidoj — pli malaltaj por eviti vakuon ===

@Preview(name = "Kanalaro — Antonia", showBackground = true, heightDp = 250)
@Composable
fun PreviewKanalaroAntonia() {
    pTemo(TemoNomo.ANTONIA) {
        KanalaroEkrano(viewModel = KanalaroViewModel(pKanaloDeponejo()))
    }
}

@Preview(name = "Kanalaro — Rugxa", showBackground = true, heightDp = 250)
@Composable
fun PreviewKanalaroRugxa() {
    pTemo(TemoNomo.RUGXA) {
        KanalaroEkrano(viewModel = KanalaroViewModel(pKanaloDeponejo()))
    }
}

@Preview(name = "Kanalaro — Verda", showBackground = true, heightDp = 250)
@Composable
fun PreviewKanalaroVerda() {
    pTemo(TemoNomo.VERDA) {
        KanalaroEkrano(viewModel = KanalaroViewModel(pKanaloDeponejo()))
    }
}

@Preview(name = "KanaloEkrano — Kernpunkto", showBackground = true, heightDp = 400)
@Composable
fun PreviewKanaloEkrano() {
    pTemo() {
        KanaloEkrano(
            kanalo = pKanaloj[1],
            elsendoDeponejo = PreviewElsendoDeponejo(listOf(pElsendo)),
            plejŝatatajDeponejo = pPlejŝatatajDeponejo(),
            agordojDeponejo = AgordojDeponejoImpl(),
            onReen = {},
        )
    }
}

@Preview(name = "Elsendo detalo", showBackground = true, heightDp = 500)
@Composable
fun PreviewElsendoDetalo() {
    pTemo() {
        ElsendoEkrano(elsendo = pElsendo, onReen = {}, onLudi = {}, onElshuti = {})
    }
}

@Preview(name = "Serchxo", showBackground = true, heightDp = 350)
@Composable
fun PreviewSerchxo() {
    pTemo() {
        SercxoEkrano(sercxoDeponejo = pSercxoDeponejo(), onElsendo = {})
    }
}

@Preview(name = "Plejŝatataj", showBackground = true, heightDp = 250)
@Composable
fun PreviewPlejŝatataj() {
    pTemo() {
        PlejŝatatajEkrano(plejŝatatajDeponejo = pPlejŝatatajDeponejo(), kanaloDeponejo = pKanaloDeponejo(), onKanalo = {})
    }
}

@Preview(name = "Elshutitaj", showBackground = true, heightDp = 250)
@Composable
fun PreviewElshutitaj() {
    pTemo() {
        ElshutitajEkrano(elshutDeponejo = pElshutDeponejo(), onReen = {}, onLudi = {}, onElsendo = {})
    }
}

@Preview(name = "Alarmoj", showBackground = true, heightDp = 350)
@Composable
fun PreviewAlarmoj() {
    pTemo() {
        AlarmoEkrano(alarmoDeponejo = pAlarmoDeponejo(), kanaloDeponejo = pKanaloDeponejo(), onReen = {})
    }
}

@Preview(name = "Agordoj", showBackground = true, heightDp = 400)
@Composable
fun PreviewAgordoj() {
    pTemo() {
        AgordojEkrano(agordojDeponejo = AgordojDeponejoImpl(), onReen = {})
    }
}

@Preview(name = "Mini ludilbreto — ludas", showBackground = true, heightDp = 80)
@Composable
fun PreviewMiniLudilbretoLudas() {
    val ludilo = PreviewLudiloRegilo(
        LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.ElsendoFonto(pElsendo), pozicioMs = 30000, dauroMs = 6916000, estasRekta = false)
    )
    pTemo() {
        MiniLudilbreto(ludilo = ludilo)
    }
}

@Preview(name = "Mini ludilbreto — rekta", showBackground = true, heightDp = 80)
@Composable
fun PreviewMiniLudilbretoRekta() {
    val ludilo = PreviewLudiloRegilo(
        LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.RektaKanalo(pKanaloj[0]), pozicioMs = 0, dauroMs = 0, estasRekta = true)
    )
    pTemo() {
        MiniLudilbreto(ludilo = ludilo)
    }
}

@Preview(name = "Mini ludilbreto — haltita", showBackground = true, heightDp = 80)
@Composable
fun PreviewMiniLudilbretoHaltita() {
    val ludilo = PreviewLudiloRegilo(
        LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = Sonfonto.ElsendoFonto(pElsendo), pozicioMs = 120000, dauroMs = 6916000, estasRekta = false)
    )
    pTemo() {
        MiniLudilbreto(ludilo = ludilo)
    }
}

@Preview(name = "Hejmo (nova)", showBackground = true, widthDp = 411, heightDp = 731)
@Composable
fun PreviewHejmoNova() {
    val elsendoDeponejo = PreviewElsendoDeponejo(listOf(pElsendo))
    pTemo() {
        HejmoEkrano(
            kanaloDeponejo = pKanaloDeponejo(),
            elsendoDeponejo = elsendoDeponejo,
            ludatojDeponejo = pLudatojDeponejo(),
            ludilo = PreviewLudiloRegilo(),
        )
    }
}

@Preview(name = "Malsupra naviga breto — Hejmo", showBackground = true, heightDp = 80)
@Composable
fun PreviewMalsupraNavigaBretoHejmo() {
    pTemo() {
        MalsupraNavigaBreto(
            nunaTab = EkranoLangeto.HEJMO,
            onHejmo = {}, onKanalaro = {}, onPlejŝatataj = {}, onSercxo = {},
        )
    }
}

@Preview(name = "Malsupra naviga breto — Kanaloj", showBackground = true, heightDp = 80)
@Composable
fun PreviewMalsupraNavigaBretoKanaloj() {
    pTemo() {
        MalsupraNavigaBreto(
            nunaTab = EkranoLangeto.KANALARO,
            onHejmo = {}, onKanalaro = {}, onPlejŝatataj = {}, onSercxo = {},
        )
    }
}

@Preview(name = "Malsupra naviga breto — Serĉo", showBackground = true, heightDp = 80)
@Composable
fun PreviewMalsupraNavigaBretoSercxo() {
    pTemo() {
        MalsupraNavigaBreto(
            nunaTab = EkranoLangeto.SERCXO,
            onHejmo = {}, onKanalaro = {}, onPlejŝatataj = {}, onSercxo = {},
        )
    }
}

@Preview(name = "Ludvico — kun eroj", showBackground = true, heightDp = 400)
@Composable
fun PreviewLudvicoKunEroj() {
    val regilo = LudvicoRegilo(
        ludilo = dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo(),
        elsendoDeponejo = object : dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo {
            override fun observiElsendojn(kanaloSlug: String) = kotlinx.coroutines.flow.MutableStateFlow(emptyList<Elsendo>())
            override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean) = emptyList<Elsendo>()
            override suspend fun getElsendo(id: String): Elsendo? = null
            override suspend fun sercxiElsendojn(teksto: String, limo: Int) = emptyList<Elsendo>()
            override suspend fun sxargxiElsendojnPorKanal(kanalo: dk.nordfalk.esperanto.domain.model.Kanalo, fortoRefresigi: Boolean) = emptyList<Elsendo>()
        },
        kanaloDeponejo = object : dk.nordfalk.esperanto.domain.repository.KanaloDeponejo {
            override fun observiKanalojn() = kotlinx.coroutines.flow.MutableStateFlow(emptyList<dk.nordfalk.esperanto.domain.model.Kanalo>())
            override suspend fun getKanalojn(fortoRefresigi: Boolean) = emptyList<dk.nordfalk.esperanto.domain.model.Kanalo>()
            override suspend fun getKanalo(slug: String) = null
        },
        plejŝatatajDeponejo = dk.nordfalk.esperanto.data.repository.PlejŝatatajDeponejoImpl(),
        ludatojDeponejo = dk.nordfalk.esperanto.data.repository.LudatojDeponejoMaketo(),
    )
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        regilo.aldoniAlVico(pElsendo)
        val e2 = pElsendo.copy(id = "kernpunkto:2024-01-02", titolo = "KP205 Sekva epizodo en vico")
        regilo.aldoniAlVico(e2)
    }
    pTemo() { LudvicoEkrano(ludvicoRegilo = regilo, onReen = {}) }
}
