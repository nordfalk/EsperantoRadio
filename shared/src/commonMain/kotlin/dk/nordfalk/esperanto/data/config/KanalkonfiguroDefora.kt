package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.repository.leguKaŝonJson
import dk.nordfalk.esperanto.data.repository.leguKorponLimigite
import dk.nordfalk.esperanto.data.repository.foriguKaŝon
import dk.nordfalk.esperanto.data.repository.skribuKaŝonJson
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logw
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get

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
 * Tempolimo por la elŝutpeto de la konfiguro (malgranda dosiero, ne rajtas bloki).
 */
const val KANALKONFIGURO_PETO_TEMPOLIMITO_MS = 15_000L

/**
 * Maksimuma grandeco de la konfigur-respondo en bitokoj — la vera konfiguro estas
 * ~21 KB; 256 KB estas malproksima supra limo. `leguKorponLimigite` readas
 * pecete kaj ĵetas tuj ĉe transpaso (misdirektita respondo ne enmemorigiĝas).
 */
const val KANALKONFIGURO_MAKSUMA_GRANDECO_BITOKOJ: Long = 256L * 1024

/**
 * La produktada elŝutilo por la defora kanalkonfiguro: `expectSuccess` (erarpaĝo
 * kun HTTP 200 ne plu povas trudi HTML-on kiel konfiguron sen kontrolado — kaj
 * ĝi jam ne pasas la pars-provon), propra peto-tempolimo kaj grandec-limigita
 * korpo-legado. Injektata kiel lambdo en `KanaloDeponejoImpl` (testoj povas
 * anstataŭigi ĝin).
 */
fun kreuDeforanElŝutilon(httpKliento: HttpClient): suspend (url: String) -> String = { url ->
    httpKliento.get(url) {
        expectSuccess = true
        timeout { requestTimeoutMillis = KANALKONFIGURO_PETO_TEMPOLIMITO_MS }
    }.leguKorponLimigite(limo = KANALKONFIGURO_MAKSUMA_GRANDECO_BITOKOJ)
}

/**
 * Elektas kaj legas la kanalkonfiguron uzotan ĉe starto, kiel la «grunddata»-fluo
 * de la malnova apo (`App.initData`): unue la persistigita (defore elŝutita)
 * versio el la diska kaŝmemoro, kun rezervo al la paktita risurco. Se la
 * persistigita teksto ne estas parsigebla (ekz. duone skribita dosiero), ĝi
 * forgesiĝas kaj la paktita uziĝas (regulo A4: unu eraro ne rajtas panei la apon).
 *
 * En EVOLUA medio ([uzuDeforanKonfiguron] = falsa) neniom da tio okazas: la
 * PAKTITA konfiguro uziĝas ĉiam, kaj ankaŭ la defora refreŝigo ne ruliĝas
 * (vidu [KanaloDeponejoImpl.refreŝiguDefore]). Tiel novaj kanaloj el la
 * Git-branĉo testiĝas sen rsync al la servilo — la defora konfiguro venkus
 * la novan paktitan liston ĝis la sekva eldono.
 */
fun leguAgordonKunRezervo(
    leganto: KanalAgordoLeganto,
    paktitaTeksto: () -> String,
    leguKonservitan: () -> String? = { leguKaŝonJson(KANALKONFIGURO_KAŜNOMO) },
    foriguKonservitan: () -> Unit = { foriguKaŝon(KANALKONFIGURO_KAŜNOMO) },
    uzuDeforanKonfiguron: Boolean = !AppStato.estasEvoluaMedio,
): KanalAgordo {
    if (!uzuDeforanKonfiguron) {
        logd("Kanalkonfiguro", "Evolua medio — uzas nur la paktitan kanalkonfiguron")
        return leganto.legu(paktitaTeksto())
    }
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
