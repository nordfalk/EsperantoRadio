package dk.nordfalk.esperanto

actual fun platformLogi(tag: String, msg: String) {
    android.util.Log.i(tag, msg)
}

actual fun platformLogd(tag: String, msg: String) {
    android.util.Log.d(tag, msg)
}

actual fun platformLogw(tag: String, msg: String) {
    android.util.Log.w(tag, msg)
}

actual fun platformLoge(tag: String, msg: String) {
    android.util.Log.e(tag, msg)
}
