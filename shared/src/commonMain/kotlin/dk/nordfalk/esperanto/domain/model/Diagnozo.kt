package dk.nordfalk.esperanto.domain.model

/**
 * Rezulto de diagnoza kontrolo — problemo aŭ averto pri la aparato/agordoj.
 *
 * La celo: frue detekti agordojn kiuj malhelpas fona ludado kaj aŭtomatan
 * daŭrigon (ekz. Samsung MARs / bateri-optimumado, blokita DNS, mankantaj permesoj).
 */
data class DiagnozoRezulto(
    val tipo: DiagnozoTipo,
    val titolo: String,
    val priskribo: String,
    val severeco: Severeco,
    val ago: DiagnozoAgo? = null,
)

/** Tipo de la problemo — uzata por ikonoj kaj intencoj. */
enum class DiagnozoTipo {
    /** Bateri-optimumado (Samsung MARs, Android Doze) limigas la aplikon en fono. */
    BATERIO_OPTIMIZATION,

    /** Sciig-permeso mankas — la media sciigo kaj sciigoj pri novaj elsendoj ne aperas. */
    NOTIFICATIONS,

    /** Ekzaktaj alarmoj ne permesitaj (Android 12+) — alarmoj povas esti prokrastitaj. */
    EXACT_ALARMS,

    /** DNS-solvado malsukcesis — ofte pro privata DNS (DNS-over-TLS) aŭ VPN. */
    DNS,

    /** Ĝenerala reta problemo (ne povas konekti al la servilo). */
    RETO,
}

/** Graveco de la problemo — regas la koloron kaj ikonon en la UI. */
enum class Severeco {
    /** Nur informa — ne blokas la ĉefan funkcion. */
    INFO,

    /** Averto — povas kaŭzi problemojn en iuj kazoj. */
    AVERTO,

    /** Ero — rekte kaŭzas ke fona ludado / aŭtoludo ĉesas. */
    ERARO,
}

/**
 * Proponita ago por ripari la problemon — ekz. "Malfermi bateriajn agordojn".
 *
 * La kampo [intenco] estas platform-specifa identigilo:
 * - Android: `"battery"`, `"notifications"`, `"exact_alarms"`, `"wireless"`
 * - Aliaj platformoj: ne uzata (ago == null)
 */
data class DiagnozoAgo(
    val etikedo: String,
    val intenco: String,
)
