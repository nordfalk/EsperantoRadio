package dk.nordfalk.esperanto.android

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dk.nordfalk.esperanto.data.config.IntentŜlosilo
import dk.nordfalk.esperanto.data.repository.LudiElsendoReceivilo
import dk.nordfalk.esperanto.data.repository.NovajElsendojKontroloWorker
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.ExoPlayerLudiloRegilo
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

    // Sciig-permeso antaŭa — alikaze la permes-dialogo blokas la Activity kaj
    // ActivityScenario.launch atendas RESUMED (G47-testo).
    @get:Rule(order = 0)
    val permesoj: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    @get:Rule(order = 1)
    val aktivecoRegulo = ActivityScenarioRule(MainActivity::class.java)

    private val kunteksto = InstrumentationRegistry.getInstrumentation().targetContext
    private val ludilo get() = ExoPlayerLudiloRegilo.akiru(kunteksto)

    @After
    fun haltu() = InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }

    /** Atendas ĝis [kondiĉo] veras, maks [maksMs]. */
    private fun atendu(maksMs: Long, kondiĉo: () -> Boolean) {
        val limo = System.currentTimeMillis() + maksMs
        while (!kondiĉo()) {
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
            .putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_FLUO, "https://archive.org/download/skeĉo_jarka/skeĉo_jarka.mp3")
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

    /** Nombro de logcat-linioj kun la markilo (la apo rajtas legi siajn proprajn logojn). */
    private fun logNombro(markilo: String): Int {
        val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d"))
        val teksto = p.inputStream.bufferedReader().readText()
        p.waitFor()
        return teksto.lines().count { markilo in it }
    }

    /**
     * G47: klako sur la sciigo (MALFERMI_ELSENDON) lanĉas MainActivity kun la intent;
     * ĉe rekreo de la Activity (ekz. ekranturno) la intent NE re-traktendas — alikaze
     * la elsendo restartus de pozicio 0 (Sentry 2026-10-30: "Ludado komencita de sciigo"
     * aperis plurfoje ene de kelkaj sekundoj dum rotacio).
     *
     * Aserto per logcat-nombro (ne per vera ludado — la emulilo ne ĉiam povas ŝargi la
     * fluon): post recreate() "Ludado komencita de sciigo" por nia elsendo aperas nur unufoje.
     */
    @Test
    fun rekreoNeRestartigasLudonDeSciigo() {
        val markilo = "testo:sciigo-rekreo"
        Thread.sleep(2_000)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        atendu(5_000) { ludilo.stato.value.nunaFonto == null }

        val intent = Intent(kunteksto, MainActivity::class.java).apply {
            action = NovajElsendojKontroloWorker.ACTION_MALFERMI_ELSENDON
            putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_ID, markilo)
            putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_TITOLO, "Testa elsendo rekreo")
            putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_FLUO, "https://archive.org/download/skeĉo_jarka/skeĉo_jarka.mp3")
            putExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_DATO, "2026-09-23")
            putExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_SLUG, "peranto")
            putExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_NOMO, "Esperanta Retradio")
        }
        IntentŜlosilo.aldonuNomon(intent)
        val scenaro = ActivityScenario.launch<MainActivity>(intent)
        try {
            // La unua traktado okazis: la intent estis fidinda kaj la ludado komencita
            atendu(15_000) { logNombro("Ludado komencita de sciigo: Testa elsendo rekreo") >= 1 }
            check(logNombro("Malfermi elsendon de sciigo: ID=$markilo") == 1)

            scenaro.recreate() // kiel ekranturno: onCreate(savedInstanceState != null)

            Thread.sleep(3_000)
            // Sen la riparo la rekreo re-traktus la intenton → dua "Ludado komencita de sciigo"
            check(logNombro("Ludado komencita de sciigo: Testa elsendo rekreo") == 1) {
                "Post rekreo la elsendo restartis — la sciig-intento estis re-traktita"
            }
            check(logNombro("Malfermi elsendon de sciigo: ID=$markilo") == 1)
        } finally {
            scenaro.close()
        }
    }
}
