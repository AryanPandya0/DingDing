package com.dingding.app

data class NotificationData(
    val packageName: String,
    val title: String?,
    val message: String?,
    val timestamp: Long
)
