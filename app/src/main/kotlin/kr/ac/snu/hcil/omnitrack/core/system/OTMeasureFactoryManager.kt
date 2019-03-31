package kr.ac.snu.hcil.omnitrack.core.system

import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager

class OTMeasureFactoryManager(val serviceManager: OTExternalServiceManager, private val itemMeasureFactoryManager: OTItemDynamicMeasureFactoryManager) {

    fun getAttachableMeasureFactories(attribute: OTAttributeDAO): List<OTMeasureFactory> {
        return serviceManager.getFilteredMeasureFactories { it.getAttributeType() == attribute.type } + itemMeasureFactoryManager.getAttachableMeasureFactories(attribute)
    }

    fun getMeasureFactoryByCode(typeCode: String): OTMeasureFactory? {
        return serviceManager.getMeasureFactoryByCode(typeCode)
                ?: itemMeasureFactoryManager.getMeasureFactoryByCode(typeCode)
    }
}