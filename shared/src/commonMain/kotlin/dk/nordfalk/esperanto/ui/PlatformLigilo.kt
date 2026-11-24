package dk.nordfalk.esperanto.ui

/**
 * Platform-specifaj funkcioj por malfermi eksterajn ligilojn kaj retpoŝton.
 *
 * expect — actual implementoj estas en androidMain, desktopMain, wasmJsMain, iosMain.
 */

/**
 * La platformo, sur kiu la apo ruliĝas. La aplikon oni povas kontroli laŭ
 * ĝi — ekz. la versio-kontrolo de la konfiguro (`aktualaVersio`, FAROTA
 * C61a) nur sur Android havas sencon (la sola platformo kun Google Play).
 */
enum class Platformo {
    /** Android (poŝtelefono/tableto) — la ĉefa celo. */
    ANDROIDO,
    /** Desktop (JVM, `desktopApp`). */
    DESKTOPO,
    /** La retumilo (wasmJs, `webApp`). */
    RETUMILO,
    /** iOS — la Xcode-projekto ankoraŭ ne ekzistas (`iosMain` jam preparita). */
    IOS,
}

/**
 * La platformo de ĉi tiu konstruo — la actual-oj faras ĝin konstanto,
 * do ĝi estas kompilece konata sen runtime-kontrolo.
 */
expect val platformo: Platformo

/**
 * Malfermas URL-on en la defaŭlta krozilo de la platformo.
 */
expect fun malfermuLigon(url: String)

/**
 * Malfermas la retpoŝto-programon kun antaŭplenigitaj temo kaj teksto.
 *
 * @param retpoŝto retpoŝtadreso de la ricevonto
 * @param temo temo de la mesaĝo
 * @param teksto korpa teksto de la mesaĝo
 */
expect fun malfermuRetpoŝton(retpoŝto: String, temo: String, teksto: String)

/**
 * Kopias tekston en la sisteman poŝon (clipboard).
 */
expect fun kopiuTekston(teksto: String)

/**
 * Disdividas ligilon: sur Android per la sistemo (share-sheet); sur web kaj desktop
 * malfermas novan fenestron kun la ligilo.
 */
expect fun kunhaviguLigilon(url: String, teksto: String)
