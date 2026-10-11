package dk.nordfalk.esperanto.data.repository

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

/**
 * OkHttp-motoro sur Android (ne CIO!).
 *
 * La CIO-motoro vokas la MALNOVAN 2-argan checkServerTrusted(ĉeno, tipo) sen la
 * gastiganto — tuj kiam la apo difinas network_security_config (nia G1-ŝanĝo),
 * Android envolvas la fid-mastrumilon en RootTrustManager kiu postulas la
 * gastiganto-konscian 3-argan variaĵon kaj ĵetas CertificateException por ĈIUJ
 * https-petoj. OkHttp uzas la gastiganto-konscian kontrolon kaj tial kongruas
 * kun network_security_config.
 */
actual val httpMotoro: HttpClientEngineFactory<HttpClientEngineConfig> =
    io.ktor.client.engine.okhttp.OkHttp
