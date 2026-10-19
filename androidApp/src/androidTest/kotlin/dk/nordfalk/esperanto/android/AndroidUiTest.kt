package dk.nordfalk.esperanto.android

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentita UI-testo por la Android-apo.
 * Bezonas emulatoron aŭ realan aparaton (./gradlew :androidApp:connectedDebugAndroidTest).
 *
 * Ne dependas de la reto: la kanaloj venas el la enpakita JSONC-konfiguro.
 */
@RunWith(AndroidJUnit4::class)
class AndroidUiTest {

    /**
     * Donu la sciig-permeson ANTAŬ ol la Activity lanĉiĝas. Alie MainActivity.petiSciigPermeson()
     * montras la sisteman permes-dialogon, MainActivity estas paŭzita, kaj la Compose-testregistro
     * (kiu nur akceptas radikojn en stato RESUMED) raportas "No compose hierarchies found".
     * connectedAndroidTest malinstalas la apon post ĉiu rulo, do la permeso ĉiam mankas komence.
     */
    @get:Rule(order = 0)
    val permesoj: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        else GrantPermissionRule.grant()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Malfermas la navigan tirkeston (menuo-butono supre maldekstre) kaj elektas eron. */
    private fun elektu(teksto: String) {
        composeRule.onNodeWithContentDescription("Naviga menuo").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(teksto) and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText(teksto) and hasClickAction()).performClick()
        composeRule.waitForIdle()
    }

    @Before
    fun atenduLaApon() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            // La naviga-menuo butono malfermas la navigan tirkeston sur la ĉefekranoj
            composeRule.onAllNodes(hasContentDescription("Naviga menuo")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun kanalaroMontrasKanalojn() {
        elektu("Kanaloj")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Varsovia Vento").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Muzaiko").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Varsovia Vento").onFirst().assertIsDisplayed()
    }

    @Test
    fun navigadoAlSercxo() {
        elektu("Serĉi")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Trovu elsendon...").assertIsDisplayed()
    }

    @Test
    fun navigadoAlAgordoj() {
        elektu("Agordoj")
        // Ne aserti la titolon "Agordoj" — la tirkest-ero kun la sama teksto
        // ankaŭ kuŝas en la semantika arbo
        composeRule.onNodeWithText("Aŭtomate daŭrigu kun alia elsendo").assertIsDisplayed()
    }

    @Test
    fun navigadoAlPlejŝatataj() {
        elektu("Plej ŝatataj")
        // La ekrano montras la klarigan tekston kun aŭ sen ŝatataj kanaloj
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Vi ricevos sciigon kiam aperas nova elsendo el ŝatata kanalo.")
                .fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Neniu plej ŝatata kanalo. Premu la koron sur kanalo por aldoni.")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test
    fun tirkestoAnimaciasDumMalfermo() {
        // Halta fram-horlogo: ni paŝas la framajn unuope kaj specimenu la
        // pozicion de la tirkest-ero "Hejmo" meze de la animacio
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithContentDescription("Naviga menuo").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        fun xPozicio(): Float =
            composeRule.onAllNodes(hasText("Hejmo") and hasClickAction())
                .onFirst().fetchSemanticsNode().positionInRoot.x

        val x1 = xPozicio()
        composeRule.mainClock.advanceTimeBy(200)
        val x2 = xPozicio()
        composeRule.mainClock.advanceTimeBy(2000)
        val x3 = xPozicio()
        composeRule.mainClock.autoAdvance = true

        // La tirkesto GLITAS: la ero moviĝas de maldekstre al la fina pozicio
        org.junit.Assert.assertTrue("la tirkest-ero devas moviĝi dum la malfermo (x1=$x1, x2=$x2)", x1 < x2)
        org.junit.Assert.assertTrue("la fina pozicio devas esti la plej dekstra (x2=$x2, x3=$x3)", x2 <= x3)
    }
}