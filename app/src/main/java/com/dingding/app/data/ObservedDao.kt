package com.dingding.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ObservedDao {
    @Query("SELECT * FROM observed_identities WHERE packageName = :packageName AND identityType = :type ORDER BY timestamp DESC")
    suspend fun getObserved(packageName: String, type: String): List<ObservedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObserved(entity: ObservedEntity): Long
}
