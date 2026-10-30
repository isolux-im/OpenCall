package im.isolux.opencall.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.DialerRole

/**
 * Vérifie sur l'appareil réel si l'enregistrement des deux côtés d'un appel est permis.
 *
 * Seule la source audio MediaRecorder.AudioSource.VOICE_CALL donne accès aux deux voix ;
 * Android l'autorise uniquement aux applications disposant de CAPTURE_AUDIO_OUTPUT
 * (autorisation système/privilégiée, accordée par le fabricant ou une ROM).
 * OpenCall n'utilise ni AccessibilityService ni aucun contournement.
 */
object RecordingSupport {

    const val CAPTURE_AUDIO_OUTPUT = "android.permission.CAPTURE_AUDIO_OUTPUT"

    fun environment(context: Context): RecordingEnvironment {
        val pm = context.packageManager
        return RecordingEnvironment(
            hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            hasMicrophone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            captureAudioOutputGranted = context.checkSelfPermission(CAPTURE_AUDIO_OUTPUT) == PackageManager.PERMISSION_GRANTED,
            isDefaultDialer = DialerRole.isDefaultDialer(context),
            recordAudioGranted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
            previousAttemptFailed = AppPrefs.get(context).recordingAttemptFailed,
        )
    }

    fun capability(context: Context): RecordingCapability = RecordingCapabilityEvaluator.evaluate(environment(context))

    /** L'enregistrement peut-il être proposé maintenant (capacité + choix utilisateur + information lue) ? */
    fun isUsable(context: Context): Boolean {
        val prefs = AppPrefs.get(context)
        return capability(context) == RecordingCapability.Supported &&
            prefs.recordingEnabled && prefs.recordingConsentAcknowledged
    }
}
