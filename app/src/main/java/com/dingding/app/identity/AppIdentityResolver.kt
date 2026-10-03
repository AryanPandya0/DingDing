package com.dingding.app.identity

import android.content.Context
import android.service.notification.StatusBarNotification

data class ParsedNotification(
    val senderId: String,
    val senderName: String,
    val isGroup: Boolean,
    val groupName: String?
)

interface AppIdentityResolver {
    val packageName: String
    val supportsDeviceContacts: Boolean

    fun searchPeople(context: Context, query: String): List<AppIdentity>
    fun getObservedPeople(context: Context): List<AppIdentity>
    fun getGroups(context: Context): List<AppIdentity>
    fun processNotification(sbn: StatusBarNotification): ParsedNotification?
    fun isRelevantNotification(sbn: StatusBarNotification): Boolean
}
