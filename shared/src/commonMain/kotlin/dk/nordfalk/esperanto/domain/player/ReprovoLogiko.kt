package dk.nordfalk.esperanto.domain.player

/**
 * Eksponenta reprovo post ludo-eraro (ekz. reto perdiĝis dum ludado).
 *
 * Inspirita de la malnova `Afspiller.onError` (ĝis 10 provoj kun duobliĝanta atendo),
 * sed kun pli longaj atendoj: la malnova atendis entute nur ~10s, kio ofte ne sufiĉas
 * por ke poŝtelefona reto revenu. Ĉi tie: 1s, 2s, 4s … maks 30s — entute ~3 minutoj.
 */
object ReprovoLogiko {
    const val MAKS_PROVOJ = 10
    private const val KOMENCA_ATENDO_MS = 1_000L
    private const val MAKS_ATENDO_MS = 30_000L

    /**
     * @param provo la numero de la reprovo (1 = unua reprovo)
     * @return atendo en ms antaŭ tiu provo, aŭ null se oni rezignu
     */
    fun atendoMs(provo: Int): Long? {
        if (provo < 1 || provo > MAKS_PROVOJ) return null
        return (KOMENCA_ATENDO_MS shl (provo - 1).coerceAtMost(15)).coerceAtMost(MAKS_ATENDO_MS)
    }
}
