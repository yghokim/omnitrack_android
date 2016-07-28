package kr.ac.snu.hcil.omnitrack.core.externals

import android.app.Activity
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTMeasureFactory<T>() : INameDescriptionResourceProvider {

    var permissionGranted = false
        protected set

    abstract fun awaitRequestValue(): T
    abstract fun requestValueAsync( handler: ((T)->Unit) )

    open fun grantPermissions(activity: Activity, handler: ((Boolean)->Unit)? = null){

    }
}