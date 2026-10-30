package im.isolux.opencall.screenshots

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import android.content.pm.PackageManager
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.ShadowStatFs
import android.media.MediaMetadataRetriever
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureScreenRoboImage
import im.isolux.opencall.blocking.BlocklistRepository
import im.isolux.opencall.data.CallLogEntry
import im.isolux.opencall.data.Contact
import im.isolux.opencall.data.ContactPhone
import im.isolux.opencall.recording.RecordingsRepository
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.CallUi
import im.isolux.opencall.telecom.InCallScreen
import im.isolux.opencall.ui.GlassTabBar
import im.isolux.opencall.ui.Tab
import im.isolux.opencall.ui.calls.CallsScreen
import im.isolux.opencall.ui.components.GlassBackground
import im.isolux.opencall.ui.contacts.ContactsScreen
import im.isolux.opencall.ui.keypad.KeypadScreen
import im.isolux.opencall.ui.lock.LockScreen
import im.isolux.opencall.ui.search.SearchScreen
import im.isolux.opencall.ui.settings.SettingsPage
import im.isolux.opencall.ui.settings.SettingsScreen
import im.isolux.opencall.ui.theme.OpenCallTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager

/**
 * Captures des écrans réels d'OpenCall, rendus sur la JVM par Robolectric (aucune maquette).
 * Les données (historique, contacts, état des rôles) sont des exemples injectés pour la capture.
 *
 * Générer : ./gradlew :app:recordRoborazziDebug — images dans app/build/outputs/roborazzi/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "fr-rFR-" + RobolectricDeviceQualifiers.Pixel7)
class FeatureScreenshotsTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val now = System.currentTimeMillis()
    private val pad = PaddingValues(bottom = 112.dp)

    private val calls = listOf(
        CallLogEntry(1, "+33612345678", "Camille Laurent", CallLog.Calls.INCOMING_TYPE, now - 12 * 60_000, 245),
        CallLogEntry(2, "0698765432", null, CallLog.Calls.MISSED_TYPE, now - 2 * 3_600_000, 0),
        CallLogEntry(3, "+33145678901", "Cabinet Dr Moreau", CallLog.Calls.OUTGOING_TYPE, now - 5 * 3_600_000, 92),
        CallLogEntry(4, "0899123456", null, CallLog.Calls.BLOCKED_TYPE, now - 26 * 3_600_000, 0),
        CallLogEntry(5, "+33687654321", "Hugo Bernard", CallLog.Calls.OUTGOING_TYPE, now - 50 * 3_600_000, 610),
        CallLogEntry(6, "+33611223344", "Léa Martin", CallLog.Calls.INCOMING_TYPE, now - 75 * 3_600_000, 38),
    )

    private val contacts = listOf(
        Contact(1, "Camille Laurent", true, null, listOf(ContactPhone("+33612345678", "Mobile"), ContactPhone("+33144556677", "Travail"))),
        Contact(2, "Hugo Bernard", true, null, listOf(ContactPhone("+33687654321", "Mobile"))),
        Contact(3, "Léa Martin", true, null, listOf(ContactPhone("+33611223344", "Mobile"))),
        Contact(4, "Cabinet Dr Moreau", false, null, listOf(ContactPhone("+33145678901", "Travail"))),
        Contact(5, "Inès Petit", false, null, listOf(ContactPhone("+33622334455", "Mobile"))),
        Contact(6, "Julien Roux", false, null, listOf(ContactPhone("+33633445566", "Mobile"))),
        Contact(7, "Nadia Garnier", false, null, listOf(ContactPhone("+33644556677", "Mobile"))),
    )

    @Before
    fun setUp() {
        // Les singletons gardent le contexte de la capture précédente : on les réinitialise.
        listOf(AppPrefs::class.java, BlocklistRepository::class.java, RecordingsRepository::class.java).forEach {
            ReflectionHelpers.setStaticField(it, "instance", null)
        }
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.POST_NOTIFICATIONS)
        val roles = shadowOf(app.getSystemService(RoleManager::class.java))
        roles.addAvailableRole(RoleManager.ROLE_DIALER)
        roles.addAvailableRole(RoleManager.ROLE_CALL_SCREENING)
        shadowOf(app.getSystemService(TelecomManager::class.java)).setDefaultDialerPackage("com.google.android.dialer")
        // Appareil de référence : téléphone Pixel 7 avec téléphonie et micro.
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_TELEPHONY, true)
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_MICROPHONE, true)
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setModel("Pixel 7")
        Shadow.extract<ShadowBiometricManager>(app.getSystemService(BiometricManager::class.java)).setCanAuthenticate(true)
    }

    private fun becomeDefaultDialer() {
        shadowOf(app.getSystemService(RoleManager::class.java)).addHeldRole(RoleManager.ROLE_DIALER)
        shadowOf(app.getSystemService(TelecomManager::class.java)).setDefaultDialerPackage(app.packageName)
        shadowOf(app).grantPermissions(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG)
    }

    private fun render(content: @Composable () -> Unit) {
        compose.setContent { OpenCallTheme { content() } }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        render(content)
        capture(name)
    }

    /** Reproduit la mise en page d'OpenCallRoot : fond de verre, contenu, barre d'onglets. */
    @Composable
    private fun Frame(tab: Tab, content: @Composable () -> Unit) {
        GlassBackground {
            Box(Modifier.fillMaxSize().statusBarsPadding()) { content() }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) { GlassTabBar(tab) {} }
        }
    }

    private val noop: (String) -> Unit = {}

    @Test fun f01_appels() = shoot("01_appels_historique") {
        Frame(Tab.CALLS) { CallsScreen(true, calls, { it == "0899123456" }, {}, noop, noop, noop, pad) }
    }

    @Test fun f02_contacts() = shoot("02_contacts_favoris") {
        Frame(Tab.CONTACTS) { ContactsScreen(true, contacts, { false }, {}, noop, noop, noop, pad) }
    }

    @Test fun f03_ficheContact() {
        render { Frame(Tab.CONTACTS) { ContactsScreen(true, contacts, { it == "+33144556677" }, {}, noop, noop, noop, pad) } }
        compose.onAllNodesWithText("Camille Laurent")[0].performClick()
        capture("03_fiche_contact_bloquer")
    }

    @Test fun f04_clavier() = shoot("04_clavier") {
        Frame(Tab.KEYPAD) { Box(Modifier.padding(pad)) { KeypadScreen("0612345678", {}, noop) } }
    }

    @Test fun f05_recherche() {
        render { Frame(Tab.SEARCH) { SearchScreen(contacts, calls, { false }, noop, noop, noop, pad) } }
        compose.onNode(hasSetTextAction()).performTextInput("ca")
        capture("05_recherche")
    }

    @Test fun f06_parametres() {
        AppPrefs.get(app).localBlocklist = setOf("0899123456", "+33178000000")
        shoot("06_parametres") { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop) } }
    }

    @Test fun f07_telephoneNonDefaut() = shoot("07_application_telephone_non_defaut") {
        Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.DIALER) }
    }

    @Test fun f08_telephoneParDefaut() {
        becomeDefaultDialer()
        shoot("08_application_telephone_par_defaut") { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.DIALER) } }
    }

    @Test fun f09_verrouillage() {
        AppPrefs.get(app).lockEnabled = true
        shoot("09_verrouillage_reglages") { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.LOCK) } }
    }

    @Test fun f10_appVerrouillee() = shoot("10_application_verrouillee") { LockScreen(onUnlock = {}, emergencyPrefill = null) }

    @Test fun f11_urgence() = shoot("11_appel_urgence_verrouille") { LockScreen(onUnlock = {}, emergencyPrefill = "112") }

    @Test fun f12_listeNoireSysteme() {
        becomeDefaultDialer()
        FakeBlockedNumberProvider.numbers = mutableListOf("0899123456", "+33178000000", "0970123456")
        Robolectric.setupContentProvider(FakeBlockedNumberProvider::class.java, BlockedNumberContract.AUTHORITY)
        shoot("12_liste_noire_systeme") { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.BLOCKLIST) } }
    }

    @Test fun f13_listeNoireSansRole() {
        AppPrefs.get(app).localBlocklist = setOf("0899123456", "+33178000000")
        shoot("13_liste_noire_sans_role") { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.BLOCKLIST) } }
    }

    @Test fun f14_enregistrementNonPrisEnCharge() {
        becomeDefaultDialer()
        shoot("14_enregistrement_non_pris_en_charge") { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.RECORDING) } }
    }

    private fun compatibleRecordingDevice() {
        // Simulation d'une installation système disposant de CAPTURE_AUDIO_OUTPUT (ROM / application préinstallée).
        becomeDefaultDialer()
        shadowOf(app).grantPermissions("android.permission.CAPTURE_AUDIO_OUTPUT", Manifest.permission.RECORD_AUDIO)
        AppPrefs.get(app).apply { recordingConsentAcknowledged = true; recordingEnabled = true }
        val dir = RecordingsRepository.get(app).directory()
        listOf(
            Triple("Appel_2026-10-30_14-02-11_+33612345678.m4a", 412_000, "51000"),
            Triple("Devis cuisine - Camille.m4a", 1_250_000, "156000"),
        ).forEach { (name, size, durationMs) ->
            val file = dir.resolve(name).apply { writeBytes(ByteArray(size)) }
            ShadowMediaMetadataRetriever.addMetadata(file.absolutePath, MediaMetadataRetriever.METADATA_KEY_DURATION, durationMs)
        }
        // Espace disque d'exemple : 128 Go dont 41 Go libres.
        ShadowStatFs.registerStats(app.filesDir, 31_250_000, 10_000_000, 10_000_000)
    }

    @Test fun f15_enregistrementAppareilCompatible() {
        compatibleRecordingDevice()
        shoot("15_enregistrement_appareil_compatible") { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.RECORDING) } }
    }

    @Test fun f15b_enregistrementsGestion() {
        compatibleRecordingDevice()
        render { Frame(Tab.SETTINGS) { SettingsScreen(pad, noop, SettingsPage.RECORDING) } }
        repeat(2) { compose.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        capture("15b_enregistrements_ecoute_gestion")
    }

    @Test fun f16_appelEntrant() {
        val call = fakeCall()
        val ui = CallUi(call, Call.STATE_RINGING, "+33612345678", "Camille Laurent", 0L, false, false)
        shoot("16_appel_entrant") { InCallScreen(listOf(ui), ui, null) }
    }

    @Test fun f17_appelEnCours() {
        val call = fakeCall()
        val ui = CallUi(call, Call.STATE_ACTIVE, "+33612345678", "Camille Laurent", now - 83_000, true, false)
        val audio = CallAudioState(false, CallAudioState.ROUTE_SPEAKER, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        shoot("17_appel_en_cours") { InCallScreen(listOf(ui), ui, audio) }
    }

    /** android.telecom.Call n'a pas de constructeur public : instance nue, seulement pour l'affichage. */
    private fun fakeCall(): Call = ReflectionHelpers.callConstructor(
        Call::class.java,
        param(Class.forName("android.telecom.Phone"), null),
        param(String::class.java, "capture"),
        param(Class.forName("android.telecom.InCallAdapter"), null),
        param(String::class.java, "im.isolux.opencall"),
        param(Int::class.javaPrimitiveType!!, 35),
    )

    @Suppress("UNCHECKED_CAST")
    private fun param(type: Class<*>, value: Any?): ClassParameter<Any?> = ClassParameter(type as Class<Any?>, value)
}

/** Fournisseur factice de BlockedNumberContract (absent de Robolectric). */
class FakeBlockedNumberProvider : ContentProvider() {
    companion object {
        var numbers = mutableListOf<String>()
    }

    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle = Bundle().apply {
        putBoolean("can_block", true)
        putBoolean("blocked", numbers.contains(arg))
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor =
        MatrixCursor(arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ID, BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)).apply {
            numbers.forEachIndexed { i, n -> addRow(arrayOf<Any>(i.toLong(), n)) }
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        values?.getAsString(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)?.let { numbers += it }
        return uri
    }

    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
    override fun getType(uri: Uri): String? = null
}
