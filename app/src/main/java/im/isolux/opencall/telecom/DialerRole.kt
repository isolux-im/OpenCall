package im.isolux.opencall.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager

/**
 * Accès aux rôles officiels Android (RoleManager). OpenCall ne demande un rôle
 * que sur action explicite de l'utilisateur et ne le force jamais.
 */
object DialerRole {

    private fun roleManager(context: Context): RoleManager? = context.getSystemService(RoleManager::class.java)

    fun isDialerAvailable(context: Context): Boolean =
        roleManager(context)?.isRoleAvailable(RoleManager.ROLE_DIALER) == true

    fun isDefaultDialer(context: Context): Boolean {
        val rm = roleManager(context)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) return rm.isRoleHeld(RoleManager.ROLE_DIALER)
        val telecom = context.getSystemService(TelecomManager::class.java)
        return telecom?.defaultDialerPackage == context.packageName
    }

    fun createDialerRequest(context: Context): Intent? =
        roleManager(context)?.takeIf { it.isRoleAvailable(RoleManager.ROLE_DIALER) }
            ?.createRequestRoleIntent(RoleManager.ROLE_DIALER)

    fun isScreeningAvailable(context: Context): Boolean =
        roleManager(context)?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true

    fun isCallScreener(context: Context): Boolean =
        roleManager(context)?.let {
            it.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && it.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } == true

    fun createScreeningRequest(context: Context): Intent? =
        roleManager(context)?.takeIf { it.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }
            ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)

    /** Écran système « Applications par défaut », pour revenir au composeur précédent. */
    fun defaultAppsSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
