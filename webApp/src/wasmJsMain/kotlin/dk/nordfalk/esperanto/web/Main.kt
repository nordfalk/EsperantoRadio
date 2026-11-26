@file:OptIn(ExperimentalComposeUiApi::class)

package dk.nordfalk.esperanto.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.initialiguSentry

fun main() {
    // Evolua medio NUR kiam la retversio rulas LOKE (la evoluiga servilo de
    // `wasmJsBrowserDevelopmentRun` servas je localhost). La publikigita
    // retversio (javabog.dk) havas ĝin falsa: ĝi estas produktada rulo kaj
    // devas uzi la DEFORAN kanalkonfiguron (la paktitaj emblemo-URL-oj estas
    // CORS-blokataj en la retumilo — la spegulitaj bildoj sur nia servilo
    // estas samdevenaj, malkovrite 2026-11-26) kaj montri la apajn novaĵojn.
    // En evolua medio la defora konfiguro NE uziĝas, por ke novaj kanaloj el
    // la Git-branĉo tuj testiĝu loke sen rsync al la servilo.
    AppStato.estasEvoluaMedio = kotlinx.browser.window.location.let { dom ->
        dom.hostname == "localhost" || dom.hostname == "127.0.0.1" || dom.hostname == "[::1]" || dom.hostname == "0.0.0.0"
    }
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
