package kr.ac.snu.hcil.omnitrack.utils

import android.arch.lifecycle.ViewModel
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApplication
import rx.subscriptions.CompositeSubscription

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class RealmViewModel : ViewModel() {
    protected open val realm = makeRealmInstance()
    protected val subscriptions = CompositeSubscription()

    protected open fun makeRealmInstance(): Realm {
        return OTApplication.app.databaseManager.getRealmInstance()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
        subscriptions.clear()
    }
}