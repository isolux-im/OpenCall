package im.isolux.opencall.lock

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import im.isolux.opencall.settings.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockAvailability {
    /** Biométrie et/ou code de l'appareil utilisables. */
    AVAILABLE,

    /** Aucun verrouillage sécurisé configuré sur l'appareil. */
    NO_SECURE_LOCK,

    UNAVAILABLE,
}

/**
 * Verrouillage facultatif de l'interface principale d'OpenCall.
 *
 * Il ne concerne que [im.isolux.opencall.MainActivity] : l'écran d'appel, les notifications
 * d'appel, l'InCallService et le service de filtrage ne sont jamais verrouillés.
 */
object AppLockManager {

    /** BiometricPrompt accepte BIOMETRIC_WEAK | DEVICE_CREDENTIAL sur toutes les versions supportées. */
    const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var neverUnlocked = true
    private var backgroundedAt: Long? = null
    private var lastInteractionAt = SystemClock.elapsedRealtime()

    /** Vrai pendant l'affichage de BiometricPrompt (peut mettre l'activité en arrière-plan). */
    var authInProgress = false
        private set

    fun availability(context: Context): LockAvailability =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> LockAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> LockAvailability.NO_SECURE_LOCK
            else -> LockAvailability.UNAVAILABLE
        }

    fun hasBiometrics(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS ||
            BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun onForeground(context: Context) {
        if (authInProgress) return
        val prefs = AppPrefs.get(context)
        if (prefs.lockEnabled && availability(context) != LockAvailability.AVAILABLE) {
            // Verrouillage Android supprimé entre-temps : on ne peut plus authentifier, on désactive.
            prefs.lockEnabled = false
        }
        if (LockPolicy.shouldLockOnResume(prefs.lockEnabled, neverUnlocked, backgroundedAt, SystemClock.elapsedRealtime(), prefs.lockTimeout)) {
            _locked.value = true
        }
        backgroundedAt = null
        lastInteractionAt = SystemClock.elapsedRealtime()
    }

    fun onBackground() {
        if (authInProgress) return
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onUserInteraction() {
        lastInteractionAt = SystemClock.elapsedRealtime()
    }

    fun checkInactivity(context: Context) {
        val prefs = AppPrefs.get(context)
        if (!_locked.value && LockPolicy.shouldLockForInactivity(prefs.lockEnabled, lastInteractionAt, SystemClock.elapsedRealtime(), prefs.lockTimeout)) {
            _locked.value = true
        }
    }

    fun lockNow() {
        _locked.value = true
    }

    /** Lors de l'activation, l'utilisateur est déjà authentifié : pas de verrouillage immédiat. */
    fun markUnlocked() {
        neverUnlocked = false
        _locked.value = false
        lastInteractionAt = SystemClock.elapsedRealtime()
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authInProgress = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authInProgress = false
                    onError(errString.toString())
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        authInProgress = true
        prompt.authenticate(info)
    }
}
