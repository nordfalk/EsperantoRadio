package dk.nordfalk.esperanto.server

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
