package dk.nordfalk.esperanto.domain.model

/**
 * Stato de elŝuto.
 */
sealed interface ElshutStato {
    data object NeElshutita : ElshutStato
    data class Elshutanta(val progreso: Float, val elshutitajBitokoj: Long, val totalajBitokoj: Long) : ElshutStato
    data object Preta : ElshutStato
    data class Eraro(val mesagho: String) : ElshutStato
    data object Pauxzita : ElshutStato
}

/**
 * Informo pri elŝutita elsendo — por la elŝutitaj-listo.
 *
 * @param dosierGrando grandeco de la dosiero en bitokoj (0 se nekonata)
 */
data class ElshutitaElsendo(
    val elsendo: Elsendo,
    val dosieroVojo: String,
    val stato: ElshutStato,
    val dosierGrando: Long = 0,
)
