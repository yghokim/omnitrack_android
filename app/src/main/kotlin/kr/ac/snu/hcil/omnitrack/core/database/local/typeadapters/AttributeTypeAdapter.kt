package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.OTStringStringEntryDAO
import kr.ac.snu.hcil.omnitrack.utils.getBooleanCompat
import kr.ac.snu.hcil.omnitrack.utils.getIntCompat
import kr.ac.snu.hcil.omnitrack.utils.getStringCompat
import java.util.*

/**
 * Created by younghokim on 2017. 11. 2..
 */
class AttributeTypeAdapter(isServerMode: Boolean, val gson: Lazy<Gson>) : ServerCompatibleTypeAdapter<OTAttributeDAO>(isServerMode) {

    override fun read(reader: JsonReader, isServerMode: Boolean): OTAttributeDAO {
        val dao = OTAttributeDAO()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue()
            } else {
                when (name) {
                    RealmDatabaseManager.FIELD_OBJECT_ID -> dao.objectId = reader.nextString()
                    "localId" -> dao.localId = reader.nextString()
                    "trackerId", "tracker" -> dao.trackerId = reader.nextString()
                    RealmDatabaseManager.FIELD_NAME -> dao.name = reader.nextString()
                    "isRequired" -> dao.isRequired = reader.nextBoolean()
                    RealmDatabaseManager.FIELD_POSITION -> dao.position = reader.nextInt()
                    "type" -> dao.type = reader.nextInt()
                    "fallbackPolicy" -> dao.fallbackValuePolicy = reader.nextInt()
                    "fallbackPreset" -> dao.fallbackPresetSerializedValue = reader.nextString()
                    RealmDatabaseManager.FIELD_USER_CREATED_AT -> dao.userCreatedAt = reader.nextLong()
                    RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> dao.userUpdatedAt = reader.nextLong()
                    RealmDatabaseManager.FIELD_IS_HIDDEN -> dao.isHidden = reader.nextBoolean()
                    RealmDatabaseManager.FIELD_IS_IN_TRASHCAN -> dao.isInTrashcan = reader.nextBoolean()

                    "connection" -> {
                        dao.serializedConnection = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                    }
                    "properties" -> {
                        val list = ArrayList<OTStringStringEntryDAO>()
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val entry = OTStringStringEntryDAO()
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "id" -> entry.id = reader.nextString()
                                    "key" -> entry.key = reader.nextString()
                                    "sVal" -> entry.value = reader.nextString()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()

                            if (isServerMode || entry.id.isBlank()) {
                                entry.id = UUID.randomUUID().toString()
                            }

                            list.add(entry)
                        }
                        reader.endArray()
                        dao.properties.addAll(list)
                    }
                    else -> reader.skipValue()
                }
            }
        }
        reader.endObject()

        return dao
    }

    override fun write(writer: JsonWriter, value: OTAttributeDAO, isServerMode: Boolean) {
        writer.beginObject()

        writer.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(value.objectId)
        writer.name("localId").value(value.localId)
        writer.name("type").value(value.type)
        writer.name(if (isServerMode) "trackerId" else "tracker").value(value.trackerId)
        writer.name(RealmDatabaseManager.FIELD_NAME).value(value.name)
        writer.name("isRequired").value(value.isRequired)
        writer.name(RealmDatabaseManager.FIELD_POSITION).value(value.position)
        writer.name("fallbackPolicy").value(value.fallbackValuePolicy)
        writer.name("fallbackPreset").value(value.fallbackPresetSerializedValue)

        writer.name(RealmDatabaseManager.FIELD_IS_HIDDEN).value(value.isHidden)
        writer.name(RealmDatabaseManager.FIELD_IS_IN_TRASHCAN).value(value.isInTrashcan)

        writer.name(RealmDatabaseManager.FIELD_USER_CREATED_AT).value(value.userCreatedAt)
        writer.name("connection").jsonValue(value.serializedConnection)
        writer.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(value.userUpdatedAt)
        writer.name("properties").beginArray()

        value.properties.forEach { prop ->
            writer.beginObject()

            if (!isServerMode)
                writer.name("id").value(prop.id)

            writer.name("key").value(prop.key)
            writer.name("sVal").value(prop.value)
            writer.endObject()
        }

        writer.endArray()

        writer.endObject()
    }

    override fun applyToManagedDao(json: JsonObject, applyTo: OTAttributeDAO) {
        json.keySet().forEach { key ->
            when (key) {
                RealmDatabaseManager.FIELD_NAME -> applyTo.name = json.getStringCompat(key) ?: ""
                "isRequired" -> applyTo.isRequired = json.getBooleanCompat(key) ?: false
                RealmDatabaseManager.FIELD_POSITION -> applyTo.position = json.getIntCompat(key) ?: 0
                "connection" -> applyTo.serializedConnection = json.getStringCompat(key)
                "type" -> json.getIntCompat(key)?.let { applyTo.type = it }
                "fallbackPolicy" -> applyTo.fallbackValuePolicy = json[key].asInt
                "fallbackPreset" -> applyTo.fallbackPresetSerializedValue = json.getStringCompat(key)

                RealmDatabaseManager.FIELD_IS_IN_TRASHCAN -> applyTo.isInTrashcan = json.getBooleanCompat(key) ?: false
                RealmDatabaseManager.FIELD_IS_HIDDEN -> applyTo.isHidden = json.getBooleanCompat(key) ?: false

                "properties" -> {
                    if (json[key].isJsonArray) {
                        val jsonProperties = json[key].asJsonArray.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }
                        jsonProperties.forEach { jsonProp ->
                            applyTo.setPropertySerializedValue(jsonProp["key"].asString, jsonProp["sVal"].asString)
                        }

                        val toRemoves = applyTo.properties.filter { prop -> jsonProperties.find { it["key"].asString == prop.key } == null }
                        applyTo.properties.removeAll(toRemoves)
                        toRemoves.forEach { it.deleteFromRealm() }
                    }
                }

                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> applyTo.userUpdatedAt = json[key].asLong

            }
        }
    }

}