package dk.nordfalk.esperanto.ui

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dk.nordfalk.esperanto.logw

/**
 * Android-aktualigo: uzas [WebView] se [uzuWebView] estas true,
 * alie uzas [htmlAlAnnotatedString] per [Text].
 *
 * Sekureco: la enhavo venas el nefidindaj RSS-priskriboj. JavaScript estas
 * malŝaltita kaj [WebViewClient] blokas ĉian navigadon ene de la WebView —
 * http(s)-ligiloj malfermiĝas ekstere, ĉio alia (ekz. `intent://`) estas
 * blokata (FAROTA G4). Ankaŭ `file://`/`content://`-aliro estas malŝaltita.
 */
@Composable
actual fun HtmlVido(html: String, modifier: Modifier, uzuWebView: Boolean) {
    if (uzuWebView) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.loadWithOverviewMode = true
                    isVerticalScrollBarEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val uri: Uri = request.url
                            val skemo = uri.scheme?.lowercase()
                            if (skemo == "http" || skemo == "https") {
                                try {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (e: Exception) {
                                    logw("HtmlVido", "Ne eblis malfermi ligilon: $uri", e)
                                }
                            } else {
                                logw("HtmlVido", "Blokas ligilon kun skemo '$skemo': $uri")
                            }
                            // Ĉiam true: neniam navigu ene de la WebView
                            return true
                        }
                    }
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
