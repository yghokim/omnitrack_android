package kr.ac.snu.hcil.omnitrack.core.database.typeadapters

import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.android.common.getBooleanCompat
import kr.ac.snu.hcil.android.common.getIntCompat
import kr.ac.snu.hcil.android.common.getLongCompat
import kr.ac.snu.hcil.android.common.getStringCompat
import java.util.*

/**
 * Created by younghokim on 2017. 11. 3..
 */
class TrackerTypeAdapter(isServerMode: Boolean, val fieldTypeAdapter: Lazy<ServerCompatibleTypeAdapter<OTFieldDAO>>, val gson: Lazy<Gson>) : ServerCompatibleTypeAdapter<OTTrackerDAO>(isServerMode) {

    override fun read(reader: JsonReader, isServerMode: Boolean): OTTrackerDAO {
        val dao = OTTrackerDAO()

        reader.beginObject()
        while(reader.hasNext())
        {
            when(reader.nextName())
            {
                BackendDbManager.FIELD_OBJECT_ID -> dao._id = reader.nextString()
                BackendDbManager.FIELD_REMOVED_BOOLEAN -> dao.removed = reader.nextBoolean()
                if (isServerMode) "user" else BackendDbManager.FIELD_USER_ID -> dao.userId = reader.nextString()
                BackendDbManager.FIELD_USER_CREATED_AT -> dao.userCreatedAt = reader.nextLong()
                BackendDbManager.FIELD_SYNCHRONIZED_AT -> if (reader.peek() == JsonToken.NULL) {
                    dao.synchronizedAt = null
                    reader.skipValue()
                } else {
                    dao.synchronizedAt = reader.nextLong()
                }
                BackendDbManager.FIELD_UPDATED_AT_LONG -> dao.userUpdatedAt = reader.nextLong()
                BackendDbManager.FIELD_POSITION -> dao.position = reader.nextInt()
                BackendDbManager.FIELD_NAME -> dao.name = reader.nextString()
                BackendDbManager.FIELD_REDIRECT_URL -> if (reader.peek() == JsonToken.NULL) {
                    dao.redirectUrl = null
                    reader.skipValue()
                } else {
                    dao.redirectUrl = reader.nextString()
                }
                "color" -> dao.color = reader.nextInt()
                "isBookmarked" -> dao.isBookmarked = reader.nextBoolean()
                BackendDbManager.FIELD_LOCKED_PROPERTIES -> dao.serializedLockedPropertyInfo = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                "flags" -> {
                    val flagObject = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java)
                    dao.serializedCreationFlags = flagObject.toString()
                }
                "fields" -> {
                    reader.beginArray()

                    while(reader.hasNext())
                    {
                        val field = fieldTypeAdapter.get().read(reader)
                        if (isServerMode && field._id.isNullOrBlank()) {
                            field._id = UUID.randomUUID().toString()
                        }
                        dao.fields.add(field)
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

        writer.name(BackendDbManager.FIELD_OBJECT_ID).value(value._id)
        writer.name(BackendDbManager.FIELD_REMOVED_BOOLEAN).value(value.removed)
        writer.name(if (isServerMode) "user" else BackendDbManager.FIELD_USER_ID).value(value.userId)
        writer.name(BackendDbManager.FIELD_USER_CREATED_AT).value(value.userCreatedAt)
        writer.name(BackendDbManager.FIELD_UPDATED_AT_LONG).value(value.userUpdatedAt)

        if (!isServerMode)
            writer.name(BackendDbManager.FIELD_SYNCHRONIZED_AT).value(value.synchronizedAt)

        writer.name("redirectUrl").value(value.redirectUrl)

        writer.name("flags").jsonValue(value.serializedCreationFlags)
        writer.name("lockedProperties").jsonValue(value.serializedLockedPropertyInfo)

        writer.name(BackendDbManager.FIELD_POSITION).value(value.position)
        writer.name(BackendDbManager.FIELD_NAME).value(value.name)
        writer.name("color").value(value.color)
        writer.name("isBookmarked").value(value.isBookmarked)
        writer.name(BackendDbManager.FIELD_LOCKED_PROPERTIES).jsonValue(value.serializedLockedPropertyInfo)
        writer.name("flags").jsonValue(value.serializedCreationFlags)
        writer.name("fields").beginArray()
        for (field in value.fields)
            {
                fieldTypeAdapter.get().write(writer, field)
            }
        writer.endArray()

        writer.endObject()
    }

    override fun applyToManagedDao(json: JsonObject, applyTo: OTTrackerDAO): Boolean {
        json.keySet().forEach{
            key->
            when(key)
            {
                BackendDbManager.FIELD_REMOVED_BOOLEAN -> applyTo.removed != json.getBooleanCompat(key) ?: false
                "user", BackendDbManager.FIELD_USER_ID -> applyTo.userId = json.getStringCompat(key)
                BackendDbManager.FIELD_USER_CREATED_AT -> applyTo.userCreatedAt = json.getLongCompat(key) ?: 0
                BackendDbManager.FIELD_UPDATED_AT_LONG -> applyTo.userUpdatedAt = json.getLongCompat(key) ?: 0
                BackendDbManager.FIELD_POSITION -> applyTo.position = json.getIntCompat(key) ?: 0
                BackendDbManager.FIELD_SYNCHRONIZED_AT -> applyTo.synchronizedAt = json.getLongCompat(key)
                BackendDbManager.FIELD_NAME -> applyTo.name = json.getStringCompat(key) ?: ""
                BackendDbManager.FIELD_REDIRECT_URL -> applyTo.redirectUrl = json.getStringCompat(key)
                "color" -> applyTo.color = json.getIntCompat(key) ?: Color.CYAN
                "isBookmarked" -> applyTo.isBookmarked = json.getBooleanCompat(key) ?: false
                "lockedProperties"->applyTo.serializedLockedPropertyInfo = json[key]?.toString() ?: "null"
                "flags" -> {
                    try {
                        val flagObject = json.getAsJsonObject(key)
                        applyTo.serializedCreationFlags = flagObject.toString()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        applyTo.serializedCreationFlags = "null"
                    }
                }
                "fields"->{
                    val jsonList = try{json[key]?.asJsonArray}catch(ex:Exception){null}
                    if(jsonList!=null)
                    {
                        val copiedAttributes = ArrayList<OTFieldDAO>(applyTo.fields)
                        applyTo.fields.clear()

                        //synchronize
                        jsonList.forEach {
                            attrJson->
                            val attrJsonObj = attrJson.asJsonObject
                            val matchedDao = copiedAttributes.find { it.localId == attrJsonObj.getStringCompat("localId") }
                            if(matchedDao != null)
                            {
                                //update field
                                fieldTypeAdapter.get().applyToManagedDao(attrJsonObj, matchedDao)
                                applyTo.fields.add(matchedDao)
                                copiedAttributes.remove(matchedDao)
                            }
                            else{
                                //add new field
                                val newAttribute = applyTo.realm.createObject(OTFieldDAO::class.java, UUID.randomUUID().toString())
                                fieldTypeAdapter.get().applyToManagedDao(attrJsonObj, newAttribute)
                                applyTo.fields.add(newAttribute)
                            }
                        }

                        //deal with dangling fields
                        copiedAttributes.forEach{
                            it.properties.deleteAllFromRealm()
                            it.deleteFromRealm()
                        }

                    }
                }
            }
        }

        return true //TODO fine-grained check for change
    }
}