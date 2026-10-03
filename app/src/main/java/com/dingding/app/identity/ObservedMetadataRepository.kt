package com.dingding.app.identity

object ObservedMetadataRepository {
    private val observedPeople = mutableMapOf<String, MutableMap<String, AppIdentity>>()
    private val observedGroups = mutableMapOf<String, MutableMap<String, AppIdentity>>()

    fun recordPerson(packageName: String, id: String, name: String) {
        if (name.isBlank()) return
        val appMap = observedPeople.getOrPut(packageName) { mutableMapOf() }
        appMap[id] = AppIdentity(
            id = id,
            displayName = name.trim(),
            type = IdentityType.PERSON,
            packageName = packageName,
            isMetadataBased = true
        )
    }

    fun recordGroup(packageName: String, id: String, name: String) {
        if (name.isBlank()) return
        val appMap = observedGroups.getOrPut(packageName) { mutableMapOf() }
        appMap[id] = AppIdentity(
            id = id,
            displayName = name.trim(),
            type = IdentityType.GROUP,
            packageName = packageName,
            isMetadataBased = true
        )
    }

    fun getObservedPeople(packageName: String): List<AppIdentity> {
        return observedPeople[packageName]?.values?.sortedBy { it.displayName } ?: emptyList()
    }

    fun getObservedGroups(packageName: String): List<AppIdentity> {
        return observedGroups[packageName]?.values?.sortedBy { it.displayName } ?: emptyList()
    }
}
