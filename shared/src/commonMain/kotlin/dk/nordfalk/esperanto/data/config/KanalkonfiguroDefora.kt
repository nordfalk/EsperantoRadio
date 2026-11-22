package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.data.repository.foriguKaŝon
import dk.nordfalk.esperanto.data.repository.leguKaŝonJson
import dk.nordfalk.esperanto.data.repository.skribuKaŝonJson
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logw

/**
 * URL de la defore elŝutebla kanalkonfiguro — la purigita strikta JSON-kopio, kiun
 * la arkiv-servilo publikigas je ĉiu rulo (vidu la skill-on `servilo-arkivo`).
 *
 * Ĝi estas la sama enhavo kiel la paktita `esperantoradio_konfiguro.json`, sed
 * sen komentoj — [KanalAgordoLeganto] tamen traktas ambaŭ formojn.
 */
const val KANALKONFIGURO_URL = "https://javabog.dk/esperanto-radio/esperantoradio_konfiguro.json"

/**
 * Kaŝnomo de la persistigita defora konfiguro: dosiero en la diska kaŝmemoro
 * (`defora_kanalkonfiguro.json`). ATENTU: pluraj KB da teksto NE persistiĝu en
 * `Settings` — sur Desktop la java.util.prefs-realigo rifuzas valorojn super
 * ~6 KB («Value too long», malkovrite 2026-11-22 ĉe reala rulado).
 */
const val KANALKONFIGURO_KAŜNOMO = "defora_kanalkonfiguro"

/**
 * Elektas kaj legas la kanalkonfiguron uzotan ĉe starto, kiel la «grunddata»-fluo
 * de la malnova apo (`App.initData`): unue la persistigita (defore elŝutita)
 * versio el la diska kaŝmemoro, kun rezervo al la paktita risurco. Se la
 * persistigita teksto ne estas parsigebla (ekz. duone skribita dosiero), ĝi
 * forgesiĝas kaj la paktita uziĝas (regulo A4: unu eraro ne rajtas panei la apon).
 */
fun leguAgordonKunRezervo(
    leganto: KanalAgordoLeganto,
    paktitaTeksto: () -> String,
    leguKonservitan: () -> String? = { leguKaŝonJson(KANALKONFIGURO_KAŜNOMO) },
    foriguKonservitan: () -> Unit = { foriguKaŝon(KANALKONFIGURO_KAŜNOMO) },
): KanalAgordo {
    val konservita = try {
        leguKonservitan()
    } catch (e: Exception) {
        logw("Kanalkonfiguro", "Ne povis legi la konservitan kanalkonfiguron", e)
        null
    }
    if (konservita != null) {
        try {
            return leganto.legu(konservita)
        } catch (e: Exception) {
            // A8: protokolu kun stacktrace; forgesu la putritan dosieron — la defora
            // refreŝigo povas skribi novan, kaj la paktita validas ĝis tiam.
            loge("Kanalkonfiguro", "Konservita kanalkonfiguro ne estas parsigebla — uzas la paktitan kaj forgesas ĝin", e)
            try {
                foriguKonservitan()
            } catch (e2: Exception) {
                logw("Kanalkonfiguro", "Ne povis forigi la putritan kanalkonfiguron", e2)
            }
        }
    }
    return leganto.legu(paktitaTeksto())
}

/** Skribas la krudan tekston de (sukcese parsita) defora konfiguro en la kaŝmemoron. */
fun konservuKanalkonfiguron(teksto: String) {
    skribuKaŝonJson(KANALKONFIGURO_KAŜNOMO, teksto)
}
