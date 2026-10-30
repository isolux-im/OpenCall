package im.isolux.opencall.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import im.isolux.opencall.BuildConfigInfo
import im.isolux.opencall.blocking.BlockingEnforcement
import im.isolux.opencall.blocking.BlocklistRepository
import im.isolux.opencall.lock.AppLockManager
import im.isolux.opencall.recording.RecordingCapability
import im.isolux.opencall.recording.RecordingSupport
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.DialerRole
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.ScreenTitle
import im.isolux.opencall.ui.components.SectionHeader
import im.isolux.opencall.ui.components.SettingRow
import im.isolux.opencall.ui.theme.Glass

enum class SettingsPage { HOME, DIALER, LOCK, BLOCKLIST, RECORDING }

@Composable
fun SettingsScreen(
    bottomPadding: PaddingValues,
    onMessage: (String) -> Unit,
    initialPage: SettingsPage = SettingsPage.HOME,
) {
    var page by rememberSaveable { mutableStateOf(initialPage) }
    BackHandler(enabled = page != SettingsPage.HOME) { page = SettingsPage.HOME }

    // Relit les statuts (rôles, autorisations) à chaque retour au premier plan.
    var resumeTick by rememberSaveable { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val prefsVersion by AppPrefs.get(LocalContext.current).version.collectAsStateWithLifecycle()
    val refreshKey = resumeTick * 31 + prefsVersion

    when (page) {
        SettingsPage.HOME -> SettingsHome(bottomPadding, refreshKey) { page = it }
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding) {
            item { SubPageHeader(titleOf(page)) { page = SettingsPage.HOME } }
            item {
                when (page) {
                    SettingsPage.DIALER -> DialerSettings(refreshKey, onMessage)
                    SettingsPage.LOCK -> LockSettings(refreshKey, onMessage)
                    SettingsPage.BLOCKLIST -> BlocklistSettings(refreshKey, onMessage)
                    SettingsPage.RECORDING -> RecordingSettings(refreshKey, onMessage)
                    SettingsPage.HOME -> Unit
                }
            }
        }
    }
}

private fun titleOf(page: SettingsPage) = when (page) {
    SettingsPage.HOME -> "Paramètres"
    SettingsPage.DIALER -> "Application Téléphone"
    SettingsPage.LOCK -> "Verrouillage"
    SettingsPage.BLOCKLIST -> "Numéros bloqués"
    SettingsPage.RECORDING -> "Enregistrement des appels"
}

@Composable
private fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour", tint = Glass.colors.textPrimary) }
        ScreenTitle(title, Modifier.padding(start = 0.dp))
    }
}

@Composable
private fun SettingsHome(bottomPadding: PaddingValues, refreshKey: Int, open: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    val c = Glass.colors
    // refreshKey force la relecture des statuts.
    val isDialer = remember(refreshKey) { DialerRole.isDefaultDialer(context) }
    val prefs = AppPrefs.get(context)
    val lockOn = remember(refreshKey) { prefs.lockEnabled }
    val enforcement = remember(refreshKey) { BlocklistRepository.get(context).enforcement() }
    val blockedCount = remember(refreshKey) { BlocklistRepository.get(context).also { it.refresh() }.entries.value.size }
    val capability = remember(refreshKey) { RecordingSupport.capability(context) }
    val chevron: @Composable () -> Unit = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding) {
        item { ScreenTitle("Paramètres") }
        item { SectionHeader("Appels") }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                SettingRow(
                    "Application Téléphone",
                    if (isDialer) "OpenCall est l'application par défaut" else "Composeur actuel conservé",
                    Icons.Rounded.Phone, c.callGreen, onClick = { open(SettingsPage.DIALER) }, trailing = chevron,
                )
                GlassDivider()
                SettingRow(
                    "Numéros bloqués",
                    "$blockedCount numéro(s) · " + when (enforcement) {
                        BlockingEnforcement.SYSTEM -> "liste système Android"
                        BlockingEnforcement.SCREENING_ONLY -> "filtrage OpenCall"
                        BlockingEnforcement.NONE -> "non appliqué"
                    },
                    Icons.Rounded.Block, c.callRed, onClick = { open(SettingsPage.BLOCKLIST) }, trailing = chevron,
                )
                GlassDivider()
                SettingRow(
                    "Enregistrement des appels",
                    when (capability) {
                        RecordingCapability.Supported -> if (prefs.recordingEnabled) "Activé" else "Désactivé"
                        RecordingCapability.NeedsDefaultDialer -> "Nécessite le rôle Téléphone"
                        RecordingCapability.NeedsMicrophonePermission -> "Autorisation micro requise"
                        is RecordingCapability.Unsupported -> "Non pris en charge sur cet appareil"
                    },
                    Icons.Rounded.FiberManualRecord, c.warning, onClick = { open(SettingsPage.RECORDING) }, trailing = chevron,
                )
            }
        }
        item { SectionHeader("Confidentialité") }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                SettingRow(
                    "Verrouillage d'OpenCall",
                    if (lockOn) "Activé · ${prefs.lockTimeout.label.lowercase()}" else "Désactivé",
                    Icons.Rounded.Lock, c.accent, onClick = { open(SettingsPage.LOCK) }, trailing = chevron,
                )
            }
        }
        item { SectionHeader("À propos") }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                SettingRow("OpenCall", "Version ${BuildConfigInfo.versionName(context)}", Icons.Rounded.Info)
            }
        }
        item {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "OpenCall n'impose aucun choix : vous pouvez conserver votre composeur, et chaque fonction n'est proposée que si Android la permet réellement.",
                    color = c.textSecondary,
                )
            }
        }
    }
}
