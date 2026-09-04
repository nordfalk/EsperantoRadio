package dk.nordfalk.esperanto.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dk.nordfalk.esperanto.EsperantoRadioApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "EsperantoRadio"
    ) {
        EsperantoRadioApp()
    }
}
