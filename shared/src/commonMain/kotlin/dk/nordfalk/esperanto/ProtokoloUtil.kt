package dk.nordfalk.esperanto

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlin.concurrent.Volatile

/**
 * Protokolaj funkcioj por la tuta apo.
 *
 * Platform-specifa eliĝo estas farata per platformLog* (expect/actual).
 *
 * Konvencio: [Komponanto] mesaĝo
 * Niveloj: d = debug, i = info, g = grava informo, w = averto, e = eraro
 */
@Volatile
var protokoloDetala: Boolean = false

fun logd(tag: String, msg: String) {
    platformLogd(tag, msg)
}

fun logi(tag: String, msg: String) {
    platformLogi(tag, msg)
    if (protokoloDetala) Sentry.logger.info("[$tag] $msg")
}

/**
 * Grava evento — ĈIAM sendata al Sentry, sendepende de [protokoloDetala]:
 * malfermo de la apo, ekludo de elsendo (kun kanalo), vekiĝo de alarmo.
 * Uzu malmulte — ĉiu voko kalkuliĝas kontraŭ la Sentry-kvoto.
 */
fun logGrava(tag: String, msg: String) {
    platformLogi(tag, msg)
    Sentry.logger.info("[$tag] $msg")
}

fun logw(tag: String, msg: String) {
    platformLogw(tag, msg)
    Sentry.logger.warn("[$tag] $msg")
}

fun loge(tag: String, msg: String) {
    platformLoge(tag, msg)
    Sentry.logger.error("[$tag] $msg")
}

/**
 * Helpaj protokolaj funkcioj kun escepto-parametro.
 *
 * Ĉiu try-catch en la apo devas uzi ĉi tiujn — neniam engluti escepton silente.
 * La stacktrace estas protokolita ĉe la erara nivelo.
 * Eraroj estas ankaŭ kaptitaj de Sentry por malproksima monitorado.
 *
 * Uzas platformLog* rekte (ne log*) por eviti duoblajn breadcrumb-ojn.
 */
fun loge(tag: String, msg: String, e: Throwable) {
    val plenaMsg = "$msg: ${e::class.simpleName}: ${e.message}"
    platformLoge(tag, plenaMsg)
    platformLoge(tag, e.stackTraceToString())
    Sentry.captureException(e)
}

/**
 * Averto kun escepto — same kiel [logw]: la escepto-resumo (tipo + mesaĝo) kaj
 * la staktraco iras al Sentry kiel avertoj (Sentry Logs — neniam error-evento,
 * do neniam konsumas la erarojn-kvoton).
 * Atendataj fiaskoj (reto, ludado, elŝuto, serĉo) uzu ĉi tion.
 */
fun logw(tag: String, msg: String, e: Throwable) {
    val plenaMsg = "$msg: ${e::class.simpleName}: ${e.message}"
    platformLogw(tag, plenaMsg)
    platformLogw(tag, e.stackTraceToString())
    Sentry.logger.warn("[$tag] $plenaMsg")
    Sentry.logger.warn("[$tag] "+e.stackTraceToString())
}
