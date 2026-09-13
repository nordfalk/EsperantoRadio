package dk.nordfalk.esperanto.ui

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlTekstoTest {

    @Test
    fun htmlAlAnnotatedStringKunGrasaTeksto() {
        val html = "<p>Hello <b>world</b></p>"
        val rezulto = htmlAlAnnotatedString(html)

        assertTrue(rezulto.text.contains("Hello"), "Teksto devas enhavi 'Hello': ${rezulto.text}")
        assertTrue(rezulto.text.contains("world"), "Teksto devas enhavi 'world': ${rezulto.text}")

        // Iu SpanStyle devas havi fontWeight = Bold
        val grasaStiloj = rezulto.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertTrue(grasaStiloj.isNotEmpty(), "Devas ekzisti grasa SpanStyle")
    }

    @Test
    fun htmlAlAnnotatedStringKunLigilo() {
        val html = "<p>Vidu <a href=\"https://ekzemplo.com\">ĉi tie</a></p>"
        val rezulto = htmlAlAnnotatedString(html)

        assertTrue(rezulto.text.contains("ĉi tie"), "Teksto devas enhavi 'ĉi tie': ${rezulto.text}")

        val ligiloj = rezulto.getLinkAnnotations(0, rezulto.length)
        assertTrue(ligiloj.isNotEmpty(), "Devas ekzisti LinkAnnotation")
        val unuaLigilo = ligiloj.first().item
        assertTrue(unuaLigilo is LinkAnnotation.Url, "Ligilo devas esti LinkAnnotation.Url")
        assertEquals("https://ekzemplo.com", (unuaLigilo as LinkAnnotation.Url).url)
    }

    @Test
    fun htmlAlAnnotatedStringKunLiniSalto() {
        val html = "<p>Linio 1</p><p>Linio 2</p>"
        val rezulto = htmlAlAnnotatedString(html)

        assertTrue(rezulto.text.contains("Linio 1"), "Teksto devas enhavi 'Linio 1': ${rezulto.text}")
        assertTrue(rezulto.text.contains("Linio 2"), "Teksto devas enhavi 'Linio 2': ${rezulto.text}")
        assertTrue(rezulto.text.contains("\n"), "Teksto devas enhavi lini-salton: ${rezulto.text}")
    }
}
