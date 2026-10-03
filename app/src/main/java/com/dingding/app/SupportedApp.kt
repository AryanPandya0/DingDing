package com.dingding.app

data class SupportedApp(
    val name: String,
    val packageName: String
)

object SupportedApps {
    val apps = listOf(
        SupportedApp(
            name = "WhatsApp",
            packageName = "com.whatsapp"
        ),
        SupportedApp(
            name = "Telegram",
            packageName = "org.telegram.messenger"
        ),
        SupportedApp(
            name = "Instagram",
            packageName = "com.instagram.android"
        ),
        SupportedApp(
            name = "Discord",
            packageName = "com.discord"
        )
    )
}
