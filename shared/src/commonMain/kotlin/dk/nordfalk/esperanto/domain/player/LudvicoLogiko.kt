package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudataElsendo

/**
 * Pura decidlogiko por aŭtomata sekva-ludado.
 *
 * Kiam elsendo finiĝas (naturfino), LudvicoLogiko decidas kian elsendon ludi
 * sekve, laŭ tri prioritatoj:
 *
 * 1. **Sekva elsendo de la sama kanalo** — la elsendo kiu sekvas la ĵus
 *    finitan en la kanala elsendolisto (indexo+1), se ĝi ne estas jam finita.
 *
 * 2. **Elsendo de ŝatata kanalo, ne jam finita** — la plej freŝa nefinita
 *    elsendo el iu el la plejŝatataj kanaloj.
 *
 * 3. **Plej freŝa elsendo, ankoraŭ ne ludata** — la plej nova elsendo el
 *    ĉiuj kanaloj, kiu neniam estis ludita (neniu LudataElsendo ekzistas).
 *
 * Se neniu kandidato troviĝas, liveras null (haltigu la ludadon).
 *
 * Ĉi tiu logiko estas pura — neniuj flankaj efikoj, neniuj korutinoj.
 * Tute testebla per unuoj-testoj.
 */
object LudvicoLogiko {

    /**
     * @param nunaElsendo la elsendo kiu ĵus finiĝis (aŭ null)
     * @param samkanalajElsendoj elsendoj de la sama kanalo, ordigitaj plej-freŝe-unue
     * @param cxiujElsendoj ĉiuj ŝargitaj elsendoj (por prioritatoj 2 kaj 3)
     * @param plejsatatajKanaloj aro de kanal-slugs kiuj estas plejŝatataj
     * @param ludatoj mapo elsendoId → LudataElsendo (ludstatuso)
     * @return la sekva elsendo ludi, aŭ null se neniu kandidato
     */
    fun deciduSekvan(
        nunaElsendo: Elsendo?,
        samkanalajElsendoj: List<Elsendo>,
        cxiujElsendoj: List<Elsendo>,
        plejsatatajKanaloj: Set<String>,
        ludatoj: Map<String, LudataElsendo>,
    ): Elsendo? {
        // Priority 1: sekva elsendo de la sama kanalo
        val sekvaSamkanala = trovSekvanSamkanalan(nunaElsendo, samkanalajElsendoj, ludatoj)
        if (sekvaSamkanala != null) return sekvaSamkanala

        val nunaId = nunaElsendo?.id

        // Priority 2: nefinita elsendo de ŝatata kanalo
        val elSxatataj = trovNefinitanElSxatataj(cxiujElsendoj, plejsatatajKanaloj, ludatoj, nunaId)
        if (elSxatataj != null) return elSxatataj

        // Priority 3: plej freŝa elsendo ankoraŭ ne ludata
        return trovPlejFresxanNeludatan(cxiujElsendoj, ludatoj, nunaId)
    }

    /**
     * Priority 1: Trovas la sekvan elsendon en la kanala listo.
     *
     * La listo estas ordigita plej-freŝe-unue (RSS-normo). La "sekva" elsendo
     * estas tiu ĉe indexo+1 post la nuna (do pli malnova).
     * Saltas elsendojn kiuj estas jam finitaj.
     */
    fun trovSekvanSamkanalan(
        nunaElsendo: Elsendo?,
        samkanalajElsendoj: List<Elsendo>,
        ludatoj: Map<String, LudataElsendo>,
    ): Elsendo? {
        if (nunaElsendo == null) return null
        val indekso = samkanalajElsendoj.indexOfFirst { it.id == nunaElsendo.id }
        if (indekso < 0 || indekso + 1 >= samkanalajElsendoj.size) return null

        // Serĉu ekde la sekva indexo, saltante finitajn elsendojn
        for (i in indekso + 1 until samkanalajElsendoj.size) {
            val kandidato = samkanalajElsendoj[i]
            val ludato = ludatoj[kandidato.id]
            if (ludato?.finita != true) return kandidato
        }
        return null
    }

    /**
     * Priority 2: Trovas la plej freŝan nefinitan elsendon el ŝatataj kanaloj.
     *
     * "Nefinita" signifas: aŭ neniam ludita, aŭ ludita sed ne tute finita.
     */
    fun trovNefinitanElSxatataj(
        cxiujElsendoj: List<Elsendo>,
        plejsatatajKanaloj: Set<String>,
        ludatoj: Map<String, LudataElsendo>,
        nunaElsendoId: String? = null,
    ): Elsendo? {
        return cxiujElsendoj
            .filter { it.kanaloSlug in plejsatatajKanaloj }
            .filter { it.id != nunaElsendoId }
            .filter { ludatoj[it.id]?.finita != true }
            .maxByOrNull { it.dato }
    }

    /**
     * Priority 3: Trovas la plej freŝan elsendon kiu neniam estis ludita
     * (neniu LudataElsendo ekzistas por ĝi).
     */
    fun trovPlejFresxanNeludatan(
        cxiujElsendoj: List<Elsendo>,
        ludatoj: Map<String, LudataElsendo>,
        nunaElsendoId: String? = null,
    ): Elsendo? {
        return cxiujElsendoj
            .filter { it.id != nunaElsendoId }
            .filter { it.id !in ludatoj }
            .maxByOrNull { it.dato }
    }
}
