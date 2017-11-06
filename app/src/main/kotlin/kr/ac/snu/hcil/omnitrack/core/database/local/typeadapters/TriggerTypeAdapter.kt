package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 11. 2..
 */
class TriggerTypeAdapter(isServerMode: Boolean, val realmProvider: Provider<Realm>) : ServerCompatibleTypeAdapter<OTTriggerDAO>(isServerMode) {

    override fun read(reader: JsonReader, isServerMode: Boolean): OTTriggerDAO {
        val dao = OTTriggerDAO()

        val trackerIds = ArrayList<String>()

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                RealmDatabaseManager.FIELD_OBJECT_ID -> dao.objectId = reader.nextString()
                RealmDatabaseManager.FIELD_USER_CREATED_AT -> dao.userCreatedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> dao.userUpdatedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN -> dao.removed = reader.nextBoolean()
                "alias" -> dao.alias = reader.nextString()
                "position" -> dao.position = reader.nextInt()
                "userId" -> dao.userId = reader.nextString()
                "conditionType" -> dao.conditionType = reader.nextInt().toByte()
                "actionType" -> dao.actionType = reader.nextInt().toByte()
                "serializedAction" -> dao.serializedAction = reader.nextString()
                "serializedCondition" -> dao.serializedCondition = reader.nextString()
                "lastTriggeredTime" -> dao.lastTriggeredTime = reader.nextLong()
                "trackers" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        trackerIds.add(reader.nextString())
                    }
                    reader.endArray()
                }
            }
        }
        reader.endObject()

        if (trackerIds.isNotEmpty()) {
            val realm = realmProvider.get()
            dao.trackers.addAll(realm.copyFromRealm(realm.where(OTTrackerDAO::class.java)
                    .equalTo(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN, false)
                    .`in`(RealmDatabaseManager.FIELD_OBJECT_ID, trackerIds.toTypedArray())
                    .findAll()))
            realm.close()
        }

        return dao
    }

    override fun write(writer: JsonWriter, value: OTTriggerDAO, isServerMode: Boolean) {
        writer.beginObject()
        writer.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(value.objectId)
        writer.name("alias").value(value.alias)
        writer.name("position").value(value.position)
        writer.name("userId").value(value.userId)
        writer.name("conditionType").value(value.conditionType)
        writer.name("serializedCondition").value(value.serializedCondition)
        writer.name("actionType").value(value.actionType)
        writer.name("serializedAction").value(value.serializedAction)
        writer.name("lastTriggeredTime").value(value.lastTriggeredTime)
        writer.name(RealmDatabaseManager.FIELD_SYNCHRONIZED_AT).value(value.synchronizedAt)
        writer.name(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN).value(value.removed)
        writer.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(value.userUpdatedAt)
        writer.name(RealmDatabaseManager.FIELD_USER_CREATED_AT).value(value.userCreatedAt)

        writer.name("trackers")
        writer.beginArray()
        for (trackerId in value.trackers.filter { it.removed == false }.map { it.objectId }) {
            writer.value(trackerId)
        }
        writer.endArray()

        writer.endObject()
    }

    override fun applyToManagedDao(json: JsonObject, applyTo: OTTriggerDAO) {

    }
}