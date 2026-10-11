package dk.nordfalk.esperanto.android

import android.content.Intent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dk.nordfalk.esperanto.data.repository.AlarmoReceivilo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.ExoPlayerLudiloRegilo
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La vekhorloĝo ludas EN LA RICEVILO (goAsync-korutino), ne en MainActivity:
 * Activity lanĉita el fona ricevilo estas blokata de la Background-Activity-Launch-
 * limigoj (Android 10+) kaj ruliĝas nur kiam la uzanto vekas la telefonon
 * (2026-10-11: alarmo 16:10 ludis nur kiam la uzanto vekis ĝin je 16:14).
 *
 * La testo sendas la ALARMO_EKIGAS-disaŭdigo kaj kontrolas ke la procez-nivela
 * ludilo mem atingas ludado — sen ke iu ajn Activity traktu la alarmo-intenton.
 * "peranto" estas podkasta kanalo, do la ricevilo devas elŝuti la RSS-fluon
 * (blogspot) kaj elekti la plej freŝan neaŭskultitan elsendon (retdependa —
 * vidu SciigoLudiTest por la sama nivelo de retdependeco).
 *
 * Rulu: ./gradlew :androidApp:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=dk.nordfalk.esperanto.android.AlarmoReceiviloTest
 */
@RunWith(AndroidJUnit4::class)
class AlarmoReceiviloTest {

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
    fun alarmoReceiviloMemKomencasLudadon() {
        // Atendu la konekton de la ludilo al la servo, poste certigu ke nenio ludas
        Thread.sleep(2_000)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        atendu(5_000) { ludilo.stato.value.nunaFonto == null }

        // La sama disaŭdigo kiun AlarmoSkedilo skedas per PendingIntent
        val intent = Intent(AlarmoReceivilo.AGO_ALARMO_EKIGAS)
            .setClass(kunteksto, AlarmoReceivilo::class.java)
            .putExtra("alarmo_id", -1) // ne ekzistas en la deponejo — nur protokolas averton
            .putExtra("kanal_slug", "peranto")
            .putExtra("etikedo", "Testa alarmo")
        kunteksto.sendBroadcast(intent)

        // La ricevilo mem trovas la elsendon kaj fiksas la fonton — SEN Activity
        atendu(30_000) { ludilo.stato.value.nunaFonto is Sonfonto.ElsendoFonto }
        assertTrue(
            "La elsendo devas esti de la alarmo-kanalo",
            (ludilo.stato.value.nunaFonto as Sonfonto.ElsendoFonto).elsendo.kanaloSlug == "peranto",
        )

        // ... kaj la ludado efektive komenciĝas (MP3 elshuto + preparo)
        atendu(30_000) { ludilo.stato.value.stato is LudantoStato.Ludas }
    }
}
