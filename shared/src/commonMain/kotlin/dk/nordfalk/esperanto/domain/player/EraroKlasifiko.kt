package dk.nordfalk.esperanto.domain.player

/**
 * Pura eraro-klasifiko (commonMain) — unuo-testata sen media3.
 *
 * La media3-dependa envolvaĵo (PlaybackException, HttpDataSource) troviĝas en
 * androidMain kaj delegas ĉi tien.
 */

object EraroKlasifiko {

    /**
     * Ĉu HTTP-statuso signifas paseman (reproveblan) eraron?
     *
     * HTTP 5xx (servila eraro) estas pasema: ekz. la randnodoj de archive.org intermite
     * redonas 500 por tute sanaj dosieroj — reprovo kelkajn sekundojn poste kutime
     * sukcesas. HTTP 4xx (ekz. 404) kaj ĉio alia restas daŭra.
     */
    fun estasPasemaHttpStatuson(status: Int): Boolean = status in 500..599
}
