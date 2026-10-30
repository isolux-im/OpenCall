package im.isolux.opencall.telecom

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import im.isolux.opencall.blocking.EmergencyNumbers
import im.isolux.opencall.blocking.PhoneNumbers

/** Résultat d'une demande d'appel. */
enum class PlaceCallResult { PLACED, NEEDS_PERMISSION, FAILED }

object CallPlacer {

    fun hasCallPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    /**
     * Passe un appel :
     * - application Téléphone par défaut : TelecomManager.placeCall (gère aussi les urgences) ;
     * - sinon : ACTION_CALL ; Android redirige d'office les numéros d'urgence vers le composeur système.
     */
    fun place(context: Context, rawNumber: String): PlaceCallResult {
        val number = PhoneNumbers.normalize(rawNumber)
        if (number.isEmpty()) return PlaceCallResult.FAILED
        val uri = Uri.fromParts("tel", number, null)
        if (!hasCallPermission(context)) {
            // Une urgence ne doit jamais échouer faute d'autorisation : on passe par le composeur système.
            if (EmergencyNumbers.isEmergency(context, number) && dialViaSystem(context, number)) return PlaceCallResult.PLACED
            return PlaceCallResult.NEEDS_PERMISSION
        }
        return try {
            if (DialerRole.isDefaultDialer(context)) {
                context.getSystemService(TelecomManager::class.java)?.placeCall(uri, Bundle())
                    ?: return PlaceCallResult.FAILED
            } else {
                context.startActivity(Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PlaceCallResult.PLACED
        } catch (e: SecurityException) {
            PlaceCallResult.NEEDS_PERMISSION
        } catch (e: ActivityNotFoundException) {
            PlaceCallResult.FAILED
        }
    }

    /** Ouvre le composeur par défaut du système (autre qu'OpenCall) avec le numéro prérempli. */
    fun dialViaSystem(context: Context, number: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
            ?.takeIf { it != context.packageName }
            ?.let { intent.setPackage(it) }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
