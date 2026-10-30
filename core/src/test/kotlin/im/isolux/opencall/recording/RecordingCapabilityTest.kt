package im.isolux.opencall.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingCapabilityTest {

    private val retailDevice = RecordingEnvironment(
        hasTelephony = true,
        hasMicrophone = true,
        captureAudioOutputGranted = false,
        isDefaultDialer = true,
        recordAudioGranted = true,
        previousAttemptFailed = false,
    )

    @Test
    fun defaultDialerRoleAloneIsNotEnough() {
        assertTrue(RecordingCapabilityEvaluator.evaluate(retailDevice) is RecordingCapability.Unsupported)
    }

    @Test
    fun privilegedInstallIsSupportedOnlyWithRoleAndMicrophone() {
        val privileged = retailDevice.copy(captureAudioOutputGranted = true)
        assertEquals(RecordingCapability.Supported, RecordingCapabilityEvaluator.evaluate(privileged))
        assertEquals(
            RecordingCapability.NeedsDefaultDialer,
            RecordingCapabilityEvaluator.evaluate(privileged.copy(isDefaultDialer = false)),
        )
        assertEquals(
            RecordingCapability.NeedsMicrophonePermission,
            RecordingCapabilityEvaluator.evaluate(privileged.copy(recordAudioGranted = false)),
        )
    }

    @Test
    fun failedRealAttemptDisablesTheFeature() {
        val failed = retailDevice.copy(captureAudioOutputGranted = true, previousAttemptFailed = true)
        assertTrue(RecordingCapabilityEvaluator.evaluate(failed) is RecordingCapability.Unsupported)
    }

    @Test
    fun noTelephony() {
        assertTrue(
            RecordingCapabilityEvaluator.evaluate(retailDevice.copy(hasTelephony = false, captureAudioOutputGranted = true))
                is RecordingCapability.Unsupported,
        )
    }

    @Test
    fun unsupportedMessageIsExact() {
        assertEquals(
            "Enregistrement des appels non pris en charge sur cet appareil.",
            RecordingCapabilityEvaluator.UNSUPPORTED_MESSAGE,
        )
    }
}
