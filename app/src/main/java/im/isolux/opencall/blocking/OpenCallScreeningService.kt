package im.isolux.opencall.blocking

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import im.isolux.opencall.settings.AppPrefs

/**
 * Filtrage des appels entrants. Android ne lie ce service que si OpenCall détient
 * le rôle ROLE_CALL_SCREENING ou ROLE_DIALER. Avec le seul rôle de filtrage,
 * le système ne transmet que les appels de numéros absents des contacts.
 * Les appels d'urgence ne sont jamais filtrés.
 */
class OpenCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val decision = try {
            val number = callDetails.handle?.schemeSpecificPart
            val info = IncomingCallInfo(
                number = number,
                presentationRestricted = callDetails.handlePresentation != TelecomManager.PRESENTATION_ALLOWED,
                isInContacts = number != null && isInContacts(number),
                isEmergency = EmergencyNumbers.isEmergency(this, number),
                isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING,
            )
            val prefs = AppPrefs.get(this)
            CallScreeningPolicy.decide(info, prefs.screeningRules, BlocklistRepository.get(this).localList())
        } catch (e: RuntimeException) {
            Log.w("OpenCallScreening", "Erreur de filtrage, appel autorisé", e)
            ScreeningDecision.Allow
        }

        val response = if (decision is ScreeningDecision.Block) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(false)
                .build()
        } else {
            CallResponse.Builder().build()
        }
        respondToCall(callDetails, response)
    }

    private fun isInContacts(number: String): Boolean {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Sans accès aux contacts, on ne peut pas savoir : on ne bloque pas comme « inconnu ».
            return true
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { it.moveToFirst() } == true
    }
}
