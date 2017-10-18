package kr.ac.snu.hcil.omnitrack.utils

import android.arch.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class RealmViewModel : ViewModel() {
    protected open val realm = makeRealmInstance()
    protected val subscriptions = CompositeDisposable()

    protected open fun makeRealmInstance(): Realm {
        return OTApplication.app.databaseManager.getRealmInstance()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
        subscriptions.clear()
    }
}