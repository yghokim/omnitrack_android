package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTStringStringEntryDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import java.util.*

/**
 * Created by younghokim on 2017. 11. 2..
 */
class AttributeTypeAdapter(val gson: Lazy<Gson>) : TypeAdapter<OTAttributeDAO>() {
    override fun read(reader: JsonReader): OTAttributeDAO {
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
                    "trackerId" -> reader.nextString()
                    RealmDatabaseManager.FIELD_NAME -> dao.name = reader.nextString()
                    "isRequired" -> dao.isRequired = reader.nextBoolean()
                    RealmDatabaseManager.FIELD_POSITION -> dao.position = reader.nextInt()
                    "type" -> dao.type = reader.nextInt()
                    "fallbackPolicy" -> dao.fallbackValuePolicy = reader.nextInt()
                    "fallbackPreset" -> dao.fallbackPresetSerializedValue = reader.nextString()
                    RealmDatabaseManager.FIELD_USER_CREATED_AT -> dao.userCreatedAt = reader.nextLong()
                    RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> dao.updatedAt = reader.nextLong()
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
                                    "k" -> entry.key = reader.nextString()
                                    "v" -> entry.value = reader.nextString()
                                }
                            }
                            reader.endObject()
                            list.add(entry)
                        }
                        reader.endArray()
                        dao.properties.addAll(list)
                    }
                }
            }
        }
        reader.endObject()

        return dao
    }

    override fun write(out: JsonWriter, value: OTAttributeDAO) {
        out.beginObject()

        out.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(value.objectId)
        out.name("localId").value(value.localId)
        out.name("trackerId").value(value.trackerId)
        out.name(RealmDatabaseManager.FIELD_NAME).value(value.name)
        out.name("isRequired").value(value.isRequired)
        out.name(RealmDatabaseManager.FIELD_POSITION).value(value.position)
        out.name("fallbackPolicy").value(value.fallbackValuePolicy)
        out.name("fallbackPreset").value(value.fallbackPresetSerializedValue)
        out.name("type").value(value.type)
        out.name(RealmDatabaseManager.FIELD_USER_CREATED_AT).value(value.userCreatedAt)
        out.name("connection").jsonValue(value.serializedConnection)
        out.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(value.updatedAt)
        out.name("properties").beginArray()

        value.properties.forEach { prop ->
            out.beginObject()
            out.name("id").value(prop.id)
            out.name("k").value(prop.key)
            out.name("v").value(prop.value)
            out.endObject()
        }

        out.endArray()

        out.endObject()
    }
}