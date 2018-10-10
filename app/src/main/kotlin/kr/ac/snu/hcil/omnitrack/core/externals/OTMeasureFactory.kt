package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import android.text.Html
import com.google.gson.stream.JsonReader
import io.reactivex.Flowable
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.calculation.AConditioner
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.Nullable

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
abstract class OTMeasureFactory(val context: Context, val parentService: OTExternalService, val factoryTypeName: String) : INameDescriptionResourceProvider {

    companion object {
        val CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE = intArrayOf(AConditioner.TYPECODE_SINGLE_NUMERIC_COMPARISON)
        val CONDITIONERS_FOR_TIMEPOINT_VALUE = intArrayOf()

        val CONFIGURATOR_STEP_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttributeDAO): Boolean {
                /*if (attr is OTNumberAttribute) {
                    val ns = NumberStyle()
                    ns.commaUnit = 3
                    ns.fractionPart = 0
                    ns.unit = "Step"
                    ns.pluralizeUnit = true
                    ns.unitPosition = NumberStyle.UnitPosition.Rear

                    attr.numberStyle = ns

                    return true
                } else*/ return false
            }
        }

        val CONFIGURATOR_DISTANCE_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttributeDAO): Boolean {
                /*if (attr is OTNumberAttribute) {
                    val ns = NumberStyle()
                    ns.commaUnit = 3
                    ns.fractionPart = 2
                    ns.unit = "km"
                    ns.pluralizeUnit = false
                    ns.unitPosition = NumberStyle.UnitPosition.Rear

                    attr.numberStyle = ns

                    return true
                } else */return false
            }
        }

        val CONFIGURATOR_FOR_HEART_RATE_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttributeDAO): Boolean {
                /*if (attr is OTNumberAttribute) {
                    val ns = NumberStyle()
                    ns.commaUnit = 3
                    ns.fractionPart = 0
                    ns.unit = "bps"
                    ns.pluralizeUnit = false
                    ns.unitPosition = NumberStyle.UnitPosition.Rear
                    attr.numberStyle = ns
                    return true
                } else*/ return false
            }
        }

        val CONFIGURATOR_FOR_TIMESPAN_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttributeDAO): Boolean {
                /*if (attr is OTTimeSpanAttribute) {
                    attr.setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, 1)
                    attr.setPropertyValue(OTTimeSpanAttribute.PROPERTY_TYPE, 0)

                    return true
                } else*/ return false
            }
        }


    }

    interface IExampleAttributeConfigurator {
        fun configureExampleAttribute(attr: OTAttributeDAO): Boolean
    }

    val typeCode: String by lazy {
        "${parentService.identifier}_$factoryTypeName"
    }

    open val supportedConditionerTypes: IntArray = intArrayOf()

    open val requiredPermissions: Array<String> = arrayOf()

    abstract fun isAttachableTo(attribute: OTAttributeDAO): Boolean

    abstract fun getAttributeType(): Int

    abstract val isRangedQueryAvailable: Boolean
    abstract val isDemandingUserInput: Boolean
    abstract val minimumGranularity: OTTimeRangeQuery.Granularity

    abstract fun makeMeasure(): OTMeasure
    abstract fun makeMeasure(reader: JsonReader): OTMeasure
    abstract fun makeMeasure(serialized: String): OTMeasure
    abstract fun serializeMeasure(measure: OTMeasure): String

    open fun getFormattedName(): CharSequence {
        val html = "<b>${context.resources.getString(nameResourceId)}</b> | ${context.resources.getString(parentService.nameResourceId)}"
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    protected abstract val exampleAttributeType: Int
    protected abstract fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator

    abstract class OTMeasure(val factory: OTMeasureFactory) {

        /*** Typename in TypeStringSerializer
         *
         */
        abstract val dataTypeName: String

        val factoryCode: String get() = this.factory.typeCode

        abstract fun getValueRequest(builder: OTItemBuilderWrapperBase, query: OTTimeRangeQuery?): Flowable<Nullable<out Any>>

        protected fun <T : OTExternalService> service(): T {
            @Suppress("UNCHECKED_CAST")
            return factory.parentService as T
        }

    }

    abstract class OTRangeQueriedMeasure(factory: OTMeasureFactory) : OTMeasure(factory) {


        abstract fun getValueRequest(start: Long, end: Long): Flowable<Nullable<out Any>>

        override fun getValueRequest(builder: OTItemBuilderWrapperBase, query: OTTimeRangeQuery?): Flowable<Nullable<out Any>> {
            val range = query!!.getRange(builder)
            return getValueRequest(range.first, range.second)
        }
    }
}