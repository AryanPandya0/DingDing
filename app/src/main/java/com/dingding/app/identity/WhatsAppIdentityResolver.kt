package com.dingding.app.identity

import android.app.Notification
import android.content.Context
import android.provider.ContactsContract
import android.service.notification.StatusBarNotification
import android.util.Log

object WhatsAppIdentityResolver : AppIdentityResolver {
    override val packageName: String = "com.whatsapp"
    override val supportsDeviceContacts: Boolean = true

    override fun isRelevantNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != packageName) return false
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val category = notification.category

        if (category == Notification.CATEGORY_CALL ||
            title.contains("call", ignoreCase = true) ||
            text.contains("call", ignoreCase = true) ||
            title.contains("incoming", ignoreCase = true)) {
            return false
        }

        if (title.contains("status", ignoreCase = true) ||
            text.contains("viewed your status", ignoreCase = true) ||
            title.contains("WhatsApp Web", ignoreCase = true) ||
            text.contains("Checking for new messages", ignoreCase = true) ||
            text.contains("Waiting for message", ignoreCase = true)) {
            return false
        }

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
            Log.e("WhatsAppResolver", "Error searching contacts", e)
        }
        return results
    }

    override fun getObservedPeople(context: Context): List<AppIdentity> {
        return ObservedMetadataRepository.getObservedPeople(packageName)
    }

    override fun getGroups(context: Context): List<AppIdentity> {
        return ObservedMetadataRepository.getObservedGroups(packageName)
    }

    override fun processNotification(sbn: StatusBarNotification): ParsedNotification? {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return null
        val conversationTitle = extras.getString(Notification.EXTRA_CONVERSATION_TITLE)

        val isGroup = !conversationTitle.isNullOrBlank()
        val groupName = if (isGroup) conversationTitle.trim() else null
        val senderName = title.trim()
        val senderId = senderName.lowercase()

        if (isGroup && groupName != null) {
            ObservedMetadataRepository.recordGroup(packageName, groupName.lowercase(), groupName)
        } else {
            ObservedMetadataRepository.recordPerson(packageName, senderId, senderName)
        }

        return ParsedNotification(
            senderId = if (isGroup) groupName!!.lowercase() else senderId,
            senderName = if (isGroup) groupName!! else senderName,
            isGroup = isGroup,
            groupName = groupName
        )
    }
}
