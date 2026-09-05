package dk.nordfalk.esperanto

import platform.foundation.NSLog

actual fun logi(tag: String, msg: String) {
    NSLog("[$tag] $msg")
}

actual fun logd(tag: String, msg: String) {
    NSLog("[$tag] $msg")
}

actual fun logw(tag: String, msg: String) {
    NSLog("[$tag] WARN $msg")
}

actual fun loge(tag: String, msg: String) {
    NSLog("[$tag] ERROR $msg")
}
