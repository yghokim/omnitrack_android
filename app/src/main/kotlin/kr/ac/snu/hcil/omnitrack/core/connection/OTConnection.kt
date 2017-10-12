package kr.ac.snu.hcil.omnitrack.core.connection

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import rx.Observable

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTConnection {

    class ConnectionTypeAdapter : TypeAdapter<OTConnection>() {
        override fun read(reader: JsonReader): OTConnection {
            /*
            if (typedQueue.getBoolean()) {
                val factoryCode = typedQueue.getString()
                val factory = OTExternalService.getMeasureFactoryByCode(typeCode = factoryCode)
                if (factory == null) {
                    println("$factoryCode is deprecated in System.")

                } else {
                    source = factory.makeMeasure(typedQueue.getString())
                }
            }

            if (typedQueue.getBoolean()) {
                rangedQuery = OTTimeRangeQuery()
                rangedQuery?.onDeserialize(typedQueue)
            }*/

            val connection = OTConnection()
            reader.beginObject()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "factory" -> {
                        var factoryCode: String = ""
                        var serialized: String = ""
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "code" -> factoryCode = reader.nextString()
                                "serialized" -> serialized = reader.nextString()
                            }
                        }
                        reader.endObject()

                        val factory = OTExternalService.getMeasureFactoryByCode(typeCode = factoryCode)
                        if (factory == null) {
                            println("$factoryCode is deprecated in System.")

                        } else {
                            connection.source = factory.makeMeasure(serialized)
                        }
                    }

                    "query" -> {
                        val adapter = OTTimeRangeQuery.TimeRangeQueryTypeAdapter()
                        connection.rangedQuery = adapter.read(reader)
                    }
                }
            }

            reader.endObject()

            return connection
        }

        override fun write(out: JsonWriter, value: OTConnection) {
            out.beginObject()

            value.source?.let {
                out.name("factory").beginObject()
                out.name("code").value(it.factoryCode)
                out.name("serialized").value(it.getSerializedString())
                out.endObject()
            }

            value.rangedQuery?.let {
                out.name("query")
                val adapter = OTTimeRangeQuery.TimeRangeQueryTypeAdapter()
                adapter.write(out, it)
            }

            out.endObject()
        }

    }

    companion object {
        val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(OTConnection::class.java, ConnectionTypeAdapter()).create()
        }

        fun fromJson(serialized: String): OTConnection {
            return parser.fromJson(serialized, OTConnection::class.java)
        }
    }

    var source: OTMeasureFactory.OTMeasure? = null
        get() {
            return field
        }
        set(value) {
            if (field != value) {
                field = value
            }

            if (isRangedQueryAvailable) {
                if (rangedQuery == null) {
                    rangedQuery = OTTimeRangeQuery()
                }
            }
        }

    val isRangedQueryAvailable: Boolean
        get() = if (source != null) {
            source?.factory?.isRangedQueryAvailable ?: false
        } else false


    var rangedQuery: OTTimeRangeQuery? = null


    constructor() : super()

    fun isValid(invalidMessages: MutableList<CharSequence>?): Boolean {
        val source = source
        if (source != null) {
            val service = source.factory.getService()
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                return true
            } else {
                invalidMessages?.add(TextHelper.fromHtml(String.format(
                        "<font color=\"blue\">${OTApplication.app.resourcesWrapped.getString(R.string.msg_service_is_not_activated_format)}</font>",
                        OTApplication.app.resourcesWrapped.getString(service.nameResourceId))))
                return false
            }
        } else {
            invalidMessages?.add(TextHelper.fromHtml(
                    "<font color=\"blue\">Connection is not supported on current version.</font>"
            ))
            return false
        }
    }

    fun getRequestedValue(builder: OTItemBuilder): Observable<Nullable<out Any>> {
        return Observable.defer {
            //if (source != null) {
            //    return@defer source!!.getValueRequest(builder, rangedQuery)
            // } else {
            return@defer Observable.error<Nullable<out Any>>(Exception("Connection source is not designated."))
            // }
        }
    }

    fun getRequestedValue(builder: OTItemBuilderWrapperBase): Observable<Nullable<out Any>> {
        return Observable.defer {
            if (source != null) {
                return@defer source!!.getValueRequest(builder, rangedQuery)
            } else {
                return@defer Observable.error<Nullable<out Any>>(Exception("Connection source is not designated."))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        else if (other is OTConnection) {
            return other.rangedQuery == this.rangedQuery && other.source == this.source
        } else return false
    }

    fun getSerializedString(): String {
        return parser.toJson(this)
    }

    fun isAvailableToRequestValue(invalidMessages: MutableList<CharSequence>? = null): Boolean {
        val source = source
        if (source != null) {
            val service = source.factory.getService()
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                return true
            } else {
                invalidMessages?.add(TextHelper.fromHtml(String.format(
                        "<font color=\"blue\">${OTApplication.app.resourcesWrapped.getString(R.string.msg_service_is_not_activated_format)}</font>",
                        OTApplication.app.resourcesWrapped.getString(service.nameResourceId))))
                return false
            }
        } else {
            invalidMessages?.add(TextHelper.fromHtml(
                    "<font color=\"blue\">Connection is not supported on current version.</font>"
            ))
            return false
        }
    }

}
