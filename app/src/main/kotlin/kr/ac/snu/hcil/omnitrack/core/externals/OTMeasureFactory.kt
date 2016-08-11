package kr.ac.snu.hcil.omnitrack.core.externals

import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTMeasureFactory<T>() : INameDescriptionResourceProvider {

    abstract fun awaitRequestValue(): T
    abstract fun requestValueAsync( handler: ((T)->Unit) )

    open val requiredPermissions: Array<String> = arrayOf()
}