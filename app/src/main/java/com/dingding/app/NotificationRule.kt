package com.dingding.app

data class NotificationRule(
    val id: Long = System.currentTimeMillis(),
    val appName: String,
    val packageName: String,
    val personId: String = "",
    val personName: String,
    val soundName: String,
    val soundUri: String? = null,
    val isGroup: Boolean = false,
    val enabled: Boolean = true
)
