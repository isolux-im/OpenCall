package im.isolux.opencall.ui.settings

import android.Manifest
import android.media.MediaPlayer
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import im.isolux.opencall.recording.RecordingCapability
import im.isolux.opencall.recording.RecordingCapabilityEvaluator
import im.isolux.opencall.recording.RecordingItem
import im.isolux.opencall.recording.RecordingNames
import im.isolux.opencall.recording.RecordingSupport
import im.isolux.opencall.recording.RecordingsRepository
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.DialerRole
import im.isolux.opencall.ui.components.EmptyState
import im.isolux.opencall.ui.components.GlassButton
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.InfoText
import im.isolux.opencall.ui.components.SectionHeader
import im.isolux.opencall.ui.components.SettingRow
import im.isolux.opencall.ui.components.StatusPill
import im.isolux.opencall.ui.components.SwitchRow
import im.isolux.opencall.ui.theme.Glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val CONSENT_TEXT =
    "Enregistrer une conversation sans en informer votre interlocuteur peut être illégal (en France, " +
        "l'article 226-1 du Code pénal sanctionne l'enregistrement de paroles privées sans consentement). " +
        "Vous devez informer chaque interlocuteur au début de l'appel et respecter la loi applicable. " +
        "OpenCall affiche un indicateur permanent pendant l'enregistrement et n'enregistre jamais à votre insu. " +
        "Les fichiers restent dans l'espace privé d'OpenCall sur cet appareil et ne sont envoyés nulle part, " +
        "sauf si vous les partagez vous-même."

private val RETENTION_OPTIONS = listOf(0 to "Conserver indéfiniment", 30 to "30 jours", 90 to "90 jours", 365 to "1 an")

