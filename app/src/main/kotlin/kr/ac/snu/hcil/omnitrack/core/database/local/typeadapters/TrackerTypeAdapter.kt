package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import io.realm.RealmList
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import java.util.*

/**
 * Created by younghokim on 2017. 11. 3..
 */
class TrackerTypeAdapter(isServerMode: Boolean, val attributeTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTAttributeDAO>>, val gson: Lazy<Gson>) : ServerCompatibleTypeAdapter<OTTrackerDAO>(isServerMode) {

    override fun read(reader: JsonReader, isServerMode: Boolean): OTTrackerDAO {
        val dao = OTTrackerDAO()

        reader.beginObject()
        while(reader.hasNext())
        {
            when(reader.nextName())
            {
                RealmDatabaseManager.FIELD_OBJECT_ID -> dao.objectId = reader.nextString()
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN -> dao.removed = reader.nextBoolean()
                if (isServerMode) "user" else RealmDatabaseManager.FIELD_USER_ID -> dao.userId = reader.nextString()
                RealmDatabaseManager.FIELD_USER_CREATED_AT -> dao.userCreatedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_SYNCHRONIZED_AT -> dao.synchronizedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> dao.userUpdatedAt = reader.nextLong()
                RealmDatabaseManager.FIELD_POSITION -> dao.position = reader.nextInt()
                RealmDatabaseManager.FIELD_NAME -> dao.name = reader.nextString()
                "color" -> dao.color = reader.nextInt()
                "isBookmarked" -> dao.isBookmarked = reader.nextBoolean()
                RealmDatabaseManager.FIELD_LOCKED_PROPERTIES_SERIALIZED -> dao.serializedLockedPropertyInfo = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                "flags" -> dao.serializedCreationFlags = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                "attributes" -> {
                    reader.beginArray()

                    while(reader.hasNext())
                    {
                        dao.attributes.add(attributeTypeAdapter.get().read(reader))
                    }

                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return dao
    }

    override fun write(writer: JsonWriter, value: OTTrackerDAO, isServerMode: Boolean) {
        writer.beginObject()

        writer.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(value.objectId)
        writer.name(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN).value(value.removed)
        writer.name(if (isServerMode) "user" else RealmDatabaseManager.FIELD_USER_ID).value(value.userId)
        writer.name(RealmDatabaseManager.FIELD_USER_CREATED_AT).value(value.userCreatedAt)
        writer.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(value.userUpdatedAt)

        if (!isServerMode)
            writer.name(RealmDatabaseManager.FIELD_SYNCHRONIZED_AT).value(value.synchronizedAt)

        writer.name(RealmDatabaseManager.FIELD_POSITION).value(value.position)
        writer.name(RealmDatabaseManager.FIELD_NAME).value(value.name)
        writer.name("color").value(value.color)
        writer.name("isBookmarked").value(value.isBookmarked)
        writer.name(RealmDatabaseManager.FIELD_LOCKED_PROPERTIES_SERIALIZED).jsonValue(value.serializedLockedPropertyInfo)
        writer.name("flags").jsonValue(value.serializedCreationFlags)
        writer.name("attributes").beginArray()
        for (attribute in value.attributes)
            {
                attributeTypeAdapter.get().write(writer, attribute)
            }
        writer.endArray()

        writer.endObject()
    }

    override fun applyToManagedDao(json: JsonObject, applyTo: OTTrackerDAO) {
        json.keySet().forEach{
            key->
            when(key)
            {
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN->applyTo.removed = json.get(key)?.asBoolean?:false
                "user", RealmDatabaseManager.FIELD_USER_ID->applyTo.userId = json.get(key)?.asString
                RealmDatabaseManager.FIELD_USER_CREATED_AT -> applyTo.userCreatedAt = json[key]?.asLong?:0
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG->applyTo.userUpdatedAt = json[key]?.asLong?:0
                RealmDatabaseManager.FIELD_POSITION->applyTo.position = json[key]?.asInt ?: 0
                RealmDatabaseManager.FIELD_SYNCHRONIZED_AT->applyTo.synchronizedAt = json[key]?.asLong
                RealmDatabaseManager.FIELD_NAME -> json[key]?.let{ applyTo.name = it.asString }
                "color"->json[key]?.let{applyTo.color = it.asInt}
                "isBookmarked"->json[key]?.let{applyTo.isBookmarked = it.asBoolean}
                "lockedProperties"->applyTo.serializedLockedPropertyInfo = json[key]?.toString() ?: "null"
                "flags"->applyTo.serializedCreationFlags = json[key]?.toString() ?: "null"
                "attributes"->{
                    val jsonList = try{json[key]?.asJsonArray}catch(ex:Exception){null}
                    if(jsonList!=null)
                    {
                        val copiedAttributes = ArrayList<OTAttributeDAO>(applyTo.attributes)
                        applyTo.attributes.clear()

                        //synchronize
                        jsonList.forEach {
                            attrJson->
                            val attrJsonObj = attrJson.asJsonObject
                            val matchedDao = copiedAttributes.find { it.localId == attrJsonObj[RealmDatabaseManager.FIELD_OBJECT_ID].asString }
                            if(matchedDao != null)
                            {
                                //update attribute
                                attributeTypeAdapter.get().applyToManagedDao(attrJsonObj, matchedDao)
                                applyTo.attributes.add(matchedDao)
                                copiedAttributes.remove(matchedDao)
                            }
                            else{
                                //add new attribute
                                val newAttribute = applyTo.realm.createObject(OTAttributeDAO::class.java, UUID.randomUUID())
                                attributeTypeAdapter.get().applyToManagedDao(attrJsonObj, newAttribute)
                                applyTo.attributes.add(newAttribute)
                            }
                        }

                        //deal with dangling attributes
                        copiedAttributes.forEach{
                            it.properties.deleteAllFromRealm()
                            it.deleteFromRealm()
                        }

                    }
                }
            }
        }
    }
}