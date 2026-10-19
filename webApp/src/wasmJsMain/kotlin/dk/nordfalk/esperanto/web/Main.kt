@file:OptIn(ExperimentalComposeUiApi::class)

package dk.nordfalk.esperanto.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.initialiguSentry

fun main() {
    // Web-rulado ĉiam estas evolu-rulado — la "Apaj novaĵoj"-dialogo ne
    // montriĝu (ĝi aperus ĉe ĉiu freŝa ŝargo kaj ĝenus dum programado)
    AppStato.estasEvoluaMedo = true
    initialiguSentry()
    // ComposeViewport anstataŭ la malrekomendita CanvasBasedWindow (Compose 1.10 traktas ĝin kiel eraron)
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        EsperantoRadioApp()
    }
}
