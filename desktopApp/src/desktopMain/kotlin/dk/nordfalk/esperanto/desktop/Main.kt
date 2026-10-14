package dk.nordfalk.esperanto.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dk.nordfalk.esperanto.EsperantoRadioApp

fun main() = application {
    // (2026-10-13, PR #109) Desktop-rulado ĉiam estas evolu-rulado (ne ekzistas
    // release-konstruo) — same kiel Android-debug-konstruoj kaj emuliloj ĝi tute
    // ne raportas al Sentry.
    // initialiguSentry()
    Window(
        onCloseRequest = ::exitApplication,
        title = "EsperantoRadio"
    ) {
        EsperantoRadioApp()
    }
}
