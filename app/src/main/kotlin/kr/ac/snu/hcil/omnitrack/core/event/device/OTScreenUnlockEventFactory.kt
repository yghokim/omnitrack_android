package kr.ac.snu.hcil.omnitrack.core.event.device

import android.content.Context
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.event.OTEventFactory

class OTScreenUnlockEventFactory(context: Context): OTEventFactory(context, "screen_unlock"){
    override fun getCategoryName(): String = "Device"

    override fun makeAttachable(arguments: JsonObject?): OTAttachableEvent {
        return OTScreenUnlockEvent(this, arguments)
    }

    override val nameResourceId: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val descResourceId: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.


    class OTScreenUnlockEvent(factory: OTScreenUnlockEventFactory, arguments: JsonObject?): OTAttachableEvent(factory, arguments){
        override fun subscribe(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun unsubscribe(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}