package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
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

    fun inject(app: OTAndroidApp) {
        onInject(app)
    }

    protected open fun onInject(app: OTAndroidApp) {
        app.applicationComponent.inject(this)
    }

    init {
        inject(app as OTAndroidApp)
        realm = realmProvider.get()
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
        subscriptions.clear()
    }
}