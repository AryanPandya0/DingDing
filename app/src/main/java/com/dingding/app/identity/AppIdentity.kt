package com.dingding.app.identity

enum class IdentityType {
    PERSON, GROUP
}

data class AppIdentity(
    val id: String,
    val displayName: String,
    val type: IdentityType,
    val packageName: String,
    val isMetadataBased: Boolean = false
)
