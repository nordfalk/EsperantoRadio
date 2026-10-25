package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import dk.nordfalk.esperanto.data.config.appContext
import io.ktor.client.HttpClient
import java.io.File

actual fun kreuElŝutDeponejo(httpKliento: HttpClient): ElŝutDeponejo {
    // getExternalFilesDir povas redoni null (ekstera stokado ne muntita) — File(null, …)
    // fariĝus RELATIVA vojo en la proceza laboratorio (PR #89). Retrofalas al la
    // interna dosierujo (same apo-privata, neniu permeso bezonata).
    val ekstera = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_PODCASTS)
    val hejjo = if (ekstera != null) {
        File(ekstera, "EsperantoRadio")
    } else {
        File(appContext.filesDir, "EsperantoRadio")
    }
    return KtorElŝutDeponejo(httpKliento) { hejjo }
}
