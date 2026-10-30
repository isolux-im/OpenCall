package im.isolux.opencall.blocking

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers
import android.util.Log
import im.isolux.opencall.settings.AppPrefs
import im.isolux.opencall.telecom.DialerRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Liste noire d'OpenCall.
 *
 * - Si Android autorise OpenCall à gérer la liste système (application Téléphone par défaut),
 *   chaque modification est écrite dans BlockedNumberContract, appliquée par le système lui-même,
 *   et une copie locale est conservée.
 * - Sinon la copie locale n'est appliquée que si OpenCall détient le rôle de filtrage
 *   (CallScreeningService) ; dans le cas contraire elle n'est pas appliquée et l'interface le dit.
 */
class BlocklistRepository private constructor(private val context: Context) {

    private val prefs = AppPrefs.get(context)
    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    fun canUseSystemList(): Boolean = try {
        BlockedNumberContract.canCurrentUserBlockNumbers(context)
    } catch (e: RuntimeException) {
        false
    }

    fun enforcement(): BlockingEnforcement = when {
        canUseSystemList() -> BlockingEnforcement.SYSTEM
        DialerRole.isCallScreener(context) -> BlockingEnforcement.SCREENING_ONLY
        else -> BlockingEnforcement.NONE
    }

    fun refresh() {
        val local = prefs.localBlocklist
        val merged = if (canUseSystemList()) {
            val system = readSystem()
            (system + local.filter { l -> system.none { PhoneNumbers.isSameNumber(it, l) } })
        } else {
            local.toList()
        }
        _entries.value = merged.distinctBy { PhoneNumbers.normalize(it) }.sortedBy { PhoneNumbers.normalize(it) }
    }

    fun isBlocked(number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        if (canUseSystemList()) {
            try {
                if (BlockedNumberContract.isBlocked(context, number)) return true
            } catch (e: RuntimeException) {
                Log.w(TAG, "isBlocked système indisponible", e)
            }
        }
        return BlocklistLogic.isBlocked(number, prefs.localBlocklist)
    }

    fun block(number: String): BlockRequestResult {
        val clean = PhoneNumbers.normalize(number)
        val result = BlocklistLogic.validate(clean, EmergencyNumbers.isEmergency(context, clean), entries.value + prefs.localBlocklist)
        if (result != BlockRequestResult.Ok) return result
        prefs.localBlocklist = prefs.localBlocklist + clean
        if (canUseSystemList()) insertSystem(clean)
        refresh()
        return BlockRequestResult.Ok
    }

    fun unblock(number: String) {
        prefs.localBlocklist = prefs.localBlocklist.filterNot { PhoneNumbers.isSameNumber(it, number) }.toSet()
        if (canUseSystemList()) {
            readSystemRows().filter { PhoneNumbers.isSameNumber(it.second, number) }.forEach { (id, _) ->
                try {
                    context.contentResolver.delete(BlockedNumbers.CONTENT_URI, "${BlockedNumbers.COLUMN_ID} = ?", arrayOf(id.toString()))
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Suppression système impossible", e)
                }
            }
        }
        refresh()
    }

    /** Synchronisation bidirectionnelle avec la liste système. Renvoie `null` si elle est inaccessible. */
    fun syncWithSystem(): SyncPlan? {
        if (!canUseSystemList()) return null
        val plan = BlocklistLogic.syncPlan(prefs.localBlocklist, readSystem())
        plan.pushToSystem.forEach { insertSystem(it) }
        if (plan.importToLocal.isNotEmpty()) prefs.localBlocklist = prefs.localBlocklist + plan.importToLocal
        refresh()
        return plan
    }

    /** Liste locale, utilisée par le CallScreeningService. */
    fun localList(): Set<String> = prefs.localBlocklist

    private fun insertSystem(number: String) {
        try {
            val values = ContentValues().apply { put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number) }
            context.contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
        } catch (e: RuntimeException) {
            Log.w(TAG, "Ajout système impossible", e)
        }
    }

    private fun readSystem(): List<String> = readSystemRows().map { it.second }

    private fun readSystemRows(): List<Pair<Long, String>> = try {
        context.contentResolver.query(
            BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumbers.COLUMN_ID, BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null, null, null,
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val n = c.getString(1) ?: continue
                    add(c.getLong(0) to n)
                }
            }
        }.orEmpty()
    } catch (e: RuntimeException) {
        Log.w(TAG, "Lecture de la liste système impossible", e)
        emptyList()
    }

    companion object {
        private const val TAG = "OpenCallBlocklist"

        @Volatile private var instance: BlocklistRepository? = null

        fun get(context: Context): BlocklistRepository = instance ?: synchronized(this) {
            instance ?: BlocklistRepository(context.applicationContext).also { instance = it }
        }
    }
}
