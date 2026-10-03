package com.dingding.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observed_identities")
data class ObservedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val identityId: String,
    val displayName: String,
    val identityType: String, // "PERSON" or "GROUP"
    val timestamp: Long = System.currentTimeMillis()
)
