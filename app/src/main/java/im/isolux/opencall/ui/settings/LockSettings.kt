package im.isolux.opencall.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import im.isolux.opencall.lock.AppLockManager
import im.isolux.opencall.lock.LockAvailability
import im.isolux.opencall.lock.LockTimeout
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.ui.components.GlassButton
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.InfoText
import im.isolux.opencall.ui.components.SectionHeader
import im.isolux.opencall.ui.components.SettingRow
import im.isolux.opencall.ui.components.SwitchRow
import im.isolux.opencall.ui.theme.Glass

@Composable
fun LockSettings(refreshKey: Int, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    // BiometricPrompt exige une FragmentActivity (MainActivity en est une).
    val activity = context as? FragmentActivity
    val c = Glass.colors
    val prefs = AppPrefs.get(context)
    val availability = remember(refreshKey) { AppLockManager.availability(context) }
    val biometrics = remember(refreshKey) { AppLockManager.hasBiometrics(context) }
    val enabled = remember(refreshKey) { prefs.lockEnabled }
    val timeout = remember(refreshKey) { prefs.lockTimeout }

    Column {
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            SwitchRow(
                title = "Verrouiller OpenCall",
                subtitle = when (availability) {
                    LockAvailability.AVAILABLE -> if (biometrics) "Biométrie ou code de l'appareil" else "Code, schéma ou mot de passe de l'appareil"
                    LockAvailability.NO_SECURE_LOCK -> "Aucun verrouillage d'écran Android configuré"
                    LockAvailability.UNAVAILABLE -> "Authentification indisponible sur cet appareil"
                },
                icon = Icons.Rounded.Lock,
                checked = enabled,
                enabled = availability == LockAvailability.AVAILABLE,
            ) { wanted ->
                AppLockManager.authenticate(
                    activity ?: return@SwitchRow,
                    title = if (wanted) "Activer le verrouillage" else "Désactiver le verrouillage",
                    subtitle = "Confirmez votre identité",
                    onSuccess = {
                        prefs.lockEnabled = wanted
                        AppLockManager.markUnlocked()
                        onMessage(if (wanted) "Verrouillage activé." else "Verrouillage désactivé.")
                    },
                    onError = { onMessage("Authentification annulée : $it") },
                )
            }
            if (availability == LockAvailability.NO_SECURE_LOCK) {
                InfoText("Configurez d'abord un code, un schéma ou une empreinte dans les paramètres de sécurité Android.")
                GlassButton("Ouvrir les paramètres de sécurité", Modifier.fillMaxWidth().padding(16.dp)) {
                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
            }
            if (enabled) {
                GlassDivider()
                SettingRow("Verrouiller maintenant", icon = Icons.Rounded.Lock, onClick = { AppLockManager.lockNow() })
            }
        }

        if (enabled) {
            SectionHeader("Verrouillage après inactivité")
            GlassCard(Modifier.padding(horizontal = 16.dp)) {
                LockTimeout.entries.forEachIndexed { i, option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { prefs.lockTimeout = option }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == timeout, onClick = { prefs.lockTimeout = option })
                        Text(option.label, color = c.textPrimary)
                    }
                    if (i < LockTimeout.entries.lastIndex) GlassDivider()
                }
            }
            InfoText("« Immédiatement » s'applique dès qu'OpenCall passe en arrière-plan ; au premier plan, l'inactivité est d'au moins une minute.")
        }

        SectionHeader("Garanties")
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            SettingRow(
                "Les appels ne sont jamais bloqués",
                "L'écran d'appel, les notifications d'appel entrant, répondre, refuser et raccrocher restent toujours accessibles, même OpenCall verrouillé.",
                Icons.Rounded.PhoneInTalk, c.callGreen,
            )
            GlassDivider()
            SettingRow(
                "Appels d'urgence toujours possibles",
                "L'écran de verrouillage d'OpenCall propose un accès direct aux numéros d'urgence, sans authentification.",
                Icons.Rounded.PhoneInTalk, c.callRed,
            )
            GlassDivider()
            SettingRow(
                "Protection des informations privées",
                "Verrouillé, OpenCall n'affiche ni historique, ni contacts, ni enregistrements. Le contenu est masqué dans les applications récentes et les captures d'écran sont bloquées.",
                Icons.Rounded.VisibilityOff,
            )
            GlassDivider()
            SettingRow(
                "Mécanismes officiels Android",
                "Authentification via BiometricPrompt (biométrie forte ou faible) ou code de l'appareil. OpenCall ne stocke aucun code ni donnée biométrique.",
                Icons.Rounded.Fingerprint,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
