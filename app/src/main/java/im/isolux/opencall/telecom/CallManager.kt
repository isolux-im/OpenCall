package im.isolux.opencall.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** État de l'appel : Call.Details.getState() depuis l'API 31, Call.getState() avant. */
@Suppress("DEPRECATION")
val Call.stateCompat: Int
    get() = if (android.os.Build.VERSION.SDK_INT >= 31) details.state else state

/** Instantané d'un appel pour l'interface. */
data class CallUi(
    val call: Call,
    val state: Int,
    val number: String?,
    val displayName: String?,
    val connectTimeMillis: Long,
    val canHold: Boolean,
    val isConference: Boolean,
)

/**
 * État partagé des appels gérés via [OpenCallInCallService]. Toutes les commandes
 * passent par les API officielles de Telecom (Call.answer, Call.reject, Call.disconnect…).
 */
object CallManager {

    private val _calls = MutableStateFlow<List<CallUi>>(emptyList())
    val calls: StateFlow<List<CallUi>> = _calls.asStateFlow()

    private val _audioState = MutableStateFlow<CallAudioState?>(null)
    val audioState: StateFlow<CallAudioState?> = _audioState.asStateFlow()

    internal var service: InCallService? = null

    private val tracked = mutableListOf<Call>()

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = publish()
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
    }

    internal fun onCallAdded(call: Call) {
        if (call !in tracked) {
            tracked += call
            call.registerCallback(callback)
        }
        publish()
    }

    internal fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        tracked -= call
        publish()
    }

    internal fun onAudioStateChanged(state: CallAudioState?) {
        _audioState.value = state
    }

    internal fun clear() {
        tracked.forEach { it.unregisterCallback(callback) }
        tracked.clear()
        publish()
    }

    @Suppress("DEPRECATION")
    private fun publish() {
        _calls.value = tracked.map { call ->
            val details = call.details
            CallUi(
                call = call,
                state = call.stateCompat,
                number = details?.handle?.schemeSpecificPart,
                displayName = details?.contactDisplayName?.takeIf { it.isNotBlank() }
                    ?: details?.callerDisplayName?.takeIf { it.isNotBlank() },
                connectTimeMillis = details?.connectTimeMillis ?: 0L,
                canHold = details?.can(Call.Details.CAPABILITY_HOLD) == true,
                isConference = details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true,
            )
        }
    }

    fun ringingCall(): CallUi? = calls.value.firstOrNull { it.state == Call.STATE_RINGING }

    /** Appel principal à afficher : l'appel entrant en priorité, sinon l'appel actif. */
    fun primaryCall(): CallUi? {
        val list = calls.value
        return list.firstOrNull { it.state == Call.STATE_RINGING }
            ?: list.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: list.firstOrNull { it.state == Call.STATE_DIALING || it.state == Call.STATE_CONNECTING }
            ?: list.firstOrNull { it.state != Call.STATE_DISCONNECTED }
            ?: list.firstOrNull()
    }

    fun answer(call: Call) = call.answer(VideoProfile.STATE_AUDIO_ONLY)

    fun reject(call: Call) = call.reject(false, null)

    fun hangUp(call: Call) {
        if (call.stateCompat == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
    }

    fun toggleHold(call: Call) {
        if (call.stateCompat == Call.STATE_HOLDING) call.unhold() else call.hold()
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
    }

    @Suppress("DEPRECATION")
    fun setSpeaker(on: Boolean) {
        service?.setAudioRoute(if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE)
    }

    @Suppress("DEPRECATION")
    fun setRoute(route: Int) {
        service?.setAudioRoute(route)
    }

    fun playDtmf(call: Call, digit: Char) {
        call.playDtmfTone(digit)
        call.stopDtmfTone()
    }
}
