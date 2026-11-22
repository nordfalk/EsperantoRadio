package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.data.repository.foriguKaŝon
import dk.nordfalk.esperanto.data.repository.leguKaŝonJson
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logw

/**
 * Fora (servila) kanalkonfiguro. La apo elŝutas ĝin de la servilo kaj konservas
 * la krudan tekston kiel dosieron en la diska kaŝmemoro ([FORA_KONFIGURO_KAŜNOMO]);
 * vidu [dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl].
 *
 * La servilo publikigas ĝin kiel purigitan striktan JSON (sen komentoj) —
 * la leganto tamen akceptas ankaŭ la JSONC-varianton de la pakita dosiero.
 *
 * ATENTU: la teksto NE persistiĝu en `Settings` — sur Desktop la java.util.prefs-
 * realigo rifuzas valorojn super `MAX_VALUE_LENGTH` (8192 signoj) per
 * `IllegalArgumentException` (malkovrite 2026-11-22, PR #195/#196).
 */
const val FORA_KONFIGURO_URL = "https://javabog.dk/esperanto-radio/esperantoradio_konfiguro.json"

/** Kaŝnomo (dosiernomo en la diska kaŝmemoro) de la persistigita fora kanalkonfiguro. */
const val FORA_KONFIGURO_KAŜNOMO = "defora_kanalkonfiguro"

/**
 * Elektas kaj legas la kanalkonfiguron uzotan ĉe starto, kiel la «grunddata»-fluo
 * de la malnova apo (`App.initData`): unue la persistigitan (defore elŝutitan)
 * version el la diska kaŝmemoro, kun rezervo al la paktita risurco. Se la
 * persistigita teksto ne estas parsigebla (ekz. duone skribita dosiero), ĝi
 * forgesiĝas kaj la paktita uziĝas (A4: unu eraro ne rajtas panei la apon).
 *
 * La persistigo estas injektebla per lambdoj — en testoj neniu reala dosiero tuŝiĝas.
 */
fun leguAgordonKunRezervo(
    leganto: KanalAgordoLeganto,
    paktitaTeksto: () -> String,
    leguKonservitan: () -> String? = { leguKaŝonJson(FORA_KONFIGURO_KAŜNOMO) },
    foriguKonservitan: () -> Unit = { foriguKaŝon(FORA_KONFIGURO_KAŜNOMO) },
): KanalAgordo {
    val konservita = try {
        leguKonservitan()
    } catch (e: Exception) {
        logw("KanaloKonfiguroFonto", "Ne povis legi la konservitan kanalkonfiguron", e)
        null
    }
    if (!konservita.isNullOrBlank()) {
        try {
            return leganto.legu(konservita)
        } catch (e: Exception) {
            // A8: protokolu kun stacktrace; forgesu la putritan kaŝon — la defora
            // refreŝigo povas skribi novan, kaj la paktita validas ĝis tiam.
            loge("KanaloKonfiguroFonto", "Konservita kanalkonfiguro ne estas parsigebla — uzas la paktitan kaj forgesas ĝin", e)
            try {
                foriguKonservitan()
            } catch (e2: Exception) {
                logw("KanaloKonfiguroFonto", "Ne povis forigi la putritan kanalkonfiguron", e2)
            }
        }
    }
    return leganto.legu(paktitaTeksto())
}
