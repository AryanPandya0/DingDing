package com.dingding.app

object RuleRepository {

    private val rules = mutableListOf<NotificationRule>()

    fun addRule(rule: NotificationRule) {
        rules.add(rule)
    }

    fun getRules(): List<NotificationRule> {
        return rules.toList()
    }

    fun deleteRule(id: Long) {
        rules.removeAll { it.id == id }
    }

    fun findRule(
        packageName: String,
        personName: String
    ): NotificationRule? {

        return rules.find {
            it.packageName == packageName &&
                    it.personName.equals(personName, ignoreCase = true) &&
                    it.enabled
        }
    }
}
