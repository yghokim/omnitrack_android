package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTItemValueEntryDAO
import kr.ac.snu.hcil.omnitrack.utils.getBooleanCompat
import kr.ac.snu.hcil.omnitrack.utils.getLongCompat
import kr.ac.snu.hcil.omnitrack.utils.getStringCompat
import java.util.*

/**
 * Created by younghokim on 2017. 11. 4..
 */
class ItemTypeAdapter(isServerMode: Boolean) : ServerCompatibleTypeAdapter<OTItemDAO>(isServerMode) {

    override fun read(reader: JsonReader, isServerMode: Boolean): OTItemDAO {
        val dao = OTItemDAO()
        reader.beginObject()
        while(reader.hasNext())
        {
            when(reader.nextName())
            {
                RealmDatabaseManager.FIELD_TRACKER_ID, "tracker" -> dao.trackerId = reader.nextString()
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
                                "attrLocalId" -> key = reader.nextString()
                                "sVal" -> {
                                    if (reader.peek() == JsonToken.NULL) {
                                        reader.skipValue()
                                    } else serializedValue = reader.nextString()
                                }
                            }
                        }
                        reader.endObject()

                        if(key!=null && serializedValue != null)
                        {
                            val entry = OTItemValueEntryDAO()
                            entry.id = UUID.randomUUID().toString()
                            entry.key = key
                            entry.value = serializedValue
                            dao.fieldValueEntries.add(entry)
                        }
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()

            }
        }
        reader.endObject()

        return dao
    }

    override fun write(writer: JsonWriter, value: OTItemDAO, isServerMode: Boolean) {
        writer.beginObject()
        writer.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(value.objectId)
        writer.name(if (isServerMode) "tracker" else RealmDatabaseManager.FIELD_TRACKER_ID).value(value.trackerId)
        writer.name(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN).value(value.removed)
        writer.name(RealmDatabaseManager.FIELD_TIMESTAMP_LONG).value(value.timestamp)
        writer.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(value.userUpdatedAt)


        if (!isServerMode)
            writer.name(RealmDatabaseManager.FIELD_SYNCHRONIZED_AT).value(value.synchronizedAt)

        writer.name("deviceId").value(value.deviceId)
        writer.name("source").value(value.source)
        writer.name("dataTable").beginArray()

        for (entry in value.fieldValueEntries)
        {
            writer.beginObject()
            writer.name("attrLocalId").value(entry.key)
            writer.name("sVal").value(entry.value)
            writer.endObject()
        }

        writer.endArray()
        writer.endObject()
    }

    override fun applyToManagedDao(json: JsonObject, applyTo: OTItemDAO) {
        json.keySet().forEach { key ->
            when (key) {
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN -> applyTo.removed = json.getBooleanCompat(key) ?: false
                "tracker", RealmDatabaseManager.FIELD_TRACKER_ID -> applyTo.trackerId = json.getStringCompat(key)
                RealmDatabaseManager.FIELD_TIMESTAMP_LONG -> json.getLongCompat(key)?.let { applyTo.timestamp = it }
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> applyTo.userUpdatedAt = json.getLongCompat(key) ?: 0
                RealmDatabaseManager.FIELD_SYNCHRONIZED_AT -> applyTo.synchronizedAt = json.getLongCompat(key)
                "deviceId" -> applyTo.deviceId = json.getStringCompat(key)
                "source" -> applyTo.source = json.getStringCompat(key)
                "dataTable" -> {
                    val jsonList = try {
                        json[key]?.asJsonArray
                    } catch (ex: Exception) {
                        null
                    }
                    if (jsonList != null) {
                        val copiedEntries = ArrayList<OTItemValueEntryDAO>(applyTo.fieldValueEntries)
                        applyTo.fieldValueEntries.clear()

                        //synchronize
                        jsonList.forEach { entryJson ->
                            val entryJsonObj = entryJson.asJsonObject
                            val matchedDao = copiedEntries.find { it.key == entryJsonObj.getStringCompat("attrLocalId") }
                            if (matchedDao != null) {
                                //update entry
                                matchedDao.value = entryJsonObj.getStringCompat("sVal")
                                applyTo.fieldValueEntries.add(matchedDao)
                                copiedEntries.remove(matchedDao)
                            } else {
                                //add new entry
                                val newEntry = applyTo.realm.createObject(OTItemValueEntryDAO::class.java, UUID.randomUUID().toString())
                                newEntry.key = entryJsonObj.getStringCompat("attrLocalId") ?: ""
                                newEntry.value = entryJsonObj.getStringCompat("sVal")
                                applyTo.fieldValueEntries.add(newEntry)
                            }
                        }

                        //deal with dangling attributes
                        copiedEntries.forEach {
                            it.deleteFromRealm()
                        }

                    }
                }
            }
        }
    }
}