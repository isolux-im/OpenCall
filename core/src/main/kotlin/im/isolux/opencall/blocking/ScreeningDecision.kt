package im.isolux.opencall.blocking

data class ScreeningRules(
    val blockListEnabled: Boolean = true,
    val blockHiddenNumbers: Boolean = false,
    val blockUnknownNumbers: Boolean = false,
)

data class IncomingCallInfo(
    /** Numéro présenté, `null` si masqué / inconnu. */
    val number: String?,
    val presentationRestricted: Boolean,
    val isInContacts: Boolean,
    val isEmergency: Boolean,
    val isIncoming: Boolean,
)

sealed interface ScreeningDecision {
    data object Allow : ScreeningDecision
    data class Block(val reason: Reason) : ScreeningDecision

    enum class Reason { BLOCKLIST, HIDDEN_NUMBER, UNKNOWN_NUMBER }
}

object CallScreeningPolicy {

    fun decide(call: IncomingCallInfo, rules: ScreeningRules, blocklist: Collection<String>): ScreeningDecision {
        // Les appels sortants et tout ce qui touche aux urgences ne sont jamais filtrés.
        if (!call.isIncoming || call.isEmergency) return ScreeningDecision.Allow
        if (call.number != null && PhoneNumbers.isFallbackEmergency(call.number)) return ScreeningDecision.Allow

        val hidden = call.presentationRestricted || call.number.isNullOrBlank()
        if (hidden) {
            return if (rules.blockHiddenNumbers) ScreeningDecision.Block(ScreeningDecision.Reason.HIDDEN_NUMBER)
            else ScreeningDecision.Allow
        }
        if (rules.blockListEnabled && BlocklistLogic.isBlocked(call.number, blocklist)) {
            return ScreeningDecision.Block(ScreeningDecision.Reason.BLOCKLIST)
        }
        if (rules.blockUnknownNumbers && !call.isInContacts) {
            return ScreeningDecision.Block(ScreeningDecision.Reason.UNKNOWN_NUMBER)
        }
        return ScreeningDecision.Allow
    }
}
