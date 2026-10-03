package com.dingding.app

data class SupportedApp(
    val id: String,
    val name: String,
    val packageName: String,
    val iconEmoji: String,
    val description: String
)

object SupportedApps {
    val apps = listOf(
        SupportedApp(
            id = "whatsapp",
            name = "WhatsApp",
            packageName = "com.whatsapp",
            iconEmoji = "🟢",
            description = "Chat & messaging"
        ),
        SupportedApp(
            id = "telegram",
            name = "Telegram",
            packageName = "org.telegram.messenger",
            iconEmoji = "✈️",
            description = "Cloud messaging"
        ),
        SupportedApp(
            id = "instagram",
            name = "Instagram",
            packageName = "com.instagram.android",
            iconEmoji = "📸",
            description = "Direct messages & alerts"
        ),
        SupportedApp(
            id = "discord",
            name = "Discord",
            packageName = "com.discord",
            iconEmoji = "🎮",
            description = "Community & DMs"
        )
    )

    fun findByPackage(packageName: String): SupportedApp? {
        return apps.find { it.packageName == packageName }
    }
}
