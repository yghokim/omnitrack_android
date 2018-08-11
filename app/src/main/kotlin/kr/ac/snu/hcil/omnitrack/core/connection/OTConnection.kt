package kr.ac.snu.hcil.omnitrack.core.connection

import android.content.Context
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.TextHelper

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTConnection {

    class ConnectionTypeAdapter(val externalServiceManager: OTExternalServiceManager) : TypeAdapter<OTConnection>() {
        override fun read(reader: JsonReader): OTConnection {
            val connection = OTConnection()
            reader.beginObject()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "factory" -> {
                        reader.beginArray()
                        val factoryCode = reader.nextString()
                        val factory = externalServiceManager.getMeasureFactoryByCode(typeCode = factoryCode)
                        if (factory == null) {
                            println("$factoryCode is not supported in System.")
                            reader.skipValue()
                        } else {
                            connection.source = factory.makeMeasure(reader)
                            if (reader.peek() != JsonToken.NAME || reader.peek() != JsonToken.END_ARRAY) {
                                reader.skipValue()
                            }
                        }
                        reader.endArray()
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
                out.name("factory")
                out.beginArray()
                out.value(it.factoryCode)
                out.jsonValue(it.factory.serializeMeasure(it))
                out.endArray()
            }

            value.rangedQuery?.let {
                out.name("query")
                val adapter = OTTimeRangeQuery.TimeRangeQueryTypeAdapter()
                adapter.write(out, it)
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
            source?.factory?.isRangedQueryAvailable == true
        } else false


    var rangedQuery: OTTimeRangeQuery? = null


    fun getRequestedValue(builder: OTItemBuilderWrapperBase): Flowable<Nullable<out Any>> {
        return Flowable.defer {
            if (source != null) {
                return@defer source!!.getValueRequest(builder, rangedQuery)
            } else {
                return@defer Flowable.error<Nullable<out Any>>(Exception("Connection source is not designated."))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        else if (other is OTConnection) {
            return other.rangedQuery == this.rangedQuery && other.source == this.source
        } else return false
    }

    fun getSerializedString(configuredContext: ConfiguredContext): String {
        return configuredContext.configuredAppComponent.getConnectionTypeAdapter().toJson(this)
    }

    fun getSerializedString(adapter: ConnectionTypeAdapter): String {
        return adapter.toJson(this)
    }

    fun isAvailableToRequestValue(invalidMessages: MutableList<CharSequence>? = null): Boolean {
        val source = source
        if (source != null) {
            val service = source.factory.parentService
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                return true
            } else {
                invalidMessages?.add(TextHelper.fromHtml(String.format(
                        "<font color=\"blue\">${source.factory.context.resources.getString(R.string.msg_service_is_not_activated_format)}</font>",
                        source.factory.context.resources.getString(service.nameResourceId))))
                return false
            }
        } else {
            invalidMessages?.add(TextHelper.fromHtml(
                    "<font color=\"blue\">Connection is not supported on current version.</font>"
            ))
            return false
        }
    }

    fun makeValidationStateObservable(context: Context): Flowable<Pair<Boolean, CharSequence?>> {
        return source?.let { source ->
            Flowable.defer {
                val service = source.factory.parentService
                service.onStateChanged.map { state ->
                    when (state) {
                        OTExternalService.ServiceState.ACTIVATED -> Pair(true, null)
                        else -> Pair<Boolean, CharSequence?>(false, TextHelper.fromHtml(String.format(
                                "<font color=\"blue\">${context.resources.getString(R.string.msg_service_is_not_activated_format)}</font>",
                                context.resources.getString(service.nameResourceId))))
                    }
                }.toFlowable(BackpressureStrategy.LATEST)
            }
        } ?: Flowable.just(Pair<Boolean, CharSequence?>(false, null))
    }

}
