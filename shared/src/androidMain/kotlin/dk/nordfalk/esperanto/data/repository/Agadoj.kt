package dk.nordfalk.esperanto.data.repository

/**
 * Nomoj de komponantoj en la `androidApp`-modulo (C19).
 *
 * `shared` ne povas referenci la klasojn rekte (la modulo dependas en la inversan
 * direkton), do la nomoj estas ĉi tie en UNU sola loko — renomado de la klasoj
 * devas ĝisdatigi nur ĉi tiujn konstantojn (kontrolita per grep, ne per 4 disaj
 * lokoj kun silenta paneo de sciigoj kaj alarmoj).
 */
internal object Agadoj {
    /** La eksportita ĉefagado (lanĉero; ankaŭ celo de alarmo- kaj sciigo-intencoj). */
    const val CHEFA_AGADO = "dk.nordfalk.esperanto.android.MainActivity"

    /** La MediaSessionService kiu posedas la ExoPlayer (fona ludado). */
    const val LUDADO_SERVO = "dk.nordfalk.esperanto.android.EsperantoLudadoServo"
}
