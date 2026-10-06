package dk.nordfalk.esperanto.android

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Klasifiko de ludil-eraroj ([estasReproveblaEraro]): HTTP 5xx estas pasema
 * (archive.org-Edge-nodoj intermite redonas 500 por sanaj dosieroj — reprovo
 * kutime sukcesas), HTTP 4xx kaj format-eraroj restas daŭraj (tuj saltas al
 * la sekva elsendo).
 *
 * Rulu: ./gradlew :androidApp:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=dk.nordfalk.esperanto.android.EraroKlasifikoTest
 */
@RunWith(AndroidJUnit4::class)
class EraroKlasifikoTest {

    private fun httpEraro(statuso: Int): PlaybackException {
        val kauxzo = HttpDataSource.InvalidResponseCodeException(
            statuso,
            "HTTP $statuso",
            null,
            emptyMap(),
            DataSpec(Uri.EMPTY),
            ByteArray(0),
        )
        return PlaybackException("HTTP $statuso", kauxzo, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
    }

    @Test
    fun http500EstasReprovebla() {
        assertTrue("Servileraro 500 devas esti reprovebla", estasReproveblaEraro(httpEraro(500)))
    }

    @Test
    fun http503EstasReprovebla() {
        assertTrue("Servileraro 503 devas esti reprovebla", estasReproveblaEraro(httpEraro(503)))
    }

    @Test
    fun http404RestasDauxra() {
        // 404 = la dosiero vere mankas — tuj saltu al la sekva elsendo, ne perdu tempon per reprovoj
        assertFalse("HTTP 404 devas esti daŭra (ne reprovebla)", estasReproveblaEraro(httpEraro(404)))
    }

    @Test
    fun retoEraroRestasReprovebla() {
        val error = PlaybackException(
            "Konekto malsukcesis",
            java.io.IOException("Connection reset"),
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        )
        assertTrue("Ret-eraro devas resti reprovebla", estasReproveblaEraro(error))
    }

    @Test
    fun formatEraroRestasDauxra() {
        val error = PlaybackException(
            "Malkodilo",
            java.io.IOException("Bad audio"),
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        )
        assertFalse("Format-eraro devas resti daŭra", estasReproveblaEraro(error))
    }

    @Test
    fun http500NesteEnKaŭzĈenoEstasReprovebla() {
        // ExoPlayer envolvas la datumfontan eraron en plurajn nivelojn
        val intern = httpEraro(500)
        val volvita = PlaybackException("Envolvita", intern, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        assertTrue("5xx en nia kaŭz-ĉeno devas esti trovita", estasReproveblaEraro(volvita))
    }
}
