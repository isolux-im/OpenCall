package im.isolux.opencall.blocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocklistLogicTest {

    @Test
    fun emergencyNumbersCanNeverBeBlocked() {
        assertEquals(BlockRequestResult.Emergency, BlocklistLogic.validate("112", false, emptyList()))
        assertEquals(BlockRequestResult.Emergency, BlocklistLogic.validate("911", false, emptyList()))
        // Détection système (TelephonyManager) prioritaire même pour un numéro inconnu de la liste de secours.
        assertEquals(BlockRequestResult.Emergency, BlocklistLogic.validate("0800123456", true, emptyList()))
    }

    @Test
    fun validateRejectsInvalidAndDuplicates() {
        assertEquals(BlockRequestResult.Invalid, BlocklistLogic.validate("ab", false, emptyList()))
        assertEquals(BlockRequestResult.Ok, BlocklistLogic.validate("0612345678", false, emptyList()))
        assertEquals(
            BlockRequestResult.AlreadyBlocked,
            BlocklistLogic.validate("+33612345678", false, listOf("06 12 34 56 78")),
        )
    }

    @Test
    fun blockAndUnblockRoundTrip() {
        val list = mutableListOf<String>()
        assertEquals(BlockRequestResult.Ok, BlocklistLogic.validate("0611111111", false, list))
        list += "0611111111"
        assertTrue(BlocklistLogic.isBlocked("+33611111111", list))
        list.removeAll { PhoneNumbers.isSameNumber(it, "0611111111") }
        assertFalse(BlocklistLogic.isBlocked("+33611111111", list))
    }

    @Test
    fun syncPlanMergesBothWaysWithoutDuplicates() {
        val plan = BlocklistLogic.syncPlan(
            local = listOf("0611111111", "0622222222", "112"),
            system = listOf("+33622222222", "0633333333"),
        )
        assertEquals(listOf("0611111111"), plan.pushToSystem)
        assertEquals(listOf("0633333333"), plan.importToLocal)
    }

    @Test
    fun searchMatchesDigitsRegardlessOfFormatting() {
        val list = listOf("06 11 11 11 11", "+33622222222")
        assertEquals(listOf("+33622222222"), BlocklistLogic.search(list, "622"))
        assertEquals(listOf("06 11 11 11 11"), BlocklistLogic.search(list, "0611"))
        assertEquals(list, BlocklistLogic.search(list, " "))
    }
}
