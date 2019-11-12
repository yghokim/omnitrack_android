package kr.ac.snu.hcil.omnitrack.core.event

import android.content.Context
import android.content.pm.PackageManager
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.OTAttachableFactory
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import java.util.ArrayList

abstract class OTEventFactory(context: Context, factoryTypeName: String)
    : OTAttachableFactory<OTEventFactory.OTAttachableEvent>(context, factoryTypeName) {

    override val typeCode: String = factoryTypeName

    private val _dependencyList = ArrayList<OTSystemDependencyResolver>()
    val dependencyList: List<OTSystemDependencyResolver> get() = _dependencyList

    abstract class OTAttachableEvent(factory: OTEventFactory, arguments: JsonObject?): OTAttachableFactory.OTAttachable(factory, arguments){
        abstract fun unsubscribe(): Boolean
        abstract fun subscribe(): Boolean
    }
}