package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemValueEntryDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import java.util.*

/**
 * Created by younghokim on 2017. 11. 4..
 */
class ItemTypeAdapter: TypeAdapter<OTItemDAO>() {
    override fun read(reader: JsonReader): OTItemDAO {
        val dao = OTItemDAO()
        reader.beginObject()
        while(reader.hasNext())
        {
            when(reader.nextName())
            {
                RealmDatabaseManager.FIELD_TRACKER_ID->dao.trackerId = reader.nextString()
                RealmDatabaseManager.FIELD_OBJECT_ID ->dao.objectId = reader.nextString()
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN -> dao.removed = reader.nextBoolean()
                RealmDatabaseManager.FIELD_SYNCHRONIZED_AT -> dao.synchronizedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_TIMESTAMP_LONG -> dao.timestamp = reader.nextLong()
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> dao.userUpdatedAt = reader.nextLong()
                "deviceId"->dao.deviceId = reader.nextString()
                "source"-> dao.source = reader.nextString()
                "dataTable" ->{
                    reader.beginArray()
                    while(reader.hasNext())
                    {
                        var key: String? = null
                        var serializedValue: String? = null
                        reader.beginObject()
                        while(reader.hasNext())
                        {
                            when(reader.nextName())
                            {
                                "attrId"->key = reader.nextString()
                                "serializedValue"->serializedValue = if(reader.peek() == JsonToken.NULL) null else reader.nextString()
                            }
                        }
                        reader.endObject()

                        if(key!=null && serializedValue != null)
                        {
                            val entry = OTItemValueEntryDAO()
                            entry.id = UUID.randomUUID().toString()
                            entry.key = key
                            entry.value = serializedValue
                        }
                    }
                    reader.endArray()
                }

            }
        }
        reader.endObject()

        return dao
    }

    override fun write(out: JsonWriter, item: OTItemDAO) {
        out.beginObject()
        out.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(item.objectId)
        out.name(RealmDatabaseManager.FIELD_TRACKER_ID).value(item.trackerId)
        out.name(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN).value(item.removed)
        out.name(RealmDatabaseManager.FIELD_TIMESTAMP_LONG).value(item.timestamp)
        out.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(item.userUpdatedAt)
        out.name(RealmDatabaseManager.FIELD_SYNCHRONIZED_AT).value(item.synchronizedAt)
        out.name("deviceId").value(item.deviceId)
        out.name("source").value(item.source)
        out.name("dataTable").beginArray()

        for(entry in item.fieldValueEntries)
        {
            out.beginObject()
            out.name("attrId").value(entry.key)
            out.name("serializedValue").value(entry.value)
            out.endObject()
        }

        out.endArray()
        out.endObject()
    }

}