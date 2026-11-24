package dk.nordfalk.esperanto.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Testoj por la platform-filtrado de la tirkest-eroj: ebloj kiuj mankas sur
 * la platformo (elŝutoj, vekhorloĝo) ne devas aperi en la naviga tirkesto.
 */
class TirkestErojTest {

    @Test
    fun androidoMontrasĈion() {
        val etikedoj = tirkestEroj(Platformo.ANDROIDO).map { it.etikedo }
        assertEquals(
            listOf("Hejmo", "Kanaloj", "Plej ŝatataj", "Serĉi", "Elŝutoj", "Vekhorloĝo", "Agordoj"),
            etikedoj,
        )
    }

    @Test
    fun desktopoMontrasElŝutojnSedNeVekhorloĝon() {
        val etikedoj = tirkestEroj(Platformo.DESKTOPO).map { it.etikedo }
        assertEquals(listOf("Hejmo", "Kanaloj", "Plej ŝatataj", "Serĉi", "Elŝutoj", "Agordoj"), etikedoj)
    }

    @Test
    fun retumiloKaŝasElŝutojnKajVekhorloĝon() {
        val etikedoj = tirkestEroj(Platformo.RETUMILO).map { it.etikedo }
        assertEquals(listOf("Hejmo", "Kanaloj", "Plej ŝatataj", "Serĉi", "Agordoj"), etikedoj)
    }

    @Test
    fun iosKaŝasElŝutojnKajVekhorloĝon() {
        val etikedoj = tirkestEroj(Platformo.IOS).map { it.etikedo }
        assertEquals(listOf("Hejmo", "Kanaloj", "Plej ŝatataj", "Serĉi", "Agordoj"), etikedoj)
    }
}
