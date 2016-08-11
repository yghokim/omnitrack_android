package kr.ac.snu.hcil.omnitrack.core.externals

import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTMeasureFactory() : INameDescriptionResourceProvider {

    val typeCode: String by lazy {
        this.javaClass.name
    }

    open val requiredPermissions: Array<String> = arrayOf()

    abstract fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean

    abstract val isRangedQueryAvailable: Boolean


    abstract class OTMeasure : ATypedQueueSerializable() {

        abstract val dataTypeName: Int
        abstract val factoryCode: String
        abstract val factory: OTMeasureFactory


        abstract fun awaitRequestValue(): Any
        abstract fun requestValueAsync(handler: ((Any) -> Unit))

    }
}