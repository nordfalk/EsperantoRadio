package dk.nordfalk.esperanto

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

/**
 * Protokolaj funkcioj por la tuta apo.
 *
 * Cxiu logi/logd/logw/loge voko aldonas Sentry-breadcrumb-on, tiel ke
 * cxiuj protokolaj mesagxoj estas videblaj en Sentry antaux eraroj.
 * Platform-specifa eligxo estas farata per platformLog* (expect/actual).
 *
 * Konvencio: [Komponanto] mesagxo
 * Niveloj: d = debug, i = info, w = warning, e = error
 */
fun logd(tag: String, msg: String) {
    platformLogd(tag, msg)
    /*
    Sentry.addBreadcrumb(Breadcrumb().apply {
        level = SentryLevel.DEBUG
        message = "[$tag] $msg"
        category = tag
    })*/
}

fun logi(tag: String, msg: String) {
    platformLogi(tag, msg)
    Sentry.addBreadcrumb(Breadcrumb().apply {
        level = SentryLevel.INFO
        message = "[$tag] $msg"
        category = tag
    })
    Sentry.captureMessage("I [$tag] $msg")
}

fun logw(tag: String, msg: String) {
    platformLogw(tag, msg)
    Sentry.addBreadcrumb(Breadcrumb().apply {
        level = SentryLevel.WARNING
        message = "[$tag] $msg"
        category = tag
    })
    Sentry.captureMessage("W [$tag] $msg")
}

fun loge(tag: String, msg: String) {
    platformLoge(tag, msg)
    Sentry.addBreadcrumb(Breadcrumb().apply {
        level = SentryLevel.ERROR
        message = "[$tag] $msg"
        category = tag
    })
    Sentry.captureMessage("E [$tag] $msg")
}

/**
 * Helpaj protokolaj funkcioj kun escepto-parametro.
 *
 * Cxiu try-catch en la apo devas uzi cxi tiujn — neniam engluti escepton silente.
 * La stacktrace estas protokolita cxe la erara nivelo.
 * Eraroj estas ankaux kaptitaj de Sentry por malproksima monitorado.
 *
 * Uzas platformLog* rekte (ne log*) por eviti duoblajn breadcrumb-ojn.
 */
fun loge(tag: String, msg: String, e: Throwable) {
    val plenaMsg = "$msg: ${e::class.simpleName}: ${e.message}"
    platformLoge(tag, plenaMsg)
    platformLoge(tag, e.stackTraceToString())
    Sentry.addBreadcrumb(Breadcrumb().apply {
        level = SentryLevel.ERROR
        message = "[$tag] $plenaMsg"
        category = tag
    })
    Sentry.captureException(e)
}

fun logw(tag: String, msg: String, e: Throwable) {
    val plenaMsg = "$msg: ${e::class.simpleName}: ${e.message}"
    platformLogw(tag, plenaMsg)
    platformLogw(tag, e.stackTraceToString())
    Sentry.addBreadcrumb(Breadcrumb().apply {
        level = SentryLevel.WARNING
        message = "[$tag] $plenaMsg"
        category = tag
    })
}
