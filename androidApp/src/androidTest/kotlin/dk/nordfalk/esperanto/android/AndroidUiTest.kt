package dk.nordfalk.esperanto.android

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
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

    /** La malsupra naviga breto — la ero kun ĉi tiu teksto kiu estas klakebla (ne sekcio-titolo). */
    private fun langeto(teksto: String) = composeRule.onNode(hasText(teksto) and hasClickAction())

    @Before
    fun atenduLaApon() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Hejmo") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun kanalaroMontrasKanalojn() {
        langeto("Kanaloj").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Varsovia Vento").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Muzaiko").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Varsovia Vento").onFirst().assertIsDisplayed()
    }

    @Test
    fun navigadoAlSercxo() {
        langeto("Serĉi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Trovu elsendon...").assertIsDisplayed()
    }

    @Test
    fun navigadoAlAgordoj() {
        composeRule.onNodeWithContentDescription("Agordoj").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Agordoj").assertIsDisplayed()
        composeRule.onNodeWithText("Aŭtomate daŭrigu kun alia elsendo").assertIsDisplayed()
    }

    @Test
    fun navigadoAlPlejŝatataj() {
        langeto("Plej ŝatataj").performClick()
        composeRule.waitForIdle()
        // La teksto aperas dufoje nur sur la ekrano mem: kiel titolo kaj kiel langeto
        composeRule.onAllNodesWithText("Plej ŝatataj").assertCountEquals(2)
    }
}
