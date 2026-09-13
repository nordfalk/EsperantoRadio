package dk.nordfalk.esperanto.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.initialiguSentry

fun main() = application {
    initialiguSentry()
    Window(
        onCloseRequest = ::exitApplication,
        title = "EsperantoRadio"
    ) {
        EsperantoRadioApp()
    }
}
