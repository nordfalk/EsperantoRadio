package dk.nordfalk.esperanto.ui

/**
 * Platform-specifaj funkcioj por malfermi eksterajn ligilojn kaj retpoŝton.
 *
 * expect — actual implementoj estas en androidMain, desktopMain, wasmJsMain, iosMain.
 */

/**
 * Malfermas URL-on en la defaŭlta krozilo de la platformo.
 */
expect fun malfermuLigon(url: String)

/**
 * Malfermas la retpoŝto-programon kun antaŭplenigitaj temo kaj teksto.
 *
 * @param retposhto retpoŝtadreso de la ricevonto
 * @param temo temo de la mesaĝo
 * @param teksto korpa teksto de la mesaĝo
 */
expect fun malfermuRetposhton(retposhto: String, temo: String, teksto: String)
