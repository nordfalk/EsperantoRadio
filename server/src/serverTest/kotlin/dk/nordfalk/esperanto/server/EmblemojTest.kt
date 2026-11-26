package dk.nordfalk.esperanto.server

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Emblemo-spegulado (FAROTA C56) kaj flua URL-normigo — la helpoj, kiujn la
 * konstruilo uzas por gastigi bildojn samdevene kaj forigi http-ligilojn.
 */
class EmblemojTest {

    @Test
    fun publikajAdresojAkceptiĝas() {
        assertTrue(Emblemoj.ĉuPublikaAdreso(InetAddress.getByName("93.184.216.34")))
    }

    @Test
    fun ipv6PublikaAkceptiĝasInternaRifuziĝas() {
        // Du-stackaj gastigantoj (A + publikaj AAAA — ekz. blogger.googleusercontent.com,
        // 17 el 28 nunaj bildogastigantoj) devas trairi: publika IPv6 estas publika.
        assertTrue(Emblemoj.ĉuPublikaAdreso(InetAddress.getByName("2606:4700:4700::1111")))
    }

    @Test
    fun nepublikajAdresojRifuziĝas() {
        // Ne-publikaj IPv4 (0/8, 10/8, 172.16/12, 192.168/16, 169.254/16, 127/8,
        // 100.64/10, 198.18/15, 224/4, 240/4) plus IPv6: :: kaj ::1 (nedifinita/
        // loopback), fd00 (ULA), fe80 (link-local), ff02 (multicast),
        // IPv4-mapeita 127.0.0.1 (Java konvertas ĝin al IPv4-objekto).
        val nepublikaj = listOf(
            "0.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.1.1",
            "127.0.0.1", "100.64.0.1", "198.18.0.1", "224.0.0.1", "250.1.2.3",
            "::", "::1", "fd00::1", "fe80::1", "fec0::1", "ff02::1", "::ffff:127.0.0.1",
        )
        for (a in nepublikaj) {
            assertFalse(Emblemoj.ĉuPublikaAdreso(InetAddress.getByName(a)), "devus rifuziĝi: $a")
        }
    }

    @Test
    fun ipLiteralojRekoniĝas() {
        assertTrue(Emblemoj.estasIpLiteralo("127.0.0.1"))
        assertTrue(Emblemoj.estasIpLiteralo("10.0.0.1"))
        assertTrue(Emblemoj.estasIpLiteralo("[::1]"))   // kiel URI.host redonas IPv6-literalon
        assertTrue(Emblemoj.estasIpLiteralo("::1"))    // sendepende de krampoj
        assertTrue(Emblemoj.estasIpLiteralo("1.2.3.4"))
        // la mallongaj numeraj formoj, kiujn InetAddress ankaŭ interpretas kiel IPv4
        assertTrue(Emblemoj.estasIpLiteralo("134744072")) // = 8.8.8.8
        assertTrue(Emblemoj.estasIpLiteralo("1.2.3"))
        assertTrue(Emblemoj.estasIpLiteralo("1.2"))
        // nomoj NENIAM estas IP-literaloj — eĉ ne kun ciferoj
        assertFalse(Emblemoj.estasIpLiteralo("example.com"))
        assertFalse(Emblemoj.estasIpLiteralo("bilder3.example.org"))
        assertFalse(Emblemoj.estasIpLiteralo("1.2.3.4.5"))
        assertFalse(Emblemoj.estasIpLiteralo("3zzz.net"))
    }

