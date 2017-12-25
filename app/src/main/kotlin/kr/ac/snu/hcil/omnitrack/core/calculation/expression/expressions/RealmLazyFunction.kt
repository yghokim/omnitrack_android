package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import com.udojava.evalex.Expression
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 11. 14..
 */
abstract class RealmLazyFunction : Expression.LazyFunction {

    @Inject lateinit var dbManager: Lazy<BackendDbManager>
    @field:[Inject Backend] lateinit var realmProvider: Factory<Realm>

    constructor(configuredContext: ConfiguredContext, name: String?, numParams: Int, booleanFunction: Boolean) : super(name, numParams, booleanFunction) {
        configuredContext.configuredAppComponent.inject(this)
    }

    constructor(configuredContext: ConfiguredContext, name: String?, numParams: Int) : super(name, numParams) {
        configuredContext.configuredAppComponent.inject(this)
    }

}