package im.isolux.opencall.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import im.isolux.opencall.blocking.BlocklistRepository
import im.isolux.opencall.data.CallLogEntry
import im.isolux.opencall.data.CallLogRepository
import im.isolux.opencall.data.Contact
import im.isolux.opencall.data.ContactsRepository
import im.isolux.opencall.telecom.CallPlacer
import im.isolux.opencall.telecom.PlaceCallResult
import im.isolux.opencall.ui.calls.CallsScreen
import im.isolux.opencall.ui.components.GlassBackground
import im.isolux.opencall.ui.components.glassSurface
import im.isolux.opencall.ui.contacts.ContactsScreen
import im.isolux.opencall.ui.keypad.KeypadScreen
import im.isolux.opencall.ui.search.SearchScreen
import im.isolux.opencall.ui.settings.SettingsScreen
import im.isolux.opencall.ui.theme.Glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Les cinq onglets définitifs, dans l'ordre. */
enum class Tab(val label: String, val icon: ImageVector) {
    CALLS("Appels", Icons.Rounded.AccessTime),
    CONTACTS("Contacts", Icons.Rounded.People),
    KEYPAD("Clavier", Icons.Rounded.Dialpad),
    SEARCH("Recherche", Icons.Rounded.Search),
    SETTINGS("Paramètres", Icons.Rounded.Settings),
}

@Composable
fun OpenCallRoot(pendingDial: String?, onDialConsumed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(Tab.CALLS) }
    var dialNumber by rememberSaveable { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var pendingCall by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { reload++ }
    LaunchedEffect(pendingDial) {
        if (pendingDial != null) {
            dialNumber = pendingDial
            tab = Tab.KEYPAD
            onDialConsumed()
        }
    }

    val blocklist = BlocklistRepository.get(context)
    val blocked by blocklist.entries.collectAsStateWithLifecycle()
    val hasCallLog = remember(reload) { CallLogRepository.hasPermission(context) }
    val hasContacts = remember(reload) { ContactsRepository.hasPermission(context) }
    var calls by remember { mutableStateOf(emptyList<CallLogEntry>()) }
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    LaunchedEffect(reload, hasCallLog, hasContacts) {
        calls = withContext(Dispatchers.IO) { CallLogRepository.load(context) }
        contacts = withContext(Dispatchers.IO) { ContactsRepository.load(context) }
        withContext(Dispatchers.IO) { blocklist.refresh() }
    }

    fun message(text: String) {
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(text) }
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reload++ }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pendingCall
        pendingCall = null
        if (granted && number != null) CallPlacer.place(context, number)
        else if (!granted) message("Autorisation d'appel refusée.")
    }

    fun call(number: String) {
        when (CallPlacer.place(context, number)) {
            PlaceCallResult.PLACED -> Unit
            PlaceCallResult.NEEDS_PERMISSION -> {
                pendingCall = number
                callPermission.launch(Manifest.permission.CALL_PHONE)
            }
            PlaceCallResult.FAILED -> message("Impossible de passer l'appel.")
        }
    }
    val isBlocked: (String) -> Boolean = { n -> blocked.any { im.isolux.opencall.blocking.PhoneNumbers.isSameNumber(it, n) } }
    val block: (String) -> Unit = { n -> message(blockResultMessage(blocklist.block(n), blocklist.enforcement())) }
    val unblock: (String) -> Unit = { n -> blocklist.unblock(n); message("Numéro débloqué.") }

    GlassBackground {
        val contentPadding = PaddingValues(bottom = 112.dp)
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (tab) {
                Tab.CALLS -> CallsScreen(
                    hasCallLog, calls, isBlocked,
                    onRequestPermission = { permissions.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS)) },
                    onCall = ::call, onBlock = block, onUnblock = unblock, bottomPadding = contentPadding,
                )
                Tab.CONTACTS -> ContactsScreen(
                    hasContacts, contacts, isBlocked,
                    onRequestPermission = { permissions.launch(arrayOf(Manifest.permission.READ_CONTACTS)) },
                    onCall = ::call, onBlock = block, onUnblock = unblock, bottomPadding = contentPadding,
                )
                Tab.KEYPAD -> Box(Modifier.padding(contentPadding)) {
                    KeypadScreen(dialNumber, { dialNumber = it }, ::call)
                }
                Tab.SEARCH -> Box(Modifier.windowInsetsPadding(WindowInsets.ime)) {
                    SearchScreen(contacts, calls, isBlocked, ::call, block, unblock, contentPadding)
                }
                Tab.SETTINGS -> SettingsScreen(contentPadding, onMessage = ::message)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            SnackbarHost(snackbar)
            GlassTabBar(tab) { tab = it }
        }
    }
}

/** Barre d'onglets flottante en verre dépoli. */
@Composable
private fun GlassTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val c = Glass.colors
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .glassSurface(RoundedCornerShape(30.dp), elevation = 12.dp)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Tab.entries.forEach { t ->
            val isSel = t == selected
            Column(
                Modifier
                    .weight(1f)
                    .background(if (isSel) c.accent.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(24.dp))
                    .clickable(role = Role.Tab) { onSelect(t) }
                    .semantics { this.selected = isSel }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(t.icon, null, tint = if (isSel) c.accent else c.textSecondary)
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (isSel) c.accent else c.textSecondary, maxLines = 1)
            }
        }
    }
}
