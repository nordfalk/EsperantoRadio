package dk.nordfalk.esperanto.navigation

/**
 * Android-aktualigo: la retumila integrigo ne koncernas denaskan apojn — no-op.
 */
internal actual object RetumilaNavigado {
    actual fun startu(onReen: () -> Unit) = Unit
    actual fun stakŜanĝiĝis(novaProfundo: Int, titolo: String?) = Unit
}
