package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.data.config.appContext
import io.ktor.client.HttpClient
import java.io.File

actual fun kreElshutDeponejo(httpKliento: HttpClient): ElshutDeponejo {
    val hejjo = File(appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_PODCASTS), "EsperantoRadio")
    return KtorElshutDeponejo(httpKliento) { hejjo }
}
