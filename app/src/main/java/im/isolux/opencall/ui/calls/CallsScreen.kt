package im.isolux.opencall.ui.calls

import android.provider.CallLog
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import im.isolux.opencall.blocking.PhoneNumbers
import im.isolux.opencall.data.CallLogEntry
import im.isolux.opencall.ui.components.Avatar
import im.isolux.opencall.ui.components.EmptyState
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.PermissionCard
import im.isolux.opencall.ui.components.ScreenTitle
import im.isolux.opencall.ui.theme.Glass

@Composable
fun CallsScreen(
    hasPermission: Boolean,
    entries: List<CallLogEntry>,
    isBlocked: (String) -> Boolean,
    onRequestPermission: () -> Unit,
    onCall: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    bottomPadding: PaddingValues,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding) {
        item { ScreenTitle("Appels") }
        if (!hasPermission) {
            item {
                PermissionCard(
                    "Historique des appels",
                    "OpenCall a besoin d'accéder au journal d'appels pour afficher l'historique. Rien n'est envoyé hors de l'appareil.",
                    "Autoriser l'accès",
                    onRequestPermission,
                )
            }
            return@LazyColumn
        }
        if (entries.isEmpty()) {
            item { EmptyState("Aucun appel récent.") }
            return@LazyColumn
        }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                entries.forEachIndexed { index, entry ->
                    CallRow(entry, isBlocked(entry.number), onCall, onBlock, onUnblock)
                    if (index < entries.lastIndex) GlassDivider()
                }
            }
        }
    }
}

@Composable
private fun CallRow(
    entry: CallLogEntry,
    blocked: Boolean,
    onCall: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
) {
    val c = Glass.colors
    var menu by remember { mutableStateOf(false) }
    val missed = entry.type == CallLog.Calls.MISSED_TYPE || entry.type == CallLog.Calls.REJECTED_TYPE
    val (icon, desc) = when (entry.type) {
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Rounded.CallMade to "Sortant"
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Rounded.CallMissed to "Manqué"
        CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Rounded.CallMissed to "Refusé"
        CallLog.Calls.BLOCKED_TYPE -> Icons.Rounded.Block to "Bloqué"
        else -> Icons.AutoMirrored.Rounded.CallReceived to "Entrant"
    }
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = entry.number.isNotBlank()) { onCall(entry.number) }
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(entry.name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name ?: entry.number.takeIf { it.isNotBlank() }?.let { PhoneNumbers.format(it) } ?: "Numéro masqué",
                color = if (missed) c.callRed else c.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, desc, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    DateUtils.getRelativeTimeSpanString(entry.date).toString() + if (blocked) " · Bloqué" else "",
                    color = c.textSecondary, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (entry.number.isNotBlank()) {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Options", tint = c.textSecondary) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Appeler") }, onClick = { menu = false; onCall(entry.number) })
                    if (blocked) {
                        DropdownMenuItem(text = { Text("Débloquer ce numéro") }, onClick = { menu = false; onUnblock(entry.number) })
                    } else {
                        DropdownMenuItem(
                            text = { Text("Bloquer ce numéro") },
                            leadingIcon = { Icon(Icons.Rounded.Block, null) },
                            onClick = { menu = false; onBlock(entry.number) },
                        )
                    }
                }
            }
        }
    }
}
