package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import io.ktor.client.HttpClient

actual fun kreuElshutDeponejo(httpKliento: HttpClient): ElshutDeponejo =
    NoOpElshutDeponejo()
