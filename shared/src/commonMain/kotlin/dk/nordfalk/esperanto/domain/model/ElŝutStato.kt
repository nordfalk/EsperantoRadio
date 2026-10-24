package dk.nordfalk.esperanto.domain.model

/**
 * Stato de elŝuto.
 */
sealed interface ElŝutStato {
    data object NeElŝutita : ElŝutStato
    data class Elŝutanta(val progreso: Float, val elŝutitajBitokoj: Long, val totalajBitokoj: Long) : ElŝutStato
    data object Preta : ElŝutStato
    data class Eraro(val mesaĝo: String) : ElŝutStato
    data object Paŭzita : ElŝutStato
}

/**
 * Informo pri elŝutita elsendo — por la elŝutitaj-listo.
 *
 * @param dosierGrando grandeco de la dosiero en bitokoj (0 se nekonata)
 */
data class ElŝutitaElsendo(
    val elsendo: Elsendo,
    val dosieroVojo: String,
    val stato: ElŝutStato,
    val dosierGrando: Long = 0,
)
