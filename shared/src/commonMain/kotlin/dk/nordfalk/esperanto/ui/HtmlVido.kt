package dk.nordfalk.esperanto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Vidigas HTML-tekston riĉe.
 *
 * @param html purigita HTML kun etikedoj
 * @param modifier Compose-modifier
 * @param uzuWebView se true, uzas WebView (nur Android) por plena HTML-vidigo;
 *                   se false, uzas AnnotatedString per [htmlAlAnnotatedString]
 */
@Composable
expect fun HtmlVido(html: String, modifier: Modifier = Modifier, uzuWebView: Boolean = false)
