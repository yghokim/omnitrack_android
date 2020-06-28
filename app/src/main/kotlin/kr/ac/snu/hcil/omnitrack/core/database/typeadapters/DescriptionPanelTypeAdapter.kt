package kr.ac.snu.hcil.omnitrack.core.database.typeadapters

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTDescriptionPanelDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.getStringCompat

/**
 * Created by younghokim on 2017. 11. 2..
 */
class DescriptionPanelTypeAdapter(val gson: Lazy<Gson>) : ServerCompatibleTypeAdapter<OTDescriptionPanelDAO>(true) {

    override fun read(reader: JsonReader, isServerMode: Boolean): OTDescriptionPanelDAO {
        val dao = OTDescriptionPanelDAO()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue()
            } else {
                when (name) {
                    BackendDbManager.FIELD_OBJECT_ID -> dao._id = reader.nextString()
                    "trackerId" -> dao.trackerId = reader.nextString()

                    "flags" -> {
                        dao.serializedCreationFlags = gson.get().fromJson<JsonObject>(reader, JsonObject::class.java).toString()
                    }
                    "content" -> dao.content = reader.nextString()
                    else -> reader.skipValue()
                }
            }
        }
        reader.endObject()

        return dao
    }

    override fun write(writer: JsonWriter, value: OTDescriptionPanelDAO, isServerMode: Boolean) {
        writer.beginObject()

        writer.name(BackendDbManager.FIELD_OBJECT_ID).value(value._id)
        writer.name("trackerId").value(value.trackerId)
        writer.name("content").value(value.content)
        writer.name("flags").jsonValue(value.serializedCreationFlags)

        writer.endObject()
    }

    override fun applyToManagedDao(json: JsonObject, applyTo: OTDescriptionPanelDAO): Boolean {
        json.keySet().forEach { key ->
            when (key) {
                "content" -> applyTo.content = json.getStringCompat(key) ?: ""
            }
        }

        return true //TODO find-grained change detection
    }

}