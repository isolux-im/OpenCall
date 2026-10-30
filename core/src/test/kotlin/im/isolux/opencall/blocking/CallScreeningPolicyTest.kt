package im.isolux.opencall.blocking

import org.junit.Assert.assertEquals
import org.junit.Test

class CallScreeningPolicyTest {

    private fun call(
        number: String? = "0612345678",
        hidden: Boolean = false,
        contact: Boolean = false,
        emergency: Boolean = false,
        incoming: Boolean = true,
    ) = IncomingCallInfo(number, hidden, contact, emergency, incoming)

    private val blocklist = listOf("0612345678")

    @Test
    fun blocklistedNumberIsRejected() {
        assertEquals(
            ScreeningDecision.Block(ScreeningDecision.Reason.BLOCKLIST),
            CallScreeningPolicy.decide(call(), ScreeningRules(), blocklist),
        )
    }

    @Test
    fun blocklistCanBeDisabled() {
        assertEquals(
            ScreeningDecision.Allow,
            CallScreeningPolicy.decide(call(), ScreeningRules(blockListEnabled = false), blocklist),
        )
    }

    @Test
    fun emergencyAndOutgoingAreNeverFiltered() {
        val rules = ScreeningRules(blockHiddenNumbers = true, blockUnknownNumbers = true)
        assertEquals(ScreeningDecision.Allow, CallScreeningPolicy.decide(call(emergency = true), rules, blocklist))
        assertEquals(ScreeningDecision.Allow, CallScreeningPolicy.decide(call(incoming = false), rules, blocklist))
        assertEquals(ScreeningDecision.Allow, CallScreeningPolicy.decide(call(number = "112"), rules, listOf("112")))
    }

    @Test
    fun hiddenAndUnknownRules() {
        assertEquals(
            ScreeningDecision.Allow,
            CallScreeningPolicy.decide(call(number = null, hidden = true), ScreeningRules(), emptyList()),
        )
        assertEquals(
            ScreeningDecision.Block(ScreeningDecision.Reason.HIDDEN_NUMBER),
            CallScreeningPolicy.decide(call(number = null, hidden = true), ScreeningRules(blockHiddenNumbers = true), emptyList()),
        )
        assertEquals(
            ScreeningDecision.Block(ScreeningDecision.Reason.UNKNOWN_NUMBER),
            CallScreeningPolicy.decide(call(number = "0699999999"), ScreeningRules(blockUnknownNumbers = true), emptyList()),
        )
        assertEquals(
            ScreeningDecision.Allow,
            CallScreeningPolicy.decide(call(number = "0699999999", contact = true), ScreeningRules(blockUnknownNumbers = true), emptyList()),
        )
    }
}
