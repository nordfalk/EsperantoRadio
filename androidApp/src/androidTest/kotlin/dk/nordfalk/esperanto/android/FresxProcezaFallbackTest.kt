package dk.nordfalk.esperanto.android

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dk.nordfalk.esperanto.data.repository.AlarmoReceivilo
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regres-testo por la freŝ-proceza (pura-fallback) kazo de la vekhorloĝo
 * (PR #111): sen transicia gardilo la unua `updateState()` post la
 * MediaController-konekto kalkulis `Haltita` (malplena media listo) kaj vokis
 * `haltuAlarmRingon()` — tio mortigis la ĵus startintan fallback-ringtonon
 * kaj la alarmo restis silenta.
 *
 * Ĉi tiu testo INTENCE ne uzas `ActivityScenarioRule`: la ludilo
 * (ExoPlayerLudiloRegilo) ankoraŭ ne ekzistas kiam la testo komenciĝas —
 * same kiel en freŝa procezo vekita de alarmo. La testoj en
 * [AlarmoReceiviloTest] ne povas kapti tiun kazon, ĉar tie la ludilo jam
 * ekzistas kaj estas konektita kiam la alarmo estas sendita.
 *
 * Ret-ne-dependa: uzas neekzistantan kanalon (tuja fallback, neniu RSS-elŝuto).
 *
 * Rulu: ./gradlew :androidApp:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=dk.nordfalk.esperanto.android.FresxProcezaFallbackTest
 */
@RunWith(AndroidJUnit4::class)
class FresxProcezaFallbackTest {

    private val kunteksto = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun purigu() {
        AlarmoReceivilo.haltuFallbackRingon()
        AlarmoReceivilo.nuliguSciigon(kunteksto)
    }

    @Test
    fun fallbackRingtonoSupervivasKonektonDeLudilo() {
        val intent = Intent(AlarmoReceivilo.AGO_ALARMO_EKIGAS)
            .setClass(kunteksto, AlarmoReceivilo::class.java)
            .putExtra("alarmo_id", -1)
            .putExtra("kanal_slug", "neekzistanta-kanalo") // ne trovita → tuj fallback
            .putExtra("etikedo", "Testa alarmo")
        kunteksto.sendBroadcast(intent)

        // La fallback-ringtono ekas (neniu reto bezonata)
        atendu(30_000) { AlarmoReceivilo.fallbackRingtonoAktiva }

        // Atendu preter la MediaController-konekton (kie updateState() unuafoje vokiĝas)
        Thread.sleep(10_000)

        assertTrue(
            "La fallback-ringtono ankoraŭ devas aktivi post la ludilo-konekto — alie la alarmo estus silenta",
            AlarmoReceivilo.fallbackRingtonoAktiva,
        )
    }

    /** Atendas ĝis [kondicxo] veras, maks [maksMs]. */
    private fun atendu(maksMs: Long, kondicxo: () -> Boolean) {
        val limo = System.currentTimeMillis() + maksMs
        while (!kondicxo()) {
            check(System.currentTimeMillis() < limo) { "Kondiĉo ne plenumita post $maksMs ms" }
            Thread.sleep(100)
        }
    }
}
