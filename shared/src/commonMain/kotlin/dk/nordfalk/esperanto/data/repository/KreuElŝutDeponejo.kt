package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import io.ktor.client.HttpClient

/**
 * Kreas la platform-specifan ElŝutDeponejo-n.
 * - Desktop/Android: KtorElŝutDeponejo (Ktor → dosiero)
 * - wasmJs/iOS: NoOpElŝutDeponejo
 */
expect fun kreuElŝutDeponejo(httpKliento: HttpClient): ElŝutDeponejo
