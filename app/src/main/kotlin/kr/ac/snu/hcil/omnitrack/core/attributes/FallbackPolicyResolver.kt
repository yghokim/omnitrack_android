package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import androidx.annotation.StringRes
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable

/**
 * Created by younghokim on 2017. 10. 30..
 */
abstract class FallbackPolicyResolver(val context: Context, @StringRes val nameResId: Int, open val isValueVolatile: Boolean = false) {

    abstract fun getFallbackValue(attribute: OTAttributeDAO, realm: Realm): Single<Nullable<out Any>>

    override fun toString(): String {
        return context.resources.getString(nameResId)
    }
}