package im.isolux.opencall.telecom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import im.isolux.opencall.blocking.PhoneNumbers
import im.isolux.opencall.data.ContactsRepository
import im.isolux.opencall.recording.CallRecorder
import im.isolux.opencall.recording.RecorderState
import im.isolux.opencall.recording.RecordingNames
import im.isolux.opencall.recording.RecordingSupport
import im.isolux.opencall.ui.components.Avatar
import im.isolux.opencall.ui.components.GlassBackground
import im.isolux.opencall.ui.components.glassSurface
import im.isolux.opencall.ui.keypad.DialPad
import im.isolux.opencall.ui.theme.Glass
import im.isolux.opencall.ui.theme.OpenCallTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Écran d'appel. Volontairement indépendant du verrouillage d'OpenCall : il s'affiche
 * au-dessus de l'écran verrouillé et permet toujours de répondre, refuser ou raccrocher.
 */
class InCallActivity : ComponentActivity() {

    private var proximityLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val pm = getSystemService(PowerManager::class.java)
        if (pm?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
            proximityLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "opencall:proximity")
        }
        handleAction(intent)
        setContent {
            OpenCallTheme {
                val calls by CallManager.calls.collectAsStateWithLifecycle()
                val audio by CallManager.audioState.collectAsStateWithLifecycle()
                LaunchedEffect(calls) {
                    if (calls.none { it.state != Call.STATE_DISCONNECTED }) {
                        delay(800)
                        if (CallManager.calls.value.none { it.state != Call.STATE_DISCONNECTED }) finish()
                    }
                }
                val primary = CallManager.primaryCall()
                LaunchedEffect(primary?.state, audio?.route) {
                    updateProximity(primary?.state == Call.STATE_ACTIVE && audio?.route == CallAudioState.ROUTE_EARPIECE)
                }
                InCallScreen(calls, primary, audio)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAction(intent)
    }

    /** « Répondre » depuis la notification d'appel entrant. */
    private fun handleAction(intent: Intent?) {
        if (intent?.action == ACTION_ANSWER) {
            CallManager.ringingCall()?.let { CallManager.answer(it.call) }
            intent.action = null
        }
    }

    private fun updateProximity(enable: Boolean) {
        val lock = proximityLock ?: return
        if (enable && !lock.isHeld) lock.acquire(60 * 60 * 1000L)
        if (!enable && lock.isHeld) lock.release()
    }

    override fun onDestroy() {
        updateProximity(false)
        super.onDestroy()
    }

    companion object {
        const val ACTION_ANSWER = "im.isolux.opencall.action.ANSWER"

        fun intent(context: Context): Intent = Intent(context, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}

@Composable
internal fun InCallScreen(calls: List<CallUi>, primary: CallUi?, audio: CallAudioState?) {
    val c = Glass.colors
    val context = LocalContext.current
    var showPad by remember { mutableStateOf(false) }
    var dtmf by remember { mutableStateOf("") }
    val recorder by CallRecorder.state.collectAsStateWithLifecycle()

    var lookedUpName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(primary?.number) {
        lookedUpName = primary?.number?.let { n -> withContext(Dispatchers.IO) { ContactsRepository.lookupName(context, n) } }
    }
    val name = primary?.displayName ?: lookedUpName
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis(); delay(1000)
        }
    }

    GlassBackground {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Avatar(name, size = 96.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                name ?: primary?.number?.let { PhoneNumbers.format(it) } ?: "Numéro masqué",
                color = c.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            )
            if (name != null && primary?.number != null) {
                Text(PhoneNumbers.format(primary.number), color = c.textSecondary)
            }
            Spacer(Modifier.height(6.dp))
            Text(statusText(primary, now), color = c.textSecondary, style = MaterialTheme.typography.titleMedium)

            val other = calls.filter { it !== primary && it.state != Call.STATE_DISCONNECTED }
            other.forEach { o ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "${o.displayName ?: o.number?.let { PhoneNumbers.format(it) } ?: "Numéro masqué"} — ${statusText(o, now)}",
                    color = c.textSecondary, style = MaterialTheme.typography.bodySmall,
                )
            }

            when (val r = recorder) {
                is RecorderState.Recording -> {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.glassSurface(RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.FiberManualRecord, null, tint = c.callRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Enregistrement ${RecordingNames.formatDuration(now - r.startedAt)} — informez votre interlocuteur",
                            color = c.textPrimary, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is RecorderState.Failed -> {
                    Spacer(Modifier.height(12.dp))
                    Text(r.message, color = c.warning, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                RecorderState.Idle -> Unit
            }

            Spacer(Modifier.weight(1f))

            if (primary == null) return@Column

            if (primary.state == Call.STATE_RINGING) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundAction("Refuser", Icons.Rounded.CallEnd, c.callRed) { CallManager.reject(primary.call) }
                    RoundAction("Répondre", Icons.Rounded.Call, c.callGreen) { CallManager.answer(primary.call) }
                }
            } else {
                if (showPad) {
                    Text(dtmf, color = c.textPrimary, fontSize = 24.sp)
                    DialPad(onDigit = { d ->
                        dtmf += d
                        CallManager.playDtmf(primary.call, d)
                    })
                } else {
                    val muted = audio?.isMuted == true
                    val speaker = audio?.route == CallAudioState.ROUTE_SPEAKER
                    val recordingAvailable = RecordingSupport.isUsable(context)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ToggleAction("Muet", Icons.Rounded.MicOff, muted) { CallManager.setMuted(!muted) }
                        ToggleAction("Clavier", Icons.Rounded.Dialpad, false) { showPad = true }
                        ToggleAction("Haut-parleur", Icons.AutoMirrored.Rounded.VolumeUp, speaker) { CallManager.setSpeaker(!speaker) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ToggleAction(
                            "Attente", Icons.Rounded.Pause, primary.state == Call.STATE_HOLDING,
                            enabled = primary.canHold,
                        ) { CallManager.toggleHold(primary.call) }
                        // Le bouton n'existe que si l'enregistrement fonctionne réellement sur l'appareil.
                        if (recordingAvailable) {
                            val recording = recorder is RecorderState.Recording
                            ToggleAction("Enregistrer", Icons.Rounded.FiberManualRecord, recording, enabled = primary.state == Call.STATE_ACTIVE || recording) {
                                if (recording) CallRecorder.stop(context, primary.call) else CallRecorder.start(context, primary.number)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (showPad) {
                        Text("Masquer", color = c.textSecondary, modifier = Modifier.clickable { showPad = false }.padding(16.dp))
                    }
                    RoundAction("Raccrocher", Icons.Rounded.CallEnd, c.callRed) { CallManager.hangUp(primary.call) }
                }
            }
            val ringingSecond = calls.firstOrNull { it !== primary && it.state == Call.STATE_RINGING }
            if (ringingSecond != null) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundAction("Refuser", Icons.Rounded.CallEnd, c.callRed) { CallManager.reject(ringingSecond.call) }
                    RoundAction("Répondre", Icons.Rounded.Call, c.callGreen) { CallManager.answer(ringingSecond.call) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun statusText(call: CallUi?, now: Long): String = when (call?.state) {
    null -> "Appel terminé"
    Call.STATE_RINGING -> "Appel entrant"
    Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_NEW, Call.STATE_SELECT_PHONE_ACCOUNT -> "Appel en cours de composition…"
    Call.STATE_ACTIVE -> if (call.connectTimeMillis > 0) RecordingNames.formatDuration(now - call.connectTimeMillis) else "En communication"
    Call.STATE_HOLDING -> "En attente"
    Call.STATE_DISCONNECTING -> "Fin de l'appel…"
    Call.STATE_DISCONNECTED -> "Appel terminé"
    else -> ""
}

@Composable
private fun RoundAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(76.dp).glassSurface(CircleShape, elevation = 6.dp).background(color.copy(alpha = 0.9f))
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Glass.colors.textPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ToggleAction(label: String, icon: ImageVector, active: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Glass.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
        Box(
            Modifier.size(64.dp).glassSurface(CircleShape)
                .background(if (active) c.accent.copy(alpha = 0.85f) else Color.Transparent)
                .clickable(enabled = enabled, role = Role.Switch, onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, label,
                tint = when {
                    !enabled -> c.textSecondary.copy(alpha = 0.5f)
                    active -> if (c.isDark) Color.Black else Color.White
                    else -> c.textPrimary
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = c.textSecondary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

