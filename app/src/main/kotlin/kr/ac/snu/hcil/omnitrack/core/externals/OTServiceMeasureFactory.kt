package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
abstract class OTServiceMeasureFactory(context: Context, val parentService: OTExternalService, factoryTypeName: String) : OTMeasureFactory(context, factoryTypeName) {

    companion object {

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

    override fun getCategoryName(): String {
        return context.resources.getString(parentService.nameResourceId)
    }

    override val typeCode: String by lazy {
        "${parentService.identifier}_$factoryTypeName"
    }

    fun <T : OTExternalService> getService(): T {
        @Suppress("UNCHECKED_CAST")
        return parentService as T
    }

    override fun onMakeFormattedName(): String {
        return "${super.onMakeFormattedName()} | ${context.resources.getString(parentService.nameResourceId)}"
    }
}