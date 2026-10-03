package com.dingding.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val packageName: String,
    val identityType: String, // "CONTACT", "GROUP", "METADATA_IDENTITY"
    val personId: String,     // stable identifier
    val personName: String,   // display name
    val soundName: String,
    val soundUri: String? = null,
    val isGroup: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
