package dk.nordfalk.esperanto.android

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource

/**
 * Klasifiko de ludil-eraroj: pasema (reprovinda) kontraŭ daŭra.
 *
 * HTTP 5xx (servileraro) estas pasema: ekz. la edge-nodoj de archive.org intermite
 * redonas 500 por tute sanaj dosieroj — reprovo kelkajn sekundojn poste kutime
 * sukcesas (la redirekto tiam elektas alian nodon). HTTP 4xx (ekz. 404) restas
 * daŭra: la dosiero vere mankas kaj la ludvico tuj saltas al la sekva elsendo.
 */
internal fun estasReproveblaEraro(error: PlaybackException): Boolean =
    estasPasemaErarKodo(error.errorCode) || estasServileraro(error)

/** Pasemaj erar-kodoj (reto, tempolimo ktp.); daŭraj (HTTP 4xx, formato, malkodilo) ne. */
private fun estasPasemaErarKodo(kodo: Int): Boolean = when (kodo) {
    PlaybackException.ERROR_CODE_UNSPECIFIED,
    PlaybackException.ERROR_CODE_REMOTE_ERROR,
    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
    PlaybackException.ERROR_CODE_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> true
    else -> false
}

/** Serĉas HTTP 5xx en la kaŭz-ĉeno (InvalidResponseCodeException.responseCode). */
private fun estasServileraro(error: PlaybackException): Boolean {
    var kauxzo: Throwable? = error
    while (kauxzo != null) {
        if (kauxzo is HttpDataSource.InvalidResponseCodeException && kauxzo.responseCode in 500..599) return true
        kauxzo = kauxzo.cause
    }
    return false
}
