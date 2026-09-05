package dk.nordfalk.esperanto

/**
 * Helpaj protokolaj funkcioj kun escepto-parametro.
 *
 * Cxiu try-catch en la apo devas uzi cxi tiujn — neniam engluti escepton silente.
 *:La stacktrace estas protokolita cxe la erara nivelo.
 */
fun loge(tag: String, msg: String, e: Throwable) {
    loge(tag, "$msg: ${e::class.simpleName}: ${e.message}")
    loge(tag, e.stackTraceToString())
}

fun logw(tag: String, msg: String, e: Throwable) {
    logw(tag, "$msg: ${e::class.simpleName}: ${e.message}")
    logw(tag, e.stackTraceToString())
}
