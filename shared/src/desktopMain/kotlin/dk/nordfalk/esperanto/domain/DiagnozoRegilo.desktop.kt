package dk.nordfalk.esperanto.domain

import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop-implemento de DiagnozoRegilo — neniuj platform-specifaj problemoj.
 */
actual class DiagnozoRegilo {
    private val _problemoj = MutableStateFlow<List<DiagnozoRezulto>>(emptyList())
    actual val problemoj: StateFlow<List<DiagnozoRezulto>> = _problemoj.asStateFlow()

    actual suspend fun kontroli(): List<DiagnozoRezulto> = emptyList()

    actual suspend fun kontroliRapide(): List<DiagnozoRezulto> = emptyList()
}

actual fun kreuDiagnozoRegilon(): DiagnozoRegilo = DiagnozoRegilo()

actual fun malfermiSistemAgordon(intenco: String) {
    // Desktop: neniu sistem-agordo malfermenda
}
