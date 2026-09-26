package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regulo 4: unu fonto-eraro ne panei la aliajn.
 *
 * La retumila Ktor-motoro ĵetas `kotlin.Error("Fail to fetch")` (ne Exception) ĉe CORS-blokado.
 * Antaŭe tio eskapis el ElsendoDeponejoImpl kaj nuligis la paralelan ŝargadon de ĉiuj kanaloj.
 */
class ElsendoDeponejoToleremoTest {

    @Test
    fun errorEnUnuFluoNeNuligasLaAliajn() = runTest {
        val klient = HttpClient(CIO)
        klient.plugin(HttpSend).intercept { peto ->
            if (peto.url.host == "rompita.example") throw Error("Fail to fetch")
            throw IllegalStateException("Reto ne uzata en ĉi tiu testo")
        }
        val deponejo = ElsendoDeponejoImpl(klient)
        val kanaloj = listOf(
            Kanalo(slug = "rompita", nomo = "Rompita", podkastaRssUrl = "https://rompita.example/feed"),
            Kanalo(slug = "alia", nomo = "Alia", podkastaRssUrl = "https://alia.example/feed"),
        )

        val rezultoj = coroutineScope {
            kanaloj.map { k -> async { deponejo.sxargxiElsendojnPorKanal(k, fortoRefresigi = true) } }.awaitAll()
        }

        // Ambaŭ revenas (malplenaj — neniu kaŝmemoro) anstataŭ ke la Error nuligu la tutan ŝargadon
        assertEquals(listOf(0, 0), rezultoj.map { it.size })
    }
}
