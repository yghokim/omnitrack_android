package kr.ac.snu.hcil.omnitrack.core.attributes

import android.support.annotation.StringRes
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable

/**
 * Created by younghokim on 2017. 10. 30..
 */
abstract class FallbackPolicyResolver(@StringRes val nameResId: Int, open val isValueVolatile:Boolean = false) {

    abstract fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>>

    override fun toString(): String {
        return OTApp.getString(nameResId)
    }
}