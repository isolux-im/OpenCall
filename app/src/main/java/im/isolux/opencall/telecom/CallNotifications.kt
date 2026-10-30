package im.isolux.opencall.telecom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.Call
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import im.isolux.opencall.R
import im.isolux.opencall.blocking.PhoneNumbers

/** Notifications d'appel (entrant plein écran, appel en cours) et d'enregistrement. */
object CallNotifications {

    const val CHANNEL_INCOMING = "incoming_calls"
    const val CHANNEL_ONGOING = "ongoing_calls"
    const val CHANNEL_RECORDING = "call_recording"
    const val ID_CALL = 1001
    const val ID_RECORDING = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_INCOMING, "Appels entrants", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Affiche l'écran d'appel entrant"
                // La sonnerie est jouée par le système (Telecom), pas par la notification.
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Appels en cours", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RECORDING, "Enregistrement des appels", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (android.os.Build.VERSION.SDK_INT < 33 ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** Affiche ou retire la notification d'appel. Renvoie `false` si elle n'a pas pu être affichée. */
    fun show(context: Context, call: CallUi?): Boolean {
        val nm = NotificationManagerCompat.from(context)
        if (call == null || call.state == Call.STATE_DISCONNECTED) {
            nm.cancel(ID_CALL)
            return true
        }
        if (!canPost(context)) return false
        val name = call.displayName ?: call.number?.let { PhoneNumbers.format(it) } ?: "Numéro masqué"
        val person = Person.Builder().setName(name).setImportant(true).build()
        val content = PendingIntent.getActivity(
            context, 0, InCallActivity.intent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hangUp = CallActionReceiver.hangUpIntent(context)

        val notification = if (call.state == Call.STATE_RINGING) {
            val answer = CallActionReceiver.answerIntent(context)
            NotificationCompat.Builder(context, CHANNEL_INCOMING)
                .setSmallIcon(R.drawable.ic_notification_call)
                .setContentTitle(name)
                .setContentText("Appel entrant")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(content)
                .setFullScreenIntent(content, true)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, hangUp, answer))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
        } else {
            NotificationCompat.Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(R.drawable.ic_notification_call)
                .setContentTitle(name)
                .setContentText(if (call.state == Call.STATE_HOLDING) "Appel en attente" else "Appel en cours")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(content)
                .setUsesChronometer(call.connectTimeMillis > 0)
                .setWhen(if (call.connectTimeMillis > 0) call.connectTimeMillis else System.currentTimeMillis())
                // Pas de CallStyle ici : Android 12+ le refuse (IllegalArgumentException) hors service
                // de premier plan ou intention plein écran, ce qui faisait planter l'application.
                .addAction(R.drawable.ic_notification_call, "Raccrocher", hangUp)
                .build()
        }
        return try {
            nm.notify(ID_CALL, notification)
            true
        } catch (e: SecurityException) {
            // Permission de notification retirée : l'appelant ouvre l'écran d'appel.
            false
        } catch (e: IllegalArgumentException) {
            // Notification refusée par le système : l'appelant ouvre l'écran d'appel.
            Log.w("OpenCallNotifications", "Notification d'appel refusée", e)
            false
        }
    }
}
