package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import io.ktor.client.HttpClient

actual fun kreuElŝutDeponejo(httpKliento: HttpClient): ElŝutDeponejo =
    NoOpElŝutDeponejo()
