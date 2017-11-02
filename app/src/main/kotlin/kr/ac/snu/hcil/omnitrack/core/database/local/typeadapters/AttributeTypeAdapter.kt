package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTStringStringEntryDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import java.util.ArrayList

/**
 * Created by younghokim on 2017. 11. 2..
 */
class AttributeTypeAdapter : TypeAdapter<OTAttributeDAO>() {
    override fun read(reader: JsonReader): OTAttributeDAO {
        val dao = OTAttributeDAO()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                RealmDatabaseManager.FIELD_OBJECT_ID -> dao.objectId = reader.nextString()
                "localId" -> dao.localId = reader.nextString()
                "trackerId" -> dao.trackerId = reader.nextString()
                "name" -> dao.name = reader.nextString()
                "isRequired" -> dao.isRequired = reader.nextBoolean()
                "pos" -> dao.position = reader.nextInt()
                "type" -> dao.type = reader.nextInt()
                "fallbackPolicy" -> dao.fallbackValuePolicy = reader.nextInt()
                "fallbackPreset" -> dao.fallbackPresetSerializedValue = reader.nextString()
                RealmDatabaseManager.FIELD_USER_CREATED_AT -> dao.userCreatedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> dao.updatedAt = reader.nextLong()
                "req" -> dao.isRequired = reader.nextBoolean()
                "conn" -> dao.serializedConnection = reader.nextString()
                "props" -> {
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
        reader.endObject()

        return dao
    }

    override fun write(out: JsonWriter, value: OTAttributeDAO) {
        out.beginObject()

        out.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(value.objectId)
        out.name("localId").value(value.localId)
        out.name("trackerId").value(value.trackerId)
        out.name("name").value(value.name)
        out.name("isRequired").value(value.isRequired)
        out.name("pos").value(value.position)
        out.name("fallbackPolicy").value(value.fallbackValuePolicy)
        out.name("fallbackPreset").value(value.fallbackPresetSerializedValue)
        out.name("type").value(value.type)
        out.name(RealmDatabaseManager.FIELD_USER_CREATED_AT).value(value.userCreatedAt)
        out.name("req").value(value.isRequired)
        out.name("conn").value(value.serializedConnection)
        out.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(value.updatedAt)
        out.name("props").beginArray()

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