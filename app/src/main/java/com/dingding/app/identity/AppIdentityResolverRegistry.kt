package com.dingding.app.identity

object AppIdentityResolverRegistry {
    private val resolvers = mapOf(
        "com.whatsapp" to WhatsAppIdentityResolver,
        "org.telegram.messenger" to TelegramIdentityResolver,
        "com.instagram.android" to InstagramIdentityResolver,
        "com.discord" to DiscordIdentityResolver
    )

    fun getResolver(packageName: String): AppIdentityResolver? {
        return resolvers[packageName]
    }
}
