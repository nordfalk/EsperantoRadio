package dk.nordfalk.esperanto.android

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
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

    @get:Rule(order = 0)
    val permesoj: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    @get:Rule(order = 1)
    val aktivecoRegulo = ActivityScenarioRule(MainActivity::class.java)

    private val kunteksto = InstrumentationRegistry.getInstrumentation().targetContext
    private val ludilo get() = ExoPlayerLudiloRegilo.akiru(kunteksto)

    @After
    fun haltu() = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        ludilo.halti() // halti() vokas haltuAlarmVokosonon() — tio ankaŭ haltigas eventualan rezervan vokosonon
    }

    /** Atendas ĝis [kondiĉo] veras, maks [maksMs]. */
    private fun atendu(maksMs: Long, kondiĉo: () -> Boolean) {
        val limo = System.currentTimeMillis() + maksMs
        while (!kondiĉo()) {
            check(System.currentTimeMillis() < limo) { "Kondiĉo ne plenumita post $maksMs ms" }
            Thread.sleep(100)
        }
    }

    @Test
    fun alarmoReceiviloMemKomencasLudadon() {
        // Atendu la konekton de la ludilo al la servo, poste certigu ke nenio ludas
        Thread.sleep(2_000)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        atendu(15_000) { ludilo.stato.value.nunaFonto == null }

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

        // ... kaj la ludado efektive komenciĝas (MP3 elŝuto + preparo)
        atendu(30_000) { ludilo.stato.value.stato is LudantoStato.Ludas }
    }

    /**
     * La alarma sciigo estas DAŬRA dum la alarmo sonas — nenio povas
     * forviŝi ĝin per svingo, do io devas nuligi ĝin kiam la ludado haltiĝas aŭ
     * finiĝas (gardo en AlarmoReceivilo.onReceive). Alie ĝi restus en la ombrumo
     * por ĉiam. (2026-10-12: sekvaĵo de la revizio de la rapida permeso-korekto.)
     *
     * Ret-ne-dependa: uzas la kanalon "muzaiko" (tuja elsendfluo) — la ludilo eniras
     * la staton Konektas (bufrado de la HLS-fluo) loke, do neniu sukcesa
     * elŝuto estas bezonata.
     */
    @Test
    fun alarmoSciigoNuliĝiĝasKiamLudadoHaltiĝas() {
        val nm = kunteksto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sciigoAktiva: () -> Boolean = { nm.activeNotifications.any { it.id == AlarmoReceivilo.SCIIGO_ID } }

        // Forigu eventualan postrestaĵon de la alia testo
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        AlarmoReceivilo.nuliguSciigon(kunteksto)
        atendu(15_000) { !sciigoAktiva() }

        // Ekigu la alarmon kun la kanalo "muzaiko" (tuja elsendfluo; bufrado komenciĝas loke)
        val intent = Intent(AlarmoReceivilo.AGO_ALARMO_EKIGAS)
            .setClass(kunteksto, AlarmoReceivilo::class.java)
            .putExtra("alarmo_id", -1)
            .putExtra("kanal_slug", "muzaiko")
            .putExtra("etikedo", "Testa alarmo")
        kunteksto.sendBroadcast(intent)

        // La sciigo aperas kaj la ludado komenciĝas (Konektas — bufrado de la HLS-fluo)
        atendu(30_000) { sciigoAktiva() }
        atendu(30_000) { ludilo.stato.value.stato != LudantoStato.Haltita }

        // Halti → la gardo nuligas la daŭran sciigon
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        atendu(15_000) { !sciigoAktiva() }
    }

    /**
     * La rezerva alarm-sono devas esti haltigebla: Ringtone.play()
     * ripetiĝas ĝis stop(), sed antaŭe nenie estis tenilo nek haltiga vojo —
     * la sciigo diris "tuŝu por malŝalti" sed nenio haltigis la sonon.
     * En la pura-rezerva-kazo la ExoPlayer neniam startas, do la halto devas
     * esti hokita rekte en halti() (ne per observado de la stato).
     *
     * Ret-ne-dependa: uzas NEekzistantan kanalon — la ricevilo tuj ludas la
     * rezervan vokosonon (neniu RSS-elŝuto, neniu reto).
     *
     * MainActivity ne haltigas la sonon dum la testo: la test-intento ne portas
     * "de_sciigo" (male ol la vera sciigo-tuŝo, vidu MainActivity.traktuAlarmIntent),
     * kaj la best-effort startActivity de la ricevilo ne estas uzanta ago.
     */
    @Test
    fun rezervaVokosonoHaltiĝiĝasPerHalti() {
        // Purigo de eventuala postrestaĵo
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        AlarmoReceivilo.haltuRezervanVokosonon()
        AlarmoReceivilo.nuliguSciigon(kunteksto)
        atendu(15_000) { !AlarmoReceivilo.rezervaVokosonoAktiva }

        val intent = Intent(AlarmoReceivilo.AGO_ALARMO_EKIGAS)
            .setClass(kunteksto, AlarmoReceivilo::class.java)
            .putExtra("alarmo_id", -1)
            .putExtra("kanal_slug", "neekzistanta-kanalo") // kanalo ne trovita → tuj rezerva vokosono
            .putExtra("etikedo", "Testa alarmo")
        kunteksto.sendBroadcast(intent)

        // La rezerva vokosono ekas (la kanalo ne ekzistas — neniu fluo, neniu reto bezonata)
        atendu(30_000) { AlarmoReceivilo.rezervaVokosonoAktiva }

        // Halti (kiel la uzanto per la mini-ludilbreto) haltigas la sonon
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
        atendu(15_000) { !AlarmoReceivilo.rezervaVokosonoAktiva }
    }

    /**
     * La rezerva vokosono MEM haltas post [AlarmoReceivilo.vokosonoMaksDaŭroMs]
     * (defaŭlte 5 minutoj) kaj la daŭra alarma sciigo estas nuligata kun ĝi —
     * Ringtone.play() alie ripetiĝus senfine kaj la daŭra sciigo restus.
     *
     * Ret-ne-dependa: same uzas neekzistantan kanalon (tuja rezerva vokosono, neniu reto).
     */
    @Test
    fun rezervaVokosonoMemHaltiĝiĝasKajNuliĝasSciigon() {
        val nm = kunteksto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sciigoAktiva: () -> Boolean = { nm.activeNotifications.any { it.id == AlarmoReceivilo.SCIIGO_ID } }

        val originala = AlarmoReceivilo.vokosonoMaksDaŭroMs
        AlarmoReceivilo.vokosonoMaksDaŭroMs = 2_000 // anstataŭ 5 minutoj
        try {
            // Purigo de eventuala postrestaĵo
            InstrumentationRegistry.getInstrumentation().runOnMainSync { ludilo.halti() }
            AlarmoReceivilo.haltuRezervanVokosonon()
            AlarmoReceivilo.nuliguSciigon(kunteksto)
            atendu(15_000) { !AlarmoReceivilo.rezervaVokosonoAktiva && !sciigoAktiva() }

            val intent = Intent(AlarmoReceivilo.AGO_ALARMO_EKIGAS)
                .setClass(kunteksto, AlarmoReceivilo::class.java)
                .putExtra("alarmo_id", -1)
                .putExtra("kanal_slug", "neekzistanta-kanalo")
                .putExtra("etikedo", "Testa alarmo")
            kunteksto.sendBroadcast(intent)

            // Sono ekas kaj la sciigo aperas
            atendu(30_000) { AlarmoReceivilo.rezervaVokosonoAktiva }
            atendu(15_000) { sciigoAktiva() }

            // Post la mem-halto: sono haltigita KAJ sciigo nuligita
            atendu(20_000) { !AlarmoReceivilo.rezervaVokosonoAktiva }
            atendu(15_000) { !sciigoAktiva() }
        } finally {
            AlarmoReceivilo.vokosonoMaksDaŭroMs = originala
        }
    }
}
