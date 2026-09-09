package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.logi

actual fun ghisdatiguSciigSkedon(plejŝatatajKanaloj: Set<String>, sciigojSxaltitaj: Boolean) {
    if (sciigojSxaltitaj && plejŝatatajKanaloj.isNotEmpty()) {
        logi("SciigSkedo", "Ŝatataj kanaloj: ${plejŝatatajKanaloj.size}, sciigoj ŝaltitaj → skedas Worker-ojn")
        NovajElsendojSkedilo.skedu(appContext)
    } else {
        logi("SciigSkedo", "Neniuj ŝatataj kanaloj aŭ sciigoj malŝaltitaj → malplanas Worker-ojn")
        NovajElsendojSkedilo.malplani(appContext)
    }
}
