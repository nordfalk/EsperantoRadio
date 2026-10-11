package dk.nordfalk.esperanto.data.repository

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

/**
 * CIO-motoro sur Android — sama kiel Desktop kaj iOS.
 *
 * AVERTO (2026-10-11, emulila testo): CIO paneigas por ĈIUJ https-petoj tuj kiam la
 * network_security_config enhavas DOMAJNO-specifajn enskribojn (ekz. cleartext-esceptojn):
 * "CertificateException: Domain specific configurations require that hostname aware
 * checkServerTrusted(X509Certificate[], String, String) is used". Tiam necesas la
 * OkHttp-motoro (gastiganto-konscia kontrolo). Nun kiam la config havas nur
 * base-config (neniuj esceptoj — klara teksto malpermesata tutmonde), CIO funkcias
 * normale kaj OkHttp estas forigita. Se iam re-aldoniĝas domajno-escepto, remetu
 * libs.ktor.client.okhttp kaj ŝanĝu ĉi tiun actual al OkHttp.
 */
actual val httpMotoro: HttpClientEngineFactory<HttpClientEngineConfig> =
    io.ktor.client.engine.cio.CIO
