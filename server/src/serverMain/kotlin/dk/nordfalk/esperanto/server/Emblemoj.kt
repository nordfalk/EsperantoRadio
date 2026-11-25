package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logw
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI
import java.security.MessageDigest
import javax.imageio.ImageIO

/**
 * Emblemoj sur la arkiva servilo (FAROTA C56): la retversio (wasmJs) gastigas
 * samdevene apud la arkivo, sed la emblemoj de la kanaloj venas el la diversaj
 * originalaj gastigantoj — ŝargo de bildoj per `fetch` estas CORS-blokita en la
 * retumilo. Tial la konstruilo spegulas ĉiun emblemon al
 * `server/data/publika/bildoj/<slug>.<ekst>` kaj resskribas la `emblemoUrl`-on
 * en la publikigita konfiguro al la arkiva URL. Tiel ĉiu bildo-ŝargo de la apo
 * (ajn platformo — Coil ŝargas per HTTP) estas samdevena kaj sen CORS.
 */
object Emblemoj {

    /**
     * Maksimuma nombro da pikseloj de spegulata bildo — ŝirmo kontraŭ
     * malkonprem-bomboj (CWE-409, PR #214): 5-MB-dosiero povas aserti eĉ
     * 50 000×50 000 pikselojn kaj la plena malkodado estus enmemorigi
     * multajn gigabitokojn da pikseloj antaŭ ol iu kontrolo povus rifuzi.
     * La dimensioj estas kontrolataj el la kapo ANTAŬ la malkodado
     * (vidu [formatoSeLegebla]); 25 MP kovras ĉiun realistican emblemon.
     */
    const val MAKS_BILDAJ_PIKSELOJ: Long = 25_000_000L

    /**
     * Maksimuma nombro da kadroj de spegulata bildo (animacia GIF ktp.) —
     * dua vojo de la malkonprem-bombo-ŝirmo (CWE-409): multmil-kadra GIF
     * estus malkodita frame post frame ankaŭ se unu kadro estas malgranda.
     */
    const val MAKS_BILDAJ_KADROJ: Long = 300L

    /** Baza URL de la spegulitaj emblemoj, derivita el `arkivoBazoUrl` (`…/kanaloj/` → la radiko). */
    fun bazaBildaUrl(arkivoBazoUrl: String): String {
        val radiko = arkivoBazoUrl.trimEnd('/')
        return if (radiko.endsWith("/kanaloj")) radiko.removeSuffix("/kanaloj") else radiko
    }

