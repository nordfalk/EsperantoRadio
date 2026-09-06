package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import io.ktor.client.HttpClient
import java.io.File

actual fun kreuElshutDeponejo(httpKliento: HttpClient): ElshutDeponejo {
    val hejjo = File(System.getProperty("user.home"), ".esperanto-radio/elshutoj")
    return KtorElshutDeponejo(httpKliento) { hejjo }
}
