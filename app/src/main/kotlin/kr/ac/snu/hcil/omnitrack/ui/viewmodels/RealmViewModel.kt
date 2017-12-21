package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.Backend
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class RealmViewModel(app: Application) : AndroidViewModel(app) {
    @field:[Inject Backend]
    protected lateinit var realmProvider: Factory<Realm>

    @Inject
    protected lateinit var dbManager: Lazy<BackendDbManager>

    protected val realm: Realm

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
        realm = realmProvider.get()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
        subscriptions.clear()
    }
}