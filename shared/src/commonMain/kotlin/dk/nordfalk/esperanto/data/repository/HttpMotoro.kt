package dk.nordfalk.esperanto.data.repository

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

/**
 * La Ktor-motoro por ĉi tiu platformo.
 * - Android, Desktop, iOS: CIO
 * - wasmJs: Js (retumila fetch) — CIO en wasmJs bezonas la `net`-modulon de Node.js
 *   kaj ĵetas "Node.js net module is not available" en retumilo.
 */
expect val httpMotoro: HttpClientEngineFactory<HttpClientEngineConfig>
