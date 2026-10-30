package im.isolux.opencall.smoke

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import im.isolux.opencall.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Lance la vraie MainActivity et parcourt les onglets et les écrans de Paramètres. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun parcoursDesOngletsEtDesParametres() {
        listOf("Contacts", "Clavier", "Recherche", "Paramètres", "Appels").forEach { tab ->
            compose.onAllNodesWithText(tab)[0].performClick()
            compose.waitForIdle()
        }
        compose.onAllNodesWithText("Paramètres")[0].performClick()
        listOf("Application Téléphone", "Numéros bloqués", "Enregistrement des appels", "Verrouillage d'OpenCall").forEach { entry ->
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(entry))
            compose.onNodeWithText(entry).performClick()
            compose.waitForIdle()
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
        }
    }
}