    @Test
    fun bildaCeloPermesListaRifuzasNenormalaĵojn() {
        // ne-normaj URL/konstruo kaj skemoj
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("ftp://example.com/bildo.png"))
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("ne-url"))
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http:///radiko.png")) // sen gastiganto
        // IP-LITERALO anstataŭ nomo — rifuzata sen eĉ resolvo
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http://127.0.0.1/bildo.png"))
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http://10.0.0.1/bildo.png"))
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http://93.184.216.34/bildo.png")) // eĉ publika
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http://[::1]/bildo.png"))
        // la unuparta numera formo, kiun InetAddress interpretas kiel IPv4 (8.8.8.8)
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http://134744072/bildo.png"))
        // STRANGAJ HAVENOJ rifuzataj (la plej rekta vojo al internaj servoj) —
        // http nur 80 (implicite aŭ eksplicite), https nur 443
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http://example.com:8080/bildo.png"))
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("http://example.com:443/bildo.png"))
        assertFalse(Emblemoj.ĉuAkcepteblaBildaCelo("https://example.com:80/bildo.png"))
        // Noto: la ADRES-validigo (publikaj adresoj, localhost ktp.) ne okazas
        // en ĉi tiu frua kontrolo — ĝin faras la Dns-hoko de la motoro
        // (vidu sekuraDnsRifuzasNepublikajnResolvojn).
    }

    @Test
    fun skemoKajHavenoUseklecIndiferenta() {
        // majusklaj skemoj estas validaj (RFC 3986 §3.1) — java.net.URI ne normigas ilin
        assertTrue(Emblemoj.skemoKajHavenoAkceptebla("HTTP", -1))
        assertTrue(Emblemoj.skemoKajHavenoAkceptebla("HTTPS", 443))
        assertTrue(Emblemoj.skemoKajHavenoAkceptebla("https", -1))
        assertTrue(Emblemoj.skemoKajHavenoAkceptebla("http", 80))
        // nenormalaj paroj
        assertFalse(Emblemoj.skemoKajHavenoAkceptebla("https", 80))
        assertFalse(Emblemoj.skemoKajHavenoAkceptebla("http", 443))
        assertFalse(Emblemoj.skemoKajHavenoAkceptebla("http", 8080))
        assertFalse(Emblemoj.skemoKajHavenoAkceptebla(null, -1))
        assertFalse(Emblemoj.skemoKajHavenoAkceptebla("ftp", -1))
    }

    @Test
    fun dimensiaLimoTesteblaCheRando() {
        // la gardon mem (larĝo×alto el la kapo, ANTAŬ la plena malkodado) oni
        // devas povi testi sen barakti per plenmalkodada fiasko: la limo estas
        // INKLUZIVA — 5000×5000 = precize 25 MP eniras, 5001×5000 ne plu
        assertTrue(Emblemoj.dimensioEnLimo(5000, 5000))
        assertFalse(Emblemoj.dimensioEnLimo(5001, 5000))
        assertFalse(Emblemoj.dimensioEnLimo(0, 0))
        assertFalse(Emblemoj.dimensioEnLimo(-1, 100))
        assertTrue(Emblemoj.dimensioEnLimo(1, 1))
    }

    @Test
    fun sekuraDnsRifuzasNepublikajnResolvojn() {
        // localhost → 127.0.0.1/::1: neniu publika adreso → escepto (sen reto)
        assertFailsWith<java.net.UnknownHostException> {
            Emblemoj.SekuraBildaDns.lookup("localhost")
        }
    }

    @Test
    fun bazaBildaUrlDerivasLaRadikon() {
        assertEquals("https://javabog.dk/esperanto-radio", Emblemoj.bazaBildaUrl("https://javabog.dk/esperanto-radio/kanaloj/"))
        assertEquals("https://javabog.dk/esperanto-radio", Emblemoj.bazaBildaUrl("https://javabog.dk/esperanto-radio/kanaloj"))
        // sentraila bazo: restas, kiel ĝi estas (neniu dua divenado)
        assertEquals("https://alia.example", Emblemoj.bazaBildaUrl("https://alia.example"))
    }

    private fun bitokoj(vararg v: Int) = v.map { it.toByte() }.toByteArray()

    @Test
    fun formatoLaŭEnhavoRekonasMagiajnSignojn() {
        // PNG (89 50 4E 47 0D 0A 1A 0A)
        assertEquals("png", Emblemoj.formatoLaŭEnhavo(bitokoj(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + bitokoj(1, 2, 3)))
        // JPEG (FF D8 FF — SOI + marko)
        assertEquals("jpg", Emblemoj.formatoLaŭEnhavo(bitokoj(0xFF, 0xD8, 0xFF, 0xE0) + bitokoj(0, 0x10)))
        // GIF89a kaj GIF87a
        assertEquals("gif", Emblemoj.formatoLaŭEnhavo("GIF89a1x".encodeToByteArray()))
        assertEquals("gif", Emblemoj.formatoLaŭEnhavo("GIF87a1x".encodeToByteArray()))
    }

    @Test
    fun formatoLaŭEnhavoRifuzasNeBildojn() {
        // SVG — la XSS-vektoro (CWE-79): ne akceptebla eĉ kun image/png-kapo
        assertEquals(null, Emblemoj.formatoLaŭEnhavo("<svg onload=alert(1)>".encodeToByteArray()))
        // HTML-erarpaĝo, malplena/trunkita enhavo, nura kapo-prefikso
        assertEquals(null, Emblemoj.formatoLaŭEnhavo("<html><body>404</body></html>".encodeToByteArray()))
        assertEquals(null, Emblemoj.formatoLaŭEnhavo("GIF".encodeToByteArray())) // tro mallonga (nur 3 signoj)
        assertEquals(null, Emblemoj.formatoLaŭEnhavo(bitokoj()))
        assertEquals(null, Emblemoj.formatoLaŭEnhavo(bitokoj(0xFF, 0xD8))) // tro mallonga JPEG
    }

    private fun veraPng(): ByteArray {
        val bildo = java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val el = ByteArrayOutputStream()
        ImageIO.write(bildo, "png", el)
        return el.toByteArray()
    }

    /** Generas animacian GIF-on kun [kadroj] 1x1-kadroj per ImageIO-sekvenco. */
    private fun animaciaGif(kadroj: Int): ByteArray {
        val bildo = java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val skribanto = ImageIO.getImageWritersByFormatName("gif").next()
        val el = java.io.ByteArrayOutputStream()
        val fluo = ImageIO.createImageOutputStream(el)
        try {
            skribanto.output = fluo
            skribanto.prepareWriteSequence(null)
            repeat(kadroj) { skribanto.writeToSequence(javax.imageio.IIOImage(bildo, null, null), null) }
            skribanto.endWriteSequence()
        } finally {
            skribanto.dispose()
            fluo.close()
        }
        return el.toByteArray()
    }

    @Test
    fun kadroBomboRifuziĝasAntaŭPlenaMalkodado() {
        // 301-kadra GIF (ĉiuj 1x1) — la limo estas 300 kadroj
        assertEquals(null, Emblemoj.formatoSeLegebla(animaciaGif(kadroj = (Emblemoj.MAKS_BILDAJ_KADROJ + 1).toInt())))
        // 60-kadra GIF (~normala animacia emblemo) validas
        assertEquals("gif", Emblemoj.formatoSeLegebla(animaciaGif(kadroj = 60)))
    }

    @Test
    fun dimensiaBomboRifuziĝasAntaŭPlenaMalkodado() {
        // Vera 4x4-png, kies IHDR-kapo estas repaĉita al 50000x50000
        // (2,5 miliardoj da pikseloj — malkonprem-bombo, CWE-409). La dimensia
        // kontrolo legas la KAPON kaj rifuzas ANTAŬ ImageIO.read povus
        // enmemorigi gigantojn da pikseloj.
        val b = veraPng().copyOf()
        fun skribu32(ofseto: Int, valoro: Int) {
            b[ofseto] = (valoro ushr 24).toByte()
            b[ofseto + 1] = (valoro ushr 16).toByte()
            b[ofseto + 2] = (valoro ushr 8).toByte()
            b[ofseto + 3] = valoro.toByte()
        }
        // PNG: 8 signoj da signaturo + 4 longo + 4 "IHDR", poste larĝo kaj alto (big-endian)
        skribu32(16, 50_000)
        skribu32(20, 50_000)
        assertEquals(null, Emblemoj.formatoSeLegebla(b))
    }

    @Test
    fun formatoSeLegeblaAkceptasVeranBildon() {
        // vera (per ImageIO generita) PNG malkodeblas
        assertEquals("png", Emblemoj.formatoSeLegebla(veraPng()))
    }

    @Test
    fun formatoSeLegeblaRifuzasKoruptitaJenJustaSignaturo() {
        // 1) PNG-signaturo + sensenca resto: la nura signatura kontrolo
        //    (formatoLaŭEnhavo) trapasus, sed la vera malkodado fiaskas
        val kunSignaturoSedRuba = bitokoj(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + "rubo, ne bildo".encodeToByteArray()
        assertEquals("png", Emblemoj.formatoLaŭEnhavo(kunSignaturoSedRuba), "averto: la nura signatura kontrolo trapasas rubon")
        assertEquals(null, Emblemoj.formatoSeLegebla(kunSignaturoSedRuba))
        // 2) vera PNG, duone fortranĉita: la kapo ĝustas, la malkodado fiaskas
        val png = veraPng()
        val trunkita = png.copyOf(png.size / 2) + bitokoj(1, 2, 3, 4)
        assertEquals("png", Emblemoj.formatoLaŭEnhavo(trunkita), "averto: la nura signatura kontrolo trapasas trunkitan")
        assertEquals(null, Emblemoj.formatoSeLegebla(trunkita))
        // 3) SVG restas rifuzata ankaŭ per la plena kontrolo
        assertEquals(null, Emblemoj.formatoSeLegebla("<svg onload=alert(1)>".encodeToByteArray()))
    }

    @Test
    fun resskriboAnstataŭigasNurPrenitajnValorojn() {
        val teksto = """
            {
              "kanaloj": [
                { "slug": "a", "emblemoUrl": "https://a.example/e.png" },
                { "slug": "b", "emblemoUrl": "https://b.example/e.png" }
              ]
            }
        """.trimIndent()
        val nova = Emblemoj.resskribuEmblemojn(
            teksto,
            mapOf("https://a.example/e.png" to "https://javabog.dk/esperanto-radio/bildoj/a.png"),
        )
        assertTrue(nova.contains("\"https://javabog.dk/esperanto-radio/bildoj/a.png\""), nova)
        assertTrue(nova.contains("\"https://b.example/e.png\""), "ne-prenita URL restu: $nova")
        assertFalse(nova.contains("\"https://a.example/e.png\""), nova)
    }

    @Test
    fun resskriboAnstataŭigasĈiujnAperojnDeSamaURL() {
        val teksto = """
            { "a": "https://komuna.example/e.png", "b": "https://komuna.example/e.png" }
        """.trimIndent()
        val nova = Emblemoj.resskribuEmblemojn(
            teksto,
            mapOf("https://komuna.example/e.png" to "https://javabog.dk/esperanto-radio/bildoj/a.png"),
        )
        assertEquals(2, Regex("\"https://javabog.dk/esperanto-radio/bildoj/a.png\"").findAll(nova).count(), nova)
    }

    @Test
    fun fluoArchiveOrgEkzistasEnHttps() {
        assertEquals(
            "https://archive.org/download/UniversealaLindo001/UniversealaLindo001.mp3",
            FluSkribilo.normiguFluanUrl("http://archive.org/download/UniversealaLindo001/UniversealaLindo001.mp3"),
        )
        // aliaj http-URL-oj ne tuŝiĝas — ne ĉiu fonto havas https-ekvivalenton
        assertEquals("http://plain.example/epizodo.mp3", FluSkribilo.normiguFluanUrl("http://plain.example/epizodo.mp3"))
        assertEquals("https://kern.punkto.info/d.mp3", FluSkribilo.normiguFluanUrl("https://kern.punkto.info/d.mp3"))
    }
}
