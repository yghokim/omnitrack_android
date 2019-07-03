package kr.ac.snu.hcil.omnitrack.core.connection

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.system.OTMeasureFactoryManager

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTConnection {

    class ConnectionTypeAdapter(val measureFactoryManager: OTMeasureFactoryManager, val timeRangeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>, val gson: Lazy<Gson>) : TypeAdapter<OTConnection>() {
        override fun read(reader: JsonReader): OTConnection {
            val connection = OTConnection()
            reader.beginObject()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "measure" -> {
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue()
                        } else {
                            var factoryCode: String? = null
                            var arguments: JsonObject? = null
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "code" -> {
                                        if (reader.peek() == JsonToken.NULL) {
                                            reader.nextNull()
                                        } else {
                                            factoryCode = reader.nextString()
                                        }
                                    }
                                    "args" -> {
                                        if (reader.peek() == JsonToken.NULL) {
                                            reader.nextNull()
                                        } else {
                                            arguments = gson.get().fromJson(reader, JsonObject::class.java)
                                        }
                                    }
                                }
                            }
                            reader.endObject()
                            if (factoryCode != null) {
                                val factory = measureFactoryManager.getMeasureFactoryByCode(typeCode = factoryCode)
                                if (factory == null) {
                                    println("$factoryCode is not supported in System.")

                                    connection.source = measureFactoryManager.serviceManager.unSupportedDummyMeasureFactory.makeMeasure(factoryCode, arguments)
                                } else {
                                    connection.source = factory.makeMeasure(arguments)
                                }
                            }
                        }
                    }

                    "query" -> {
                        connection.rangedQuery = timeRangeQueryTypeAdapter.get().read(reader)
                    }
                }
            }

            reader.endObject()

            return connection
        }

        override fun write(out: JsonWriter, value: OTConnection) {
            out.beginObject()

            value.source?.let {
                out.name("measure")
                out.beginObject()
                        .name("code").value(it.factoryCode)
                        .name("args").jsonValue(gson.get().toJson(it.arguments))
                out.endObject()
            }

            value.rangedQuery?.let {
                out.name("query")
                timeRangeQueryTypeAdapter.get().write(out, it)
            }

            out.endObject()
        }

    }

    var source: OTMeasureFactory.OTMeasure? = null
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
            source?.getFactory<OTMeasureFactory>()?.isRangedQueryAvailable == true
        } else false


    var rangedQuery: OTTimeRangeQuery? = null


    fun getRequestedValue(builder: OTItemBuilderWrapperBase): Single<Nullable<out Any>> {
        return Single.defer {
            if (source != null) {
                return@defer source!!.getValueRequest(builder, rangedQuery)
            } else {
                return@defer Single.error<Nullable<out Any>>(Exception("Connection source is not designated."))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        else if (other is OTConnection) {
            return other.rangedQuery == this.rangedQuery && other.source == this.source
        } else return false
    }

    fun getSerializedString(context: Context): String {
        return (context.applicationContext as OTAndroidApp).applicationComponent.getConnectionTypeAdapter().toJson(this)
    }

    fun getSerializedString(adapter: ConnectionTypeAdapter): String {
        return adapter.toJson(this)
    }


    fun makeAvailabilityCheckObservable(field: OTFieldDAO): Observable<Pair<Boolean, List<CharSequence>?>> {
        val source = source
        if (source != null) {
            return source.getFactory<OTMeasureFactory>().makeAvailabilityCheckObservable(field)
        } else {
            return Observable.just(Pair(false, listOf(TextHelper.fromHtml(
                    "<font color=\"blue\">This field is connected to the service that is not supported in this version of the app.</font>"
            ).toString())))
        }
    }

    fun isAvailableToRequestValue(field: OTFieldDAO, invalidMessages: MutableList<CharSequence>? = null): Boolean {
        val source = source
        if (source != null) {
            return source.getFactory<OTMeasureFactory>().isAvailableToRequestValue(field, invalidMessages)
        } else {
            invalidMessages?.add(TextHelper.fromHtml(
                    "<font color=\"blue\">This field is connected to the service that is not supported in this version of the app.</font>"
            ))
            return false
        }
    }

}
