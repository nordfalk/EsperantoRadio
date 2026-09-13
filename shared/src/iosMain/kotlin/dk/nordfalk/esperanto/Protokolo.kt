package dk.nordfalk.esperanto

import platform.foundation.NSLog

actual fun platformLogi(tag: String, msg: String) {
    NSLog("[$tag] $msg")
}

actual fun platformLogd(tag: String, msg: String) {
    NSLog("[$tag] $msg")
}

actual fun platformLogw(tag: String, msg: String) {
    NSLog("[$tag] WARN $msg")
}

actual fun platformLoge(tag: String, msg: String) {
    NSLog("[$tag] ERROR $msg")
}
