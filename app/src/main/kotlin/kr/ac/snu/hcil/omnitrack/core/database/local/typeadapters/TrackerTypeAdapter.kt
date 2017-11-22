package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.utils.getBooleanCompat
import kr.ac.snu.hcil.omnitrack.utils.getIntCompat
import kr.ac.snu.hcil.omnitrack.utils.getLongCompat
import kr.ac.snu.hcil.omnitrack.utils.getStringCompat
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
                        val attribute = attributeTypeAdapter.get().read(reader)
                        if (isServerMode && attribute.objectId.isNullOrBlank()) {
                            attribute.objectId = UUID.randomUUID().toString()
                        }
                        dao.attributes.add(attribute)
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
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN -> applyTo.removed = json.getBooleanCompat(key) ?: false
                "user", RealmDatabaseManager.FIELD_USER_ID -> applyTo.userId = json.getStringCompat(key)
                RealmDatabaseManager.FIELD_USER_CREATED_AT -> applyTo.userCreatedAt = json.getLongCompat(key) ?: 0
                RealmDatabaseManager.FIELD_UPDATED_AT_LONG -> applyTo.userUpdatedAt = json.getLongCompat(key) ?: 0
                RealmDatabaseManager.FIELD_POSITION -> applyTo.position = json.getIntCompat(key) ?: 0
                RealmDatabaseManager.FIELD_SYNCHRONIZED_AT -> applyTo.synchronizedAt = json.getLongCompat(key)
                RealmDatabaseManager.FIELD_NAME -> applyTo.name = json.getStringCompat(key) ?: ""
                "color" -> applyTo.color = json.getIntCompat(key) ?: OTApp.instance.colorPalette[0]
                "isBookmarked" -> applyTo.isBookmarked = json.getBooleanCompat(key) ?: false
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
                            val matchedDao = copiedAttributes.find { it.localId == attrJsonObj.getStringCompat("localId") }
                            if(matchedDao != null)
                            {
                                //update attribute
                                attributeTypeAdapter.get().applyToManagedDao(attrJsonObj, matchedDao)
                                applyTo.attributes.add(matchedDao)
                                copiedAttributes.remove(matchedDao)
                            }
                            else{
                                //add new attribute
                                val newAttribute = applyTo.realm.createObject(OTAttributeDAO::class.java, UUID.randomUUID().toString())
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