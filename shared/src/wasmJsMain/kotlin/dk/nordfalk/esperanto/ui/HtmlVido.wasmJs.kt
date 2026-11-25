package dk.nordfalk.esperanto.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * wasmJs-aktualigo: uzas [htmlAlAnnotatedString] per [Text] — envolvita en
 * [SelectionContainer], ĉar la reta UI estas kanvaso kaj la retumilo mem ne
 * povas elekti tekston. La elekto tiel funkcias ene de la apo (muso-treno).
 * (Kopii per klavaro necesas aparte provi sur la celataj retumiloj.)
 */
@Composable
actual fun HtmlVido(html: String, modifier: Modifier, uzuWebView: Boolean) {
    SelectionContainer {
        Text(
            text = htmlAlAnnotatedString(html),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}
