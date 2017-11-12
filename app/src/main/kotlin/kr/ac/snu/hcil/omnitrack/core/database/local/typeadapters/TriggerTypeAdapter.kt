package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 11. 2..
 */
class TriggerTypeAdapter(isServerMode: Boolean, val gson: Lazy<Gson>, val realmProvider: Provider<Realm>, val triggerSystemManager: Lazy<OTTriggerSystemManager>) : ServerCompatibleTypeAdapter<OTTriggerDAO>(isServerMode) {

    override fun read(reader: JsonReader, isServerMode: Boolean): OTTriggerDAO {
        val dao = OTTriggerDAO()

        val trackerIds = ArrayList<String>()

        reader.beginObject()
        while (reader.hasNext()) {
            val nextName = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue()
                continue
            }
            when (nextName) {
                RealmDatabaseManager.FIELD_OBJECT_ID -> dao.objectId = reader.nextString()
                RealmDatabaseManager.FIELD_USER_CREATED_AT -> dao.userCreatedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> dao.userUpdatedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN -> dao.removed = reader.nextBoolean()
                "alias" -> dao.alias = reader.nextString()
                "position" -> dao.position = reader.nextInt()
                "userId", "user" -> dao.userId = reader.nextString()
                "isOn" -> dao.isOn = reader.nextBoolean()
                "conditionType" -> dao.conditionType = reader.nextInt().toByte()
                "actionType" -> dao.actionType = reader.nextInt().toByte()
                "action", "serializedAction" -> dao.serializedAction = reader.nextString()
                "condition", "serializedCondition" -> dao.serializedCondition = reader.nextString()
                "lastTriggeredTime" -> dao.lastTriggeredTime = reader.nextLong()
                "lockedProperties" -> dao.serializedLockedPropertyInfo = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                "trackers" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        trackerIds.add(reader.nextString())
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
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
        writer.name(RealmDatabaseManager.FIELD_POSITION).value(value.position)
        writer.name(if (isServerMode) "user" else "userId").value(value.userId)
        writer.name("isOn").value(value.isOn)
        writer.name("conditionType").value(value.conditionType)
        writer.name("condition").value(value.serializedCondition)
        writer.name("actionType").value(value.actionType)
        writer.name("action").value(value.serializedAction)
        writer.name("lastTriggeredTime").value(value.lastTriggeredTime)
        writer.name(RealmDatabaseManager.FIELD_LOCKED_PROPERTIES_SERIALIZED).jsonValue(value.serializedLockedPropertyInfo)
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
        var switchChanged = false
        var removedFlagChanged = false
        var conditionChanged = false
        json.keySet().forEach{
            key->
            when(key)
            {
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN-> {
                    if (!json.get(key).isJsonNull) {
                        val value = json.get(key)?.asBoolean == true
                        if(applyTo.removed != value)
                        {
                            applyTo.removed = value
                            removedFlagChanged = true
                        }
                    }
                }
                "user", RealmDatabaseManager.FIELD_USER_ID->applyTo.userId = json.get(key)?.asString
                RealmDatabaseManager.FIELD_USER_CREATED_AT -> applyTo.userCreatedAt = json[key]?.asLong?:0
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG->applyTo.userUpdatedAt = json[key]?.asLong?:0
                RealmDatabaseManager.FIELD_POSITION->applyTo.position = json[key]?.asInt ?: 0
                RealmDatabaseManager.FIELD_SYNCHRONIZED_AT->applyTo.synchronizedAt = json[key]?.asLong
                "alias" -> json[key]?.let{ applyTo.alias = it.asString }
                RealmDatabaseManager.FIELD_LOCKED_PROPERTIES_SERIALIZED->applyTo.serializedLockedPropertyInfo = json[key]?.toString() ?: "null"
                "isOn" ->{
                    val switchValue = json[key]?.asBoolean == true
                    if(switchValue != applyTo.isOn)
                    {
                        applyTo.isOn = switchValue
                        switchChanged = true
                    }
                }
                "trackers"->{
                    val jsonList = try{json[key]?.asJsonArray}catch(ex:Exception){null}
                    applyTo.trackers.clear()
                    if(jsonList!=null)
                    {
                        val realm = realmProvider.get()
                        applyTo.trackers.addAll(realm.copyFromRealm(realm.where(OTTrackerDAO::class.java)
                                .equalTo(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN, false)
                                .`in`(RealmDatabaseManager.FIELD_OBJECT_ID, jsonList.map { it.asString }.toTypedArray())
                                .findAll()))
                        realm.close()
                    }
                }
                "conditionType" -> {
                    val value = json[key].asByte
                    if (applyTo.conditionType != value) {
                        conditionChanged = true
                        applyTo.conditionType = value
                    }
                }
                "condition" -> {
                    val value = json[key].toString()
                    if (applyTo.serializedCondition != value) {
                        applyTo.serializedCondition = value
                    }
                }
                "actionType" -> {
                    applyTo.actionType = json[key].asByte
                }
                "action" -> {
                    applyTo.serializedAction = json[key].toString()
                }
            }
        }

        if (applyTo.removed) {
            triggerSystemManager.get().tryCheckOutFromSystem(applyTo)
        } else {
            triggerSystemManager.get().tryCheckInToSystem(applyTo)
        }
    }
}