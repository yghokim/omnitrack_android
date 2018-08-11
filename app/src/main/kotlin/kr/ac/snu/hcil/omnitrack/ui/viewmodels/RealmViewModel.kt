package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class RealmViewModel(app: Application) : AndroidViewModel(app) {
    @field:[Inject Backend]
    protected lateinit var realmProvider: Factory<Realm>

    @Inject
    protected lateinit var dbManager: Lazy<BackendDbManager>

    protected val configuredContext: ConfiguredContext = (app as OTApp).currentConfiguredContext

    protected val realm: Realm

    protected val subscriptions = CompositeDisposable()

    fun inject(app: OTAndroidApp) {
        onInject(app.currentConfiguredContext)
    }

    protected open fun onInject(configuredContext: ConfiguredContext) {
        val component = configuredContext.configuredAppComponent
        component.inject(this)
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