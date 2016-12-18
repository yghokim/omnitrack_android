package kr.ac.snu.hcil.omnitrack.core.connection

import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import rx.Observable

/**
 * Created by Young-Ho Kim on 2016-08-11.
 */
class OTConnection : ATypedQueueSerializable {

    companion object {
        val NULL = Any()
    }

    var source: OTMeasureFactory.OTMeasure? = null
        get() {
            return field
        }
        set(value) {
            if (field != value) {
                field = value
            }

            if (isRangedQueryAvailable) {
                if (rangedQuery == null) {
                    rangedQuery = OTTimeRangeQuery()
                }
            }
        }

    val isRangedQueryAvailable: Boolean
        get() = if (source != null) {
            source?.factory?.isRangedQueryAvailable ?: false
        } else false


    var rangedQuery: OTTimeRangeQuery? = null


    constructor() : super()
    constructor(serialized: String) : super(serialized)


    fun getRequestedValue(builder: OTItemBuilder): Observable<Any?> {
        if (source != null) {
            return Observable.create {
                subscriber ->
                source!!.requestValueAsync(builder, rangedQuery) {
                    value ->
                    println("connection value retrieval result")
                    println(value)
                    if (!subscriber.isUnsubscribed) {
                        if (value != null) {
                            subscriber.onNext(value)
                        } else {
                            subscriber.onNext(NULL)
                        }
                        subscriber.onCompleted()
                    }
                }
            }
        } else {
            return Observable.just<Any?>(null)
        }
    }

    fun requestValueAsync(builder: OTItemBuilder, handler: (Any?) -> Unit) {
        if (source != null) {
            source!!.requestValueAsync(builder, rangedQuery, handler)
        } else {
            handler.invoke(null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        else if (other is OTConnection) {
            return other.rangedQuery == this.rangedQuery && other.source == this.source
        } else return false
    }

    override fun onSerialize(typedQueue: SerializableTypedQueue) {
        typedQueue.putBoolean(source != null)
        if (source != null) {
            typedQueue.putString(source!!.factoryCode)
            typedQueue.putString(source!!.getSerializedString())
        }

        typedQueue.putBoolean(rangedQuery != null)
        if (rangedQuery != null) {
            rangedQuery?.onSerialize(typedQueue)
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

        if (typedQueue.getBoolean()) {
            rangedQuery = OTTimeRangeQuery()
            rangedQuery?.onDeserialize(typedQueue)
        }
    }

}
