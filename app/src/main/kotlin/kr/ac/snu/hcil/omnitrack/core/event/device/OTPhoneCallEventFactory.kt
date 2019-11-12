package kr.ac.snu.hcil.omnitrack.core.event.device

import android.content.Context
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.event.OTEventFactory

class OTPhoneCallEventFactory(context: Context) : OTEventFactory(context, "phone_call"){

    override fun getCategoryName(): String ="Phone"

    override fun makeAttachable(arguments: JsonObject?): OTAttachableEvent {
        return OTPhoneCallEvent(this, arguments)
    }

    override val nameResourceId: Int = R.string.event_phone_call_name
    override val descResourceId: Int = R.string.event_phone_call_desc

    class OTPhoneCallEvent(factory: OTPhoneCallEventFactory, arguments: JsonObject?): OTAttachableEvent(factory, arguments){

        override fun unsubscribe(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun subscribe(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}