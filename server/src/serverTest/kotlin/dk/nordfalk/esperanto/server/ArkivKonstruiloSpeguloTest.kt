package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.domain.model.Elsendo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La klasigo de bildeto-URL-oj en speguluElsendajnBildetojn (PR #214):
 * - ALIAJ samdevenaj URL-oj (ekz. kanal-emblemo kiel `<itunes:image>` aŭ bildo
 *   gastigata ĉe nia servilo) restas SENŜANĜE — la retversio ŝargas ilin sen CORS,
 *   do re-spegulado estus nenecesa kaj povus anstataŭigi ilin per alia bildo;
 * - NIAJ elsendlaj speguloj restas, se la dosiero ekzistas.
 * Ambaŭ vojoj ne tuŝas la reton — tute senretaj testoj.
 */
class ArkivKonstruiloSpeguloTest {

    private val bazo = "https://javabog.dk/esperanto-radio"
    private val bildujo: File = createTempDirectory("spegulo-testo").toFile()
    private val http = HttpClient(CIO)

    @AfterTest
    fun fermu() {
        http.close()
        bildujo.deleteRecursively()
    }

    private fun elsendo(bildoUrl: String) = Elsendo(
        id = "testo:1",
        kanaloSlug = "testo",
        titolo = "Testa elsendo",
        dato = "2026-11-26",
        fluo = "https://example.com/elp.mp3",
        bildoUrl = bildoUrl,
    )

    @Test
    fun aliaSamdevenaUrlRestasSenŝanĝe() {
        val rezulto = ArkivKonstruilo.speguluElsendajnBildetojn(
            listOf(
                elsendo("$bazo/bildoj/emblemo.jpg"),                       // kanal-emblemo
                elsendo("$bazo/apo/bildo.png"),                            // alia vojo de nia servilo
                elsendo("$bazo/bildoj/elsendoj/alia-radiko.jpg"),          // nia spegulo, mankanta (sen reto)
            ),
            http, "$bazo/kanaloj/", RssParsilo(), retumado = false, bildujo = bildujo,
        )
        assertEquals("$bazo/bildoj/emblemo.jpg", rezulto[0].bildoUrl)
        assertEquals("$bazo/apo/bildo.png", rezulto[1].bildoUrl)
        // nia spegulo mankas kaj retumado estas malŝaltita → la originala URL restas
        assertEquals("$bazo/bildoj/elsendoj/alia-radiko.jpg", rezulto[2].bildoUrl)
        assertTrue(bildujo.listFiles().isNullOrEmpty(), "neniu dosiero kreiĝu")
    }

    @Test
    fun niaSpeguloKunEkzistantaDosieroRestas() {
        val radiko = Emblemoj.bildetaRadiko("https://ekstera.example/bildo.jpg")
        File(bildujo, "$radiko.png").writeBytes(byteArrayOf(1, 2, 3))
        val rezulto = ArkivKonstruilo.speguluElsendajnBildetojn(
            listOf(elsendo("$bazo/bildoj/elsendoj/$radiko.png")),
            http, "$bazo/kanaloj/", RssParsilo(), retumado = true, bildujo = bildujo,
        )
        assertEquals("$bazo/bildoj/elsendoj/$radiko.png", rezulto[0].bildoUrl)
    }
}
