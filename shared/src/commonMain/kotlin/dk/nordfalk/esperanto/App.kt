package dk.nordfalk.esperanto

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.repository.KanalDeponejoImpl
import dk.nordfalk.esperanto.ui.KanalaroEkrano
import dk.nordfalk.esperanto.ui.KanalaroViewModel

/**
 * La radika Compose-funkcio por la tuta apo.
 * Komuna trans Android, iOS, Desktop, Web.
 */
@Composable
fun EsperantoRadioApp() {
    MaterialTheme {
        val viewModel = remember {
            val leganto = KanalAgordoLeganto()
            val deponejo = KanalDeponejoImpl(
                leganto = leganto,
                bundledTeksto = ::leguBundledKanalkonfiguron
            )
            KanalaroViewModel(deponejo)
        }
        KanalaroEkrano(viewModel = viewModel)
    }
}
