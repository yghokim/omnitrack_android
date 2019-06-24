package kr.ac.snu.hcil.omnitrack.core.fields

import android.content.Context
import androidx.annotation.StringRes
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by younghokim on 2017. 10. 30..
 */
abstract class FallbackPolicyResolver(val context: Context, @StringRes val nameResId: Int, open val isValueVolatile: Boolean = false) {

    abstract fun getFallbackValue(field: OTFieldDAO, realm: Realm): Single<Nullable<out Any>>

    override fun toString(): String {
        return context.resources.getString(nameResId)
    }
}