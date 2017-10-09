package kr.ac.snu.hcil.omnitrack.core.connection

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
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

    fun isValid(invalidMessages: MutableList<CharSequence>?): Boolean {
        val source = source
        if (source != null) {
            val service = source.factory.getService()
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                return true
            } else {
                invalidMessages?.add(TextHelper.fromHtml(String.format(
                        "<font color=\"blue\">${OTApplication.app.resourcesWrapped.getString(R.string.msg_service_is_not_activated_format)}</font>",
                        OTApplication.app.resourcesWrapped.getString(service.nameResourceId))))
                return false
            }
        } else {
            invalidMessages?.add(TextHelper.fromHtml(
                    "<font color=\"blue\">Connection is not supported on current version.</font>"
            ))
            return false
        }
    }

    fun getRequestedValue(builder: OTItemBuilder): Observable<Nullable<out Any>> {
        return Observable.defer {
            if (source != null) {
                return@defer source!!.getValueRequest(builder, rangedQuery)
            } else {
                return@defer Observable.error<Nullable<out Any>>(Exception("Connection source is not designated."))
            }
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
