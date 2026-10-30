package im.isolux.opencall.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import im.isolux.opencall.data.Contact
import im.isolux.opencall.ui.components.Avatar
import im.isolux.opencall.ui.components.EmptyState
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.PermissionCard
import im.isolux.opencall.ui.components.ScreenTitle
import im.isolux.opencall.ui.components.SectionHeader
import im.isolux.opencall.ui.theme.Glass

@Composable
fun ContactsScreen(
    hasPermission: Boolean,
    contacts: List<Contact>,
    isBlocked: (String) -> Boolean,
    onRequestPermission: () -> Unit,
    onCall: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    bottomPadding: PaddingValues,
) {
    var selected by remember { mutableStateOf<Contact?>(null) }
    val favorites = contacts.filter { it.starred }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding) {
        item { ScreenTitle("Contacts") }
        if (!hasPermission) {
            item {
                PermissionCard(
                    "Contacts",
                    "Autorisez OpenCall à lire vos contacts pour les afficher, gérer vos favoris et identifier les appelants.",
                    "Autoriser l'accès",
                    onRequestPermission,
                )
            }
            return@LazyColumn
        }
        if (favorites.isNotEmpty()) {
            item { SectionHeader("Favoris") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(favorites, key = { it.id }) { fav ->
                        Column(
                            Modifier.width(72.dp).clickable { selected = fav },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Avatar(fav.name, size = 60.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(fav.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Glass.colors.textPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        item { SectionHeader("Tous les contacts") }
        if (contacts.isEmpty()) {
            item { EmptyState("Aucun contact avec numéro de téléphone.") }
        } else {
            item {
                GlassCard(Modifier.padding(horizontal = 16.dp)) {
                    contacts.forEachIndexed { i, contact ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = contact }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(contact.name)
                            Spacer(Modifier.width(12.dp))
                            Text(contact.name, color = Glass.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (contact.phones.any { isBlocked(it.number) }) {
                                Icon(Icons.Rounded.Block, "Bloqué", tint = Glass.colors.callRed)
                            }
                        }
                        if (i < contacts.lastIndex) GlassDivider()
                    }
                }
            }
        }
    }

    selected?.let { contact ->
        ContactSheet(contact, isBlocked, onCall, onBlock, onUnblock) { selected = null }
    }
}

/** Fiche contact : appeler, bloquer / débloquer chaque numéro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSheet(
    contact: Contact,
    isBlocked: (String) -> Boolean,
    onCall: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Glass.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(contact.name, size = 84.dp)
            Spacer(Modifier.height(8.dp))
            Text(contact.name, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
            if (contact.starred) Text("Favori", color = c.textSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                contact.phones.forEachIndexed { i, phone ->
                    val blocked = isBlocked(phone.number)
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(phone.label, color = c.textSecondary, style = MaterialTheme.typography.labelMedium)
                            Text(PhoneNumbers.format(phone.number), color = c.textPrimary)
                            if (blocked) Text("Bloqué", color = c.callRed, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { if (blocked) onUnblock(phone.number) else onBlock(phone.number) }) {
                            Text(if (blocked) "Débloquer" else "Bloquer", color = if (blocked) c.accent else c.callRed)
                        }
                        IconButton(onClick = { onDismiss(); onCall(phone.number) }) {
                            Icon(Icons.Rounded.Call, "Appeler", tint = c.callGreen)
                        }
                    }
                    if (i < contact.phones.lastIndex) GlassDivider()
                }
            }
        }
    }
}
