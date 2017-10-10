package kr.ac.snu.hcil.omnitrack.core.database.local

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import java.util.*

/**
 * Created by younghokim on 2017. 10. 1..
 */
open class OTTrackerDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null

    @Index
    var userId: String? = null

    var name: String = ""

    var position: Int = 0
    var color: Int = 0
    var isBookmarked: Boolean = false

    var attributes = RealmList<OTAttributeDAO>()
    var removedAttributes = RealmList<OTAttributeDAO>()

    var creationFlags = RealmList<OTStringStringEntryDAO>()
    var isEditable: Boolean = true

    var userCreatedAt: Long = System.currentTimeMillis()
    var synchronizedAt: Long? = null // store server time of when synchronized perfectly.
    var updatedAt: Long = System.currentTimeMillis()
    var removed: Boolean = false

    val creationFlagsMap: Map<String, String> get() = RealmDatabaseManager.convertRealmEntryListToDictionary(creationFlags)
}


open class OTAttributeDAO : RealmObject() {
    class AttrDaoJsonTypeAdapter : TypeAdapter<OTAttributeDAO>() {
        override fun read(reader: JsonReader): OTAttributeDAO {
            val dao = OTAttributeDAO()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> dao.objectId = reader.nextString()
                    "localId" -> dao.localId = reader.nextString()
                    "trackerId" -> dao.trackerId = reader.nextString()
                    "name" -> dao.name = reader.nextString()
                    "pos" -> dao.position = reader.nextInt()
                    "type" -> dao.type = reader.nextInt()
                    "fallbackPolicy" -> dao.fallbackValuePolicy = reader.nextInt()
                    "fallbackPreset" -> dao.fallbackPresetSerializedValue = reader.nextString()
                    "userCreatedAt" -> dao.userCreatedAt = reader.nextLong()
                    "updatedAt" -> dao.updatedAt = reader.nextLong()
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

            out.name("id").value(value.objectId)
            out.name("localId").value(value.localId)
            out.name("trackerId").value(value.trackerId)
            out.name("name").value(value.name)
            out.name("pos").value(value.position)
            out.name("fallbackPolicy").value(value.fallbackValuePolicy)
            out.name("fallbackPreset").value(value.fallbackPresetSerializedValue)
            out.name("type").value(value.type)
            out.name("userCreatedAt").value(value.userCreatedAt)
            out.name("req").value(value.isRequired)
            out.name("conn").value(value.serializedConnection)
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

    @PrimaryKey
    var objectId: String? = null

    var localId: String = ""

    @Index
    var trackerId: String? = null

    var name: String = ""

    var position: Int = 0
    var serializedConnection: String? = null
    var type: Int = -1
    var isRequired: Boolean = false
    var properties = RealmList<OTStringStringEntryDAO>()

    //If connection is specified, this policy is overriden.
    var fallbackValuePolicy: Int = DEFAULT_VALUE_POLICY_NULL
    var fallbackPresetSerializedValue: String? = null

    var userCreatedAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()

    fun getParsedConnection(): OTConnection? {
        return serializedConnection?.let { OTConnection.fromJson(it) }
    }

    fun initializePropertiesWithDefaults(realm: Realm) {
        val attributeHelper = OTAttributeManager.getAttributeHelper(type)
        attributeHelper.propertyKeys.forEach { key ->
            setPropertySerializedValue(key, attributeHelper.getPropertyHelper<Any>(key).getSerializedValue(attributeHelper.getPropertyInitialValue(key)!!))
        }
    }

    fun setPropertySerializedValue(key: String, serializedValue: String): Boolean {
        var changed = false
        val match = properties.find { it.key == key }
        if (match != null) {
            if (match.value != serializedValue)
                changed = true

            match.value = serializedValue
        } else {
            properties.add(OTStringStringEntryDAO().apply {
                this.id = UUID.randomUUID().toString()
                this.key = key
                this.value = serializedValue
            })
            changed = true
        }

        return changed
    }

    fun getPropertySerializedValue(key: String): String? {
        return properties.find { it.key == key }?.value
    }

    companion object {

        const val DEFAULT_VALUE_POLICY_NULL = 0
        const val DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE = 1
        const val DEFAULT_VALUE_POLICY_FILL_WITH_PRESET = 2
        const val DEFAULT_VALUE_POLICY_FILL_WITH_LAST_ITEM = 3

        val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(OTAttributeDAO::class.java, AttrDaoJsonTypeAdapter()).create()
        }
    }
}