package dk.nordfalk.esperanto.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Testoj por [KanaloDeponejoImpl.kreuElŝutanton] — la higiena elŝutanto de la
 * fora kanalkonfiguro: sukcesa respondo redoniĝas, HTTP-eraro ĵetas
 * (expectSuccess — fiaskpaĝo ne estu provata kiel konfiguro), tro granda
 * respondo ĵetas jam laŭ la deklarita Content-Length (G45).
 *
 * Rulas en desktopTest ĉar ĝi bezonas `ktor-client-mock` (MockEngine).
 */
class KanaloDeponejoElŝutantoTest {

    private val konfiguro = """{ "kanaloj": [ { "kodo": "aaa", "nomo": "A" } ] }"""

    private fun kliento(motoro: MockEngine) = HttpClient(motoro) { install(HttpTimeout) }

    @Test
    fun sukcesaRespondo_redoniĝas() = runTest {
        val motoro = MockEngine { respond(konfiguro, HttpStatusCode.OK) }
        val elŝutanto = KanaloDeponejoImpl.kreuElŝutanton(kliento(motoro))
        assertEquals(konfiguro, elŝutanto(KanaloDeponejoImpl.KONFIGURO_URL_DE_RETO))
    }

    @Test
    fun http500_ĵetas_kaj_ne_redonas_la_korpon() = runTest {
        val motoro = MockEngine { respond("<html>Eraro</html>", HttpStatusCode.InternalServerError) }
        val elŝutanto = KanaloDeponejoImpl.kreuElŝutanton(kliento(motoro))
        assertFailsWith<Exception> {
            elŝutanto(KanaloDeponejoImpl.KONFIGURO_URL_DE_RETO)
        }
    }

    @Test
    fun troGrandaDeklaritaKorpo_ĵetas_jam_el_ContentLength() = runTest {
        // Deklarita Content-Length super la limo (1 MB) — neniu bitoko estu legata (G45)
        val granda = "x".repeat(2_000_000)
        val motoro = MockEngine {
            respond(granda, HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, granda.length.toString()))
        }
        val elŝutanto = KanaloDeponejoImpl.kreuElŝutanton(kliento(motoro))
        assertFailsWith<RespondoTroGrandaException> {
            elŝutanto(KanaloDeponejoImpl.KONFIGURO_URL_DE_RETO)
        }
    }
}
