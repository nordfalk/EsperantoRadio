package dk.nordfalk.esperanto

actual fun platformLogi(tag: String, msg: String) {
    System.err.println("[$tag] $msg")
}

actual fun platformLogd(tag: String, msg: String) {
    System.err.println("[$tag] $msg")
}

actual fun platformLogw(tag: String, msg: String) {
    System.err.println("[$tag] WARN $msg")
}

actual fun platformLoge(tag: String, msg: String) {
    System.err.println("[$tag] ERROR $msg")
}
