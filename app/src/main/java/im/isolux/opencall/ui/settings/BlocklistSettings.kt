package im.isolux.opencall.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import im.isolux.opencall.blocking.BlockRequestResult
import im.isolux.opencall.blocking.BlockingEnforcement
import im.isolux.opencall.blocking.BlocklistLogic
import im.isolux.opencall.blocking.BlocklistRepository
import im.isolux.opencall.blocking.PhoneNumbers
import im.isolux.opencall.data.ContactsRepository
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.DialerRole
import im.isolux.opencall.ui.blockResultMessage
import im.isolux.opencall.ui.components.EmptyState
import im.isolux.opencall.ui.components.GlassButton
import im.isolux.opencall.ui.components.GlassCard
import im.isolux.opencall.ui.components.GlassDivider
import im.isolux.opencall.ui.components.InfoText
import im.isolux.opencall.ui.components.SectionHeader
import im.isolux.opencall.ui.components.SettingRow
import im.isolux.opencall.ui.components.StatusPill
import im.isolux.opencall.ui.components.SwitchRow
import im.isolux.opencall.ui.components.glassSurface
import im.isolux.opencall.ui.theme.Glass

@Composable
fun BlocklistSettings(refreshKey: Int, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val c = Glass.colors
    val repo = BlocklistRepository.get(context)
    val prefs = AppPrefs.get(context)
    val entries by repo.entries.collectAsStateWithLifecycle()
    val enforcement = remember(refreshKey) { repo.enforcement() }
    val rules = remember(refreshKey) { prefs.screeningRules }
    val screeningAvailable = remember(refreshKey) { DialerRole.isScreeningAvailable(context) }
    val hasContacts = remember(refreshKey) { ContactsRepository.hasPermission(context) }
    var newNumber by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(refreshKey) { repo.refresh() }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (repo.enforcement() != BlockingEnforcement.NONE) repo.syncWithSystem()
        repo.refresh()
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) prefs.screeningRules = prefs.screeningRules.copy(blockUnknownNumbers = true)
    }

    Column {
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                when (enforcement) {
                    BlockingEnforcement.SYSTEM -> {
                        StatusPill("Liste de blocage système Android", c.callGreen)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "OpenCall est l'application Téléphone : les numéros sont enregistrés dans la liste de blocage d'Android, appliquée par le système (et partagée avec les applications compatibles, comme les SMS).",
                            color = c.textSecondary, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    BlockingEnforcement.SCREENING_ONLY -> {
                        StatusPill("Filtrage OpenCall actif", c.warning)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "OpenCall filtre les appels via le service de filtrage Android. Limite imposée par Android : seuls les appels de numéros absents de vos contacts lui sont transmis. Pour bloquer un contact, définissez OpenCall comme application Téléphone.",
                            color = c.textSecondary, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    BlockingEnforcement.NONE -> {
                        StatusPill("Liste non appliquée", c.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Les numéros sont enregistrés, mais aucun appel n'est bloqué : OpenCall n'est ni l'application Téléphone par défaut ni le service de filtrage des appels. Choisissez l'une des options ci-dessous si vous le souhaitez.",
                            color = c.textSecondary, style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(12.dp))
                        GlassButton("Utiliser OpenCall comme application Téléphone", Modifier.fillMaxWidth()) {
                            DialerRole.createDialerRequest(context)?.let(roleLauncher::launch)
                        }
                        if (screeningAvailable) {
                            Spacer(Modifier.height(8.dp))
                            GlassButton("Utiliser OpenCall pour le filtrage uniquement", Modifier.fillMaxWidth()) {
                                DialerRole.createScreeningRequest(context)?.let(roleLauncher::launch)
                            }
                        }
                    }
                }
                if (enforcement == BlockingEnforcement.SYSTEM) {
                    Spacer(Modifier.height(12.dp))
                    GlassButton("Synchroniser avec la liste système", Modifier.fillMaxWidth()) {
                        val plan = repo.syncWithSystem()
                        onMessage(
                            if (plan == null) "Liste système inaccessible."
                            else "Synchronisé : ${plan.pushToSystem.size} ajouté(s) au système, ${plan.importToLocal.size} importé(s).",
                        )
                    }
                }
            }
        }

        SectionHeader("Ajouter un numéro")
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newNumber,
                    onValueChange = { newNumber = it },
                    placeholder = { Text("Numéro de téléphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                GlassButton("Bloquer", tint = c.callRed, enabled = newNumber.isNotBlank()) {
                    val result = repo.block(newNumber)
                    onMessage(blockResultMessage(result, repo.enforcement()))
                    if (result == BlockRequestResult.Ok) newNumber = ""
                }
            }
            InfoText("Les numéros d'urgence ne peuvent pas être bloqués. Vous pouvez aussi bloquer depuis l'historique ou une fiche contact.")
        }

        SectionHeader("Numéros bloqués (${entries.size})")
        if (entries.size > 5) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Rechercher dans la liste noire") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).glassSurface(RoundedCornerShape(18.dp)),
            )
        }
        val shown = BlocklistLogic.search(entries, query)
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            if (shown.isEmpty()) {
                EmptyState(if (entries.isEmpty()) "Aucun numéro bloqué." else "Aucun numéro ne correspond.")
            }
            shown.forEachIndexed { i, number ->
                SettingRow(
                    PhoneNumbers.format(number),
                    icon = Icons.Rounded.Block,
                    iconTint = c.callRed,
                ) {
                    IconButton(onClick = {
                        repo.unblock(number)
                        onMessage("${PhoneNumbers.format(number)} débloqué.")
                    }) { Icon(Icons.Rounded.Delete, "Débloquer ${PhoneNumbers.format(number)}", tint = c.textSecondary) }
                }
                if (i < shown.lastIndex) GlassDivider()
            }
        }

        SectionHeader("Règles de filtrage")
        GlassCard(Modifier.padding(horizontal = 16.dp)) {
            val active = enforcement != BlockingEnforcement.NONE
            SwitchRow(
                "Appliquer la liste noire",
                if (enforcement == BlockingEnforcement.SYSTEM) "Toujours appliquée par Android pour la liste système" else null,
                Icons.Rounded.Block,
                checked = rules.blockListEnabled,
                enabled = active && enforcement != BlockingEnforcement.SYSTEM,
            ) { prefs.screeningRules = rules.copy(blockListEnabled = it) }
            GlassDivider()
            SwitchRow(
                "Bloquer les numéros masqués",
                "Appels sans numéro présenté",
                Icons.Rounded.VisibilityOff,
                checked = rules.blockHiddenNumbers,
                enabled = active,
            ) { prefs.screeningRules = rules.copy(blockHiddenNumbers = it) }
            GlassDivider()
            SwitchRow(
                "Bloquer les numéros inconnus",
                if (hasContacts) "Numéros absents de vos contacts" else "Nécessite l'accès aux contacts",
                Icons.Rounded.PersonOff,
                checked = rules.blockUnknownNumbers && hasContacts,
                enabled = active,
            ) { wanted ->
                if (wanted && !hasContacts) contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                else prefs.screeningRules = rules.copy(blockUnknownNumbers = wanted)
            }
            if (!active) {
                InfoText("Les règles nécessitent le rôle Téléphone ou le rôle de filtrage des appels : elles ne sont pas proposées sans eux.")
            } else {
                InfoText("Les appels d'urgence et les appels sortants ne sont jamais filtrés. Les appels bloqués restent visibles dans l'historique.")
            }
        }
        if (enforcement == BlockingEnforcement.SYSTEM) {
            InfoText("Astuce : « Synchroniser » fusionne la liste système et la copie locale d'OpenCall, utile après un changement de composeur.", Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}
