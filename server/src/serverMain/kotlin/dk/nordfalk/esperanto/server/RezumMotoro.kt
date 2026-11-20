package dk.nordfalk.esperanto.server

/**
 * Generas mallongan rezultoon (4–9 linioj) el priskribo aŭ transskribo.
 *
 * Fazo 1: ekstrakta rezumo (determinisma, testebla, sen ekstera LLM).
 * Fazo 2 (poste): eta LLM per HTTP — la interfaco jam permesas tion.
 */
interface RezumMotoro {
    fun rezumu(teksto: String, titolo: String): String
}

/** Apendita al ĉiu AI-generita rezulto — travidebleco pri la fonto. */
const val AI_AVERTO = "Rezumo farita de la sondosiero per AI. Eraroj povas okazi"

/**
 * Ekstrakta rezumanto: prenas la unuajn frazojn ĝis 4–9 linioj (~90 signoj po linio),
 * evitante tro mallongajn frazojn komence. Se la teksto mem estas mallonga, redonas ĝin.
 */
class EkstraktaRezumMotoro : RezumMotoro {
    override fun rezumu(teksto: String, titolo: String): String {
        val plata = teksto.replace(Regex("\\s+"), " ").trim()
        if (plata.isEmpty()) return AI_AVERTO
        // frazoj: fina punkto sekvata de spaceto kaj majusklo, aŭ fino de teksto
        val frazoj = plata.split(Regex("(?<=[.!?])\\s+(?=[A-ZĜĈŜĴŬ])")).map { it.trim() }.filter { it.isNotEmpty() }
        if (frazoj.size >= 4) {
            // sufiĉe da frazoj: elektu la unuajn ĝis 4–9 linioj (~90 signoj po linio)
            val elektitaj = mutableListOf<String>()
            var linioj = 0
            for (f in frazoj) {
                elektitaj.add(f)
                linioj += (f.length + 89) / 90
                if (linioj >= 4 && elektitaj.size >= 4) break
                if (linioj >= 9) break
            }
            return elektitaj.joinToString("\n") + "\n\n" + AI_AVERTO
        }
        // Malmulte da frazoj (ekz. STT-eligo sen punktigo): tranĉu la tekston laŭ vortoj
        // en linioj de ~90 signoj kaj prenu 4–6 liniojn.
        val vortoj = plata.split(' ')
        val linioj = mutableListOf<String>()
        var nuna = StringBuilder()
        for (v in vortoj) {
            if (nuna.isNotEmpty() && nuna.length + v.length + 1 > 90) {
                linioj.add(nuna.toString())
                nuna = StringBuilder()
                if (linioj.size >= 6) break
            }
            if (nuna.isNotEmpty()) nuna.append(' ')
            nuna.append(v)
        }
        if (nuna.isNotEmpty() && linioj.size < 6) linioj.add(nuna.toString())
        val elektitaj = if (linioj.size >= 4) linioj else listOf(plata)
        return elektitaj.joinToString("\n") + "\n\n" + AI_AVERTO
    }
}
