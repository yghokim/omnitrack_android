package kr.ac.snu.hcil.omnitrack.core.system

import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager

class OTMeasureFactoryManager(val serviceManager: OTExternalServiceManager, private val itemMeasureFactoryManager: OTItemDynamicMeasureFactoryManager) {

    fun getAttachableMeasureFactories(field: OTFieldDAO): List<OTMeasureFactory> {
        return serviceManager.getFilteredMeasureFactories { it.getAttributeType() == field.type } + itemMeasureFactoryManager.getAttachableMeasureFactories(field)
    }

    fun getMeasureFactoryByCode(typeCode: String): OTMeasureFactory? {
        return serviceManager.getMeasureFactoryByCode(typeCode)
                ?: itemMeasureFactoryManager.getMeasureFactoryByCode(typeCode)
    }
}