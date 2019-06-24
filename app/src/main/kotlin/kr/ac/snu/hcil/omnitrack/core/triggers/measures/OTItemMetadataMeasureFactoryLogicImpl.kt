package kr.ac.snu.hcil.omnitrack.core.triggers.measures

import android.content.Context
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import javax.inject.Inject

abstract class OTItemMetadataMeasureFactoryLogicImpl(protected val context: Context) {

    @field:[Inject Backend]
    protected lateinit var realmProvider: Factory<Realm>

    @Inject
    protected lateinit var dbManager: Lazy<BackendDbManager>

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    protected abstract fun checkAvailability(tracker: OTTrackerDAO, invalidMessages: MutableList<CharSequence>?): Boolean

    fun isAvailableToRequestValue(field: OTFieldDAO, invalidMessages: MutableList<CharSequence>? = null): Boolean {
        val trackerId = field.trackerId
        if (trackerId != null) {
            val realm = realmProvider.get()
            val tracker = dbManager.get().getTrackerQueryWithId(trackerId, realm).findFirst()
            if (tracker != null) {
                realm.close()
                return checkAvailability(tracker, invalidMessages)
            } else {
                invalidMessages?.add(
                        context.getString(R.string.msg_no_trigger_or_reminders_are_assigned)
                )
                realm.close()
                return false
            }
        } else return true //Attribute is being created. So skip the checking.

    }

}