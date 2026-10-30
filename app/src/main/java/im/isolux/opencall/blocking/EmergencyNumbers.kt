package im.isolux.opencall.blocking

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager

object EmergencyNumbers {

    /** Détection officielle (TelephonyManager) complétée par la liste de secours. */
    fun isEmergency(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        if (PhoneNumbers.isFallbackEmergency(number)) return true
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return false
        return try {
            context.getSystemService(TelephonyManager::class.java)?.isEmergencyNumber(PhoneNumbers.normalize(number)) == true
        } catch (e: RuntimeException) {
            // IllegalStateException si le service téléphonie est indisponible : on reste prudent.
            false
        }
    }
}
