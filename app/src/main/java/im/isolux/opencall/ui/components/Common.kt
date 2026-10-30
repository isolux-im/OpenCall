package im.isolux.opencall.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import im.isolux.opencall.ui.theme.Glass

/** Carte invitant à accorder une autorisation Android, avec explication. */
@Composable
fun PermissionCard(title: String, explanation: String, button: String, onRequest: () -> Unit) {
    GlassCard(Modifier.padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Glass.colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(explanation, style = MaterialTheme.typography.bodyMedium, color = Glass.colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            GlassButton(button, Modifier.fillMaxWidth(), onClick = onRequest)
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Text(
        text,
        color = Glass.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}
