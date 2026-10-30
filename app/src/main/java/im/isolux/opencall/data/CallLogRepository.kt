package im.isolux.opencall.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog

data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val durationSeconds: Long,
)

object CallLogRepository {

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    fun load(context: Context, limit: Int = 300): List<CallLogEntry> {
        if (!hasPermission(context)) return emptyList()
        val projection = arrayOf(
            CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION,
        )
        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, limit.toString()).build(),
                projection, null, null, "${CallLog.Calls.DATE} DESC",
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            CallLogEntry(
                                id = c.getLong(0),
                                number = c.getString(1).orEmpty(),
                                name = c.getString(2)?.takeIf { it.isNotBlank() },
                                type = c.getInt(3),
                                date = c.getLong(4),
                                durationSeconds = c.getLong(5),
                            ),
                        )
                    }
                }
            }.orEmpty()
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
