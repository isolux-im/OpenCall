package im.isolux.opencall

import android.app.Application
import im.isolux.opencall.recording.RecordingsRepository
import im.isolux.opencall.telecom.CallNotifications
import kotlin.concurrent.thread

class OpenCallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CallNotifications.createChannels(this)
        thread(name = "opencall-retention") { RecordingsRepository.get(this).applyRetention() }
    }
}
