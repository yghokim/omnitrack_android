package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class RealmViewModel(app: Application) : AndroidViewModel(app) {
    @Inject
    protected lateinit var realm: Realm

    @Inject
    protected lateinit var dbManager: Lazy<RealmDatabaseManager>

    protected val subscriptions = CompositeDisposable()

    fun inject(app: OTApp) {
        onInject(app)
    }

    protected open fun onInject(app: OTApp) {
        val component = app.applicationComponent
        component.inject(this)
    }

    init {
        inject(app as OTApp)
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
        subscriptions.clear()
    }
}