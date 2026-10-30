package im.isolux.opencall.recording

import android.content.Context
import android.content.Intent
import android.telecom.Call
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.CallUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RecorderState {
    data object Idle : RecorderState
    data class Recording(val startedAt: Long, val fileName: String, val automatic: Boolean) : RecorderState
    data class Failed(val message: String) : RecorderState
}

/** Pilotage de [RecordingService]. Aucun enregistrement ne démarre sans capacité réelle et accord préalable. */
object CallRecorder {

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    /** Appels pour lesquels l'utilisateur a arrêté l'enregistrement automatique. */
    private val autoSkipped = mutableSetOf<Call>()

    internal fun update(newState: RecorderState) {
        _state.value = newState
    }

    fun start(context: Context, number: String?, automatic: Boolean = false): Boolean {
        if (!RecordingSupport.isUsable(context)) return false
        if (_state.value is RecorderState.Recording) return true
        val intent = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_START)
            .putExtra(RecordingService.EXTRA_NUMBER, number)
            .putExtra(RecordingService.EXTRA_AUTOMATIC, automatic)
        return try {
            context.startForegroundService(intent)
            true
        } catch (e: RuntimeException) {
            _state.value = RecorderState.Failed("Impossible de démarrer l'enregistrement : ${e.message}")
            false
        }
    }

    fun stop(context: Context, call: Call? = null) {
        if (call != null) autoSkipped += call
        if (_state.value !is RecorderState.Recording) return
        try {
            context.startService(Intent(context, RecordingService::class.java).setAction(RecordingService.ACTION_STOP))
        } catch (e: IllegalStateException) {
            // Service déjà arrêté par le système : rien à enregistrer.
            _state.value = RecorderState.Idle
        }
    }

    /** Appelé par l'InCallService à chaque changement d'état des appels. */
    fun onCallsChanged(context: Context, calls: List<CallUi>) {
        autoSkipped.retainAll(calls.map { it.call }.toSet())
        val live = calls.filter { it.state == Call.STATE_ACTIVE || it.state == Call.STATE_HOLDING }
        if (live.isEmpty()) {
            stop(context)
            return
        }
        val prefs = AppPrefs.get(context)
        val active = live.firstOrNull { it.state == Call.STATE_ACTIVE } ?: return
        if (prefs.recordingAuto && active.call !in autoSkipped && _state.value !is RecorderState.Recording) {
            start(context, active.number, automatic = true)
        }
    }
}
