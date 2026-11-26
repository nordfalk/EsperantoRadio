package dk.nordfalk.esperanto.server

import com.sun.net.httpserver.HttpServer
import dk.nordfalk.esperanto.data.repository.RespondoTroGrandaException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.prepareGet
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Empiria testo de la FLUANTA grandec-limo de leguBitokojnLimigite (PR #214,
 * peto de CodeRabbit): respondo PLI GRANDA ol la limo SEN Content-Length
 * devas esti rifuzata DUM la legado — la legado ne okazas ĝis la fino (aŭ
 * ĝi enmemorigus 5+ MiB antaŭ la kontrolo).
 */
class ArkivKonstruiloFluoTest {

    private val servilo = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val kliento = HttpClient(CIO)

    @AfterTest
    fun fermu() {
        servilo.stop(0)
        kliento.close()
    }

    /** respondas [mb] MiB sencenhavon SEN Content-Length (chunked-fluo). */
    private fun ekiguServilon(mb: Int) {
        servilo.createContext("/bildo") { interŝanĝo ->
            // sendResponseHeaders(200, 0) = longo NEKONATA → neniu Content-Length-kapo
            interŝanĝo.sendResponseHeaders(200, 0)
            val fluo = interŝanĝo.responseBody
            repeat(mb) {
                fluo.write(ByteArray(1024 * 1024))
                fluo.flush()
            }
            interŝanĝo.close()
        }
        servilo.start()
    }

    @Test
    fun respondoPliGrandaOlLimoSenaContentLengthRifuziĝasDumLegado() {
        ekiguServilon(mb = 7) // 7 MiB > 5 MiB-limo
        runBlocking {
            kliento.prepareGet("http://127.0.0.1:${servilo.address.port}/bildo").execute { respondo ->
                // neniu Content-Length en la respondo — la kontrolo devas kapti dum la fluo
                assertEquals(null, respondo.headers["Content-Length"])
                try {
                    ArkivKonstruilo.leguBitokojnLimigite(respondo, 5L * 1024 * 1024)
                    throw AssertionError("respondo de 7 MiB sen Content-Length devus esti rifuzita")
                } catch (e: RespondoTroGrandaException) {
                    // atendite: rifuzita tuj post transpaso de la limo, senleginte la finan 2 MiB
                }
            }
        }
    }

    @Test
    fun respondoEneDeLimoMalgrauxSenaContentLengthPleneLegiĝas() {
        ekiguServilon(mb = 2) // 2 MiB < 5 MiB
        runBlocking {
            kliento.prepareGet("http://127.0.0.1:${servilo.address.port}/bildo").execute { respondo ->
                val bitokoj = ArkivKonstruilo.leguBitokojnLimigite(respondo, 5L * 1024 * 1024)
                assertEquals(2L * 1024 * 1024, bitokoj.size.toLong())
            }
        }
    }
}
