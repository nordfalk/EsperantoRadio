package dk.nordfalk.esperanto.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Inkrementa stato de la arkiv-konstruilo (unu rulo = unu aktualigo). */
@Serializable
data class Stato(
    val lastaRulo: String = "",
    val kanaloj: Map<String, KanalStato> = emptyMap(),
)

@Serializable
data class KanalStato(
    val lastaSukceso: String = "",
    val nombroDaElsendoj: Int = 0,
    val lastaEraro: String = "",
)

object StatoLeganto {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun legu(dosiero: File): Stato =
        if (dosiero.isFile) runCatching { json.decodeFromString<Stato>(dosiero.readText()) }.getOrDefault(Stato())
        else Stato()

    fun skribu(dosiero: File, stato: Stato) {
        dosiero.parentFile.mkdirs()
        dosiero.writeText(json.encodeToString(stato))
    }
}
