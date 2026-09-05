package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import io.ktor.client.HttpClient

actual fun kreElshutDeponejo(httpKliento: HttpClient): ElshutDeponejo =
    NoOpElshutDeponejo()
