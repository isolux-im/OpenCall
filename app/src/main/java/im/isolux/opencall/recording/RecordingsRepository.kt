package im.isolux.opencall.recording

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.StatFs
import androidx.core.content.FileProvider
import im.isolux.opencall.settings.AppPrefs
import java.io.File

data class RecordingItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val durationMillis: Long,
)

/** Enregistrements réalisés par OpenCall, stockés dans l'espace privé de l'application. */
class RecordingsRepository private constructor(private val context: Context) {

    fun directory(): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun list(): List<RecordingItem> = directory().listFiles { f -> f.isFile && f.name.endsWith(".${RecordingNames.EXTENSION}") }
        .orEmpty()
        .map { f ->
            RecordingItem(f, RecordingNames.displayName(f.name), f.length(), f.lastModified(), duration(f))
        }
        .sortedByDescending { it.modifiedAt }

    fun rename(item: RecordingItem, newName: String): Boolean {
        val clean = RecordingNames.sanitize(newName) ?: return false
        val target = File(directory(), clean)
        if (target.exists()) return false
        return item.file.renameTo(target)
    }

    fun delete(item: RecordingItem): Boolean = item.file.delete()

    fun deleteAll(): Int = list().count { it.file.delete() }

    fun totalSize(): Long = directory().listFiles().orEmpty().sumOf { it.length() }

    fun freeSpace(): Long = try {
        StatFs(context.filesDir.absolutePath).availableBytes
    } catch (e: IllegalArgumentException) {
        0L
    }

    fun applyRetention(now: Long = System.currentTimeMillis()): Int {
        val days = AppPrefs.get(context).recordingRetentionDays
        val files = directory().listFiles().orEmpty().map { it.name to it.lastModified() }
        val expired = RecordingNames.expired(files, now, days)
        return expired.count { File(directory(), it).delete() }
    }

    fun shareIntent(item: RecordingItem): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.recordings", item.file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("audio/mp4")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, "Partager l'enregistrement")
    }

    private fun duration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: RuntimeException) {
            0L
        } finally {
            try { retriever.release() } catch (e: Exception) { /* ignoré */ }
        }
    }

    companion object {
        private const val DIR = "recordings"

        @Volatile private var instance: RecordingsRepository? = null

        fun get(context: Context): RecordingsRepository = instance ?: synchronized(this) {
            instance ?: RecordingsRepository(context.applicationContext).also { instance = it }
        }
    }
}
