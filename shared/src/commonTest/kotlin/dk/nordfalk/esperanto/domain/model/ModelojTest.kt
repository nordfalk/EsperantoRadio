package dk.nordfalk.esperanto.domain.model

import dk.nordfalk.esperanto.data.config.KanalDto
import dk.nordfalk.esperanto.data.config.alKanal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelojTest {

    @Test
    fun kanalEstasRektaKiamRektaElsendaSonoUrlEkzistas() {
        val kanal = Kanal(
            slug = "muzaiko",
            nomo = "Muzaiko",
            rektaElsendaSonoUrl = "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"
        )
        assertTrue(kanal.estasRekta)
        assertFalse(kanal.havasPodkastojn)
    }

    @Test
    fun kanalHavasPodkastojnKiamRssUrlEkzistas() {
        val kanal = Kanal(
            slug = "kernpunkto",
            nomo = "Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"
        )
        assertFalse(kanal.estasRekta)
        assertTrue(kanal.havasPodkastojn)
    }

    @Test
    fun kanalSenFluoj() {
        val kanal = Kanal(slug = "3zzz", nomo = "3ZZZ")
        assertFalse(kanal.estasRekta)
        assertFalse(kanal.havasPodkastojn)
    }

    @Test
    fun alKanalKonvertasDtoĜuste() {
        val dto = KanalDto(
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

        val kanal = dto.alKanal()

        assertEquals("muzaiko", kanal.slug)
        assertEquals("Muzaiko", kanal.nomo)
        assertEquals("https://esperanto-radio.com/_muzaiko.png", kanal.emblemoUrl)
        assertEquals("https://fluo.muzaiko.info/hls/muzaiko/live.m3u8", kanal.rektaElsendaSonoUrl)
        assertEquals("https://javabog.dk/privat/muzaiko_malplena_podkasto.rss", kanal.podkastaRssUrl)
        assertEquals("https://muzaiko.info/", kanal.retejoUrl)
        assertEquals("info@muzaiko.info", kanal.retposhto)
        assertTrue(kanal.ignoruTitolon)
        assertFalse(kanal.montruTitolojn)
        assertTrue(kanal.estasRekta)
        assertTrue(kanal.havasPodkastojn)
    }

    @Test
    fun alKalKunDefaŭltajValoroj() {
        val dto = KanalDto(kodo = "test", nomo = "Test")
        val kanal = dto.alKanal()

        assertEquals("test", kanal.slug)
        assertFalse(kanal.ignoruTitolon)
        assertTrue(kanal.montruTitolojn)
        assertFalse(kanal.uzuWebViewPorElsendo)
    }

    @Test
    fun elsendoKunFluo() {
        val elsendo = Elsendo(
            id = "kernpunkto:2022-11-09",
            kanalSlug = "kernpunkto",
            titolo = "KP204 Pigmentoj",
            stream = "https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3",
            dato = "2022-11-09",
            dauro = 6916,
        )
        assertEquals("kernpunkto:2022-11-09", elsendo.id)
        assertEquals(6916, elsendo.dauro)
        assertFalse(elsendo.estasRekta)
    }

    @Test
    fun sonfontoRektaKanalo() {
        val kanal = Kanal(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "x")
        val fonto = Sonfonto.RektaKanalo(kanal)
        assertTrue(fonto is Sonfonto.RektaKanalo)
    }

    @Test
    fun sonfontoElsendo() {
        val elsendo = Elsendo(id = "x", kanalSlug = "y", titolo = "t", stream = "s", dato = "2024-01-01")
        val fonto = Sonfonto.ElsendoFonto(elsendo)
        assertTrue(fonto is Sonfonto.ElsendoFonto)
    }

    @Test
    fun sonfontoLokaElsendo() {
        val elsendo = Elsendo(id = "x", kanalSlug = "y", titolo = "t", stream = "s", dato = "2024-01-01")
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
        val elsendo = Elsendo(id = "x", kanalSlug = "y", titolo = "t", stream = "s", dato = "2024-01-01")
        val elshutita = ElshutitaElsendo(elsendo, "/tmp/x.mp3", ElshutStato.Preta)
        assertEquals("x", elshutita.elsendo.id)
        assertEquals("/tmp/x.mp3", elshutita.dosieroVojo)
        assertTrue(elshutita.stato is ElshutStato.Preta)
    }
}
