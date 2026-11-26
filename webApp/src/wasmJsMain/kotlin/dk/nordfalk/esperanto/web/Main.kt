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
    AppStato.estasEvoluaMedio = true
    // La erar-raportado defaŭlte ŜALTITA: la retejo estas nia propra kanalo
    // (kiel Play/Aptoide, ne F-Droid) — do defaŭlto ŝaltita, malŝaltebla
    // en Agordoj → Privateco; la opt-in-regolo de PR #148 ne aplikiĝas.
    AppStato.erarRaportadoDefaŭlta = true
    initialiguSentry()
    // ComposeViewport anstataŭ la malrekomendita CanvasBasedWindow (Compose 1.10 traktas ĝin kiel eraron)
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        EsperantoRadioApp()
    }
}
