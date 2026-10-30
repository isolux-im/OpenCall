package im.isolux.opencall.recording

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import im.isolux.opencall.R
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.CallNotifications
import im.isolux.opencall.telecom.InCallActivity
import java.io.File

/**
 * Service de premier plan (type microphone) qui enregistre via la source VOICE_CALL.
 * Une notification permanente signale l'enregistrement. Si l'enregistrement ne produit
 * aucun son réel, il est supprimé et la fonction est désactivée pour cet appareil.
 */
class RecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAt = 0L
    private var maxAmplitude = 0
    private val handler = Handler(Looper.getMainLooper())
    private val amplitudeProbe = object : Runnable {
        override fun run() {
            recorder?.let {
                try {
                    maxAmplitude = maxOf(maxAmplitude, it.maxAmplitude)
                } catch (e: RuntimeException) { /* enregistreur arrêté */ }
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(
                intent.getStringExtra(EXTRA_NUMBER),
                intent.getBooleanExtra(EXTRA_AUTOMATIC, false),
            )
            ACTION_STOP -> stopRecording()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(number: String?, automatic: Boolean) {
        if (recorder != null) return
        val notification = NotificationCompat.Builder(this, CallNotifications.CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_notification_record)
            .setContentTitle("Enregistrement de l'appel en cours")
            .setContentText("Informez votre interlocuteur que l'appel est enregistré.")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(this, 1, InCallActivity.intent(this), PendingIntent.FLAG_IMMUTABLE),
            )
            .build()
        try {
            startForeground(CallNotifications.ID_RECORDING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } catch (e: RuntimeException) {
            fail("Android a refusé l'accès au microphone en arrière-plan.", markDevice = false)
            return
        }

        val file = File(RecordingsRepository.get(this).directory(), RecordingNames.newFileName(System.currentTimeMillis(), number))
        val mr = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(64_000)
            mr.setAudioSamplingRate(16_000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
        } catch (e: Exception) {
            Log.w(TAG, "VOICE_CALL indisponible", e)
            mr.release()
            file.delete()
            fail("La source audio de l'appel est refusée par cet appareil.", markDevice = true)
            return
        }
        recorder = mr
        output = file
        startedAt = System.currentTimeMillis()
        maxAmplitude = 0
        handler.postDelayed(amplitudeProbe, 500)
        CallRecorder.update(RecorderState.Recording(startedAt, file.name, automatic))
    }

    private fun stopRecording() {
        val mr = recorder
        val file = output
        recorder = null
        output = null
        handler.removeCallbacks(amplitudeProbe)
        if (mr == null || file == null) {
            CallRecorder.update(RecorderState.Idle)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val duration = System.currentTimeMillis() - startedAt
        var ok = true
        try {
            mr.stop()
        } catch (e: RuntimeException) {
            ok = false
        } finally {
            mr.release()
        }
        // Aucun signal audio sur plusieurs secondes : le système fournit un flux vide.
        val silent = duration > 5_000 && maxAmplitude == 0
        if (!ok || silent || file.length() < 1024) {
            file.delete()
            if (silent || !ok) {
                fail("Aucun son d'appel n'a pu être capté : enregistrement non pris en charge.", markDevice = true)
                return
            }
        }
        CallRecorder.update(RecorderState.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fail(message: String, markDevice: Boolean) {
        if (markDevice) {
            val prefs = AppPrefs.get(this)
            prefs.recordingAttemptFailed = true
            prefs.recordingEnabled = false
            prefs.recordingAuto = false
        }
        CallRecorder.update(RecorderState.Failed(message))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (recorder != null) stopRecording()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OpenCallRecording"
        const val ACTION_START = "im.isolux.opencall.recording.START"
        const val ACTION_STOP = "im.isolux.opencall.recording.STOP"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_AUTOMATIC = "automatic"
    }
}
