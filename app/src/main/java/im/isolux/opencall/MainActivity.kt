package im.isolux.opencall

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import im.isolux.opencall.blocking.EmergencyNumbers
import im.isolux.opencall.blocking.PhoneNumbers
import im.isolux.opencall.lock.AppLockManager
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.ui.OpenCallRoot
import im.isolux.opencall.ui.lock.LockScreen
import im.isolux.opencall.ui.theme.OpenCallTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    /** Numéro reçu via ACTION_DIAL / tel: à afficher dans le clavier. */
    private val pendingDial = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(5_000)
                    AppLockManager.checkInactivity(this@MainActivity)
                }
            }
        }
        lifecycleScope.launch {
            AppPrefs.get(this@MainActivity).version.collect { applySecureFlag() }
        }

        setContent {
            OpenCallTheme {
                val locked by AppLockManager.locked.collectAsStateWithLifecycle()
                val dial by pendingDial.collectAsStateWithLifecycle()
                if (locked) {
                    val emergencyPrefill = dial?.takeIf { EmergencyNumbers.isEmergency(this, it) }
                    LockScreen(onUnlock = ::unlock, emergencyPrefill = emergencyPrefill)
                } else {
                    OpenCallRoot(dial) { pendingDial.value = null }
                }
            }
        }
    }

    private fun unlock() {
        AppLockManager.authenticate(
            this,
            title = "Déverrouiller OpenCall",
            subtitle = "Biométrie ou code de l'appareil",
            onSuccess = { AppLockManager.markUnlocked() },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_DIAL || intent?.action == Intent.ACTION_VIEW) {
            val number = intent.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart
            pendingDial.value = number?.let { PhoneNumbers.normalize(it) } ?: ""
        }
    }

    override fun onStart() {
        super.onStart()
        AppLockManager.onForeground(this)
        applySecureFlag()
    }

    override fun onStop() {
        if (!isChangingConfigurations) AppLockManager.onBackground()
        super.onStop()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        AppLockManager.onUserInteraction()
    }

    /** Verrouillage actif : contenu masqué dans les applications récentes et captures d'écran bloquées. */
    private fun applySecureFlag() {
        if (AppPrefs.get(this).lockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
