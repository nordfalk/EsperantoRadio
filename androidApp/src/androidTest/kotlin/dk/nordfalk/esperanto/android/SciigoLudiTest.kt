package dk.nordfalk.esperanto.android

import android.content.Intent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dk.nordfalk.esperanto.data.repository.LudiElsendoReceivilo
import dk.nordfalk.esperanto.data.repository.NovajElsendojKontroloWorker
import dk.nordfalk.esperanto.domain.model.Sonfonto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La "Ludi"-butono en la sciigo pri novaj elsendoj sendas elsendaĵon al [LudiElsendoReceivilo],
 * kiu ludas per PROPRA MediaController. La apo devas tamen rekoni la elsendon (per la Sonfonto
 * en la MediaMetadata-ekstraĵoj) kaj montri ĝin en la mini-ludilbreto. La breto montras ĝuste
 * `ludilo.stato.nunaFonto` (kaŝita se null), do la testo kontrolas tiun staton.
 *
 * (Kontrolas la staton de la ludilo anstataŭ la UI — do ne bezonas la Compose-testregulon nek
 * GrantPermissionRule; vidu AndroidUiTest por UI-testoj.)
 *
 * Rulu: ./gradlew :androidApp:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=dk.nordfalk.esperanto.android.SciigoLudiTest
 */
@RunWith(AndroidJUnit4::class)
class SciigoLudiTest {

    @get:Rule
    val aktivecoRegulo = ActivityScenarioRule(MainActivity::class.java)

    private val kunteksto = InstrumentationRegistry.getInstrumentation().targetContext
    private val ludilo get() = ExoPlayerLudiloRegilo.akiru(kunteksto)

    @After
    fun haltu() = InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }

    /** Atendas ĝis [kondicxo] veras, maks [maksMs]. */
    private fun atendu(maksMs: Long, kondicxo: () -> Boolean) {
        val limo = System.currentTimeMillis() + maksMs
        while (!kondicxo()) {
            check(System.currentTimeMillis() < limo) { "Kondiĉo ne plenumita post $maksMs ms" }
            Thread.sleep(100)
        }
    }

    @Test
    fun ludiButonoEnSciigoMontrasElsendonEnMiniLudilbreto() {
        // Atendu la konekton de la ludilo al la servo, poste certigu ke nenio ludas
        Thread.sleep(2_000)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        atendu(5_000) { ludilo.stato.value.nunaFonto == null }

        // Sama elsendaĵo kiel la PendingIntent de la "Ludi"-ago en NovajElsendojKontroloWorker
        val intent = Intent(NovajElsendojKontroloWorker.ACTION_LUDI_ELSENDON)
            .setClass(kunteksto, LudiElsendoReceivilo::class.java)
            .putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_ID, "testo:sciigo-1")
            .putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_TITOLO, "Testa elsendo el sciigo")
            .putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_FLUO, "https://archive.org/download/skecxo_jarka/skecxo_jarka.mp3")
            .putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_DATO, "2026-09-23")
            .putExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_SLUG, "peranto")
            .putExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_NOMO, "Esperanta Retradio")
        kunteksto.sendBroadcast(intent)

        // La procez-nivela ludilo rekonas la elsendon komencitan de la alia MediaController
        atendu(15_000) { ludilo.stato.value.nunaFonto is Sonfonto.ElsendoFonto }
        val elsendo = (ludilo.stato.value.nunaFonto as Sonfonto.ElsendoFonto).elsendo
        assertEquals("testo:sciigo-1", elsendo.id)
        assertEquals("Testa elsendo el sciigo", elsendo.titolo)
        assertEquals("Esperanta Retradio", elsendo.kanaloNomo)
    }
}
