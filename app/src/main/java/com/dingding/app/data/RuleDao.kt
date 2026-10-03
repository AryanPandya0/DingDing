package com.dingding.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules")
    suspend fun getAllRules(): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE packageName = :packageName")
    suspend fun getRulesForApp(packageName: String): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("SELECT * FROM rules WHERE packageName = :packageName AND (personId = :identifier OR personName = :identifier) AND enabled = 1 LIMIT 1")
    suspend fun findActiveRule(packageName: String, identifier: String): RuleEntity?
}
