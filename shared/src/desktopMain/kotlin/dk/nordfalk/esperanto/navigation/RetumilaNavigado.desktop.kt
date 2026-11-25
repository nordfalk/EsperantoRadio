package dk.nordfalk.esperanto.navigation

/**
 * Desktop-aktualigo: la retumila integrigo ne koncernas fenestrajn apojn — no-op.
 */
internal actual object RetumilaNavigado {
    actual fun startu(onReen: () -> Unit, onReenProponata: () -> Boolean) = Unit
    actual fun stakŜanĝiĝis(novaProfundo: Int, titolo: String?) = Unit
}
