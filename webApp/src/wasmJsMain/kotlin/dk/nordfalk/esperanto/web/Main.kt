@file:OptIn(ExperimentalComposeUiApi::class)

package dk.nordfalk.esperanto.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import dk.nordfalk.esperanto.EsperantoRadioApp

fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        EsperantoRadioApp()
    }
}
