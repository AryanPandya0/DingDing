package com.dingding.app

data class NotificationRule(
    val id: Long = System.currentTimeMillis(),
    val appName: String,
    val packageName: String,
    val personName: String,
    val soundName: String,
    val enabled: Boolean = true
)
