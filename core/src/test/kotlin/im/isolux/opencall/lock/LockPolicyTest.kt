package im.isolux.opencall.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {

    @Test
    fun disabledLockNeverLocks() {
        assertFalse(LockPolicy.shouldLockOnResume(false, true, 0L, 1_000_000L, LockTimeout.IMMEDIATE))
        assertFalse(LockPolicy.shouldLockForInactivity(false, 0L, 1_000_000L, LockTimeout.SECONDS_30))
    }

    @Test
    fun coldStartLocksWhenEnabled() {
        assertTrue(LockPolicy.shouldLockOnResume(true, true, null, 0L, LockTimeout.MINUTES_15))
    }

    @Test
    fun resumeRespectsTimeout() {
        assertTrue(LockPolicy.shouldLockOnResume(true, false, 1_000L, 1_000L, LockTimeout.IMMEDIATE))
        assertFalse(LockPolicy.shouldLockOnResume(true, false, 1_000L, 20_000L, LockTimeout.SECONDS_30))
        assertTrue(LockPolicy.shouldLockOnResume(true, false, 1_000L, 31_000L, LockTimeout.SECONDS_30))
        assertFalse(LockPolicy.shouldLockOnResume(true, false, null, 31_000L, LockTimeout.SECONDS_30))
    }

    @Test
    fun inactivityInForeground() {
        assertFalse(LockPolicy.shouldLockForInactivity(true, 0L, 59_000L, LockTimeout.IMMEDIATE))
        assertTrue(LockPolicy.shouldLockForInactivity(true, 0L, 60_000L, LockTimeout.IMMEDIATE))
        assertTrue(LockPolicy.shouldLockForInactivity(true, 0L, 30_000L, LockTimeout.SECONDS_30))
        assertFalse(LockPolicy.shouldLockForInactivity(true, 0L, 4 * 60_000L, LockTimeout.MINUTES_5))
    }
}
