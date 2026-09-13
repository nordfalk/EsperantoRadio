package dk.nordfalk.esperanto.ui

import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android-aktualigo: uzas [WebView] se [uzuWebView] estas true,
 * alie uzas [htmlAlAnnotatedString] per [Text].
 */
@Composable
actual fun HtmlVido(html: String, modifier: Modifier, uzuWebView: Boolean) {
    if (uzuWebView) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true
                    isVerticalScrollBarEnabled = false
                }
            },
            modifier = modifier,
            update = { webview ->
                val htmlKunCss = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1'/>" +
                    "<style>body{font-size:16px;line-height:1.5;padding:8px;margin:0;}" +
                    "img{max-width:100%;height:auto;}</style></head><body>$html</body></html>"
                webview.loadDataWithBaseURL(null, htmlKunCss, "text/html", "UTF-8", null)
            }
        )
    } else {
        Text(
            text = htmlAlAnnotatedString(html),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }
}
