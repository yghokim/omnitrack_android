package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

import android.content.Context
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import java.math.BigDecimal

class OTDataDrivenTriggerCondition : ATriggerCondition(OTTriggerDAO.CONDITION_TYPE_DATA) {

    enum class ComparisonMethod(val symbol: String, val code: String, val symbolImageResourceId: Int) {
        /*
        BiggerThan(">", R.drawable.symbol_bigger), BiggerOrEqual("≧", R.drawable.symbol_bigger_or_equal), Equal("=", R.drawable.symbol_equal), SmallerOfEqual("≦", R.drawable.symbol_smaller_or_equal), SmallerThan("<", R.drawable.symbol_smaller)
    */
        Exceed(">", "exceed", R.drawable.icon_threshold_up),
        Drop("<", "drop", R.drawable.icon_threshold_down)
    }

    class ConditionTypeAdapter(val externalServiceManager: OTExternalServiceManager, val timeRangeQueryTypeAdapter: Lazy<OTTimeRangeQuery.TimeRangeQueryTypeAdapter>) : TypeAdapter<OTDataDrivenTriggerCondition>() {
        override fun write(out: JsonWriter, value: OTDataDrivenTriggerCondition) {
            out.beginObject()
            out.name("factory")
            value.measure?.let {
                out.beginArray()
                        .value(it.factoryCode)
                        .value(it.getFactory<OTMeasureFactory>().serializeMeasure(it))
                        .endArray()
            } ?: out.nullValue()

            out.name("query")
            timeRangeQueryTypeAdapter.get().write(out, value.timeQuery)

            out.name("threshold").value(value.threshold)
            out.name("comparison").value(value.comparison.code)
            out.endObject()
        }

        override fun read(reader: JsonReader): OTDataDrivenTriggerCondition {
            val condition = OTDataDrivenTriggerCondition()

            reader.beginObject()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "threshold" -> condition.threshold = reader.nextDouble().toBigDecimal()
                    "comparison" -> {
                        val code = reader.nextString()
                        condition.comparison = ComparisonMethod.values().find { it.code == code }
                                ?: ComparisonMethod.Exceed
                    }
                    "query" -> condition.timeQuery = timeRangeQueryTypeAdapter.get().read(reader)
                    "factory" -> {
                        if (reader.peek() == JsonToken.NULL) {
                            reader.skipValue()
                        } else {
                            reader.beginArray()
                            val factoryCode = reader.nextString()
                            val factory = externalServiceManager.getMeasureFactoryByCode(typeCode = factoryCode)
                            if (factory == null) {
                                println("$factoryCode is not supported in System.")
                                reader.skipValue()
                            } else {
                                condition.measure = factory.makeMeasure(reader)
                                if (reader.peek() != JsonToken.NAME || reader.peek() != JsonToken.END_ARRAY) {
                                    reader.skipValue()
                                }
                            }
                            reader.endArray()
                        }
                    }
                    else -> reader.skipValue()
                }
            }

            reader.endObject()

            return condition
        }

    }

    var measure: OTMeasureFactory.OTMeasure? = null
    var timeQuery: OTTimeRangeQuery = OTTimeRangeQuery.Preset.PresentDate.makeQueryInstance()

    var threshold: BigDecimal = BigDecimal.ZERO
    var comparison: ComparisonMethod = ComparisonMethod.Exceed

    override fun getSerializedString(): String {
        return OTApp.daoSerializationComponent.dataDrivenConditionTypeAdapter().toJson(this)
    }

    override fun isConfigurationValid(context: Context): Single<Pair<Boolean, List<CharSequence>?>> {
        return Single.defer {
            val measure = this.measure
            return@defer if (measure == null) Single.just(Pair(false, listOf("No measure is attached.")))
            else {
                val factory = measure.getFactory<OTMeasureFactory>()
                if (factory is OTServiceMeasureFactory) {
                    /*
                    return@defer factory.parentService.onStateChanged.firstOrError().map { state ->
                        when (state) {
                            OTExternalService.ServiceState.ACTIVATED -> Pair(true, null)
                            else -> Pair<Boolean, List<CharSequence>?>(false, listOf(TextHelper.fromHtml(String.format(
                                    "<font color=\"blue\">${context.resources.getString(R.string.msg_service_is_not_activated_format)}</font>",
                                    context.resources.getString(factory.parentService.nameResourceId)))))
                        }
                    }*/
                    return@defer Single.just(Pair(true, null))
                } else {
                    return@defer Single.just(Pair(false, listOf("The measure is not a service measure.")))
                }
            }
        }
    }

    fun passesThreshold(value: BigDecimal): Boolean {
        return when (comparison) {
            ComparisonMethod.Drop -> value < threshold
            ComparisonMethod.Exceed -> value > threshold
            else -> throw IllegalArgumentException("Undefined comparison method.")
        }
    }

    override fun writeEventLogContent(table: JsonObject) {
        table.add("connection_factory", measure?.factoryCode?.toJson())
        table.add("threshold", threshold.toJson())
        table.add("comparison", comparison.code.toJson())
    }

    override fun makeInformationText(): CharSequence {
        return "Actuated when [${measure?.getFactory<OTMeasureFactory>()?.getFormattedName()}] [${comparison}] ${threshold}"
    }

    override fun equals(other: Any?): Boolean {
        return if (other === this) {
            true
        } else if (other is OTDataDrivenTriggerCondition) {
            measure == other.measure && threshold == other.threshold && comparison == other.comparison
        } else false
    }
}