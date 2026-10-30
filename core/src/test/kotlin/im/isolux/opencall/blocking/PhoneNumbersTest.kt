package im.isolux.opencall.blocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun normalize_keepsDigitsAndLeadingPlus() {
        assertEquals("+33612345678", PhoneNumbers.normalize(" +33 6 12-34.56 (78) "))
        assertEquals("0612345678", PhoneNumbers.normalize("06 12 34 56 78"))
        assertEquals("*31#123", PhoneNumbers.normalize("*31#123"))
        assertEquals("33612", PhoneNumbers.normalize("33+612"))
    }

    @Test
    fun sameNumber_nationalAndInternationalFormats() {
        assertTrue(PhoneNumbers.isSameNumber("06 12 34 56 78", "+33 6 12 34 56 78"))
        assertTrue(PhoneNumbers.isSameNumber("0612345678", "0612345678"))
        assertFalse(PhoneNumbers.isSameNumber("0612345678", "0612345679"))
    }

    @Test
    fun sameNumber_shortNumbersMustMatchExactly() {
        assertFalse(PhoneNumbers.isSameNumber("3644", "13644"))
        assertTrue(PhoneNumbers.isSameNumber("3644", "36 44"))
        assertFalse(PhoneNumbers.isSameNumber("", ""))
    }

    @Test
    fun emergencyFallback() {
        listOf("112", "911", "15", "17", "18", "999", "1 1 2").forEach {
            assertTrue(it, PhoneNumbers.isFallbackEmergency(it))
        }
        assertFalse(PhoneNumbers.isFallbackEmergency("0612345678"))
    }

    @Test
    fun format_frenchNumbers() {
        assertEquals("06 12 34 56 78", PhoneNumbers.format("0612345678"))
        assertEquals("+33 6 12 34 56 78", PhoneNumbers.format("+33612345678"))
    }
}
