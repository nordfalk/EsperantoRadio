package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import io.ktor.client.HttpClient
import java.io.File

actual fun kreuElŝutDeponejo(httpKliento: HttpClient): ElŝutDeponejo {
    val hejjo = File(System.getProperty("user.home"), ".esperanto-radio/elŝutoj")
    return KtorElŝutDeponejo(httpKliento) { hejjo }
}
