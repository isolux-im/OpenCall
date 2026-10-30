package im.isolux.opencall.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import im.isolux.opencall.blocking.PhoneNumbers
import im.isolux.opencall.data.CallLogEntry
import im.isolux.opencall.data.Contact
import im.isolux.opencall.ui.components.Avatar
import im.isolux.opencall.ui.components.EmptyState
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.ScreenTitle
import im.isolux.opencall.ui.components.SectionHeader
import im.isolux.opencall.ui.components.glassSurface
import im.isolux.opencall.ui.contacts.ContactSheet
import im.isolux.opencall.ui.theme.Glass

@Composable
fun SearchScreen(
    contacts: List<Contact>,
    calls: List<CallLogEntry>,
    isBlocked: (String) -> Boolean,
    onCall: (String) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    bottomPadding: PaddingValues,
) {
    val c = Glass.colors
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Contact?>(null) }
    val q = query.trim()
    val digits = PhoneNumbers.digitsOnly(q)
    val contactHits = if (q.isEmpty()) emptyList() else contacts.filter { contact ->
        contact.name.contains(q, ignoreCase = true) ||
            (digits.length >= 2 && contact.phones.any { PhoneNumbers.digitsOnly(it.number).contains(digits) })
    }
    val callHits = if (q.isEmpty()) emptyList() else calls.filter { e ->
        (e.name?.contains(q, ignoreCase = true) == true) || (digits.length >= 2 && PhoneNumbers.digitsOnly(e.number).contains(digits))
    }.distinctBy { PhoneNumbers.normalize(it.number) }.take(20)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding) {
        item { ScreenTitle("Recherche") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Nom ou numéro") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = c.accent.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).glassSurface(RoundedCornerShape(18.dp)),
            )
        }
        if (q.isNotEmpty() && contactHits.isEmpty() && callHits.isEmpty()) {
            item { EmptyState("Aucun résultat pour « $q ».") }
        }
        if (contactHits.isNotEmpty()) {
            item { SectionHeader("Contacts") }
            item {
                GlassCard(Modifier.padding(horizontal = 16.dp)) {
                    contactHits.forEachIndexed { i, contact ->
                        ResultRow(contact.name, contact.phones.firstOrNull()?.number?.let { PhoneNumbers.format(it) }) { selected = contact }
                        if (i < contactHits.lastIndex) GlassDivider()
                    }
                }
            }
        }
        if (callHits.isNotEmpty()) {
            item { SectionHeader("Historique") }
            item {
                GlassCard(Modifier.padding(horizontal = 16.dp)) {
                    callHits.forEachIndexed { i, e ->
                        ResultRow(e.name ?: PhoneNumbers.format(e.number), if (e.name != null) PhoneNumbers.format(e.number) else null) { onCall(e.number) }
                        if (i < callHits.lastIndex) GlassDivider()
                    }
                }
            }
        }
    }
    selected?.let { ContactSheet(it, isBlocked, onCall, onBlock, onUnblock) { selected = null } }
}

@Composable
private fun ResultRow(title: String, subtitle: String?, onClick: () -> Unit) {
    val c = Glass.colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(title)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = c.textPrimary)
            if (subtitle != null) Text(subtitle, color = c.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
