package dk.nordfalk.esperanto

actual fun logi(tag: String, msg: String) {
    println("[$tag] $msg")
}

actual fun logd(tag: String, msg: String) {
    println("[$tag] $msg")
}

actual fun logw(tag: String, msg: String) {
    println("[$tag] WARN $msg")
}

actual fun loge(tag: String, msg: String) {
    println("[$tag] ERROR $msg")
}
