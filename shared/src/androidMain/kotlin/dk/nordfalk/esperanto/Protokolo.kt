package dk.nordfalk.esperanto

actual fun logi(tag: String, msg: String) {
    android.util.Log.i(tag, msg)
}

actual fun logd(tag: String, msg: String) {
    android.util.Log.d(tag, msg)
}

actual fun logw(tag: String, msg: String) {
    android.util.Log.w(tag, msg)
}

actual fun loge(tag: String, msg: String) {
    android.util.Log.e(tag, msg)
}
