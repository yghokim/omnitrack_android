package kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

/**
 * Created by younghokim on 2017. 11. 28..
 */
open class UsageLog : RealmObject() {

    companion object {
        val typeAdapter: UsageLogTypeAdapter by lazy {
            UsageLogTypeAdapter()
        }
    }

    class UsageLogTypeAdapter : TypeAdapter<UsageLog>() {
        override fun read(reader: JsonReader): UsageLog {
            val usageLog = UsageLog()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {

                }
            }
            reader.endObject()

            return usageLog
        }

        override fun write(out: JsonWriter, value: UsageLog) {
            out.beginObject()
            out.name("localId").value(value.id)
            out.name("name").value(value.name)
            out.name("sub").value(value.sub)
            out.name("content").jsonValue(value.contentJson)
            out.name("user").value(value.userId)
            out.name("deviceId").value(value.deviceId)
            out.name("timestamp").value(value.timestamp)

            out.endObject()
        }

    }

    @PrimaryKey
    var id: Long = 0

    var userId: String? = null

    var deviceId: String? = null

    var timestamp: Long = System.currentTimeMillis()

    var name: String = ""
    var sub: String? = null
    var contentJson: String = "{}"

    @Index
    var isSynchronized: Boolean = false
}