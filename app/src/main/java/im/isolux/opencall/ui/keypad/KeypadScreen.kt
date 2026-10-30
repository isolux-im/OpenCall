package im.isolux.opencall.ui.keypad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import im.isolux.opencall.blocking.PhoneNumbers
import im.isolux.opencall.ui.components.glassSurface
import im.isolux.opencall.ui.theme.Glass

private val KEYS = listOf(
    listOf('1' to "", '2' to "ABC", '3' to "DEF"),
    listOf('4' to "GHI", '5' to "JKL", '6' to "MNO"),
    listOf('7' to "PQRS", '8' to "TUV", '9' to "WXYZ"),
    listOf('*' to "", '0' to "+", '#' to ""),
)

/** Pavé numérique en verre, réutilisé par l'écran d'appel (DTMF) et l'écran d'urgence. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPad(onDigit: (Char) -> Unit, onLongZero: (() -> Unit)? = null) {
    val c = Glass.colors
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        KEYS.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { (digit, letters) ->
                    Column(
                        Modifier.size(76.dp).glassSurface(CircleShape)
                            .combinedClickable(
                                role = Role.Button,
                                onClickLabel = digit.toString(),
                                onClick = { onDigit(digit) },
                                onLongClick = if (digit == '0' && onLongZero != null) onLongZero else null,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(digit.toString(), fontSize = 30.sp, color = c.textPrimary, fontWeight = FontWeight.Normal)
                        if (letters.isNotEmpty()) {
                            Text(letters, fontSize = 10.sp, color = c.textSecondary, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadScreen(number: String, onNumberChange: (String) -> Unit, onCall: (String) -> Unit) {
    val c = Glass.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
            Text(
                PhoneNumbers.format(number).ifEmpty { "" },
                fontSize = 34.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
        }
        DialPad(onDigit = { onNumberChange(number + it) }, onLongZero = { onNumberChange(number + "+") })
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(56.dp))
            Box(
                Modifier.size(76.dp).glassSurface(CircleShape, elevation = 6.dp)
                    .background(c.callGreen.copy(alpha = 0.92f))
                    .combinedClickableCompat(enabled = number.isNotBlank(), label = "Appeler") { onCall(number) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Call, "Appeler", tint = Color.White, modifier = Modifier.size(34.dp)) }
            IconButton(onClick = { onNumberChange(number.dropLast(1)) }, enabled = number.isNotEmpty(), modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, "Effacer", tint = if (number.isEmpty()) Color.Transparent else c.textSecondary)
            }
        }
        Text(
            "Appui long sur 0 pour « + »",
            color = c.textSecondary, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(enabled: Boolean, label: String, onClick: () -> Unit): Modifier =
    combinedClickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
