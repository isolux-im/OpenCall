package im.isolux.opencall.lock

/** Délais proposés pour le verrouillage après inactivité. */
enum class LockTimeout(val millis: Long, val label: String) {
    IMMEDIATE(0L, "Immédiatement"),
    SECONDS_30(30_000L, "Après 30 secondes"),
    MINUTE_1(60_000L, "Après 1 minute"),
    MINUTES_5(5 * 60_000L, "Après 5 minutes"),
    MINUTES_15(15 * 60_000L, "Après 15 minutes");

    companion object {
        fun fromName(name: String?): LockTimeout = entries.firstOrNull { it.name == name } ?: MINUTE_1
    }
}

object LockPolicy {

    /** Au retour au premier plan : faut-il verrouiller ? */
    fun shouldLockOnResume(
        enabled: Boolean,
        neverUnlocked: Boolean,
        backgroundedAt: Long?,
        now: Long,
        timeout: LockTimeout,
    ): Boolean {
        if (!enabled) return false
        if (neverUnlocked) return true
        if (backgroundedAt == null) return false
        return now - backgroundedAt >= timeout.millis
    }

    /** Au premier plan, sans interaction depuis [lastInteractionAt]. */
    fun shouldLockForInactivity(
        enabled: Boolean,
        lastInteractionAt: Long,
        now: Long,
        timeout: LockTimeout,
    ): Boolean {
        if (!enabled) return false
        // « Immédiatement » concerne le passage en arrière-plan ; au premier plan on garde un minimum d'une minute.
        val effective = if (timeout == LockTimeout.IMMEDIATE) LockTimeout.MINUTE_1.millis else timeout.millis
        return now - lastInteractionAt >= effective
    }
}
