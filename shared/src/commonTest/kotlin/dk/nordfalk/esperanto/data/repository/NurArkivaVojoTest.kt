package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * FAROTA C56: sur la retumilo (wasmJs) la originalaj fluoj estas CORS-blokitaj —
 * `uzuOriginajnFluojn = false` devas elŝuti NUR la arkivan fonton.
 * Ambaŭ kazoj kun la sama kanalo: neniu peto al la originala URL en la nur-arkiva
 * vojo; ambaŭ fontoj en la kunfanda vojo. (La petoj fiaskas intence — ni nur
 * observas, KIUJN fontojn la deponejo provas; la fiasko-vojo estas tolerata per A4.)
 */
class NurArkivaVojoTest {

    private fun kanalo() = Kanalo(
        slug = "ekzemplo",
        nomo = "Ekzemplo",
        emblemoUrl = "https://ekzemplo.example/emblemo.png",
        podkastaRssUrl = "https://ekzemplo.example/fluo.xml",
    )

    @Test
    fun nurArkivaVojoNeProvasLaOriginalanFluon() = runTest {
        val petoj = mutableListOf<String>()
        val klient = HttpClient(CIO)
        klient.plugin(HttpSend).intercept { peto ->
            petoj.add(peto.url.toString())
            throw IllegalStateException("Reto ne uzata en ĉi tiu testo")
        }
        val deponejo = ElsendoDeponejoImpl(
            klient,
            arkivoBazoUrl = "https://arkivo.example/kanaloj/",
            uzuOriginajnFluojn = false,
        )

        deponejo.ŝargiElsendojnPorKanal(kanalo(), fortoRefresigi = true)

        assertEquals(listOf("https://arkivo.example/kanaloj/ekzemplo.xml"), petoj)
    }

    @Test
    fun kunfandaVojoProvasAmbaŭFontojn() = runTest {
        val petoj = mutableListOf<String>()
        val sekurigo = Mutex() // ambaŭ fontoj aldonas samtempe el apartaj fadenoj
        val klient = HttpClient(CIO)
        klient.plugin(HttpSend).intercept { peto ->
            sekurigo.withLock { petoj.add(peto.url.toString()) }
            throw IllegalStateException("Reto ne uzata en ĉi tiu testo")
        }
        val deponejo = ElsendoDeponejoImpl(
            klient,
            arkivoBazoUrl = "https://arkivo.example/kanaloj/",
            uzuOriginajnFluojn = true,
        )

        deponejo.ŝargiElsendojnPorKanal(kanalo(), fortoRefresigi = true)

        assertTrue("https://arkivo.example/kanaloj/ekzemplo.xml" in petoj, "petoj: $petoj")
        assertTrue("https://ekzemplo.example/fluo.xml" in petoj, "petoj: $petoj")
        assertFalse(petoj.any { it.startsWith("https://ekzemplo.example/emblemo") }, "emblemo ne estu ŝargata de la deponejo: $petoj")
    }
}
