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
     * Dosiernoma sufikso el la `Content-Type` de la respondo. Se nekonata, nur
     * konataj BILD-sufiksoj en la URL akceptiĝas (sufikso kiel `.php` aǔ `asp`
     * ne rajtas iĝi dosiernomo — Apache mem decidus laŭ ĝi la MIME-specon de la
     * servata dosiero); `jpg` kiel lasta rezervo (bildoj speguliĝas 1:1 — la
     * sufikso nur nomas la dosieron, ĝi ne transformas la enhavon).
     */
    fun sufikso(contentType: String?, url: String): String {
        val elTipo = when (contentType?.substringBefore(';')?.trim()?.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/svg+xml" -> "svg"
            else -> null
        }
        if (elTipo != null) return elTipo
        val elUrl = url.substringBeforeLast('?').substringAfterLast('.').lowercase()
        return if (elUrl in setOf("jpg", "jpeg", "png", "gif", "webp", "svg")) elUrl else "jpg"
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
