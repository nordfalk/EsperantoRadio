package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto

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

@Preview(name = "Plejsatataj", showBackground = true, heightDp = 250)
@Composable
fun PreviewPlejsatataj() {
    pTemo() {
        PlejsatatajEkrano(plejsatatajDeponejo = pPlejsatatajDeponejo(), kanaloDeponejo = pKanaloDeponejo(), onKanalo = {})
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
        )
    }
}
