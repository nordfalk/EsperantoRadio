package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.data.config.appContext
import io.ktor.client.HttpClient
import java.io.File

actual fun kreuElshutDeponejo(httpKliento: HttpClient): ElshutDeponejo {
    // getExternalFilesDir povas redoni null (ekstera stokado ne muntita) — File(null, …)
    // fariĝus RELATIVA vojo en la proceza laboratorio (FAROTA G12). Retrofalas al la
    // interna dosierujo (same app-privata, neniu permeso bezonata).
    val ekstera = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_PODCASTS)
    val hejjo = if (ekstera != null) {
        File(ekstera, "EsperantoRadio")
    } else {
        File(appContext.filesDir, "EsperantoRadio")
    }
    return KtorElshutDeponejo(httpKliento) { hejjo }
}
