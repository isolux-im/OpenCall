package im.isolux.opencall.recording

/** Données d'entrée de l'évaluation, collectées sur l'appareil par [RecordingSupport]. */
data class RecordingEnvironment(
    val hasTelephony: Boolean,
    val hasMicrophone: Boolean,
    /** Permission signature/privilégiée exigée par la source audio VOICE_CALL. */
    val captureAudioOutputGranted: Boolean,
    val isDefaultDialer: Boolean,
    val recordAudioGranted: Boolean,
    /** Un essai réel précédent a échoué (fichier vide ou erreur MediaRecorder). */
    val previousAttemptFailed: Boolean,
)

sealed interface RecordingCapability {
    /** Aucune méthode autorisée ne permet d'enregistrer les deux côtés de l'appel. */
    data class Unsupported(val reason: String) : RecordingCapability

    /** Capacité présente mais une condition dépend de l'utilisateur. */
    data object NeedsDefaultDialer : RecordingCapability
    data object NeedsMicrophonePermission : RecordingCapability

    data object Supported : RecordingCapability
}

object RecordingCapabilityEvaluator {

    const val UNSUPPORTED_MESSAGE = "Enregistrement des appels non pris en charge sur cet appareil."

    fun evaluate(env: RecordingEnvironment): RecordingCapability = when {
        !env.hasTelephony -> RecordingCapability.Unsupported("Cet appareil ne dispose pas de la téléphonie.")
        !env.hasMicrophone -> RecordingCapability.Unsupported("Aucun microphone disponible.")
        !env.captureAudioOutputGranted -> RecordingCapability.Unsupported(
            "Android réserve la source audio des appels (VOICE_CALL) aux applications système " +
                "disposant de l'autorisation CAPTURE_AUDIO_OUTPUT. Le rôle d'application Téléphone " +
                "par défaut ne donne pas cet accès, et OpenCall ne contourne pas cette protection.",
        )
        env.previousAttemptFailed -> RecordingCapability.Unsupported(
            "Un essai d'enregistrement a échoué sur cet appareil : le fabricant ne fournit pas l'audio de l'appel.",
        )
        !env.isDefaultDialer -> RecordingCapability.NeedsDefaultDialer
        !env.recordAudioGranted -> RecordingCapability.NeedsMicrophonePermission
        else -> RecordingCapability.Supported
    }
}
