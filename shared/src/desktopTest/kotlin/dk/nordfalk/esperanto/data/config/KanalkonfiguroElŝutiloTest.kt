package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.data.repository.RespondoTroGrandaException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Testoj por [kreuDeforanElŝutilon] — la PRODUKTADA reta drato de la defora
 * kanalkonfiguro (injektata en `KanaloDeponejoImpl`): ĝusta URL, HTTP-eraroj ne
 * pasas kiel konfiguro (`expectSuccess`), tro granda korpo ĵetas tuj
 * (`leguKorponLimigite` kun la konfigur-propra limo).
 *
 * Rulas en desktopTest ĉar ĝi bezonas `ktor-client-mock` (MockEngine).
 */
class KanalkonfiguroElŝutiloTest {

    @Test
    fun sukcesaPetoLegasLaKorpon() = runTest {
        val ricevitaj = mutableListOf<String>()
        val motoro = MockEngine { peto ->
            ricevitaj.add(peto.url.toString())
            respond("""{"kanaloj":[{"kodo":"muzaiko","nomo":"Muzaiko"}]}""", HttpStatusCode.OK)
        }
        val elŝutilo = kreuDeforanElŝutilon(HttpClient(motoro) { install(HttpTimeout) })

        assertEquals("""{"kanaloj":[{"kodo":"muzaiko","nomo":"Muzaiko"}]}""", elŝutilo(KANALKONFIGURO_URL))
        assertEquals(listOf(KANALKONFIGURO_URL), ricevitaj, "La elŝutilo devas peti precize la konfiguran URL-on")
    }

    @Test
    fun httpEraroNePasasKielKonfiguro() = runTest {
        // ekz. kaptiva portalo aŭ provizore rompita servilo kun HTTP 500
        val motoro = MockEngine { respond("<html>Eraro</html>", HttpStatusCode.InternalServerError) }
        val elŝutilo = kreuDeforanElŝutilon(HttpClient(motoro) { install(HttpTimeout) })

        // expectSuccess: la eraro ĵetiĝas kaj la refreŝigo-logiko kaptas ĝin —
        // HTML-erarpaĝo neniam povas esti traktata kvazaŭ ĝi estus konfiguro
        assertFailsWith<ResponseException> { elŝutilo(KANALKONFIGURO_URL) }
    }

    @Test
    fun troGrandaKorpoĴetasTuj() = runTest {
        // Deklarita Content-Length transpasas la konfigur-limon (256 KB) —
        // tio gardas kontraŭ misdirektitaj respondoj (MP3-anstataŭo ktp., G45)
        val motoro = MockEngine {
            respond(
                "x".repeat(300 * 1024),
                HttpStatusCode.OK,
                headersOf("Content-Length", (300L * 1024).toString()),
            )
        }
        val elŝutilo = kreuDeforanElŝutilon(HttpClient(motoro) { install(HttpTimeout) })

        assertFailsWith<RespondoTroGrandaException> { elŝutilo(KANALKONFIGURO_URL) }
    }
}
