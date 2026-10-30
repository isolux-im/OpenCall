package im.isolux.opencall.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.app.NotificationManagerCompat
import im.isolux.opencall.recording.CallRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service lié par Telecom uniquement lorsque OpenCall est l'application Téléphone par défaut.
 * Il n'est soumis à aucun verrouillage : l'écran d'appel et les notifications restent toujours
 * disponibles pour répondre, refuser ou raccrocher, y compris pour les appels d'urgence.
 */
class OpenCallInCallService : InCallService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null

    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
        observer = scope.launch {
            CallManager.calls.collect { calls ->
                val primary = CallManager.primaryCall()
                val posted = CallNotifications.show(this@OpenCallInCallService, primary)
                // Sans notification plein écran, un appel entrant doit quand même afficher l'écran d'appel.
                if (!posted && primary?.state == Call.STATE_RINGING) openInCallScreen()
                CallRecorder.onCallsChanged(this@OpenCallInCallService, calls)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(call)
        val ringing = call.stateCompat == Call.STATE_RINGING
        // Appel sortant (utilisateur au premier plan) ou appel entrant sans notification possible :
        // on ouvre directement l'écran d'appel.
        if (!ringing || !CallNotifications.canPost(this)) openInCallScreen()
    }

    private fun openInCallScreen() {
        try {
            startActivity(InCallActivity.intent(this))
        } catch (e: RuntimeException) {
            // Lancement en arrière-plan refusé : la notification d'appel reste le point d'entrée.
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.onCallRemoved(call)
    }

    @Deprecated("Remplacé par onCallEndpointChanged en API 34, toujours appelé par Telecom.")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        CallManager.onAudioStateChanged(audioState)
    }

    override fun onDestroy() {
        CallRecorder.stop(this)
        CallManager.clear()
        CallManager.service = null
        NotificationManagerCompat.from(this).cancel(CallNotifications.ID_CALL)
        scope.cancel()
        super.onDestroy()
    }
}
