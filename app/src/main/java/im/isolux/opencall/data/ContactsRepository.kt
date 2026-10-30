package im.isolux.opencall.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone

data class ContactPhone(val number: String, val label: String)

data class Contact(
    val id: Long,
    val name: String,
    val starred: Boolean,
    val photoUri: Uri?,
    val phones: List<ContactPhone>,
)

object ContactsRepository {

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Contacts disposant d'au moins un numéro, triés par nom. */
    fun load(context: Context): List<Contact> {
        if (!hasPermission(context)) return emptyList()
        val projection = arrayOf(
            Phone.CONTACT_ID, Phone.DISPLAY_NAME_PRIMARY, Phone.STARRED, Phone.PHOTO_THUMBNAIL_URI,
            Phone.NUMBER, Phone.TYPE, Phone.LABEL,
        )
        val byId = LinkedHashMap<Long, Contact>()
        try {
            context.contentResolver.query(
                Phone.CONTENT_URI, projection, null, null, "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val number = c.getString(4) ?: continue
                    val label = Phone.getTypeLabel(context.resources, c.getInt(5), c.getString(6)).toString()
                    val existing = byId[id]
                    val phone = ContactPhone(number, label)
                    if (existing == null) {
                        byId[id] = Contact(
                            id = id,
                            name = c.getString(1) ?: number,
                            starred = c.getInt(2) == 1,
                            photoUri = c.getString(3)?.let(Uri::parse),
                            phones = listOf(phone),
                        )
                    } else if (existing.phones.none { it.number.filter(Char::isDigit) == number.filter(Char::isDigit) }) {
                        byId[id] = existing.copy(phones = existing.phones + phone)
                    }
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return byId.values.toList()
    }

    fun lookupName(context: Context, number: String): String? {
        if (!hasPermission(context) || number.isBlank()) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: RuntimeException) {
            null
        }
    }
}
