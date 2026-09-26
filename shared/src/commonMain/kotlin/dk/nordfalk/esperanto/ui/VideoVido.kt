package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dk.nordfalk.esperanto.domain.model.Elsendo

/**
 * Vidigas la filmotrackon de videa elsendo (ekz. CRI: HLS/MP4-video).
 *
 * Sur Android tio ligas [androidx.media3.ui.PlayerView] al la ludilo de la
 * ludado-servo (`VideoLudiloPonto`) — la sama ludilo kiu jam ludas la sonon.
 * Sur la aliaj platformoj (Desktop/Web/iOS) la fluoj ne ludiĝas entute
 * (CRI estas kaŝita per `videblaNurSur`), do la aktualigo estas malplena.
 *
 * @param elsendo la elsendo — la filmo montriĝas nur se la servo-ludilo
 *   efektive ludas ĝian fluon
 * @param modifier Compose-modifier
 * @param montru se false, la komponanto ne desegnas nenion
 */
@Composable
expect fun VideoVido(elsendo: Elsendo, modifier: Modifier = Modifier, montru: Boolean)
