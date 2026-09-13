package dk.nordfalk.esperanto.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * wasmJs-aktualigo: uzas [htmlAlAnnotatedString] per [Text].
 */
@Composable
actual fun HtmlVido(html: String, modifier: Modifier, uzuWebView: Boolean) {
    Text(
        text = htmlAlAnnotatedString(html),
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}
