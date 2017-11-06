package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager

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
                "lockedProperties" -> dao.serializedLockedPropertyInfo = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                "flags" -> dao.serializedCreationFlags = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                "attributes" -> {
                    reader.beginArray()

                    while(reader.hasNext())
                    {
                        dao.attributes.add(attributeTypeAdapter.get().read(reader))
                    }

                    reader.endArray()
                }
                "removedAttributes" -> {
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
        writer.name("lockedProperties").jsonValue(value.serializedLockedPropertyInfo)
        writer.name("flags").jsonValue(value.serializedCreationFlags)
        writer.name("attributes").beginArray()
        for (attribute in value.attributes)
            {
                attributeTypeAdapter.get().write(writer, attribute)
            }
        writer.endArray()

        writer.name("removedAttributes").beginArray()
        for (attribute in value.removedAttributes)
        {
            attributeTypeAdapter.get().write(writer, attribute)
        }
        writer.endArray()

        writer.endObject()
    }

    override fun applyToManagedDao(json: JsonObject, applyTo: OTTrackerDAO) {

    }
}