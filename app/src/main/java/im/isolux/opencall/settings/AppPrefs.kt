package im.isolux.opencall.settings

import android.content.Context
import android.content.SharedPreferences
import im.isolux.opencall.blocking.ScreeningRules
import im.isolux.opencall.lock.LockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Préférences d'OpenCall, exposées en StateFlow pour l'interface. */
class AppPrefs private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("opencall_settings", Context.MODE_PRIVATE)

    private val _version = MutableStateFlow(0)
    /** Incrémenté à chaque modification : permet à Compose de relire les valeurs. */
    val version: StateFlow<Int> = _version.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _version.value++ }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    // --- Verrouillage
    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    var lockTimeout: LockTimeout
        get() = LockTimeout.fromName(prefs.getString(KEY_LOCK_TIMEOUT, null))
        set(value) = prefs.edit().putString(KEY_LOCK_TIMEOUT, value.name).apply()

    // --- Liste noire et filtrage
    var localBlocklist: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKLIST, emptySet())!!.toSet()
        set(value) = prefs.edit().putStringSet(KEY_BLOCKLIST, value.toSet()).apply()

    var screeningRules: ScreeningRules
        get() = ScreeningRules(
            blockListEnabled = prefs.getBoolean(KEY_RULE_LIST, true),
            blockHiddenNumbers = prefs.getBoolean(KEY_RULE_HIDDEN, false),
            blockUnknownNumbers = prefs.getBoolean(KEY_RULE_UNKNOWN, false),
        )
        set(value) = prefs.edit()
            .putBoolean(KEY_RULE_LIST, value.blockListEnabled)
            .putBoolean(KEY_RULE_HIDDEN, value.blockHiddenNumbers)
            .putBoolean(KEY_RULE_UNKNOWN, value.blockUnknownNumbers)
            .apply()

    // --- Enregistrement
    var recordingEnabled: Boolean
        get() = prefs.getBoolean(KEY_REC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REC_ENABLED, value).apply()

    var recordingAuto: Boolean
        get() = prefs.getBoolean(KEY_REC_AUTO, false)
        set(value) = prefs.edit().putBoolean(KEY_REC_AUTO, value).apply()

    var recordingConsentAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_REC_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_REC_CONSENT, value).apply()

    var recordingAttemptFailed: Boolean
        get() = prefs.getBoolean(KEY_REC_FAILED, false)
        set(value) = prefs.edit().putBoolean(KEY_REC_FAILED, value).apply()

    /** Durée de conservation en jours (0 = illimitée). */
    var recordingRetentionDays: Int
        get() = prefs.getInt(KEY_REC_RETENTION, 0)
        set(value) = prefs.edit().putInt(KEY_REC_RETENTION, value).apply()

    companion object {
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout"
        private const val KEY_BLOCKLIST = "blocklist_local"
        private const val KEY_RULE_LIST = "rule_blocklist"
        private const val KEY_RULE_HIDDEN = "rule_hidden"
        private const val KEY_RULE_UNKNOWN = "rule_unknown"
        private const val KEY_REC_ENABLED = "recording_enabled"
        private const val KEY_REC_AUTO = "recording_auto"
        private const val KEY_REC_CONSENT = "recording_consent"
        private const val KEY_REC_FAILED = "recording_attempt_failed"
        private const val KEY_REC_RETENTION = "recording_retention_days"

        @Volatile private var instance: AppPrefs? = null

        fun get(context: Context): AppPrefs =
            instance ?: synchronized(this) { instance ?: AppPrefs(context).also { instance = it } }
    }
}
