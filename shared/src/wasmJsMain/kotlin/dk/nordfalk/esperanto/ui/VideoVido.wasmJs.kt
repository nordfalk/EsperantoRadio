package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dk.nordfalk.esperanto.domain.model.Elsendo

/** Web-aktualigo: malplena — HTMLAudioElement ne povas ludi HLS/MP4-videojn (CRI estas kaŝita). */
@Composable
actual fun VideoVido(elsendo: Elsendo, modifier: Modifier, montru: Boolean) {
    // neniu — videaj fluoj ne ludiĝas sur Web
}
