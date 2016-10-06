package kr.ac.snu.hcil.omnitrack.core.externals

import android.text.Html
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.core.attributes.OTTimeSpanAttribute
import kr.ac.snu.hcil.omnitrack.core.calculation.AConditioner
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTMeasureFactory() : INameDescriptionResourceProvider {

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
        this.javaClass.name
    }

    abstract val service: OTExternalService

    open val supportedConditionerTypes: IntArray = intArrayOf()

    open val requiredPermissions: Array<String> = arrayOf()

    abstract fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean

    abstract val isRangedQueryAvailable: Boolean
    abstract val isDemandingUserInput: Boolean


    abstract fun makeMeasure(): OTMeasure
    abstract fun makeMeasure(serialized: String): OTMeasure

    open fun getFormattedName(): CharSequence {
        val html = "<b>${OTApplication.app.resources.getString(nameResourceId)}</b> | ${OTApplication.app.getString(service.nameResourceId)}"
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    protected abstract val exampleAttributeType: Int
    protected abstract fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator


    open fun makeNewExampleAttribute(): OTAttribute<out Any> {
        val attr = OTAttribute.Companion.createAttribute(OTApplication.app.currentUser,
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


        abstract fun awaitRequestValue(query: OTTimeRangeQuery?): Any
        abstract fun requestValueAsync(builder: OTItemBuilder, query: OTTimeRangeQuery?, handler: (Any?) -> Unit)
        abstract fun requestLatestValueAsync(handler: (Any?) -> Unit)
    }

    abstract class OTRangeQueriedMeasure: OTMeasure{
        constructor() : super()
        constructor(serialized: String) : super(serialized)

        abstract fun requestValueAsync(start: Long, end: Long, handler: (Any?) -> Unit)
        override final fun requestValueAsync(builder: OTItemBuilder, query: OTTimeRangeQuery?, handler: (Any?) -> Unit) {

            val range = query!!.getRange(builder)
            requestValueAsync(range.first, range.second, handler)
        }

        override final fun requestLatestValueAsync(handler: (Any?) -> Unit) {
            val range = TimeHelper.getTodayRange()
            requestValueAsync(range.first, range.second, handler)
        }
    }
}