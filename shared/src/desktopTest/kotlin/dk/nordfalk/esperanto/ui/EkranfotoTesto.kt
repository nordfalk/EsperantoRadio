package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Severeco
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.NoOpLudiloRegilo
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
 * Ekranfota testoj — kaptas ĉiun ekranon kiel PNG-dosieron.
 *
 * Rulu per: ./gradlew :shared:desktopTest --tests "*EkranfotoTesto*"
 * Dosieroj estas savitaj al shared/build/ekranfotoj/
 */
@OptIn(ExperimentalTestApi::class)
class EkranfotoTesto {

    private val testKanaloj = listOf(
        Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"),
        Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"),
        Kanalo(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://www.podkasto.net/feed/"),
    )

    private val testElsendo = Elsendo(
        id = "kernpunkto:2024-01-01",
        kanaloSlug = "kernpunkto",
        titolo = "KP204 Pigmentoj kaj koloroj en la naturo",
        priskribo = "Hodiaŭ ni parolas pri pigmentoj kaj koloroj en la naturon.",
        fluo = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
        dato = "2024-01-01",
        daŭro = 6916,
    )

    private fun falsaKanaloDeponejo() = object : KanaloDeponejo {
        private val _kanaloj = MutableStateFlow(testKanaloj)
        override fun observiKanalojn() = _kanaloj.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = _kanaloj.value
        override suspend fun getKanalo(slug: String) = _kanaloj.value.find { it.slug == slug }
    }

    private fun kaptuKajSavu(test: ComposeUiTest, nomo: String) {
        val dosiero = File("build/ekranfotoj", "$nomo.png")
        dosiero.parentFile.mkdirs()
        // Malfermita dialogo estas aparta kompona radiko — kaptu la lastan
        // (la dialogon se ĝi malfermitas, alikaze la solan ekrano-radikon).
        val radikoj = test.onAllNodes(SemanticsMatcher("estasRadiko") { it.isRoot })
        val nombro = radikoj.fetchSemanticsNodes().size
        val lastaRadiko = radikoj[nombro - 1]
        val bitmap = lastaRadiko.captureToImage().asSkiaBitmap()
        val pngBytes = SkiaImage.makeFromBitmap(bitmap).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes
        if (pngBytes != null) {
            dosiero.writeBytes(pngBytes)
            println("[EkranfotoTesto] Savis: ${dosiero.absolutePath}")
        }
    }

    @Test
    fun ekranfoto_kanalaro() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanaloDeponejo())
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                KanalaroEkrano(viewModel = viewModel)
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "01_kanalaro")
    }

    @Test
    fun ekranfoto_kanalaro_ruĝa() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanaloDeponejo())
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.RUĜA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                KanalaroEkrano(viewModel = viewModel)
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "temo_ruĝa_kanalaro")
    }

    @Test
    fun ekranfoto_kanalaro_verda() = runComposeUiTest {
        val viewModel = KanalaroViewModel(falsaKanaloDeponejo())
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
                ElsendoEkrano(elsendo = testElsendo, onReen = {}, onLudi = {}, onElŝuti = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "02_elsendo_detalo")
    }

    @Test
    fun ekranfoto_serĉo() = runComposeUiTest {
        val serĉoDeponejo = object : dk.nordfalk.esperanto.domain.repository.SerĉoDeponejo {
            override suspend fun serĉi(teksto: String, limo: Int) =
                if (teksto.length >= 2) listOf(testElsendo) else emptyList()
        }
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                SerĉoEkrano(serĉoDeponejo = serĉoDeponejo, onElsendo = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "03_serĉo")
    }

    @Test
    fun ekranfoto_plejŝatataj() = runComposeUiTest {
        val plejDeponejo = object : dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo {
            private val _set = MutableStateFlow(setOf("muzaiko", "kernpunkto"))
            override fun observiPlejŝatatajn() = _set.asStateFlow()
            override suspend fun baskuliPlejŝaton(kanaloSlug: String) {}
            override suspend fun estasPlejŝatata(kanaloSlug: String) = kanaloSlug in _set.value
        }
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                PlejŝatatajEkrano(plejŝatatajDeponejo = plejDeponejo, kanaloDeponejo = falsaKanaloDeponejo(), onKanalo = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "04_plejŝatataj")
    }

    @Test
    fun ekranfoto_elŝutitaj() = runComposeUiTest {
        val elŝutDeponejo = object : ElŝutDeponejo {
            private val _elŝutoj = MutableStateFlow(
                mapOf(testElsendo.id to ElŝutitaElsendo(testElsendo, "/tmp/test.mp3", ElŝutStato.Preta))
            )
            override fun observiElŝutojn() = _elŝutoj.asStateFlow()
            override fun observiElŝutStaton(elsendoId: String) = MutableStateFlow(ElŝutStato.Preta).asStateFlow()
            override suspend fun elŝuti(elsendo: Elsendo) {}
            override suspend fun haltigi(elsendoId: String) {}
            override suspend fun forigi(elsendoId: String) {}
            override suspend fun getLokaDosieroVojo(elsendoId: String) = "/tmp/test.mp3"
            override fun estasElŝutita(elsendoId: String) = true
        }
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                ElŝutitajEkrano(elŝutDeponejo = elŝutDeponejo, ludilo = NoOpLudiloRegilo(), onReen = {}, onLudi = {}, onElsendo = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "05_elŝutitaj")
    }

    @Test
    fun ekranfoto_alarmoj() = runComposeUiTest {
        val alarmoDeponejo = MemorAlarmoDeponejo(
            listOf(
                Alarmo(id = 1, horo = 6, minuto = 45, ripeto = 0x7f, kanaloSlug = "muzaiko", aktiva = true, etikedo = "Matene"),
                Alarmo(id = 2, horo = 22, minuto = 0, ripeto = 0, kanaloSlug = "kernpunkto", aktiva = false),
            )
        )
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                AlarmoEkrano(alarmoDeponejo = alarmoDeponejo, kanaloDeponejo = falsaKanaloDeponejo(), onReen = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "06_alarmoj")
    }

    /** Redaktilo malfermita per klako sur ekzistanta alarmo — montras la ciferblidan tempo-elektilon. */
    @Test
    fun ekranfoto_alarmo_redaktilo() = runComposeUiTest {
        val alarmoDeponejo = MemorAlarmoDeponejo(
            listOf(
                Alarmo(id = 1, horo = 10, minuto = 0, ripeto = 0x7f, kanaloSlug = "muzaiko", aktiva = true, etikedo = "Matene"),
            )
        )
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                AlarmoEkrano(alarmoDeponejo = alarmoDeponejo, kanaloDeponejo = falsaKanaloDeponejo(), onReen = {})
            }
        }
        waitForIdle()
        onNodeWithText("10:00").performClick()
        waitForIdle()
        kaptuKajSavu(this, "06a_alarmo_redaktilo")
        // Tempo-elektilo kiel dialogo
        onNodeWithText("10:00").performClick()
        waitForIdle()
        kaptuKajSavu(this, "06b_alarmo_tempo_dialogo")
        // "Nuligi" ekzistas ankaŭ en la redaktilo malantaŭ la dialogo — fermu per "Bone"
        onNodeWithText("Bone").performClick()
        waitForIdle()
        // Kanal-elektilo kiel dialogo
        onNodeWithText("Muzaiko").performClick()
        waitForIdle()
        kaptuKajSavu(this, "06c_alarmo_kanalo_dialogo")
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
            LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.ElsendoFonto(testElsendo), pozicioMs = 30000, daŭroMs = 6916000, estasRekta = false)
        )
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                MiniLudilbreto(ludilo = ludilo, modifier = Modifier.fillMaxSize())
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "08_mini_ludilbreto")
    }

    @Test
    fun ekranfoto_mini_ludilbreto_elfaldita() = runComposeUiTest {
        val ludilo = EkranfotoLudiloRegilo(
            LudantoInformo(stato = LudantoStato.Ludas, nunaFonto = Sonfonto.ElsendoFonto(testElsendo), pozicioMs = 30000, daŭroMs = 6916000, estasRekta = false)
        )
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                MiniLudilbreto(ludilo = ludilo, modifier = Modifier.fillMaxSize(), elfaldita = true, onElfalditaŜanĝo = {})
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "09_mini_ludilbreto_elfaldita")
    }

    /** Falsa ElsendoDeponejo kiu ĉiam liveras unu testelsendon. */
    private fun falsaElsendoDeponejo() = object : ElsendoDeponejo {
        private val fluo = MutableStateFlow(listOf(testElsendo)).asStateFlow()
        override fun observiElsendojn(kanaloSlug: String): StateFlow<List<Elsendo>> = fluo
        override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean): List<Elsendo> = listOf(testElsendo)
        override suspend fun getElsendo(id: String): Elsendo? = testElsendo
        override suspend fun serĉiElsendojn(teksto: String, limo: Int): List<Elsendo> = listOf(testElsendo)
        override suspend fun ŝargiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> = listOf(testElsendo)
    }

    /** Unu diagnoza problemo por la avertokartaj ekranfotoj. */
    private fun avertaProblemo() = listOf(
        DiagnozoRezulto(
            tipo = DiagnozoTipo.BATERIO_OPTIMIZATION,
            titolo = "Bateri-optimumado estas ŝaltita",
            priskribo = "La sistemo povas limigi la aplikon en la fono.",
            severeco = Severeco.ERARO,
        )
    )

    @Test
    fun ekranfoto_hejmo_avertokarto() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                HejmoEkrano(
                    kanaloDeponejo = falsaKanaloDeponejo(),
                    elsendoDeponejo = falsaElsendoDeponejo(),
                    diagnozoProblemoj = MutableStateFlow(avertaProblemo()),
                )
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "10_hejmo_avertokarto")
    }

    @Test
    fun ekranfoto_agordoj_avertosigno() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = temuKolorskemo(TemoNomo.ANTONIA, false), typography = MuzaikoTiparo, shapes = MuzaikoFormoj) {
                AgordojEkrano(
                    agordojDeponejo = AgordojDeponejoImpl(),
                    onReen = {},
                    diagnozoProblemoj = MutableStateFlow(avertaProblemo()),
                )
            }
        }
        waitForIdle()
        kaptuKajSavu(this, "11_agordoj_avertosigno")
    }
}

private class EkranfotoLudiloRegilo(initial: LudantoInformo = LudantoInformo(stato = LudantoStato.Haltita)) : LudiloRegilo {
    private val _stato = MutableStateFlow(initial)
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita, nunaFonto = fonto, pozicioMs = komencoPozicioMs, estasRekta = fonto is Sonfonto.RektaKanalo)
    }
    override fun ludi() { _stato.value = _stato.value.copy(stato = LudantoStato.Ludas) }
    override fun paŭzigi() { _stato.value = _stato.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { _stato.value = _stato.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLaŭtecon(volumeno: Float) {}
    override val laŭteco: StateFlow<Float> = MutableStateFlow(1f)
}
