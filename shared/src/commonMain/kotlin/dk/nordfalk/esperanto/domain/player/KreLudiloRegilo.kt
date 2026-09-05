package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kreas la platform-specifan LudiloRegilo-n.
 * Sur Android: uzas Media3 ExoPlayer.
 * Sur Desktop/Web: provizore no-op (neniu sono, sed UI funkcias).
 */
expect fun kreLudiloRegilo(): LudiloRegilo
