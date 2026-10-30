package im.isolux.opencall.recording

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RecordingNames {

    const val EXTENSION = "m4a"
    private const val MAX_NAME = 80

    fun newFileName(timestampMillis: Long, number: String?): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date(timestampMillis))
        val who = number?.filter { it.isDigit() || it == '+' }?.takeIf { it.isNotEmpty() } ?: "inconnu"
        return "Appel_${date}_$who.$EXTENSION"
    }

    /**
     * Nettoie un nom saisi par l'utilisateur : pas de séparateur de chemin,
     * pas de nom caché, longueur limitée. Renvoie `null` si le nom est inutilisable.
     */
    fun sanitize(input: String): String? {
        val base = input.trim().removeSuffix(".$EXTENSION")
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .trim('.', ' ')
            .take(MAX_NAME)
        if (base.isBlank()) return null
        return "$base.$EXTENSION"
    }

    fun displayName(fileName: String): String = fileName.removeSuffix(".$EXTENSION")

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes o"
        bytes < 1024 * 1024 -> String.format(Locale.FRANCE, "%.1f Ko", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.FRANCE, "%.1f Mo", bytes / (1024.0 * 1024))
        else -> String.format(Locale.FRANCE, "%.2f Go", bytes / (1024.0 * 1024 * 1024))
    }

    fun formatDuration(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    /** Fichiers à supprimer selon la durée de conservation ([retentionDays] ≤ 0 : jamais). */
    fun expired(files: List<Pair<String, Long>>, now: Long, retentionDays: Int): List<String> {
        if (retentionDays <= 0) return emptyList()
        val limit = now - retentionDays * 24L * 3600 * 1000
        return files.filter { it.second < limit }.map { it.first }
    }
}
