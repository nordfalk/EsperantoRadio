@file:OptIn(ExperimentalComposeUiApi::class)

package dk.nordfalk.esperanto.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.initialiguSentry

fun main() {
    initialiguSentry()
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        EsperantoRadioApp()
    }
}
