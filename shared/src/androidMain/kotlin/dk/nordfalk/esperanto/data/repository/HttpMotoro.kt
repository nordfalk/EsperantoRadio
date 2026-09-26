package dk.nordfalk.esperanto.data.repository

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

actual val httpMotoro: HttpClientEngineFactory<HttpClientEngineConfig> = CIO
