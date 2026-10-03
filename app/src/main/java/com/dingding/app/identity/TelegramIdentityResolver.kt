package com.dingding.app.identity

import android.app.Notification
import android.content.Context
import android.provider.ContactsContract
import android.service.notification.StatusBarNotification
import android.util.Log

object TelegramIdentityResolver : AppIdentityResolver {
    override val packageName: String = "org.telegram.messenger"
    override val supportsDeviceContacts: Boolean = true

    override fun isRelevantNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != packageName) return false
        val notification = sbn.notification
        val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        return title.isNotBlank()
    }

    override fun searchPeople(context: Context, query: String): List<AppIdentity> {
        val results = mutableListOf<AppIdentity>()
        if (query.isBlank()) return results
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            )
            context.contentResolver.query(uri, projection, selection, selectionArgs, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val seen = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                    if (!name.isNullOrBlank() && !seen.contains(name)) {
                        seen.add(name)
                        results.add(
                            AppIdentity(
                                id = contactId ?: name.trim().lowercase(),
                                displayName = name.trim(),
                                type = IdentityType.PERSON,
                                packageName = packageName,
                                isMetadataBased = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramResolver", "Error searching contacts", e)
        }
        return results
    }

    override fun getObservedPeople(context: Context): List<AppIdentity> {
        return ObservedMetadataRepository.getObservedPeople(context, packageName)
    }

    override fun getGroups(context: Context): List<AppIdentity> {
        return ObservedMetadataRepository.getGroups(context, packageName)
    }

    override fun processNotification(context: Context, sbn: StatusBarNotification): ParsedNotification? {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return null
        val conversationTitle = extras.getString(Notification.EXTRA_CONVERSATION_TITLE)

        val isGroup = !conversationTitle.isNullOrBlank()
        val groupName = if (isGroup) conversationTitle.trim() else null
        val senderName = title.trim()
        val senderId = senderName.lowercase()

        if (isGroup && groupName != null) {
            ObservedMetadataRepository.recordGroup(context, packageName, groupName.lowercase(), groupName)
        } else {
            ObservedMetadataRepository.recordPerson(context, packageName, senderId, senderName)
        }

        return ParsedNotification(
            senderId = if (isGroup) groupName!!.lowercase() else senderId,
            senderName = if (isGroup) groupName!! else senderName,
            isGroup = isGroup,
            groupName = groupName
        )
    }
}
