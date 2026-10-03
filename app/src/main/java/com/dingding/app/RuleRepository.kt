package com.dingding.app

import android.content.Context
import com.dingding.app.data.DingDingDatabase
import com.dingding.app.data.RuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object RuleRepository {
    private var db: DingDingDatabase? = null

    fun init(context: Context) {
        if (db == null) {
            db = DingDingDatabase.getDatabase(context.applicationContext)
        }
    }

    suspend fun addRule(context: Context, rule: NotificationRule) {
        init(context)
        val dao = db?.ruleDao() ?: return
        val entity = RuleEntity(
            id = rule.id,
            appName = rule.appName,
            packageName = rule.packageName,
            identityType = if (rule.isGroup) "GROUP" else "CONTACT",
            personId = if (rule.personId.isBlank()) rule.personName.trim().lowercase() else rule.personId,
            personName = rule.personName,
            soundName = rule.soundName,
            soundUri = rule.soundUri,
            isGroup = rule.isGroup,
            enabled = rule.enabled,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertRule(entity)
    }

    suspend fun getRules(context: Context): List<NotificationRule> {
        init(context)
        val dao = db?.ruleDao() ?: return emptyList()
        return dao.getAllRules().map { it.toNotificationRule() }
    }

    suspend fun getRulesForApp(context: Context, packageName: String): List<NotificationRule> {
        init(context)
        val dao = db?.ruleDao() ?: return emptyList()
        return dao.getRulesForApp(packageName).map { it.toNotificationRule() }
    }

    suspend fun deleteRule(context: Context, id: Long) {
        init(context)
        db?.ruleDao()?.deleteRule(id)
    }

    fun findRule(context: Context, packageName: String, identifier: String): NotificationRule? {
        init(context)
        val dao = db?.ruleDao() ?: return null
        return try {
            runBlocking(Dispatchers.IO) {
                dao.findActiveRule(packageName, identifier.trim().lowercase())
                    ?: dao.findActiveRule(packageName, identifier.trim())
            }?.toNotificationRule()
        } catch (e: Exception) {
            null
        }
    }
}

private fun RuleEntity.toNotificationRule(): NotificationRule {
    return NotificationRule(
        id = this.id,
        appName = this.appName,
        packageName = this.packageName,
        personId = this.personId,
        personName = this.personName,
        soundName = this.soundName,
        soundUri = this.soundUri,
        isGroup = this.isGroup,
        enabled = this.enabled
    )
}
