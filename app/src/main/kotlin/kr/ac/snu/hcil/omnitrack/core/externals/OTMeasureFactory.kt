package kr.ac.snu.hcil.omnitrack.core.externals

import kr.ac.snu.hcil.omnitrack.core.OTTimeRangeQuery
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

    abstract val service: OTExternalService

    open val requiredPermissions: Array<String> = arrayOf()

    abstract fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean

    abstract val isRangedQueryAvailable: Boolean

    abstract fun makeMeasure(): OTMeasure
    abstract fun makeMeasure(serialized: String): OTMeasure


    abstract class OTMeasure : ATypedQueueSerializable {

        /*** Typename in TypeStringSerializer
         *
         */
        abstract val dataTypeName: String

        abstract val factoryCode: String
        abstract val factory: OTMeasureFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)


        abstract fun awaitRequestValue(query: OTTimeRangeQuery?): Any
        abstract fun requestValueAsync(query: OTTimeRangeQuery?, handler: ((Any) -> Unit))
    }
}