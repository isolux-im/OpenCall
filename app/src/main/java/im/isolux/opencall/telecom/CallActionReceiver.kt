package im.isolux.opencall.telecom

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Action « Refuser / Raccrocher » des notifications d'appel. « Répondre » ouvre directement
 * [InCallActivity] (Android 12+ interdit de lancer une activité depuis un récepteur de notification).
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_HANG_UP) return
        val call = CallManager.primaryCall()?.call ?: return
        CallManager.hangUp(call)
    }

    companion object {
        const val ACTION_HANG_UP = "im.isolux.opencall.action.HANG_UP"

        fun hangUpIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, CallActionReceiver::class.java).setAction(ACTION_HANG_UP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun answerIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            2,
            InCallActivity.intent(context).setAction(InCallActivity.ACTION_ANSWER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
