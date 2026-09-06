package dk.nordfalk.esperanto.domain.model

import dk.nordfalk.esperanto.data.config.KanaloDto
import dk.nordfalk.esperanto.data.config.alKanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelojTest {

    @Test
    fun kanalEstasRektaKiamRektaElsendaSonoUrlEkzistas() {
        val kanalo = Kanalo(
            slug = "muzaiko",
            nomo = "Muzaiko",
            rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"
        )
        assertTrue(kanalo.estasRekta)
        assertFalse(kanalo.havasPodkastojn)
    }

    @Test
    fun kanalHavasPodkastojnKiamRssUrlEkzistas() {
        val kanalo = Kanalo(
            slug = "kernpunkto",
            nomo = "Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"
        )
        assertFalse(kanalo.estasRekta)
        assertTrue(kanalo.havasPodkastojn)
    }

    @Test
    fun kanalSenFluoj() {
        val kanalo = Kanalo(slug = "3zzz", nomo = "3ZZZ")
        assertFalse(kanalo.estasRekta)
        assertFalse(kanalo.havasPodkastojn)
    }

    @Test
    fun alKanalKonvertasDtoĜuste() {
        val dto = KanaloDto(
            kodo = "muzaiko",
            nomo = "Muzaiko",
            emblemoUrl = "https://esperanto-radio.com/_muzaiko.png",
            rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8",
            elsendojRssUrl = "https://javabog.dk/privat/muzaiko_malplena_podkasto.rss",
            hejmpaghoButono = "https://muzaiko.info/",
            retposhto = "info@muzaiko.info",
            elsendojRssIgnoruTitolon = true,
            montruTitolojn = false,
        )

        val kanalo = dto.alKanalo()

        assertEquals("muzaiko", kanalo.slug)
        assertEquals("Muzaiko", kanalo.nomo)
        assertEquals("https://esperanto-radio.com/_muzaiko.png", kanalo.emblemoUrl)
        assertEquals("https://fluo.muzaiko.info/hls/muzaiko/live.m3u8", kanalo.rektaElsendaSonoUrl)
        assertEquals("https://javabog.dk/privat/muzaiko_malplena_podkasto.rss", kanalo.podkastaRssUrl)
        assertEquals("https://muzaiko.info/", kanalo.retejoUrl)
        assertEquals("info@muzaiko.info", kanalo.retposhto)
        assertTrue(kanalo.ignoruTitolon)
        assertFalse(kanalo.montruTitolojn)
        assertTrue(kanalo.estasRekta)
        assertTrue(kanalo.havasPodkastojn)
    }

    @Test
    fun alKalKunDefaŭltajValoroj() {
        val dto = KanaloDto(kodo = "test", nomo = "Test")
        val kanalo = dto.alKanalo()

        assertEquals("test", kanalo.slug)
        assertFalse(kanalo.ignoruTitolon)
        assertTrue(kanalo.montruTitolojn)
        assertFalse(kanalo.uzuWebViewPorElsendo)
    }

    @Test
    fun elsendoKunFluo() {
        val elsendo = Elsendo(
            id = "kernpunkto:2022-11-09",
            kanaloSlug = "kernpunkto",
            titolo = "KP204 Pigmentoj",
            fluo = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
            dato = "2022-11-09",
            dauro = 6916,
        )
        assertEquals("kernpunkto:2022-11-09", elsendo.id)
        assertEquals(6916, elsendo.dauro)
        assertFalse(elsendo.estasRekta)
    }

    @Test
    fun sonfontoRektaKanalo() {
        val kanalo = Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "x")
        val fonto = Sonfonto.RektaKanalo(kanalo)
        assertTrue(fonto is Sonfonto.RektaKanalo)
    }

    @Test
    fun sonfontoElsendo() {
        val elsendo = Elsendo(id = "x", kanaloSlug = "y", titolo = "t", fluo = "s", dato = "2024-01-01")
        val fonto = Sonfonto.ElsendoFonto(elsendo)
        assertTrue(fonto is Sonfonto.ElsendoFonto)
    }

    @Test
    fun sonfontoLokaElsendo() {
        val elsendo = Elsendo(id = "x", kanaloSlug = "y", titolo = "t", fluo = "s", dato = "2024-01-01")
        val fonto = Sonfonto.LokaElsendo(elsendo, "/tmp/x.mp3")
        assertTrue(fonto is Sonfonto.LokaElsendo)
        assertEquals("/tmp/x.mp3", fonto.dosieroVojo)
        assertEquals("x", fonto.elsendo.id)
    }

    @Test
    fun elshutStatoNeElshutita() {
        val stato = ElshutStato.NeElshutita
        assertTrue(stato is ElshutStato.NeElshutita)
    }

    @Test
    fun elshutStatoElshutanta() {
        val stato = ElshutStato.Elshutanta(0.5f, 500L, 1000L)
        assertTrue(stato is ElshutStato.Elshutanta)
        assertEquals(0.5f, stato.progreso)
        assertEquals(500L, stato.elshutitajBitokoj)
        assertEquals(1000L, stato.totalajBitokoj)
    }

    @Test
    fun elshutStatoPreta() {
        val stato = ElshutStato.Preta
        assertTrue(stato is ElshutStato.Preta)
    }

    @Test
    fun elshutStatoEraro() {
        val stato = ElshutStato.Eraro("HTTP 404")
        assertTrue(stato is ElshutStato.Eraro)
        assertEquals("HTTP 404", stato.mesagho)
    }

    @Test
    fun elshutStatoPauxzita() {
        val stato = ElshutStato.Pauxzita
        assertTrue(stato is ElshutStato.Pauxzita)
    }

    @Test
    fun elshutitaElsendoKampoj() {
        val elsendo = Elsendo(id = "x", kanaloSlug = "y", titolo = "t", fluo = "s", dato = "2024-01-01")
        val elshutita = ElshutitaElsendo(elsendo, "/tmp/x.mp3", ElshutStato.Preta)
        assertEquals("x", elshutita.elsendo.id)
        assertEquals("/tmp/x.mp3", elshutita.dosieroVojo)
        assertTrue(elshutita.stato is ElshutStato.Preta)
    }
}
