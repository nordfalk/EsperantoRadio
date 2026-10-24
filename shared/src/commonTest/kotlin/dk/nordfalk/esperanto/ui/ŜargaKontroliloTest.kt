package dk.nordfalk.esperanto.ui

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Testoj por la dedupigo + TTL de [ŜargaKontrolilo].
 * Antaŭondoj: la kanallisto reŝargiĝis je ĉiu ekran-eniro (navigado), sen
 * ajna kontrolo (KMP-erar-esplorado 2026-10-20).
 */
@OptIn(ExperimentalTime::class)
class ŜargaKontroliloTest {

    /** Falsa horloĝo — tempopunktojn ni ŝovas permane en la testoj. */
    private class FalsaHorloĝo : () -> Instant {
        var tempo = Instant.parse("2026-10-20T12:00:00Z")
        override fun invoke(): Instant = tempo
        fun pasigu(minutes: Long) {
            tempo = Instant.fromEpochMilliseconds(tempo.toEpochMilliseconds() + minutes * 60_000)
        }
    }

    @Test
    fun duaVokoEneDeTtlPreterpasas() = runTest {
        val horloĝo = FalsaHorloĝo()
        val kontrolilo = ŜargaKontrolilo(ttl = 5.minutes, horloĝo = horloĝo)
        var kalkulo = 0

        kontrolilo.rulu("Testo", false) { kalkulo++ }
        kontrolilo.rulu("Testo", false) { kalkulo++ }

        assertEquals(1, kalkulo, "La dua voko ene de la TTL devas esti preterpasita")
    }

    @Test
    fun postTtlLaVokoDenoveRulas() = runTest {
        val horloĝo = FalsaHorloĝo()
        val kontrolilo = ŜargaKontrolilo(ttl = 5.minutes, horloĝo = horloĝo)
        var kalkulo = 0

        kontrolilo.rulu("Testo", false) { kalkulo++ }
        horloĝo.pasigu(6)
        kontrolilo.rulu("Testo", false) { kalkulo++ }

        assertEquals(2, kalkulo, "Post la TTL la ŝargado devas denove ruli")
    }

    @Test
    fun fortoRefresigiPreterpasasTtl() = runTest {
        val horloĝo = FalsaHorloĝo()
        val kontrolilo = ŜargaKontrolilo(ttl = 5.minutes, horloĝo = horloĝo)
        var kalkulo = 0

        kontrolilo.rulu("Testo", false) { kalkulo++ }
        kontrolilo.rulu("Testo", fortoRefresigi = true) { kalkulo++ }

        assertEquals(2, kalkulo, "Malsupren-tiro (fortoRefresigi) devas ĉiam ruli")
    }

    @Test
    fun fiaskoNeKalkulasKielFreŝa() = runTest {
        val horloĝo = FalsaHorloĝo()
        val kontrolilo = ŜargaKontrolilo(ttl = 5.minutes, horloĝo = horloĝo)
        var kalkulo = 0

        try {
            kontrolilo.rulu("Testo", false) { error("reta fiasko") }
        } catch (_: IllegalStateException) {
            // atendita
        }
        kontrolilo.rulu("Testo", false) { kalkulo++ }

        assertEquals(1, kalkulo, "Fiaskinta ŝargado devas reprobebla tuj — neniu TTL-noto")
    }

    @Test
    fun samtempajVokojDedupiĝas() = runTest {
        val horloĝo = FalsaHorloĝo()
        val kontrolilo = ŜargaKontrolilo(ttl = 5.minutes, horloĝo = horloĝo)
        var kalkulo = 0

        // Du samtempaj vokoj: la unua tenas la mutexon ĝis la fino, la dua tiam
        // vidas freŝan rezulton kaj preterpasas sin.
        coroutineScope {
            val unua = async { kontrolilo.rulu("Testo", false) { kalkulo++ } }
            val dua = async { kontrolilo.rulu("Testo", false) { kalkulo++ } }
            awaitAll(unua, dua)
        }

        assertEquals(1, kalkulo, "Samtempa dua voko devas atendi kaj poste preterpasi")
    }
}
