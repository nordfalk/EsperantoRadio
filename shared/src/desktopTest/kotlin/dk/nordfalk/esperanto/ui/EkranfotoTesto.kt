package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.data.repository.MemorAlarmoDeponejo
import dk.nordfalk.esperanto.ui.TemoNomo
import dk.nordfalk.esperanto.ui.temuKolorskemo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage
import kotlin.test.Test

/**
 * Ekranfota testoj — kaptas cxiun ekranon kiel PNG-dosieron.
 *
 * Rulu per: ./gradlew :shared:desktopTest --tests "*EkranfotoTesto*"
 * Dosieroj estas savitaj al shared/build/ekranfotoj/
 */
@OptIn(ExperimentalTestApi::class)
class EkranfotoTesto {

    private val testKanaloj = listOf(
        Kanal(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"),
        Kanal(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"),
        Kanal(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://www.podkasto.net/feed/"),
    )

    private val testElsendo = Elsendo(
        id = "kernpunkto:2024-01-01",
        kanalSlug = "kernpunkto",
        titolo = "KP204 Pigmentoj kaj koloroj en la naturo",
        priskribo = "Hodiaux ni parolas pri pigmentoj kaj koloroj en la naturon.",
        stream = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
        dato = "2024-01-01",
        dauro = 6916,
    )

    private fun falsaKanalDeponejo() = object : KanalDeponejo {
        private val _kanaloj = MutableStateFlow(testKanaloj)
        override fun observiKanalojn() = _kanaloj.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = _kanaloj.value
        override suspend fun getKanal(slug: String) = _kanaloj.value.find { it.slug == slug }
    }

    private fun kaptuKajSavu(test: ComposeUiTest, nomo: String) {
        val dosiero = File("build/ekranfotoj", "$nomo.png")
        dosiero.parentFile.mkdirs()
        val bitmap = test.onRoot().captureToImage().asSkiaBitmap()
        val pngBytes = SkiaImage.makeFromBitmap(bitmap).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes
        if (pngBytes != null) {
            dosiero.writeBytes(pngBytes)
            println("[EkranfotoTesto] Savis: ${dosiero.absolutePath}")
        }
    }

    @Test
    fun ekranfoto_kanalaro() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanalDeponejo())
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                KanalaroEkrano(viewModel = viewModel)
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "01_kanalaro")
    }

    @Test
    fun ekranfoto_kanalaro_rugxa() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanalDeponejo())
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.RUGXA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                KanalaroEkrano(viewModel = viewModel)
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "temo_rugxa_kanalaro")
    }

    @Test
    fun ekranfoto_kanalaro_verda() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanalDeponejo())
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.VERDA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                KanalaroEkrano(viewModel = viewModel)
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "temo_verda_kanalaro")
    }

    @Test
    fun ekranfoto_elsendo_detalo() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                ElsendoEkrano(elsendo = testElsendo, onReen = {}, onLudi = {}, onElshuti = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "02_elsendo_detalo")
    }

    @Test
    fun ekranfoto_serchxo() = runComposeUiTest {
        val sercxoDeponejo = object : dk.nordfalk.esperanto.domain.repository.SercxoDeponejo {
            override suspend fun sercxi(taxto: String, limo: Int) =
                if (taxto.length >= 2) listOf(testElsendo) else emptyList()
        }
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                SercxoEkrano(sercxoDeponejo = sercxoDeponejo, onReen = {}, onElsendo = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "03_serchxo")
    }

    @Test
    fun ekranfoto_plejsatataj() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo {
            private val _set = MutableStateFlow(setOf("muzaiko", "kernpunkto"))
            override fun observiPlejsatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejsaton(kanalSlug: String) {}
            override suspend fun estasPlejsatata(kanalSlug: String) = kanalSlug in _set.value
        }
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                PlejsatatajEkrano(plejsatatajDeponejo = plejDeponejo, kanalDeponejo = falsaKanalDeponejo(), onReen = {}, onKanal = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "04_plejsatataj")
    }

    @Test
    fun ekranfoto_elshutitaj() = runComposeUiTest {
        val elshutDeponejo = object : ElshutDeponejo {
            private val _elshutoj = MutableStateFlow(
                mapOf(testElsendo.id to ElshutitaElsendo(testElsendo, "/tmp/test.mp3", ElshutStato.Preta))
            )
            override fun observiElshutojn() = _elshutoj.asStateFlow()
            override fun observiElshutStaton(elsendoId: String) = MutableStateFlow(ElshutStato.Preta).asStateFlow()
            override suspend fun elshuti(elsendo: Elsendo) {}
            override suspend fun haltigi(elsendoId: String) {}
            override suspend fun forigi(elsendoId: String) {}
            override suspend fun getLokaDosieroVojo(elsendoId: String) = "/tmp/test.mp3"
            override fun estasElshutita(elsendoId: String) = true
        }
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                ElshutitajEkrano(elshutDeponejo = elshutDeponejo, onReen = {}, onLudi = {}, onElsendo = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "05_elshutitaj")
    }

    @Test
    fun ekranfoto_alarmoj() = runComposeUiTest {
        val alarmoDeponejo = MemorAlarmoDeponejo(
            listOf(
                Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanalSlug = "muzaiko", aktiva = true, etikedo = "Matene"),
                Alarmo(id = 2, horo = 22, minuto = 0, ripeto = 0, kanalSlug = "kernpunkto", aktiva = false),
            )
        )
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                AlarmoEkrano(alarmoDeponejo = alarmoDeponejo, kanalDeponejo = falsaKanalDeponejo(), onReen = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "06_alarmoj")
    }

    @Test
    fun ekranfoto_agordoj() = runComposeUiTest {
        val agordojDeponejo = AgordojDeponejoImpl()
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                AgordojEkrano(agordojDeponejo = agordojDeponejo, onReen = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "07_agordoj")
    }

    @Test
    fun ekranfoto_mini_ludilbreto() = runComposeUiTest {
        val ludilo = EkranfotoLudiloRegilo(
            LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.ElsendoFonto(testElsendo), pozicioMs = 30000, dauroMs = 6916000, estasRekta = false)
        )
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                MiniLudilbreto(ludilo = ludilo, modifier = Modifier.fillMaxSize())
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "08_mini_ludilbreto")
    }
}

private class EkranfotoLudiloRegilo(initial: LudantoInformo = LudantoInformo(stato = LudantoStato.Haltita)) : LudiloRegilo {
    private val _stato = MutableStateFlow(initial)
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = fonto, pozicioMs = komencoPozicioMs, estasRekta = fonto is Sonfonto.RektaKanalo)
    }
    override fun ludi() { _stato.value = _stato.value.copy(stato = LudantoStato.Ludas) }
    override fun pauxzigi() { _stato.value = _stato.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { _stato.value = _stato.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLauxtecon(volumeno: Float) {}
}
