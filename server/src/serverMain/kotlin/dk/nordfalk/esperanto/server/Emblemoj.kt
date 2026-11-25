package dk.nordfalk.esperanto.server

import dk.nordfalk.esperanto.logw
import java.io.ByteArrayInputStream
import java.net.Inet6Address
import java.net.InetAddress
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
     * Kiel [formatoLaŭEnhavo], sed ankaŭ validigas ke la enhavo vere estas
     * malkodebla bildo (ImageIO-malkodado de la tuta datumo — ne nur la magiaj
     * signoj): trunkita, koruptita aŭ kaŝvestita enhavo kun ĝusta signaturo
     * estas rifuzata (→ null; la emblemo restas ĉe la originala URL, A4).
     */
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
     * Ĉu la adreso estas PUBLIKA — pli strikta ol la InetAddress- oportunaj
     * kontroloj (tiuj akceptas ekz. `fd00::1` (IPv6-ULA) kaj `100.64.0.1`
     * (CGNAT-IPv4)). Kovritaj: IPv4 0/8, 10/8, 127/8, 172.16/12, 192.168/16,
     * 169.254/16, 100.64/10, 198.18/15, 224/4 (multicast), 240/4 (rezervita);
     * IPv6 ::1, fe80::/10, fc00::/7 (ULA), ff00::/8, IPv4-mapeitaj (::ffff:),
     * 6to4 (2002::/16) kaj teredo (2001:0::/32) kun enkopigitaj privataj IPv4.
     * (SSRF-ŝirmo de la bildo-spegele, CWE-918, PR #214.)
     */
    fun ĉuPublikaAdreso(adreso: InetAddress): Boolean {
        val b = adreso.address
        if (adreso is Inet6Address) {
            // IPv4-mapeita adreso ::ffff:a.b.c.d → kontrolu kiel IPv4
            if (b.size == 16 && (0 until 10).all { b[it].toInt() == 0 } &&
                (b[10].toInt() and 0xFF) == 0xFF && (b[11].toInt() and 0xFF) == 0xFF
            ) return ipv4Publika(b, 12)
            val o0 = b[0].toInt() and 0xFF
            val o1 = b[1].toInt() and 0xFF
            return when {
                (o0 and 0xFE) == 0xFC -> false                       // fc00::/7 (ULA, fd00::1)
                o0 == 0xFE && (o1 and 0xC0) == 0x80 -> false          // fe80::/10 (link-local)
                o0 == 0xFF -> false                                   // ff00::/8 (multicast)
                o0 == 0x20 && o1 == 0x02 -> ipv4Publika(b, 2)          // 6to4 2002::/16
                o0 == 0x20 && o1 == 0x01 && b[2].toInt() == 0 && b[3].toInt() == 0 ->
                    ipv4Publika(b, 12)                                 // teredo 2001:0::/32
                else -> true
            }
        }
        return ipv4Publika(b)
    }

    /** IPv4-intervaloj, kiuj NE estas publike. [de] = komenco de la 4 adres-bitokoj. */
    private fun ipv4Publika(b: ByteArray, de: Int = 0): Boolean {
        val o0 = b[de].toInt() and 0xFF
        val o1 = b[de + 1].toInt() and 0xFF
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

    fun formatoSeLegebla(bitokoj: ByteArray): String? {
        val formato = formatoLaŭEnhavo(bitokoj) ?: return null
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
