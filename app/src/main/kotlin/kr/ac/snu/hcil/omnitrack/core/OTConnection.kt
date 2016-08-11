package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTConnection : ATypedQueueSerializable {

    companion object {

    }

    var source: OTMeasureFactory.OTMeasure? = null
        get() {
            return field
        }
        set(value) {
            if (field != value) {
                field = value
            }
        }

    val isTimeQueryAvailable: Boolean
        get() = if (source != null) {
            source?.factory?.isRangedQueryAvailable ?: false
        } else false


    constructor() : super()
    constructor(serialized: String) : super(serialized)


    override fun onSerialize(typedQueue: SerializableTypedQueue) {
        typedQueue.putBoolean(source != null)
        if (source != null) {
            typedQueue.putString(source!!.factoryCode)
            typedQueue.putString(source!!.getSerializedString())
        }
    }

    override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        if (typedQueue.getBoolean()) {
            val factoryCode = typedQueue.getString()
            val factory = OTExternalService.getMeasureFactoryByCode(typeCode = factoryCode)
            if (factory == null) {
                println("$factoryCode is deprecated in System.")

            } else {
                source = factory.makeMeasure(typedQueue.getString())
            }
        }
    }

}
