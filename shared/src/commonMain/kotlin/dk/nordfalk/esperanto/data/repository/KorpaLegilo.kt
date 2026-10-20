package dk.nordfalk.esperanto.data.repository

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable

/**
 * Maksimuma grandeco de RSS-respondo-korpo en bajtoj (G45 / KMP-1H kaj KMP-1R).
 * Legitimaj fluoj estas maks. kelkcent KB (la plej granda konata fiksaĵo ~310 KB);
 * 1 MB estas sufiĉe malproksima supra limo por legitimaj fluoj, sed atentu
 * (FAROTA C49): kontrolu periode (ekz. ĉiusemajne) per Sentry ĉu reala fluo
 * ĵetas [RespondoTroGrandaException] — tiam oni devas levi la limon.
 * Misdirektita respondo (ekz. 60 MB MP3-eldonaĵo anstataŭ fluo — KMP-1H)
 * mortigis la apoton per `OutOfMemoryError` sur malaltklasaj aparatoj, ĉar
 * `bodyAsText()` enmemorigas la tutan korpon antaŭ kontroli ĝin.
 */
const val MAKSIMUMA_FLUA_GRANDECO_BAJTOJ: Long = 1L * 1024 * 1024

/**
 * Ĵetata kiam la respondo-korpo transpasas la permesatan limon — ĉu laŭ la
 * deklarita `Content-Length`, ĉu dum la flu-legado. Atendata fiasko (regulo 4):
 * kaptu per `logw`, ne `loge` — ĝi ne devas konsumi la Sentry-eraro-kvoton.
 */
class RespondoTroGrandaException(val grandeco: Long, val limo: Long) :
    Exception("Respondo-korpo ($grandeco bajtoj) transpasas la limon de $limo bajtoj")

/**
 * Legas la korpon de respondo kun grandec-limo (G45).
 *
 * Kontraŭe al `bodyAsText()` (kiu enmemorigas la tutan korpon senlime), tio:
 * 1. antaŭkontrolas `Content-Length` kaj fiaskas tuj se la servilo deklaras pli
 *    grandan korpon ol la limo (neniu bajto estas legata);
 * 2. legas la korpon pecete per `bodyAsChannel()` kaj ĉesas tuj post kiam la
 *    limo estas transpasita — ankaŭ kiam `Content-Length` mankas aŭ mensogas.
 *
 * La legita enhavo tamen devas esti validata kiel fluo antaŭ kaŝado
 * (`kontroluFluecon` en [ElsendoDeponejoImpl]) — grandeca limo ne anstataŭas
 * enhavan kontrolon.
 */
suspend fun HttpResponse.leguKorponLimigite(
    limo: Long = MAKSIMUMA_FLUA_GRANDECO_BAJTOJ,
): String {
    val deklarita = headers[HttpHeaders.ContentLength]?.toLongOrNull()
    if (deklarita != null && deklarita > limo) {
        throw RespondoTroGrandaException(deklarita, limo)
    }
    val kanalo = bodyAsChannel()
    val pecoj = mutableListOf<ByteArray>()
    val buf = ByteArray(64 * 1024)
    var totalo = 0L
    while (true) {
        val n = kanalo.readAvailable(buf)
        if (n <= 0) break
        totalo += n
        if (totalo > limo) throw RespondoTroGrandaException(totalo, limo)
        pecoj.add(buf.copyOf(n))
    }
    val ĉiuj = ByteArray(totalo.toInt())
    var pozicio = 0
    for (peco in pecoj) {
        peco.copyInto(ĉiuj, pozicio)
        pozicio += peco.size
    }
    return ĉiuj.decodeToString()
}
