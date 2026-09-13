package dk.nordfalk.esperanto.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

/**
 * Konvertas HTML-on al [AnnotatedString] per Ksoup.
 *
 * Subtenas: `<b>`, `<strong>`, `<i>`, `<em>`, `<a href>`, `<br>`, `<p>`,
 * `<div>`, `<h1>`–`<h3>`, `<ul>`, `<ol>`, `<li>`, `<img>`.
 *
 * Pura funkcio — tute testebla sur Desktop.
 */
fun htmlAlAnnotatedString(html: String): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")
    val doc = Ksoup.parse(html)
    val builder = AnnotatedString.Builder()
    trairuNodojn(doc.body().childNodes(), builder, TipStako())
    return builder.toAnnotatedString()
}

private data class TipStako(
    val grasa: Boolean = false,
    val kursiva: Boolean = false,
    val ligiloUrl: String? = null,
)

private fun TipStako.alSpanStyle(): SpanStyle? {
    return when {
        grasa && kursiva -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        grasa -> SpanStyle(fontWeight = FontWeight.Bold)
        kursiva -> SpanStyle(fontStyle = FontStyle.Italic)
        else -> null
    }
}

private fun trairuNodojn(nodoj: List<Node>, builder: AnnotatedString.Builder, stako: TipStako) {
    for (nodo in nodoj) {
        when (nodo) {
            is TextNode -> {
                val teksto = nodo.text()
                val spanStilo = stako.alSpanStyle()
                val ligilo = stako.ligiloUrl
                when {
                    ligilo != null -> {
                        builder.pushLink(LinkAnnotation.Url(ligilo))
                        if (spanStilo != null) builder.withStyle(spanStilo) { append(teksto) }
                        else builder.append(teksto)
                        builder.pop()
                    }
                    spanStilo != null -> builder.withStyle(spanStilo) { append(teksto) }
                    else -> builder.append(teksto)
                }
            }
            is Element -> trairuElementon(nodo, builder, stako)
            else -> {}
        }
    }
}

private fun trairuElementon(el: Element, builder: AnnotatedString.Builder, stako: TipStako) {
    when (el.tagName().lowercase()) {
        "br" -> builder.append("\n")
        "p", "div" -> {
            // Lini-salto antaŭ la bloko (krom komence)
            if (builder.length > 0) builder.append("\n")
            trairuNodojn(el.childNodes(), builder, stako)
            builder.append("\n")
        }
        "b", "strong" -> trairuNodojn(el.childNodes(), builder, stako.copy(grasa = true))
        "i", "em" -> trairuNodojn(el.childNodes(), builder, stako.copy(kursiva = true))
        "a" -> {
            val href = el.attr("href")
            if (href.isNotEmpty()) {
                trairuNodojn(el.childNodes(), builder, stako.copy(ligiloUrl = href))
            } else {
                trairuNodojn(el.childNodes(), builder, stako)
            }
        }
        "h1", "h2", "h3" -> {
            if (builder.length > 0) builder.append("\n")
            val grandeco = when (el.tagName().lowercase()) {
                "h1" -> 24.sp
                "h2" -> 20.sp
                else -> 18.sp
            }
            builder.withStyle(SpanStyle(fontSize = grandeco, fontWeight = FontWeight.Bold)) {
                trairuNodojn(el.childNodes(), builder, stako.copy(grasa = true))
            }
            builder.append("\n")
        }
        "ul" -> {
            if (builder.length > 0) builder.append("\n")
            for (ero in el.children()) {
                if (ero.tagName().lowercase() == "li") {
                    builder.append("- ")
                    trairuNodojn(ero.childNodes(), builder, stako)
                    builder.append("\n")
                }
            }
        }
        "ol" -> {
            if (builder.length > 0) builder.append("\n")
            var n = 1
            for (ero in el.children()) {
                if (ero.tagName().lowercase() == "li") {
                    builder.append("$n. ")
                    trairuNodojn(ero.childNodes(), builder, stako)
                    builder.append("\n")
                    n++
                }
            }
        }
        "li" -> {
            // Aŭtonoma <li> ekster listo — traktu kiel liinion
            builder.append("- ")
            trairuNodojn(el.childNodes(), builder, stako)
            builder.append("\n")
        }
        "img" -> {
            val alt = el.attr("alt")
            builder.append(if (alt.isNotEmpty()) "[$alt]" else "[bildo]")
        }
        "script", "style" -> { /* ignori */ }
        else -> trairuNodojn(el.childNodes(), builder, stako)
    }
}