    /**
     * Determinas la bild-formaton laŭ la ENHAVO de la elŝutitaj bitokoj (magiaj
     * signoj), NE laŭ Content-Type aŭ dosiernomo — kadre de la XSS-ŝirmo (PR #210):
     * nur enhavo, kiu vere estas legebla kiel PNG, JPEG aŭ GIF, speguliĝas
     * (decido de la posedanto 2026-11-24). SVG (povus ruli skripton samdevene,
     * CWE-79), HTML, erar-paĝoj, WebP kaj ĉio alia estas rifuzataj → `null`
     * (la emblemo restas ĉe la originala URL, regulo A4).
     */
    fun formatoLaŭEnhavo(bitokoj: ByteArray): String? {
        fun kongruas(vararg signoj: Int): Boolean {
            if (bitokoj.size < signoj.size) return false
            for (i in signoj.indices) if ((bitokoj[i].toInt() and 0xFF) != signoj[i]) return false
            return true
        }
        return when {
            kongruas(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "png"
            kongruas(0xFF, 0xD8, 0xFF) -> "jpg"
            kongruas(0x47, 0x49, 0x46, 0x38, 0x39, 0x61) -> "gif" // GIF89a
            kongruas(0x47, 0x49, 0x46, 0x38, 0x37, 0x61) -> "gif" // GIF87a
            else -> null
        }
    }

    /**
     * Dosiernoma radiko por spegulata elsenda bildeto: SHA-1 de la originala URL
     * (40 hex-signoj). La sama URL ĉiam ricevas la saman nomon — tial la
     * ekzisto-kontrolo sutas: jam spegulita bildeto neniam re-elŝutiĝas, kaj
     * pluraj kanaloj kun la sama bildo kunhavas unun spegulon.
     */
    fun bildetaRadiko(url: String): String =
        MessageDigest.getInstance("SHA-1").digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /**
     * Permes-lista regulo de la posedanto (2026-11-25): bildo-celo akceptiĝas
     * NUR se ĝi estas PUBLIKA IPv4-adreso. Ĉio alia estas „nenormala" kaj
     * rifuzata sen kompenso — ĉIU IPv6 (ankaŭ publika kiel 2606:4700…, ULA,
     * link-local, teredo, 6to4, IPv4-mapeita), privata, loopback, link-local,
     * CGNAT, benchmark-, multicast- kaj rezervita IPv4. (SSRF-ŝirmo de la
     * bildo-spegele, CWE-918, PR #214; simpligita laŭ posedanto-decido.)
     */
    fun ĉuPublikaAdreso(adreso: InetAddress): Boolean {
        if (adreso !is Inet4Address) return false // nenia IPv6, kia ajn ĝi estas
        val o0 = adreso.address[0].toInt() and 0xFF
        val o1 = adreso.address[1].toInt() and 0xFF
        return !(o0 == 0 ||                                          // 0.0.0.0/8
            o0 == 10 || o0 == 127 ||                                // 10/8, 127/8
            (o0 == 172 && o1 in 16..31) ||                          // 172.16/12
            (o0 == 192 && o1 == 168) ||                             // 192.168/16
            (o0 == 169 && o1 == 254) ||                              // 169.254/16
            (o0 == 100 && (o1 and 0xC0) == 64) ||                    // 100.64/10 (CGNAT)
            (o0 == 198 && (o1 == 18 || o1 == 19)) ||                 // 198.18/15
            (o0 and 0xF0) == 0xE0 ||                                 // 224/4 (multicast)
            o0 >= 240)                                               // 240/4 (rezervita)
    }

    /** Skemoj permesataj por bildo-elŝuto (permes-listo: ĉiu alia skemo estas „nenormala"). */
    private val SKEMOJ = setOf("http", "https")

    /**
     * Ĉu la bildo-URL tute estas akceptebla laŭ la permes-listo (posedanto-decido
     * 2026-11-25 — „ĉio nenormala → rifuzu"):
     * - skemo http(s) kaj gastiganto estas NOMO, ne IP-literalo;
     * - ĈIU adreso, al kiu la nomo resolvas, estas publika IPv4
     *   ([ĉuPublikaAdreso]) — neniu IPv6, neniu interna/privata reto.
     * Malplena/neparsebla URL, nekonata skemo, IP-literalo, neresolvebla aŭ
     * ne-publika resolvo → false (la bildo restas ĉe la originala URL, A4).
     */
    fun ĉuAkcepteblaBildaCelo(url: String): Boolean {
        return try {
            val uri = URI(url)
            val gastiganto = uri.host ?: return false
            uri.scheme in SKEMOJ && !estasIpLiteralo(gastiganto) &&
                InetAddress.getAllByName(gastiganto).all { ĉuPublikaAdreso(it) }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ĉu la gastiganto estas IP-LITERALO (ne DNS-nomo): IPv4 (`1.2.3.4`) aŭ
     * IPv6 (`[::1]`, kun aŭ sen krampoj). Tia „IP-adreso anstataŭ nomo" estas
     * nenormala por podkasto-bildo kaj rifuziĝas sen eĉ provi resolvon.
     */
    fun estasIpLiteralo(gastiganto: String): Boolean {
        val nuda = gastiganto.removePrefix("[").removeSuffix("]")
        if (':' in nuda) return true
        val pecoj = nuda.split('.')
        return pecoj.size == 4 && pecoj.all { it.isNotEmpty() && it.all(Char::isDigit) }
    }

    /**
     * Kiel [formatoLaŭEnhavo], sed ankaŭ validigas ke la enhavo vere estas
     * malkodebla bildo (plena ImageIO-malkodado): trunkita, koruptita aŭ
     * kaŝvestita enhavo kun ĝusta signaturo estas rifuzata (→ null; la
     * emblemo restas ĉe la originala URL, A4).
     *
     * Datum-ŝirmoj antaŭ la malkodado (CWE-409 — malkonprem-bomboj), legitaj el
     * la kapo per [javax.imageio.ImageReader] SEN malkodi pikselojn:
     * - dimensio: maks. [MAKS_BILDAJ_PIKSELOJ] pikseloj (ekz. 5-MB-PNG kun
     *   kapo 50000×50000 estus postulinta multajn GiB da piksela memoro);
     * - kadroj: maks. [MAKS_BILDAJ_KADROJ] (animacia GIF kun miloj da kadroj).
     */
    fun formatoSeLegebla(bitokoj: ByteArray): String? {
        val formato = formatoLaŭEnhavo(bitokoj) ?: return null
        // Kap-kontroloj ANTAŬ la plena malkodado: la ImageReader legas
        // larĝon, alton kaj kadro-nombron el la metadatumoj — neniuj
        // pikseloj estas malkodataj ĝis la fina validigo sube.
        val enigo = ImageIO.createImageInputStream(ByteArrayInputStream(bitokoj))
        val leganto = try {
            val legantoj = ImageIO.getImageReaders(enigo)
            if (!legantoj.hasNext()) {
                enigo.close()
                return null
            }
            val l = legantoj.next()
            l.input = enigo
            l
        } catch (e: Exception) {
            // A8 + nelegebla kapo ne estas akceptebla bildo
            logw("Emblemoj", "ne eblis legi la bildan kapon — rifuzata", e)
            enigo.close()
            return null
        }
        try {
            val larĝo = leganto.getWidth(0).toLong()
            val alto = leganto.getHeight(0).toLong()
            if (larĝo <= 0 || alto <= 0 || larĝo * alto > MAKS_BILDAJ_PIKSELOJ) {
                logw("Emblemoj", "bildo rifuzata pro dimensio $larĝo×$alto (limo: $MAKS_BILDAJ_PIKSELOJ pikseloj)")
                return null
            }
            // Kadro-nombro (CWE-409, dua vojo): animacia GIF kun multegaj kadroj
            val kadroj = try {
                leganto.getNumImages(true).toLong()
            } catch (e: Exception) {
                // iuj legantoj ne subtenas la nombradon — atendata, ne eraro
                logd("Emblemoj", "kadro-nombro ne legebla per ${leganto::class.simpleName} — preterlasata: ${e::class.simpleName}: ${e.message}")
                null
            }
            if (kadroj != null && kadroj > MAKS_BILDAJ_KADROJ) {
                logw("Emblemoj", "bildo rifuzata pro kadro-nombro $kadroj (limo: $MAKS_BILDAJ_KADROJ)")
                return null
            }
        } catch (e: Exception) {
            // A8: neniam silente engluti — kaj nelegebla kapo ne estas akceptebla bildo
            logw("Emblemoj", "ne eblis legi la bildan kap-dimensionon — rifuzata", e)
            return null
        } finally {
            // purigu la risurcojn en ĈIU vojo — ankaŭ frua rifuzo/escepto
            leganto.dispose()
            enigo.close()
        }
        return try {
            if (ImageIO.read(ByteArrayInputStream(bitokoj)) != null) formato else null
        } catch (e: Exception) {
            // A8: neniam silente engluti
            logw("Emblemoj", "emblemo aspektas kiel $formato sed ne malkodeblas", e)
            null
        }
    }

    /**
     * Anstataŭigas la emblemo-URL-OJN en la publikigota konfiguro-teksto laŭ
     * [mapigo] (malnova URL → nova arkiva URL). La anstataŭigo estas laŭlitera
     * (ne regex) kaj trafas ĉiun aperon de la ekzakta valoro en citiloj; ŝlosiloj,
     * kiuj ne elŝutiĝis sukcese, restas ĉe la originala URL.
     */
    fun resskribuEmblemojn(konfiguroTeksto: String, mapigo: Map<String, String>): String {
        var rezulto = konfiguroTeksto
        for ((malnova, nova) in mapigo) {
            rezulto = rezulto.replace("\"$malnova\"", "\"$nova\"")
        }
        return rezulto
    }
}
