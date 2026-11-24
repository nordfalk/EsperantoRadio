package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.repository.SettingsKeys

/**
 * Legas la uzanto-decidon pri erar-raportado al Sentry (PR #148/C50):
 * eksplicita agordo, aŭ — se la uzanto ankoraŭ neniam decidis — la
 * instalaĵo-specifa defaŭlto ([AppStato.erarRaportadoDefaŭlta]):
 * ŝaltita nur por instalaĵoj el KONATAJ ne-F-Droid-kanaloj; ĉio alia
 * (F-Droid, nekonata instala fonto) estas opt-in.
 * Por la platforma enirpunkto, kiu devas decidi ANTAŬ la Sentry-inicialigo —
 * en UI-kodo iru tra AgordojDeponejo.
 */
fun leguErarRaportadanKonsenton(): Boolean =
    kreuSettings().getBoolean(SettingsKeys.ERAR_RAPORTADO, AppStato.erarRaportadoDefaŭlta)

/**
 * Instalantoj, pri kiuj ni CERTAS ke ili ne estas F-Droid (kanaloj de ni):
 * - `com.android.vending` — Google Play
 * - `com.android.packageinstaller` — la sistema surŝutilo (adb, dosier-
 *   administrilo): neniam F-Droid
 * Estonte: Aptoide ktp. aldoniĝu ĉi tie tuj kiam la kanalo ekzistas.
 */
private val KONATAJ_NE_FDROID_INSTALANTOJ = setOf(
    "com.android.vending",
    "com.android.packageinstaller",
)

/**
 * Defaŭlto de la erar-raportado laŭ la INSTAL-FONTO de la APK (2026-11-24;
 * anstataŭigas la subskriban kontrolon de PR #148 — decido de la posedanto:
 * **nur klare konataj ne-F-Droid-kanaloj estas opt-out; F-Droid kaj ĉio
 * nekonata estas opt-in**): per reprodukteblaj konstruoj (RB) F-Droid
 * disdonas APK-on subskribitan per NIA atestilo — la subskribo do ne plu
 * distingas la kanalojn (trovo de la aŭtomata revizio en PR #200).
 *
 * - `org.fdroid.*` (la F-Droid-kliento kaj la privilegiita kromaĵo) →
 *   **opt-in**: defaŭlte malŝaltita, unufoja konsento-demando ĉe la unua
 *   starto (alie F-Droid atribuus la kontraŭecon „Tracking").
 * - Nekonata instala fonto (aliulo, anstataŭigita kliento, malkaptebla
 *   lego, `null`) → same **opt-in** — la privatece konservema direkto.
 * - [KONATAJ_NE_FDROID_INSTALANTOJ] → **defaŭlte ŝaltita** (opt-out;
 *   malŝaltebla en Agordoj → Privateco).
 */
fun erarRaportadaDefaŭltoLaŭInstalanto(instalanto: String?): Boolean =
    instalanto in KONATAJ_NE_FDROID_INSTALANTOJ
