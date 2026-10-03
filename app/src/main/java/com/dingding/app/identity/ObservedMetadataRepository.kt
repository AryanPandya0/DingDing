package com.dingding.app.identity

import android.content.Context
import com.dingding.app.data.DingDingDatabase
import com.dingding.app.data.ObservedEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object ObservedMetadataRepository {
    private var db: DingDingDatabase? = null

    fun init(context: Context) {
        if (db == null) {
            db = DingDingDatabase.getDatabase(context.applicationContext)
        }
    }

    fun recordPerson(context: Context, packageName: String, id: String, name: String) {
        if (name.isBlank()) return
        init(context)
        try {
            runBlocking(Dispatchers.IO) {
                db?.observedDao()?.insertObserved(
                    ObservedEntity(
                        packageName = packageName,
                        identityId = id,
                        displayName = name.trim(),
                        identityType = "PERSON",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun recordGroup(context: Context, packageName: String, id: String, name: String) {
        if (name.isBlank()) return
        init(context)
        try {
            runBlocking(Dispatchers.IO) {
                db?.observedDao()?.insertObserved(
                    ObservedEntity(
                        packageName = packageName,
                        identityId = id,
                        displayName = name.trim(),
                        identityType = "GROUP",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun getObservedPeople(context: Context, packageName: String): List<AppIdentity> {
        init(context)
        return try {
            runBlocking(Dispatchers.IO) {
                db?.observedDao()?.getObserved(packageName, "PERSON")?.map {
                    AppIdentity(
                        id = it.identityId,
                        displayName = it.displayName,
                        type = IdentityType.PERSON,
                        packageName = it.packageName,
                        isMetadataBased = true
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getGroups(context: Context, packageName: String): List<AppIdentity> {
        init(context)
        return try {
            runBlocking(Dispatchers.IO) {
                db?.observedDao()?.getObserved(packageName, "GROUP")?.map {
                    AppIdentity(
                        id = it.identityId,
                        displayName = it.displayName,
                        type = IdentityType.GROUP,
                        packageName = it.packageName,
                        isMetadataBased = true
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