@Composable
fun RecordingSettings(refreshKey: Int, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val c = Glass.colors
    val prefs = AppPrefs.get(context)
    val repo = RecordingsRepository.get(context)
    val capability = remember(refreshKey) { RecordingSupport.capability(context) }
    var listVersion by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<RecordingItem>>(emptyList()) }
    var totalSize by remember { mutableStateOf(0L) }
    var freeSpace by remember { mutableStateOf(0L) }
    LaunchedEffect(refreshKey, listVersion) {
        withContext(Dispatchers.IO) {
            val list = repo.list()
            val total = repo.totalSize()
            val free = repo.freeSpace()
            withContext(Dispatchers.Main) { items = list; totalSize = total; freeSpace = free }
        }
    }
    var consentDialog by remember { mutableStateOf(false) }
    var autoDialog by remember { mutableStateOf(false) }
    var deleteAllDialog by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) onMessage("Accès au micro refusé : l'enregistrement reste désactivé.")
        listVersion++
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { listVersion++ }

    Column {
        // --- Compatibilité réelle de l'appareil
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                when (capability) {
                    is RecordingCapability.Unsupported -> {
                        StatusPill("Non pris en charge", c.textSecondary)
                        Spacer(Modifier.height(10.dp))
                        Text(RecordingCapabilityEvaluator.UNSUPPORTED_MESSAGE, color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(capability.reason, color = c.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    RecordingCapability.NeedsDefaultDialer -> {
                        StatusPill("Rôle Téléphone requis", c.warning)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Cet appareil autorise la capture audio des appels, mais OpenCall doit être l'application Téléphone par défaut pour savoir quand un appel est en cours.",
                            color = c.textSecondary, style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        GlassButton("Utiliser OpenCall comme application Téléphone", Modifier.fillMaxWidth()) {
                            DialerRole.createDialerRequest(context)?.let(roleLauncher::launch)
                        }
                    }
                    RecordingCapability.NeedsMicrophonePermission -> {
                        StatusPill("Autorisation micro requise", c.warning)
                        Spacer(Modifier.height(10.dp))
                        GlassButton("Autoriser le micro", Modifier.fillMaxWidth()) { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    }
                    RecordingCapability.Supported -> StatusPill("Pris en charge sur cet appareil", c.callGreen)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vérifié sur : ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}).",
                    color = c.textSecondary, style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        // --- Réglages : uniquement si l'enregistrement fonctionne réellement
        if (capability == RecordingCapability.Supported) {
            SectionHeader("Enregistrement")
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                SwitchRow(
                    "Activer l'enregistrement des appels",
                    "Affiche le bouton « Enregistrer » pendant un appel",
                    Icons.Rounded.FiberManualRecord,
                    checked = prefs.recordingEnabled,
                ) { wanted ->
                    if (wanted && !prefs.recordingConsentAcknowledged) consentDialog = true
                    else {
                        prefs.recordingEnabled = wanted
                        if (!wanted) prefs.recordingAuto = false
                    }
                }
                if (prefs.recordingEnabled) {
                    GlassDivider()
                    SettingRow("Enregistrement manuel", "Bouton « Enregistrer » sur l'écran d'appel", Icons.Rounded.TouchApp)
                    GlassDivider()
                    SwitchRow(
                        "Enregistrement automatique",
                        "Démarre dès que l'appel est établi ; arrêt possible à tout moment",
                        Icons.Rounded.FiberManualRecord,
                        checked = prefs.recordingAuto,
                    ) { wanted -> if (wanted) autoDialog = true else prefs.recordingAuto = false }
                }
            }
        }

        // --- Information légale et confidentialité (toujours visible)
        SectionHeader("Consentement et confidentialité")
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            SettingRow("Informez vos interlocuteurs", CONSENT_TEXT, Icons.Rounded.Gavel)
            GlassDivider()
            SettingRow(
                "Aucun contournement",
                "OpenCall n'utilise pas de service d'accessibilité ni d'enregistrement par le haut-parleur pour contourner les restrictions d'Android.",
                Icons.Rounded.Gavel,
            )
        }

        // --- Liste et stockage : affichés s'il existe des enregistrements ou si la fonction est disponible
        if (items.isNotEmpty() || capability == RecordingCapability.Supported) {
            SectionHeader("Enregistrements (${items.size})")
            RecordingsList(items, onChanged = { listVersion++ }, onMessage = onMessage)

            SectionHeader("Stockage")
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                SettingRow(
                    "Espace utilisé : ${RecordingNames.formatSize(totalSize)}",
                    "Espace libre sur l'appareil : ${RecordingNames.formatSize(freeSpace)}",
                    Icons.Rounded.Storage,
                )
                GlassDivider()
                Text("Durée de conservation", color = c.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                RETENTION_OPTIONS.forEach { (days, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            prefs.recordingRetentionDays = days
                            repo.applyRetention(); listVersion++
                        }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = prefs.recordingRetentionDays == days, onClick = {
                            prefs.recordingRetentionDays = days
                            repo.applyRetention(); listVersion++
                        })
                        Text(label, color = c.textPrimary)
                    }
                }
                if (items.isNotEmpty()) {
                    GlassButton("Supprimer tous les enregistrements", Modifier.fillMaxWidth().padding(16.dp), tint = c.callRed) {
                        deleteAllDialog = true
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (consentDialog) {
        AlertDialog(
            onDismissRequest = { consentDialog = false },
            title = { Text("Avant d'activer l'enregistrement") },
            text = { Text(CONSENT_TEXT) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.recordingConsentAcknowledged = true
                    prefs.recordingEnabled = true
                    consentDialog = false
                }) { Text("J'ai compris, activer") }
            },
            dismissButton = { TextButton(onClick = { consentDialog = false }) { Text("Annuler") } },
        )
    }
    if (autoDialog) {
        AlertDialog(
            onDismissRequest = { autoDialog = false },
            title = { Text("Enregistrement automatique") },
            text = {
                Text(
                    "Tous les appels établis seront enregistrés automatiquement. Un indicateur reste affiché pendant l'appel et une notification permanente signale l'enregistrement. " +
                        "Vous restez tenu d'informer chaque interlocuteur.",
                )
            },
            confirmButton = { TextButton(onClick = { prefs.recordingAuto = true; autoDialog = false }) { Text("Activer") } },
            dismissButton = { TextButton(onClick = { autoDialog = false }) { Text("Annuler") } },
        )
    }
    if (deleteAllDialog) {
        AlertDialog(
            onDismissRequest = { deleteAllDialog = false },
            title = { Text("Tout supprimer ?") },
            text = { Text("Les ${items.size} enregistrements seront définitivement supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    val n = repo.deleteAll()
                    deleteAllDialog = false
                    listVersion++
                    onMessage("$n enregistrement(s) supprimé(s).")
                }) { Text("Supprimer", color = c.callRed) }
            },
            dismissButton = { TextButton(onClick = { deleteAllDialog = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun RecordingsList(items: List<RecordingItem>, onChanged: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val c = Glass.colors
    val repo = RecordingsRepository.get(context)
    var playing by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    var renameTarget by remember { mutableStateOf<RecordingItem?>(null) }
    var deleteTarget by remember { mutableStateOf<RecordingItem?>(null) }

    fun stopPlayback() {
        player[0]?.release()
        player[0] = null
        playing = null
        progress = 0f
    }

    DisposableEffect(Unit) { onDispose { stopPlayback() } }
    LaunchedEffect(playing) {
        while (playing != null) {
            player[0]?.let { mp ->
                try {
                    if (mp.duration > 0) progress = mp.currentPosition.toFloat() / mp.duration
                } catch (e: IllegalStateException) { /* lecteur libéré */ }
            }
            delay(250)
        }
    }

    GlassCard(Modifier.padding(horizontal = 16.dp)) {
        if (items.isEmpty()) EmptyState("Aucun enregistrement réalisé par OpenCall.")
        items.forEachIndexed { i, item ->
            val isPlaying = playing == item.file.name
            Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (isPlaying) stopPlayback()
                        else {
                            stopPlayback()
                            try {
                                player[0] = MediaPlayer().apply {
                                    setDataSource(item.file.absolutePath)
                                    setOnCompletionListener { stopPlayback() }
                                    prepare()
                                    start()
                                }
                                playing = item.file.name
                            } catch (e: Exception) {
                                stopPlayback()
                                onMessage("Lecture impossible : fichier illisible.")
                            }
                        }
                    }) {
                        Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Écouter", tint = c.accent)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = c.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${DateUtils.formatDateTime(context, item.modifiedAt, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)} · " +
                                "${RecordingNames.formatDuration(item.durationMillis)} · ${RecordingNames.formatSize(item.sizeBytes)}",
                            color = c.textSecondary, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { renameTarget = item }) { Icon(Icons.Rounded.Edit, "Renommer", tint = c.textSecondary) }
                    IconButton(onClick = { context.startActivity(repo.shareIntent(item)) }) { Icon(Icons.Rounded.Share, "Partager", tint = c.textSecondary) }
                    IconButton(onClick = { deleteTarget = item }) { Icon(Icons.Rounded.Delete, "Supprimer", tint = c.callRed) }
                }
                if (isPlaying) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(end = 12.dp))
            }
            if (i < items.lastIndex) GlassDivider()
        }
    }

    renameTarget?.let { item ->
        var name by remember(item) { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renommer") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (playing == item.file.name) stopPlayback()
                    val ok = repo.rename(item, name)
                    onMessage(if (ok) "Enregistrement renommé." else "Nom invalide ou déjà utilisé.")
                    renameTarget = null
                    onChanged()
                }) { Text("Renommer") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Annuler") } },
        )
    }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Supprimer l'enregistrement ?") },
            text = { Text(item.name) },
            confirmButton = {
                TextButton(onClick = {
                    if (playing == item.file.name) stopPlayback()
                    onMessage(if (repo.delete(item)) "Enregistrement supprimé." else "Suppression impossible.")
                    deleteTarget = null
                    onChanged()
                }) { Text("Supprimer", color = c.callRed) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Annuler") } },
        )
    }
}
