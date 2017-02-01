package kr.ac.snu.hcil.omnitrack.core.externals

import android.text.Html
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeSpanAttribute
import kr.ac.snu.hcil.omnitrack.core.calculation.AConditioner
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable
import rx.Observable

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
abstract class OTMeasureFactory(val factoryTypeName: String) : INameDescriptionResourceProvider {

    companion object {
        val CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE = intArrayOf(AConditioner.TYPECODE_SINGLE_NUMERIC_COMPARISON)
        val CONDITIONERS_FOR_TIMEPOINT_VALUE = intArrayOf()

        val CONFIGURATOR_STEP_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttribute<out Any>): Boolean {
                if (attr is OTNumberAttribute) {
                    val ns = NumberStyle()
                    ns.commaUnit = 3
                    ns.fractionPart = 0
                    ns.unit = "Step"
                    ns.pluralizeUnit = true
                    ns.unitPosition = NumberStyle.UnitPosition.Rear

                    attr.numberStyle = ns

                    return true
                } else return false
            }
        }

        val CONFIGURATOR_DISTANCE_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttribute<out Any>): Boolean {
                if (attr is OTNumberAttribute) {
                    val ns = NumberStyle()
                    ns.commaUnit = 3
                    ns.fractionPart = 2
                    ns.unit = "km"
                    ns.pluralizeUnit = false
                    ns.unitPosition = NumberStyle.UnitPosition.Rear

                    attr.numberStyle = ns

                    return true
                } else return false
            }
        }

        val CONFIGURATOR_FOR_HEART_RATE_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttribute<out Any>): Boolean {
                if (attr is OTNumberAttribute) {
                    val ns = NumberStyle()
                    ns.commaUnit = 3
                    ns.fractionPart = 0
                    ns.unit = "bps"
                    ns.pluralizeUnit = false
                    ns.unitPosition = NumberStyle.UnitPosition.Rear
                    attr.numberStyle = ns
                    return true
                } else return false
            }
        }

        val CONFIGURATOR_FOR_TIMESPAN_ATTRIBUTE = object : IExampleAttributeConfigurator {
            override fun configureExampleAttribute(attr: OTAttribute<out Any>): Boolean {
                if (attr is OTTimeSpanAttribute) {
                    attr.setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, 1)
                    attr.setPropertyValue(OTTimeSpanAttribute.PROPERTY_TYPE, 0)

                    return true
                } else return false
            }
        }


    }

    interface IExampleAttributeConfigurator {
        fun configureExampleAttribute(attr: OTAttribute<out Any>): Boolean
    }

    val typeCode: String by lazy {
        "${service.identifier}_${factoryTypeName}"
    }

    abstract val service: OTExternalService

    open val supportedConditionerTypes: IntArray = intArrayOf()

    open val requiredPermissions: Array<String> = arrayOf()

    abstract fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean

    abstract val isRangedQueryAvailable: Boolean
    abstract val isDemandingUserInput: Boolean
    abstract val minimumGranularity: OTTimeRangeQuery.Granularity

    abstract fun makeMeasure(): OTMeasure
    abstract fun makeMeasure(serialized: String): OTMeasure

    open fun getFormattedName(): CharSequence {
        val html = "<b>${OTApplication.app.resources.getString(nameResourceId)}</b> | ${OTApplication.app.getString(service.nameResourceId)}"
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    protected abstract val exampleAttributeType: Int
    protected abstract fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator


    open fun makeNewExampleAttribute(tracker: OTTracker): OTAttribute<out Any> {

        val attr = OTAttribute.Companion.createAttribute(tracker,
                "${OTApplication.app.getString(service.nameResourceId)} ${OTApplication.app.resources.getString(nameResourceId)}",
                exampleAttributeType)

        getExampleAttributeConfigurator().configureExampleAttribute(attr)

        val connection = OTConnection()
        connection.source = makeMeasure()
        if (isRangedQueryAvailable) {
            connection.rangedQuery = OTTimeRangeQuery.Preset.PresentDate.makeQueryInstance()
        }
        attr.valueConnection = connection

        return attr
    }

    abstract class OTMeasure : ATypedQueueSerializable {

        /*** Typename in TypeStringSerializer
         *
         */
        abstract val dataTypeName: String

        val factoryCode: String get() = factory.typeCode
        abstract val factory: OTMeasureFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        abstract fun getValueRequest(builder: OTItemBuilder, query: OTTimeRangeQuery?): Observable<Result<out Any>>
    }

    abstract class OTRangeQueriedMeasure: OTMeasure{

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        abstract fun getValueRequest(start: Long, end: Long): Observable<Result<out Any>>

        override fun getValueRequest(builder: OTItemBuilder, query: OTTimeRangeQuery?): Observable<Result<out Any>> {
            val range = query!!.getRange(builder)
            return getValueRequest(range.first, range.second)
        }
    }
}