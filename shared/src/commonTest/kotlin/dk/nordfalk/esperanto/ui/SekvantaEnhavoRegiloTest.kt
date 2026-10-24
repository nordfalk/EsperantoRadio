package dk.nordfalk.esperanto.ui

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Testoj por [SekvantaEnhavoRegilo] — regulo 1 (gluiĝema): la miniludilbreto
 * montras la laste VIDATAN detalan celon aŭ LUDATAN fonton; nura navigado ne
 * ŝanĝas la enhavon (neniu metodo "navigas", do ĝi ne povas ŝanĝi ĝin).
 */
class SekvantaEnhavoRegiloTest {

    private val elsendoA = Elsendo(id = "a:1", kanaloSlug = "a", titolo = "Elsendo A", fluo = "", dato = "2026-01-01")
    private val elsendoB = Elsendo(id = "b:1", kanaloSlug = "b", titolo = "Elsendo B", fluo = "", dato = "2026-01-02")
    private val kanaloK = Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://x.com/m.m3u8")

    @Test
    fun semoUzigxasKiamNenioEstasVidataAuxLudata() {
        val regilo = SekvantaEnhavoRegilo(semo = { elsendoA })
        // Post restarto, nenio vidata/ludata → la persista laste ludata elsendo
        assertEquals(elsendoA, regilo.elsendo())
        assertNull(regilo.sekvantaKanalo)
    }

    @Test
    fun vidataElsendoGajnasSuperLaSemoKajRestas() {
        val regilo = SekvantaEnhavoRegilo(semo = { elsendoA })
        regilo.vidisElsendon(elsendoB)
        assertEquals(elsendoB, regilo.elsendo())
        // Neniu nova evento (ekz. reen-navigado al la ĉefekrano) — la enhavo restas
        assertEquals(elsendoB, regilo.elsendo())
    }

    @Test
    fun ludadoSenVidadoGajnas() {
        val regilo = SekvantaEnhavoRegilo(semo = { elsendoA })
        regilo.vidisElsendon(elsendoB)
        // Rekte ludita elsendo (sen rigardi ĝian detalon) iĝas la sekvanta enhavo
        regilo.ludisFonton(Sonfonto.ElsendoFonto(elsendoA))
        assertEquals(elsendoA, regilo.elsendo())
    }

    @Test
    fun haltoNeSxangxasLaEnhavon() {
        val regilo = SekvantaEnhavoRegilo(semo = { elsendoA })
        regilo.ludisFonton(Sonfonto.ElsendoFonto(elsendoB))
        // Haltigo (nenio ludas) — la laste ludata restas la sekvanta enhavo
        regilo.ludisFonton(null)
        assertEquals(elsendoB, regilo.elsendo())
    }

    @Test
    fun vidataKanaloForpusxasLaElsendon() {
        val regilo = SekvantaEnhavoRegilo(semo = { elsendoA })
        regilo.vidisElsendon(elsendoB)
        regilo.vidisKanalon(kanaloK)
        // Kanala sekvanta enhavo venas unue; la elsendo-forpuŝo ne gravas
        assertEquals(kanaloK, regilo.sekvantaKanalo)
    }

    @Test
    fun vidataElsendoForpusxasLaKanalon() {
        val regilo = SekvantaEnhavoRegilo(semo = { elsendoA })
        regilo.vidisKanalon(kanaloK)
        regilo.vidisElsendon(elsendoB)
        assertNull(regilo.sekvantaKanalo)
        assertEquals(elsendoB, regilo.elsendo())
    }

    @Test
    fun rektaKanaloLudadoIgasKanalanSekvanton() {
        val regilo = SekvantaEnhavoRegilo(semo = { elsendoA })
        regilo.vidisElsendon(elsendoB)
        regilo.ludisFonton(Sonfonto.RektaKanalo(kanaloK))
        assertEquals(kanaloK, regilo.sekvantaKanalo)
        // La elsenda sekvanto ne plu montriĝas (kanalo venas unue)
        assertNull(regilo.sekvantaElsendo)
    }
}
