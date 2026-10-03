package com.dingding.app.identity

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification

object DiscordIdentityResolver : AppIdentityResolver {
    override val packageName: String = "com.discord"
    override val supportsDeviceContacts: Boolean = false

    override fun isRelevantNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != packageName) return false
        val title = sbn.notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
        return title.isNotBlank()
    }

    override fun searchPeople(context: Context, query: String): List<AppIdentity> {
        return emptyList()
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
