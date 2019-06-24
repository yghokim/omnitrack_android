package kr.ac.snu.hcil.omnitrack.core.system

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.measures.OTDataDrivenConditionMetTimeMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.measures.OTDataDrivenConditionMetValueMeasureFactory

class OTItemDynamicMeasureFactoryManager(val context: Context) {


    private val supportedMeasureFactories: List<OTMeasureFactory> by lazy {
        listOf(
                OTDataDrivenConditionMetTimeMeasureFactory(context),
                OTDataDrivenConditionMetValueMeasureFactory(context)
        )
    }

    fun getAttachableMeasureFactories(field: OTFieldDAO): List<OTMeasureFactory> {
        return supportedMeasureFactories.filter {
            it.getAttributeType() == field.type && it.isAvailableToRequestValue(field, null)
        }
    }

    fun getMeasureFactoryByCode(typeCode: String): OTMeasureFactory? {
        return supportedMeasureFactories.find { it.typeCode == typeCode }
    }
}