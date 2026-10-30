package im.isolux.opencall.ui.lock

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.isolux.opencall.blocking.EmergencyNumbers
import im.isolux.opencall.telecom.CallPlacer
import im.isolux.opencall.telecom.PlaceCallResult
import im.isolux.opencall.ui.components.GlassBackground
import im.isolux.opencall.ui.components.GlassButton
import im.isolux.opencall.ui.components.glassSurface
import im.isolux.opencall.ui.keypad.DialPad
import im.isolux.opencall.ui.theme.Glass

/**
 * Écran affiché à la place du contenu d'OpenCall lorsqu'il est verrouillé.
 * Rien de privé n'est composé derrière. Les appels d'urgence restent accessibles.
 */
@Composable
fun LockScreen(onUnlock: () -> Unit, emergencyPrefill: String?) {
    val c = Glass.colors
    var emergency by rememberSaveable { mutableStateOf(emergencyPrefill != null) }

    // Propose l'authentification dès l'affichage.
    LaunchedEffect(Unit) { if (!emergency) onUnlock() }

    GlassBackground {
        if (emergency) {
            EmergencyDialer(emergencyPrefill.orEmpty()) { emergency = false }
            return@GlassBackground
        }
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(96.dp).glassSurface(CircleShape, elevation = 8.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = c.accent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("OpenCall est verrouillé", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Vos appels entrants continuent d'être reçus normalement.",
                color = c.textSecondary, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            GlassButton("Déverrouiller", Modifier.fillMaxWidth(), onClick = onUnlock)
            Spacer(Modifier.height(12.dp))
            GlassButton("Appel d'urgence", Modifier.fillMaxWidth(), tint = c.callRed) { emergency = true }
        }
    }
}

@Composable
private fun EmergencyDialer(prefill: String, onBack: () -> Unit) {
    val c = Glass.colors
    val context = LocalContext.current
    var number by rememberSaveable { mutableStateOf(prefill) }
    val allowed = EmergencyNumbers.isEmergency(context, number)
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Appel d'urgence", style = MaterialTheme.typography.titleLarge, color = c.callRed, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Seuls les numéros d'urgence (112, 15, 17, 18, 911…) peuvent être composés ici.",
            color = c.textSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.weight(1f))
        Text(number, fontSize = 36.sp, color = c.textPrimary, fontWeight = FontWeight.Medium)
        DialPad(onDigit = { if (number.length < 8) number += it })
        Box(
            Modifier.size(76.dp).glassSurface(CircleShape, elevation = 6.dp)
                .background(if (allowed) c.callRed else c.textSecondary.copy(alpha = 0.3f))
                .clickable(enabled = allowed, role = Role.Button, onClickLabel = "Appeler les urgences") {
                    placeEmergency(context, number)
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Call, "Appeler", tint = Color.White, modifier = Modifier.size(34.dp)) }
        TextButton(onClick = { number = number.dropLast(1) }) { Text("Effacer", color = c.textSecondary) }
        TextButton(onClick = onBack) { Text("Retour", color = c.accent) }
    }
}

private fun placeEmergency(context: Context, number: String) {
    if (CallPlacer.place(context, number) != PlaceCallResult.PLACED) CallPlacer.dialViaSystem(context, number)
}
