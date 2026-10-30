package im.isolux.opencall.smoke

import android.provider.CallLog
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import im.isolux.opencall.data.CallLogEntry
import im.isolux.opencall.data.Contact
import im.isolux.opencall.data.ContactPhone
import im.isolux.opencall.ui.calls.CallsScreen
import im.isolux.opencall.ui.contacts.ContactsScreen
import im.isolux.opencall.ui.search.SearchScreen
import im.isolux.opencall.ui.theme.OpenCallTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Les listes doivent tenir avec un carnet d'adresses et un historique volumineux. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-xxhdpi")
class LargeDataTest {

    @get:Rule
    val compose = createComposeRule()

    private val contacts = (1..3000).map { i ->
        Contact(i.toLong(), "Contact %04d".format(i), i % 50 == 0, null, listOf(ContactPhone("+3361%07d".format(i), "Mobile")))
    }
    private val calls = (1..300).map { i ->
        CallLogEntry(i.toLong(), "+3361%07d".format(i), "Contact %04d".format(i), CallLog.Calls.INCOMING_TYPE, 1_700_000_000_000 - i * 60_000L, 30)
    }

    @Test fun contactsVolumineux() {
        compose.setContent { OpenCallTheme { ContactsScreen(true, contacts, { false }, {}, {}, {}, {}, PaddingValues()) } }
        compose.waitForIdle()
    }

    @Test fun historiqueVolumineux() {
        compose.setContent { OpenCallTheme { CallsScreen(true, calls, { false }, {}, {}, {}, {}, PaddingValues()) } }
        compose.waitForIdle()
    }

    @Test fun rechercheVolumineuse() {
        compose.setContent { OpenCallTheme { SearchScreen(contacts, calls, { false }, {}, {}, {}, PaddingValues()) } }
        compose.waitForIdle()
    }
}
