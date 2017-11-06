package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager

/**
 * Created by younghokim on 2017. 11. 3..
 */
class TrackerTypeAdapter(val attributeTypeAdapter: Lazy<TypeAdapter<OTAttributeDAO>>, val gson: Lazy<Gson>): TypeAdapter<OTTrackerDAO>() {

    override fun read(reader: JsonReader): OTTrackerDAO {
        val dao = OTTrackerDAO()

        while(reader.hasNext())
        {
            when(reader.nextName())
            {
                RealmDatabaseManager.FIELD_OBJECT_ID -> dao.objectId = reader.nextString()
                RealmDatabaseManager.FIELD_REMOVED_BOOLEAN -> dao.removed = reader.nextBoolean()
                RealmDatabaseManager.FIELD_USER_ID -> dao.userId = reader.nextString()
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

            }
        }

        return dao
    }

    override fun write(out: JsonWriter, tracker: OTTrackerDAO) {
        out.beginObject()

        out.name(RealmDatabaseManager.FIELD_OBJECT_ID).value(tracker.objectId)
        out.name(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN).value(tracker.removed)
        out.name(RealmDatabaseManager.FIELD_USER_ID).value(tracker.userId)
        out.name(RealmDatabaseManager.FIELD_USER_CREATED_AT).value(tracker.userCreatedAt)
        out.name(RealmDatabaseManager.FIELD_SYNCHRONIZED_AT).value(tracker.synchronizedAt)
        out.name(RealmDatabaseManager.FIELD_UPDATED_AT_LONG).value(tracker.userUpdatedAt)

        out.name(RealmDatabaseManager.FIELD_POSITION).value(tracker.position)
        out.name(RealmDatabaseManager.FIELD_NAME).value(tracker.name)
        out.name("color").value(tracker.color)
        out.name("isBookmarked").value(tracker.isBookmarked)
        out.name("lockedProperties").jsonValue(tracker.serializedLockedPropertyInfo)
        out.name("flags").jsonValue(tracker.serializedCreationFlags)
        out.name("attributes").beginArray()
            for(attribute in tracker.attributes)
            {
                attributeTypeAdapter.get().write(out, attribute)
            }
        out.endArray()

        out.name("removedAttributes").beginArray()
        for(attribute in tracker.removedAttributes)
        {
            attributeTypeAdapter.get().write(out, attribute)
        }
        out.endArray()

        out.endObject()
    }

}