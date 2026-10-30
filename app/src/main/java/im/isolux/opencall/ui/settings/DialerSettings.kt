package im.isolux.opencall.ui.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.PhoneForwarded
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import im.isolux.opencall.telecom.CallNotifications
import im.isolux.opencall.telecom.DialerRole
import im.isolux.opencall.ui.components.GlassButton
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.InfoText
import im.isolux.opencall.ui.components.SectionHeader
import im.isolux.opencall.ui.components.SettingRow
import im.isolux.opencall.ui.components.StatusPill
import im.isolux.opencall.ui.theme.Glass

/** Fonctions d'OpenCall et rôle Android nécessaire. */
private val FEATURES_WITHOUT_ROLE = listOf(
    "Historique des appels (avec votre autorisation)",
    "Contacts et favoris",
    "Clavier : les appels sont passés via Android",
    "Recherche",
    "Verrouillage d'OpenCall",
    "Liste noire : gestion de la liste (application uniquement avec un rôle)",
)

private val FEATURES_WITH_ROLE = listOf(
    "Écran d'appel OpenCall pour les appels entrants et sortants",
    "Répondre, refuser, raccrocher, mettre en attente, muet, haut-parleur, DTMF",
    "Liste de blocage système Android (BlockedNumberContract)",
    "Règles de filtrage (numéros masqués, inconnus)",
    "Enregistrement des appels, uniquement si l'appareil fournit l'audio de l'appel",
)

@Composable
fun DialerSettings(refreshKey: Int, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val c = Glass.colors
    val available = remember(refreshKey) { DialerRole.isDialerAvailable(context) }
    val isDialer = remember(refreshKey) { DialerRole.isDefaultDialer(context) }
    val currentLabel = remember(refreshKey) {
        val pkg = context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        pkg?.let {
            try {
                context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                it
            }
        }
    }
    val notificationsOk = remember(refreshKey) { CallNotifications.canPost(context) }
    var declined by rememberSaveable { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && DialerRole.isDefaultDialer(context)) {
            declined = false
            onMessage("OpenCall est maintenant votre application Téléphone.")
            if (Build.VERSION.SDK_INT >= 33 && !CallNotifications.canPost(context)) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            declined = true
            onMessage("Rôle Téléphone non accordé : votre composeur actuel est conservé.")
        }
    }

    Column {
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                if (isDialer) {
                    StatusPill("OpenCall est l'application Téléphone par défaut", c.callGreen)
                } else {
                    StatusPill("Composeur par défaut : ${currentLabel ?: "aucun"}", c.textSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isDialer) {
                        "OpenCall gère l'écran d'appel, les appels entrants et la liste de blocage système. " +
                            "Android ne permet pas à une application de rendre ce rôle elle-même : utilisez les paramètres Android pour choisir un autre composeur."
                    } else {
                        "Vous pouvez utiliser OpenCall comme application Téléphone ou conserver votre composeur actuel. " +
                            "Ce choix vous appartient et peut être modifié à tout moment dans les paramètres Android."
                    },
                    color = c.textSecondary, style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                when {
                    !available -> InfoText("Le rôle Téléphone n'est pas disponible sur cet appareil (pas de téléphonie ou profil restreint).", color = c.warning)
                    isDialer -> GlassButton("Choisir une autre application Téléphone", Modifier.fillMaxWidth()) {
                        context.startActivity(DialerRole.defaultAppsSettingsIntent())
                    }
                    else -> GlassButton("Utiliser OpenCall comme application Téléphone", Modifier.fillMaxWidth(), tint = c.callGreen) {
                        DialerRole.createDialerRequest(context)?.let(roleLauncher::launch)
                    }
                }
                if (declined && !isDialer) {
                    Spacer(Modifier.height(8.dp))
                    InfoText(
                        "Si la fenêtre Android ne s'affiche plus (refus répétés), le choix reste possible dans Paramètres Android › Applications › Applications par défaut › Application de téléphone.",
                    )
                    GlassButton("Ouvrir les applications par défaut", Modifier.fillMaxWidth()) {
                        context.startActivity(DialerRole.defaultAppsSettingsIntent())
                    }
                }
            }
        }

        if (isDialer && !notificationsOk) {
            SectionHeader("Attention")
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                InfoText(
                    "Les notifications d'OpenCall sont désactivées : l'écran d'appel entrant peut ne pas s'afficher sur l'écran verrouillé. Activez-les pour recevoir vos appels correctement.",
                    color = c.warning,
                )
                GlassButton("Autoriser les notifications", Modifier.fillMaxWidth().padding(16.dp)) {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        SectionHeader(if (isDialer) "Fonctions actives" else "Disponible sans le rôle Téléphone")
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            FEATURES_WITHOUT_ROLE.forEachIndexed { i, f ->
                SettingRow(f, icon = Icons.Rounded.Check, iconTint = c.callGreen)
                if (i < FEATURES_WITHOUT_ROLE.lastIndex) GlassDivider()
            }
        }
        SectionHeader("Nécessite le rôle Téléphone par défaut")
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            FEATURES_WITH_ROLE.forEachIndexed { i, f ->
                SettingRow(
                    f,
                    subtitle = if (isDialer) null else "Indisponible tant que le rôle n'est pas accordé",
                    icon = if (isDialer) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.PhoneForwarded,
                    iconTint = if (isDialer) c.callGreen else c.textSecondary,
                )
                if (i < FEATURES_WITH_ROLE.lastIndex) GlassDivider()
            }
        }
        if (!isDialer) {
            InfoText(
                "Sans ce rôle, les appels entrants restent gérés par votre composeur actuel : OpenCall ne les intercepte pas et ne prétend pas les gérer.",
                Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
