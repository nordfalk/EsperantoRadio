package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import io.ktor.client.HttpClient

/**
 * Kreas la platform-specifan ElshutDeponejo-n.
 * - Desktop/Android: KtorElshutDeponejo (Ktor → dosiero)
 * - wasmJs/iOS: NoOpElshutDeponejo
 */
expect fun kreuElshutDeponejo(httpKliento: HttpClient): ElshutDeponejo
