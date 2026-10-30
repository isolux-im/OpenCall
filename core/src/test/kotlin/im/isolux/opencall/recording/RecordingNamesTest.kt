package im.isolux.opencall.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingNamesTest {

    @Test
    fun newFileNameContainsNumberAndExtension() {
        val name = RecordingNames.newFileName(0L, "+33 6 12")
        assertTrue(name, name.startsWith("Appel_") && name.endsWith("_+33612.m4a"))
        assertTrue(RecordingNames.newFileName(0L, null).endsWith("_inconnu.m4a"))
    }

    @Test
    fun sanitizePreventsPathTraversal() {
        assertEquals("_.._etc_passwd.m4a", RecordingNames.sanitize("/../etc/passwd"))
        assertEquals("Réunion client.m4a", RecordingNames.sanitize("  Réunion client.m4a "))
        assertNull(RecordingNames.sanitize("   "))
        assertNull(RecordingNames.sanitize("..."))
    }

    @Test
    fun retention() {
        val day = 24L * 3600 * 1000
        val files = listOf("old" to 0L, "new" to 40 * day)
        assertEquals(listOf("old"), RecordingNames.expired(files, 41 * day, 30))
        assertEquals(emptyList<String>(), RecordingNames.expired(files, 41 * day, 0))
    }

    @Test
    fun formatting() {
        assertEquals("1:05", RecordingNames.formatDuration(65_000))
        assertEquals("1:00:00", RecordingNames.formatDuration(3_600_000))
        assertEquals("512 o", RecordingNames.formatSize(512))
    }
}
